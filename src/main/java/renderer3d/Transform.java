package renderer3d;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class Transform {

	public static ImagePlus transform(final ImagePlus in, final float[] inv,
			final int w, final int h, final int d) {
		final ImageProcessor[] inProcessors = new ImageProcessor[in
				.getStackSize()];
		for (int z = 0; z < inProcessors.length; z++)
			inProcessors[z] = in.getStack().getProcessor(z + 1);

		System.out.println("d = " + d);

		final int wIn = in.getWidth();
		final int hIn = in.getHeight();
		final int dIn = in.getNSlices();
		final int nC = in.getNChannels();
		// final int nT = in.getNFrames();

		final ExecutorService exec = Executors.newFixedThreadPool(Runtime
				.getRuntime().availableProcessors());

		ImageStack outStack = new ImageStack(w, h);
		for (int z = 0; z < d * nC; z++)
			outStack.addSlice(inProcessors[0].createProcessor(w, h));
		ImagePlus ret = new ImagePlus(in.getTitle() + "-transformed", outStack);
		ret.setDimensions(nC, d, 1);

		final boolean isColor = in.getType() == ImagePlus.COLOR_RGB;

		for(int ic = 0; ic < nC; ic++) {
			for (int iz = 0; iz < d; iz++) {
				final int z = iz;
				final int c = ic;
				final int t = in.getFrame() - 1;
				final int stackIndex = ret.getStackIndex(c + 1, iz + 1, t + 1);
				final ImageProcessor ip = outStack.getProcessor(stackIndex);
				exec.submit(new Runnable() {
					@Override
					public void run() {
						try {
							float[] result = new float[3];
							for (int y = 0, xy = 0; y < h; y++) {
								for (int x = 0; x < w; x++, xy++) {
									apply(inv, x, y, z, result);
									if (result[0] < 0 || result[1] < 0
											|| result[2] < 0
											|| result[0] >= wIn - 1
											|| result[1] >= hIn - 1
											|| result[2] >= dIn - 1) {
										ip.setf(xy, 0);
										continue;
									}

									float ret = isColor ?
											interpolateRGB(in, c, t, inProcessors, result) :
											interpolateGray(in, c, t, inProcessors, result);
									ip.setf(xy, ret);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
		exec.shutdown();
		try {
			exec.awaitTermination(1, TimeUnit.DAYS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(in.isComposite()) {
			System.out.println("isComposite");
			CompositeImage comp = new CompositeImage(ret);
			comp.setLuts(((CompositeImage)in).getLuts());
			ret = comp;
		}
		return ret;
	}

	public static float interpolateGray(ImagePlus imp, int channel, int frame, ImageProcessor[] ips, float[] pos) {

		if (pos[0] < 0 || pos[1] < 0 || pos[2] < 0
				|| pos[0] >= imp.getWidth() - 1
				|| pos[1] >= imp.getHeight() - 1
				|| pos[2] >= imp.getNSlices() - 1) {
			return 0;
		}

		int lx = (int) pos[0];
		int ly = (int) pos[1];
		int lz = (int) pos[2];
		float xR = 1 + lx - pos[0];
		float yR = 1 + ly - pos[1];
		float zR = 1 + lz - pos[2];

		ImageProcessor ip0 = ips[imp.getStackIndex(channel + 1, lz + 1, frame + 1) - 1];
		ImageProcessor ip1 = ips[imp.getStackIndex(channel + 1, lz + 2, frame + 1) - 1];

		int ux = lx + 1, uy = ly + 1;

		float v000 = ip0.getf(lx, ly);
		float v001 = ip1.getf(lx, ly);
		float v010 = ip0.getf(lx, uy);
		float v011 = ip1.getf(lx, uy);
		float v100 = ip0.getf(ux, ly);
		float v101 = ip1.getf(ux, ly);
		float v110 = ip0.getf(ux, uy);
		float v111 = ip1.getf(ux, uy);

		return xR
				* (yR * (zR * v000 + (1 - zR) * v001) + (1 - yR)
						* (zR * v010 + (1 - zR) * v011))
				+ (1 - xR)
				* (yR * (zR * v100 + (1 - zR) * v101) + (1 - yR)
						* (zR * v110 + (1 - zR) * v111));
	}

	private static float interpolateRGB(ImagePlus imp, int channel, int frame, ImageProcessor[] ips, float[] pos) {
		int lx = (int) pos[0];
		int ly = (int) pos[1];
		int lz = (int) pos[2];
		float xR = 1 + lx - pos[0];
		float yR = 1 + ly - pos[1];
		float zR = 1 + lz - pos[2];

		ImageProcessor ip0 = ips[imp.getStackIndex(channel + 1, lz + 1, frame + 1) - 1];
		ImageProcessor ip1 = ips[imp.getStackIndex(channel + 1, lz + 2, frame + 1) - 1];

		int ux = lx + 1, uy = ly + 1;

		int v000 = ip0.get(lx, ly);
		int v001 = ip1.get(lx, ly);
		int v010 = ip0.get(lx, uy);
		int v011 = ip1.get(lx, uy);
		int v100 = ip0.get(ux, ly);
		int v101 = ip1.get(ux, ly);
		int v110 = ip0.get(ux, uy);
		int v111 = ip1.get(ux, uy);

		int i000 = (v000 & 0xff0000) >> 16;
		int i001 = (v001 & 0xff0000) >> 16;
		int i010 = (v010 & 0xff0000) >> 16;
		int i011 = (v011 & 0xff0000) >> 16;
		int i100 = (v100 & 0xff0000) >> 16;
		int i101 = (v101 & 0xff0000) >> 16;
		int i110 = (v110 & 0xff0000) >> 16;
		int i111 = (v111 & 0xff0000) >> 16;

		int red =  Math.round(xR
				* (yR * (zR * i000 + (1 - zR) * i001) + (1 - yR)
						* (zR * i010 + (1 - zR) * i011))
				+ (1 - xR)
				* (yR * (zR * i100 + (1 - zR) * i101) + (1 - yR)
						* (zR * i110 + (1 - zR) * i111)));

		i000 = (v000 & 0xff00) >> 8;
		i001 = (v001 & 0xff00) >> 8;
		i010 = (v010 & 0xff00) >> 8;
		i011 = (v011 & 0xff00) >> 8;
		i100 = (v100 & 0xff00) >> 8;
		i101 = (v101 & 0xff00) >> 8;
		i110 = (v110 & 0xff00) >> 8;
		i111 = (v111 & 0xff00) >> 8;

		int green =  Math.round(xR
				* (yR * (zR * i000 + (1 - zR) * i001) + (1 - yR)
						* (zR * i010 + (1 - zR) * i011))
				+ (1 - xR)
				* (yR * (zR * i100 + (1 - zR) * i101) + (1 - yR)
						* (zR * i110 + (1 - zR) * i111)));

		i000 = (v000 & 0xff);
		i001 = (v001 & 0xff);
		i010 = (v010 & 0xff);
		i011 = (v011 & 0xff);
		i100 = (v100 & 0xff);
		i101 = (v101 & 0xff);
		i110 = (v110 & 0xff);
		i111 = (v111 & 0xff);

		int blue =  Math.round(xR
				* (yR * (zR * i000 + (1 - zR) * i001) + (1 - yR)
						* (zR * i010 + (1 - zR) * i011))
				+ (1 - xR)
				* (yR * (zR * i100 + (1 - zR) * i101) + (1 - yR)
						* (zR * i110 + (1 - zR) * i111)));

		return (red << 16) + (green << 8) + blue;
	}

	public static boolean intersect(double pw, double ph, double pd, float[] r0, double[] rd, double[] nearfar) {
		double[] bb0 = new double[] {0, 0, 0};
		double[] bb1 = new double[] {pw - 1, ph - 1, pd - 1};
		return intersect(bb0, bb1, r0, rd, nearfar);
	}

	/*
	 * https://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm
	 *
	 * nearfar: distances from r0 in multiples of rd.
	 */
	private static boolean intersect(double[] bb0, double[] bb1, float[] r0, double[] rd, double[] nearfar) {
		double Tnear = Double.NEGATIVE_INFINITY;
		double Tfar = Double.POSITIVE_INFINITY;
		for(int dir = 0; dir < 3; dir++) {
			boolean parallel = Math.abs(rd[dir]) < 10e-5;
			if(parallel) {
				if(r0[dir] < bb0[dir] || r0[dir] > bb1[dir])
					return false;
				else
					continue;
			}
			double T1 = (bb0[dir] - r0[dir]) / rd[dir];
			double T2 = (bb1[dir] - r0[dir]) / rd[dir];
			if(T1 > T2) {
				double tmp = T1;
				T1 = T2;
				T2 = tmp;
			}
			if(T1 > Tnear)
				Tnear = T1;
			if(T2 < Tfar)
				Tfar = T2;
			if(Tnear > Tfar)
				return false;
			if(Tfar < 0)
				return false;
		}
		nearfar[0] = Tnear;
		nearfar[1] = Tfar;
		return true;
	}

	private static void invert3x3(float[] mat) {
		double sub00 = mat[5] * mat[10] - mat[6] * mat[9];
		double sub01 = mat[4] * mat[10] - mat[6] * mat[8];
		double sub02 = mat[4] * mat[9] - mat[5] * mat[8];
		double sub10 = mat[1] * mat[10] - mat[2] * mat[9];
		double sub11 = mat[0] * mat[10] - mat[2] * mat[8];
		double sub12 = mat[0] * mat[9] - mat[1] * mat[8];
		double sub20 = mat[1] * mat[6] - mat[2] * mat[5];
		double sub21 = mat[0] * mat[6] - mat[2] * mat[4];
		double sub22 = mat[0] * mat[5] - mat[1] * mat[4];
		double det = mat[0] * sub00 - mat[1] * sub01 + mat[2] * sub02;

		mat[0] = (float) (sub00 / det);
		mat[1] = -(float) (sub10 / det);
		mat[2] = (float) (sub20 / det);
		mat[4] = -(float) (sub01 / det);
		mat[5] = (float) (sub11 / det);
		mat[6] = -(float) (sub21 / det);
		mat[8] = (float) (sub02 / det);
		mat[9] = -(float) (sub12 / det);
		mat[10] = (float) (sub22 / det);
	}

	public static float[] fromAngleAxis(float[] axis, float rad, float[] matrix) {
		if(matrix == null)
			matrix = new float[12];

		double c = Math.cos(rad);
		double s = Math.sin(rad);
		double ux = axis[0];
		double uy = axis[1];
		double uz = axis[2];

		matrix[0]  = (float)(c + ux * ux * (1 - c));
		matrix[5]  = (float)(c + uy * uy * (1 - c));
		matrix[10] = (float)(c + uz * uz * (1 - c));

		matrix[1]  = (float)(ux * uy * (1 - c) - uz * s);
		matrix[2]  = (float)(ux * uz * (1 - c) + uy * s);
		matrix[4]  = (float)(uy * ux * (1 - c) + uz * s);
		matrix[6]  = (float)(uy * uz * (1 - c) - ux * s);
		matrix[8]  = (float)(uz * ux * (1 - c) - uy * s);
		matrix[9]  = (float)(uz * uy * (1 - c) + ux * s);

		matrix[3]  = 0;
		matrix[7]  = 0;
		matrix[11] = 0;

		return matrix;
	}

	public static float[] fromScale(float sx, float sy, float sz, float[] matrix) {
		if(matrix == null)
			matrix = new float[12];
		matrix[0] = sx;
		matrix[5] = sy;
		matrix[10] = sz;
		matrix[1] = matrix[2] = matrix[3] = matrix[4] = matrix[6] = matrix[7] = matrix[8] = matrix[9] = matrix[11] = 0;
		return matrix;
	}

	public static float[] fromScale(float scale, float[] matrix) {
		return fromScale(scale, scale, scale, matrix);
	}

	/**
	 * scale * matrix.
	 * @param sx
	 * @param sy
	 * @param sz
	 * @param matrix
	 * @return
	 */
	public static float[] applyScale(float sx, float sy, float sz, float[] matrix) {
		matrix[0] *= sx;
		matrix[1] *= sx;
		matrix[2] *= sx;
		matrix[3] *= sx;
		matrix[4] *= sy;
		matrix[5] *= sy;
		matrix[6] *= sy;
		matrix[7] *= sy;
		matrix[8] *= sz;
		matrix[9] *= sz;
		matrix[10] *= sz;
		matrix[11] *= sz;
		return matrix;
	}

	/**
	 * matrix * scale
	 * @param sx
	 * @param sy
	 * @param sz
	 * @param matrix
	 * @return
	 */
	public static float[] applyScale(float[] matrix, float sx, float sy, float sz) {
		matrix[0] *= sx;
		matrix[1] *= sy;
		matrix[2] *= sz;
		matrix[4] *= sx;
		matrix[5] *= sy;
		matrix[6] *= sz;
		matrix[8] *= sx;
		matrix[9] *= sy;
		matrix[10] *= sz;
		return matrix;
	}

	public static float[] applyScale(float s, float[] matrix) {
		return applyScale(s, s, s, matrix);
	}

	public static float[] applyScale(float[] matrix, float s) {
		return applyScale(matrix, s, s, s);
	}

	public static float[] fromCalibration(float pw, float ph, float pd, float xo, float yo, float zo, float[] matrix) {
		float[] m = fromScale(pw, ph, pd, matrix);
		m[3] = xo;
		m[7] = yo;
		m[8] = zo;
		return m;
	}

	public static float[] fromTranslation(float dx, float dy, float dz, float[] matrix) {
		if(matrix == null)
			matrix = new float[12];
		matrix[1] = matrix[2] = matrix[4] = matrix[6] = matrix[8] = matrix[9] = 0;
		matrix[0] = matrix[5] = matrix[10] = 1;
		matrix[3] = dx;
		matrix[7] = dy;
		matrix[11] = dz;
		return matrix;
	}

	public static float[] applyTranslation(float dx, float dy, float dz, float[] matrix) {
		matrix[3] += dx;
		matrix[7] += dy;
		matrix[11] += dz;
		return matrix;
	}

	public static float[] applyTranslation(float[] matrix, float dx, float dy, float dz) {
		matrix[3]  += matrix[0] * dx + matrix[1] * dy + matrix[2] * dz;
		matrix[7]  += matrix[4] * dx + matrix[5] * dy + matrix[6] * dz;
		matrix[11] += matrix[8] * dx + matrix[9] * dy + matrix[10] * dz;
		return matrix;
	}

	public static float[] fromIdentity(float[] matrix) {
		return fromScale(1, matrix);
	}

	public static void invert(float[] mat) {
		float dx = -mat[3];
		float dy = -mat[7];
		float dz = -mat[11];
		invert3x3(mat);

		mat[3] = mat[0] * dx + mat[1] * dy + mat[2] * dz;
		mat[7] = mat[4] * dx + mat[5] * dy + mat[6] * dz;
		mat[11] = mat[8] * dx + mat[9] * dy + mat[10] * dz;
	}

	public static float[] apply(float[] mat, float x, float y, float z,
			float[] result) {
		if(result == null)
			result = new float[3];
		result[0] = mat[0] * x + mat[1] * y + mat[2] * z + mat[3];
		result[1] = mat[4] * x + mat[5] * y + mat[6] * z + mat[7];
		result[2] = mat[8] * x + mat[9] * y + mat[10] * z + mat[11];
		return result;
	}

	public static float[] mul(float[] m1, float[] m2) {
		float[] res = new float[12];
		res[0] = m1[0] * m2[0] + m1[1] * m2[4] + m1[2] * m2[8];
		res[1] = m1[0] * m2[1] + m1[1] * m2[5] + m1[2] * m2[9];
		res[2] = m1[0] * m2[2] + m1[1] * m2[6] + m1[2] * m2[10];
		res[3] = m1[0] * m2[3] + m1[1] * m2[7] + m1[2] * m2[11] + m1[3];

		res[4] = m1[4] * m2[0] + m1[5] * m2[4] + m1[6] * m2[8];
		res[5] = m1[4] * m2[1] + m1[5] * m2[5] + m1[6] * m2[9];
		res[6] = m1[4] * m2[2] + m1[5] * m2[6] + m1[6] * m2[10];
		res[7] = m1[4] * m2[3] + m1[5] * m2[7] + m1[6] * m2[11] + m1[7];

		res[8] = m1[8] * m2[0] + m1[9] * m2[4] + m1[10] * m2[8];
		res[9] = m1[8] * m2[1] + m1[9] * m2[5] + m1[10] * m2[9];
		res[10] = m1[8] * m2[2] + m1[9] * m2[6] + m1[10] * m2[10];
		res[11] = m1[8] * m2[3] + m1[9] * m2[7] + m1[10] * m2[11] + m1[11];

		return res;
	}

	@SuppressWarnings("unused")
	private static final int a00 = 0, a01 = 1, a02 = 2, a03 = 3,
			a10 = 4, a11 = 5, a12 = 6, a13 = 7,
			a20 = 8, a21 = 9, a22 = 10, a23 = 11;

	/**
	 * this conversion uses conventions as described on page:
	 * http://www.euclideanspace.com/maths/geometry/rotations/euler/index.htm
	 * Coordinate System: right hand
	 * Positive angle: right hand
	 * Order of euler angles: heading first, then attitude, then bank
	 * matrix row column ordering:
	 *
	 * parameters[0] - heading
	 * parameters[1] - attitude
	 * parameters[2] - bank
	 */
	public static final void guessEulerAngles(float[]  m, double[] parameters) {
	    // Assuming the angles are in radians.
		if (m[a10] > 0.998) { // singularity at north pole
			parameters[0] = Math.atan2(m[a02], m[a22]);
			parameters[1] = Math.PI/2;
			parameters[2] = 0;
			return;
		}
		if (m[a10] < -0.998) { // singularity at south pole
			parameters[0] = Math.atan2(m[a02], m[a22]);
			parameters[1] = -Math.PI/2;
			parameters[2] = 0;
			return;
		}
		parameters[0] = Math.atan2(-m[a20], m[a00]);
		parameters[2] = Math.atan2(-m[a12], m[a11]);
		parameters[1] = Math.asin(m[a10]);
	}

	public static final void guessEulerAngles(float[]  m, float[] parameters) {
	    // Assuming the angles are in radians.
		if (m[a10] > 0.998) { // singularity at north pole
			parameters[0] = (float)Math.atan2(m[a02], m[a22]);
			parameters[1] = (float)(Math.PI/2);
			parameters[2] = 0;
			return;
		}
		if (m[a10] < -0.998) { // singularity at south pole
			parameters[0] = (float)Math.atan2(m[a02], m[a22]);
			parameters[1] = (float)(-Math.PI/2);
			parameters[2] = 0;
			return;
		}
		parameters[0] = (float)Math.atan2(-m[a20], m[a00]);
		parameters[2] = (float)Math.atan2(-m[a12], m[a11]);
		parameters[1] = (float)Math.asin(m[a10]);
	}

	/** this conversion uses NASA standard aeroplane conventions as described on page:
	 *   http://www.euclideanspace.com/maths/geometry/rotations/euler/index.htm
	 *   Coordinate System: right hand
	 *   Positive angle: right hand
	 *   Order of euler angles: heading first, then attitude, then bank
	 *   matrix row column ordering:
	 *   [m00 m01 m02]
	 *   [m10 m11 m12]
	 *   [m20 m21 m22]
	 */
	public static final void fromEulerAngles(float[] m, double[] parameters) {
	    // Assuming the angles are in radians.
	    double ch = Math.cos(parameters[0]);
	    double sh = Math.sin(parameters[0]);
	    double ca = Math.cos(parameters[1]);
	    double sa = Math.sin(parameters[1]);
	    double cb = Math.cos(parameters[2]);
	    double sb = Math.sin(parameters[2]);

	    m[a00] = (float)(ch * ca);
	    m[a01] = (float)(sh * sb - ch * sa * cb);
	    m[a02] = (float)(ch * sa * sb + sh * cb);
	    m[a10] = (float)(sa);
	    m[a11] = (float)(ca * cb);
	    m[a12] = (float)(-ca * sb);
	    m[a20] = (float)(-sh * ca);
	    m[a21] = (float)(sh * sa * cb + ch * sb);
	    m[a22] = (float)(-sh * sa * sb + ch * cb);
	}
}
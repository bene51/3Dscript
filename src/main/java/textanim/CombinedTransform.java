package textanim;

import ij.measure.Calibration;
import renderer3d.Transform;

public class CombinedTransform {

	private float[] pdIn;
	private float[] pdOut;
	private float[] fromCalib;
	private float[] toTransform;

	private float[] translation;
	private float[] rotation;
	private float   scale;

	private float[] center;

	public CombinedTransform(float[] pdIn, float[] pdOut, float[] center) {
		this.pdIn   = pdIn;
		this.pdOut  = pdOut;
		this.center = center;

		fromCalib   = Transform.fromCalibration( pdIn[0],  pdIn[1],  pdIn[2], 0, 0, 0, null);
		toTransform = Transform.fromCalibration(pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
		Transform.invert(toTransform);

		translation = new float[3];
		rotation    = Transform.fromIdentity(null);
		scale       = 1;
	}

	public void setOutputSpacing(float[] pdOut) {
		System.arraycopy(pdOut, 0, this.pdOut, 0, 3);
		toTransform = Transform.fromCalibration(pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
		Transform.invert(toTransform);
	}

	public void setOutputSpacingZ(float pdOutZ) {
		pdOut[2] = pdOutZ;
		toTransform = Transform.fromCalibration(pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
		Transform.invert(toTransform); // TODO invert can be faster because it's a diagonal matrix
	}

	public void setZStep(float zStep) {
		setOutputSpacingZ(pdIn[2] * zStep);
	}

	public float[] getOutputSpacing() {
		return pdOut;
	}

	public float[] getInputSpacing() {
		return pdIn;
	}

	@Override
	public CombinedTransform clone() {
		float[] pdIn   = new float[3];
		float[] pdOut  = new float[3];
		float[] center = new float[3];
		System.arraycopy(this.pdIn,   0, pdIn,   0, 3);
		System.arraycopy(this.pdOut,  0, pdOut,  0, 3);
		System.arraycopy(this.center, 0, center, 0, 3);
		CombinedTransform ct = new CombinedTransform(pdIn, pdOut, center);
		System.arraycopy(translation, 0, ct.translation, 0, 3);
		System.arraycopy(rotation,    0, ct.rotation,    0, 12);
		ct.scale = scale;
		return ct;
	}

	public float[] guessEulerAnglesDegree() {
		float[] eulerAngles = new float[3];
		Transform.guessEulerAngles(rotation, eulerAngles);
		eulerAngles[0] = eulerAngles[0] * 180 / (float)Math.PI;
		eulerAngles[1] = eulerAngles[1] * 180 / (float)Math.PI;
		eulerAngles[2] = eulerAngles[2] * 180 / (float)Math.PI;
		return eulerAngles;
	}

	public float[] getRotation() {
		return rotation;
	}

	public float[] getTranslation() {
		return translation;
	}

	public float getScale() {
		return scale;
	}

	public void translateBy(float dx, float dy, float dz, boolean inOutputPixels) {
		if(inOutputPixels) {
			dx *= pdOut[0];
			dy *= pdOut[1];
			dz *= pdOut[2];
		}
		translation[0] += dx;
		translation[1] += dy;
		translation[2] += dz;
	}

	/**
	 * in degrees.
	 * @param ax
	 * @param ay
	 */
	public void rotateBy(float ax, float ay) {
		float[] rx = Transform.fromAngleAxis(new float[] {0, 1, 0}, ax * (float)Math.PI / 180f, null);
		float[] ry = Transform.fromAngleAxis(new float[] {1, 0, 0}, ay * (float)Math.PI / 180f, null);
		float[] r = Transform.mul(rx, ry);
		float[] rot = Transform.mul(r, rotation);

		System.arraycopy(rot, 0, rotation, 0, 12);
	}

	public void zoomInto(int ex, int ey, float factor) {

		float[] transform = calculateForwardTransform();

		// calculate the current output pixel coordinate of the rotation center
		// transform is the transformation that gets pixel coordinates as input
		// and transforms to pixel output
		float[] c = Transform.apply(transform, center[0] / pdIn[0], center[1] / pdIn[1], center[2] / pdIn[2], null);

		// dx and dy are the x- and y-distances of the mouse point to the rotation center
		// imagine a output size of 10x10, a rotation center at (5,5), the mouse at (1,1)
		// and a scale factor of 0.5
		// then dx = (4, 4)
		float dx = c[0] - ex;
		float dy = c[1] - ey;

		// calculate where the transformed (scaled) mouse point appears (using rotcenter as scaling
		// center)
		// in the example: p = (5,5) - 0.5*(4,4) = (3,3)
		float px = c[0] - factor * dx;
		float py = c[1] - factor * dy;

		// the transformed mouse point is at (px, py) (3,3), but should be at the original (untransformed)
		// mouse position (1, 1), therefore, we need to shift the image back
		translation[0] += (ex - px) * pdOut[0];
		translation[1] += (ey - py) * pdOut[1];

		scale *= factor;


		// TODO
//		Calibration cal = out.getCalibration();
//		cal.pixelWidth = pdOut[0] / scale[0];
//		cal.pixelHeight = pdOut[1] / scale[0];
	}

	public void setTransformation(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		Transform.fromEulerAngles(rotation, new double[] {
				Math.PI * ax / 180,
				Math.PI * ay / 180,
				Math.PI * az / 180});

		scale = s;

		translation[0] = dx;
		translation[1] = dy;
		translation[2] = dz;
	}

	/**
	 * <code>fwdMatrix</code> is a matrix from real-world coordinates to real-world
	 * coordinates, i.e. it will still be pre-multiplied with <code>toTransform</code>
	 * and post-multiplied with <code>fromCalib</code>.
	 * @param fwdMatrix
	 */
	public void setTransformation(float[] fwdMatrix) {
		/*
		 * The forward transformation matrix is
		 *        T * C^{-1} * S * R * C = M
		 *            T * C^{-1} * S * R = M * C^{-1}
		 *            C^{-1} * T * R * S = M * C^{-1}  | translations are commutative, rotation and uniform scale are also commutative
		 *                         T * R = C * M * C^{-1} * S^{-1}
		 *
		 * Scale is just the length of an arbitrary row or column vector
		 * (https://gamedev.stackexchange.com/questions/74527/how-do-i-disassemble-a-3x3-transformation-matrix-into-rotation-and-scaling-matri)
		 *
		 */

		scale = (float)Math.sqrt(fwdMatrix[0] * fwdMatrix[0] + fwdMatrix[1] * fwdMatrix[1] + fwdMatrix[2] * fwdMatrix[2]);

		System.arraycopy(fwdMatrix, 0, rotation, 0, 12); // rotation is now M
		Transform.applyTranslation(rotation, center[0], center[1], center[2]);
		Transform.applyScale(rotation, 1.0f / scale);
		Transform.applyTranslation(-center[0], -center[1], -center[2], rotation);

		// the translational part goes into translation
		translation[0] = rotation[3];
		translation[1] = rotation[7];
		translation[2] = rotation[11];

		rotation[3] = rotation[7] = rotation[11] = 0;
	}

	public void adjustOutputCalibration(Calibration cal) {
		cal.pixelWidth  = pdOut[0] / scale;
		cal.pixelHeight = pdOut[1] / scale;
	}

	public float[] calculateInverseTransform() {
		float[] fwd = calculateForwardTransform();
		return calculateInverseTransform(fwd);
	}

	public float[] calculateForwardTransform() {
		return calculateForwardTransform(scale, translation, rotation, center, fromCalib, toTransform);
	}

	public float[] calculateForwardTransformWithoutCalibration() {
		float[] scaleM = Transform.fromScale(scale, scale, 1, null);
		float[] centerM = Transform.fromTranslation(-center[0], -center[1], -center[2], null);

		float[] x = Transform.mul(scaleM, Transform.mul(rotation, centerM));
		Transform.applyTranslation(center[0], center[1], center[2], x);
		Transform.applyTranslation(translation[0], translation[1], translation[2], x);

		return x;
	}

	/**
	 * Calculates toTransform * Translation * Center^{-1} * Scale * Rotation * Center
	 * @param scale
	 * @param translation
	 * @param rotation
	 * @param center
	 * @param fromCalib
	 * @param toTransform
	 * @return
	 */
	private static float[] calculateForwardTransform(float scale, float[] translation, float[] rotation, float[] center, float[] fromCalib, float[] toTransform) {
		float[] scaleM = Transform.fromScale(scale, scale, 1, null);
		float[] centerM = Transform.fromTranslation(-center[0], -center[1], -center[2], null);

		float[] x = Transform.mul(scaleM, Transform.mul(rotation, centerM));
		Transform.applyTranslation(center[0], center[1], center[2], x);
		Transform.applyTranslation(translation[0], translation[1], translation[2], x);

//		System.out.println("row 1: " + Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]));
//		System.out.println("row 2: " + Math.sqrt(x[4] * x[4] + x[5] * x[5] + x[6] * x[6]));
//		System.out.println("row 3: " + Math.sqrt(x[8] * x[8] + x[9] * x[9] + x[10] * x[10]));
//		System.out.println("trans = " + rotation[3] + ", " + rotation[7] + ", " + rotation[11]);

		x = Transform.mul(x, fromCalib);
		x = Transform.mul(toTransform, x);

		return x;
	}

	public static float[] calculateInverseTransform(float[] fwd) {
		float[] copy = new float[12];
		System.arraycopy(fwd, 0, copy, 0, 12);
		Transform.invert(copy);
		return copy;
	}
}

package renderer3d;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/*
 * TODOs
 *   - Change output dimensions on the fly
 *   - interactive plugin to change rendering parameters
 *   - change input image/texture dynamically
 *   - CPU fallback
 */
public class CudaRaycaster {

	static {
		System.loadLibrary("CudaRaycaster");
	}

	private static native void initRaycaster8(
			int nChannels,
			int width, int height, int depth,
			int wOut, int hOut);

	private static native void initRaycaster16(
			int nChannels,
			int width, int height, int depth,
			int wOut, int hOut);

	public static native void close();

	private static native void setTexture8(int channel, byte[][] data);
	private static native void setTexture16(int channel, short[][] data);

	private static native void setBackground(int[] data, int w, int h);
	private static native void clearBackground();

	private static native void setTargetSize(int width, int height);

	private static native void setBoundingBox(int bx, int by, int bz, int bw, int bh, int pb);

	private static native void setKernel(String program);

	private static native int[] cast(
			float[] inverseTransform,
			float near,
			float far,
			float alphacorr,
			float[][] channelSettings,
			int bgred, int bggreen, int bgblue);

	private static native void white(int channel);


	protected ImagePlus image;
	protected int wOut;
	protected int hOut;
	protected int wIn, hIn, dIn, nChannels;
	protected BoundingBox bbox;
	protected Color bg = Toolbar.getBackgroundColor();

	public CudaRaycaster(ImagePlus imp, int wOut, int hOut) {
		wIn = imp.getWidth();
		hIn = imp.getHeight();
		dIn = imp.getNSlices();
		this.wOut = wOut;
		this.hOut = hOut;
		this.nChannels = imp.getNChannels();

		Calibration cal = imp.getCalibration();
		bbox = new BoundingBox(wIn, hIn, dIn, cal.pixelWidth, cal.pixelHeight, cal.pixelDepth);

		if(imp.getType() == ImagePlus.GRAY8)
			initRaycaster8(nChannels, wIn, hIn, dIn, wOut, hOut);
		else if(imp.getType() == ImagePlus.GRAY16)
			initRaycaster16(nChannels, wIn, hIn, dIn, wOut, hOut);
		else
			throw new RuntimeException("Only 8- and 16-bit images are supported");
		setImage(imp);
		setKernel(OpenCLProgram.makeSource(nChannels, false, false));
	}

	public BoundingBox getBoundingBox() {
		return bbox;
	}

	private static float[] calculateForwardTransform(float scale, float[] translation, float[] rotation, float[] center, float[] fromCalib, float[] toTransform) {
		float[] scaleM = Transform.fromScale(scale, null);
		float[] transM = Transform.fromTranslation(translation[0], translation[1], translation[2], null);
		float[] centerM = Transform.fromTranslation(-center[0], -center[1], -center[2], null);

		float[] x = Transform.mul(scaleM, Transform.mul(rotation, centerM));
		Transform.applyTranslation(center[0], center[1], center[2], x);
		x = Transform.mul(transM, x);

		x = Transform.mul(x, fromCalib);
		x = Transform.mul(toTransform, x);

		return x;
	}

	public ImagePlus getImage() {
		return image;
	}

	public void setImage(ImagePlus imp) {
		this.image = imp;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getNSlices();
		int nChannels = imp.getNChannels();

		if(w != wIn || h != hIn || d != dIn || nChannels != this.nChannels)
			throw new IllegalArgumentException("Image dimensions must remain the same.");


		if(imp.getType() == ImagePlus.GRAY8) {
			for(int c = 0; c < nChannels; c++) {
				byte[][] image = new byte[d][];
				for(int z = 0; z < d; z++) {
					int idx = imp.getStackIndex(c + 1, z + 1, imp.getT());
					image[z] = (byte[])imp.getStack().getPixels(idx);
				}
				setTexture8(c, image);
			}
		}
		else if(imp.getType() == ImagePlus.GRAY16) {
			for(int c = 0; c < nChannels; c++) {
				short[][] image = new short[d][];
				for(int z = 0; z < d; z++) {
					int idx = imp.getStackIndex(c + 1, z + 1, imp.getT());
					image[z] = (short[])imp.getStack().getPixels(idx);
				}
				setTexture16(c, image);
			}
		}
		else
			throw new RuntimeException("Only 8- and 16-bit images are supported");
	}

	public void setBackground(ColorProcessor cp) {
		if(cp == null) {
			clearBackground();
			setKernel(OpenCLProgram.makeSource(nChannels, false, false));
			return;
		}
		int[] rgb = (int[])cp.getPixels();
		setBackground(rgb, cp.getWidth(), cp.getHeight());
		// setKernel(OpenCLProgram.makeSourceForMIP(nChannels, true));
		setKernel(OpenCLProgram.makeSource(nChannels, true, false));
	}

	public void setBackground(Color c) {
		this.bg = c;
	}

	// TODO take a RenderingState object as an argument
	public ImageProcessor project(
			float[] fwdTransform,
			float[] invTransform,
			double[][] channelProperties,
			float alphacorr,
			float near,
			float far) {

		float[][] channelSettings = new float[channelProperties.length][];
		for(int c = 0; c < channelSettings.length; c++) {
			channelSettings[c] = new float[] {
					(float)(channelProperties[c][ExtendedRenderingState.INTENSITY_MIN]),
					(float)(channelProperties[c][ExtendedRenderingState.INTENSITY_MAX]),
					(float)(channelProperties[c][ExtendedRenderingState.INTENSITY_GAMMA]),
					(float)(channelProperties[c][ExtendedRenderingState.ALPHA_MIN]),
					(float)(channelProperties[c][ExtendedRenderingState.ALPHA_MAX]),
					(float)(channelProperties[c][ExtendedRenderingState.ALPHA_GAMMA]),
					(float)(channelProperties[c][ExtendedRenderingState.WEIGHT]),
					(int)(channelProperties[c][ExtendedRenderingState.CHANNEL_COLOR_RED]),
					(int)(channelProperties[c][ExtendedRenderingState.CHANNEL_COLOR_GREEN]),
					(int)(channelProperties[c][ExtendedRenderingState.CHANNEL_COLOR_BLUE]),
//					channelColors[c].getRed(), channelColors[c].getGreen(), channelColors[c].getBlue(), // color
			};
		}
		// TODO remove this line and set from GUI
		Color bg = Toolbar.getBackgroundColor();
		int[] result = cast(invTransform, near, far, alphacorr, channelSettings, bg.getRed(), bg.getGreen(), bg.getBlue());

		ColorProcessor ret = new ColorProcessor(wOut, hOut, result);
		bbox.drawBoundingBox(ret, fwdTransform, invTransform);
		bbox.drawScalebar(ret, fwdTransform, invTransform);

		float len = (float)Math.sqrt(invTransform[2] * invTransform[2] + invTransform[6] * invTransform[6] + invTransform[10] * invTransform[10]);

		bbox.drawFrontClippingPlane(ret, fwdTransform, invTransform, near / len);
		return ret;
	}

	public void clear(int channel) {
		white(channel);
	}

	public void setTgtSize(int w, int h) {
		wOut = w;
		hOut = h;
		setTargetSize(w, h);
	}

	public int getTargetWidth() {
		return wOut;
	}

	public int getTargetHeight() {
		return hOut;
	}

	public void setBBox(int bx0, int by0, int bz0, int bx1, int by1, int bz1) {
		setBoundingBox(bx0, by0, bz0, bx1 - bx0, by1 - by0, bz1 - bz0);
	}

	public void setProgram(String src) {
		setKernel(src);
	}

	public void crop(
			final ImagePlus in,
			final ByteProcessor mask,
			final float[] fwdTransform) {
//		clear(0);
//		if(true)
//			return;
		final ImageProcessor[] inProcessors = new ImageProcessor[in
				.getStackSize()];
		for (int z = 0; z < inProcessors.length; z++)
			inProcessors[z] = in.getStack().getProcessor(z + 1);

		final int wIn = in.getWidth();
		final int hIn = in.getHeight();
		final int dIn = in.getNSlices();
		final int nC = in.getNChannels();

		final ExecutorService exec = Executors.newFixedThreadPool(Runtime
				.getRuntime().availableProcessors());

		final int wOut = mask.getWidth();
		final int hOut = mask.getHeight();

		final int n = nC * dIn;
		final AtomicInteger prog = new AtomicInteger(0);

		for(int ic = 0; ic < nC; ic++) {
			for (int iz = 0; iz < dIn; iz++) {
				final int z = iz;
				final int c = ic;
				final int t = in.getFrame() - 1;
				final int stackIndex = in.getStackIndex(c + 1, iz + 1, t + 1);
				final ImageProcessor ip = inProcessors[stackIndex - 1];
				exec.submit(new Runnable() {
					@Override
					public void run() {
						try {
							float[] result = new float[3];
							for (int y = 0, xy = 0; y < hIn; y++) {
								for (int x = 0; x < wIn; x++, xy++) {
									Transform.apply(fwdTransform, x, y, z, result);
									if (result[0] < 0
											|| result[1] < 0
											|| result[0] >= wOut - 1
											|| result[1] >= hOut - 1
											|| mask.getf(Math.round(result[0]), Math.round(result[1])) == 0) {
										ip.setf(xy, 0);
										continue;
									}
								}
							}
							int progress = prog.incrementAndGet();
							IJ.showProgress(progress, n);
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

		setImage(in);
	}
}

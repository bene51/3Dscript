package animation3d.renderer3d;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import animation3d.textanim.CombinedTransform;
import animation3d.util.Transform;
import ij.IJ;
import ij.ImagePlus;
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
public class OpenCLRaycaster {

	static {
		System.loadLibrary("OpenCLRaycaster");
	}

	public static native String getUNCForPath(String path);

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

	private static native void calculateGradients(int channel, float dzByDx);
	public static native void clearGradients(int channel);

	private static native void setBackground(int[] data, int w, int h);
	private static native void clearBackground();

	private static native void setTargetSize(int width, int height);

	private static native void setKernel(String program);

	private static native int[] cast(
			float[] inverseTransform,
			float alphacorr,
			float[][] channelSettings,
			int bgred, int bggreen, int bgblue);

	private static native void setColorLUT(int channel, int[] lut);
	private static native void clearColorLUT(int channel);

	private static native void white(int channel);


	protected ImagePlus image;
	protected int wOut;
	protected int hOut;
	protected int wIn, hIn, dIn, nChannels;
	protected int tIndex;
	protected BoundingBox bbox;
	protected Scalebar sbar;

	private Progress loadingProgress = null;

	public OpenCLRaycaster(ImagePlus imp, int wOut, int hOut) {
		this(imp, wOut, hOut, null);
	}

	public OpenCLRaycaster(ImagePlus imp, int wOut, int hOut, Progress loadingProgress) {
		wIn = imp.getWidth();
		hIn = imp.getHeight();
		dIn = imp.getNSlices();
		this.wOut = wOut;
		this.hOut = hOut;
		this.nChannels = imp.getNChannels();
		this.loadingProgress = loadingProgress;


		Calibration cal = imp.getCalibration();
		bbox = new BoundingBox(wIn, hIn, dIn, cal.pixelWidth, cal.pixelHeight, cal.pixelDepth);
		sbar = new Scalebar(wIn, hIn, dIn, cal.pixelWidth, cal.pixelHeight, cal.pixelDepth);

		if(imp.getType() == ImagePlus.GRAY8)
			initRaycaster8(nChannels, wIn, hIn, dIn, wOut, hOut);
		else if(imp.getType() == ImagePlus.GRAY16)
			initRaycaster16(nChannels, wIn, hIn, dIn, wOut, hOut);
		else
			throw new RuntimeException("Only 8- and 16-bit images are supported");
		boolean[] useLights = new boolean[nChannels];
		boolean[] colorLUT = new boolean[nChannels];
		setKernel(OpenCLProgram.makeSource(nChannels, false, false, false, useLights, colorLUT));
		setImage(imp);
	}

	public BoundingBox getBoundingBox() {
		return bbox;
	}

	public Scalebar getScalebar() {
		return sbar;
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
		tIndex = imp.getT();

		if(w != wIn || h != hIn || d != dIn || nChannels != this.nChannels)
			throw new IllegalArgumentException("Image dimensions must remain the same.");

		int nSlices = nChannels * d;

		if(imp.getType() == ImagePlus.GRAY8) {
			for(int c = 0, i = 0; c < nChannels; c++) {
				byte[][] image = new byte[d][];
				for(int z = 0; z < d; z++, i++) {
					int idx = imp.getStackIndex(c + 1, z + 1, imp.getT());
					image[z] = (byte[])imp.getStack().getPixels(idx);
					if(loadingProgress != null) loadingProgress.setProgress((double)(i + 1) / nSlices);
				}
				setTexture8(c, image);
			}
		}
		else if(imp.getType() == ImagePlus.GRAY16) {
			for(int c = 0, i = 0; c < nChannels; c++) {
				short[][] image = new short[d][];
				for(int z = 0; z < d; z++, i++) {
					int idx = imp.getStackIndex(c + 1, z + 1, imp.getT());
					image[z] = (short[])imp.getStack().getPixels(idx);
					if(loadingProgress != null) loadingProgress.setProgress((double)(i + 1) / nSlices);
				}
				setTexture16(c, image);
			}
		}
		else
			throw new RuntimeException("Only 8- and 16-bit images are supported");
	}

	public void calculateGradients(int channel) {
		float dzByDx = (float)(image.getCalibration().pixelDepth / image.getCalibration().pixelWidth);
		calculateGradients(channel, dzByDx);
	}

	public void setBackground(ColorProcessor cp, boolean combinedAlpha, boolean mip, boolean[] useLights, boolean[] colorLUT) {
		if(cp == null) {
			clearBackground();
			setKernel(OpenCLProgram.makeSource(nChannels, false, combinedAlpha, mip, useLights, colorLUT));
			return;
		}
		int[] rgb = (int[])cp.getPixels();
		setBackground(rgb, cp.getWidth(), cp.getHeight());
		// setKernel(OpenCLProgram.makeSourceForMIP(nChannels, true));
		setKernel(OpenCLProgram.makeSource(nChannels, true, combinedAlpha, mip, useLights, colorLUT));
	}

	public void setLookupTable(int channel, int[] lut) {
		if(lut != null)
			setColorLUT(channel, lut);
		else
			clearColorLUT(channel);
	}

	public int[] makeRandomLUT() {
		int l = 1 << image.getBitDepth();
		int[] lut = new int[l];
		for(int i = 0; i < l; i++)
			lut[i] = Color.HSBtoRGB((float)Math.random(), 1, 1);

		lut[0] = new Color(0, 0, 0).getRGB();
		lut[1] = new Color(255, 0, 0).getRGB();
		lut[2] = new Color(0, 255, 0).getRGB();
		lut[3] = new Color(0, 0, 255).getRGB();
		return lut;
	}

	// TODO take a RenderingState object as an argument
	public synchronized ImageProcessor project(ExtendedRenderingState kf) {

		CombinedTransform transform = kf.getFwdTransform();
		float[] fwdTransform = transform.calculateForwardTransform();
		float[] invTransform = CombinedTransform.calculateInverseTransform(fwdTransform);

		// calculate an opacity correction factor
		// https://stackoverflow.com/questions/12494439/opacity-correction-in-raycasting-volume-rendering
		// - reference sample spacing (dx on the website) is pw
		// - real sample spacing depends on the angle and the zStep, which in turn influences the transform's pdOut
		// - real sample spacing in pixel coordinates is (inv[2], inv[6], inv[10])
		// - multiplied with the input pixel spacings, this is a vector whose length is \tilde{dx} (on the website)
		// - the correction factor is then \tilde{dx} / dx.
		float[] pdIn = transform.getInputSpacing();
		float dx = pdIn[0] * invTransform[2];
		float dy = pdIn[1] * invTransform[6];
		float dz = pdIn[2] * invTransform[10];
		float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
		float alphacorr = len / pdIn[0];

		double[][] channelProperties = kf.getChannelProperties();
		double[] nonChannelProperties = kf.getNonChannelProperties();

		float l = (float)Math.sqrt(invTransform[2] * invTransform[2] + invTransform[6] * invTransform[6] + invTransform[10] * invTransform[10]);

		float[] center = transform.getCenter();
		float cz = fwdTransform[8] * center[0] / pdIn[0] +
				fwdTransform[9] * center[1] / pdIn[1] +
				fwdTransform[10] * center[2] / pdIn[2] + fwdTransform[11];

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
					(int)Math.round(channelProperties[c][ExtendedRenderingState.CHANNEL_COLOR_RED]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.CHANNEL_COLOR_GREEN]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.CHANNEL_COLOR_BLUE]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.BOUNDINGBOX_XMIN]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.BOUNDINGBOX_YMIN]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.BOUNDINGBOX_ZMIN]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.BOUNDINGBOX_XMAX]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.BOUNDINGBOX_YMAX]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.BOUNDINGBOX_ZMAX]),
					(float)(cz * l + channelProperties[c][ExtendedRenderingState.NEAR]),
					(float)(cz * l + channelProperties[c][ExtendedRenderingState.FAR]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.USE_LIGHT]),
					(float)(channelProperties[c][ExtendedRenderingState.LIGHT_K_OBJECT]),
					(float)(channelProperties[c][ExtendedRenderingState.LIGHT_K_DIFFUSE]),
					(float)(channelProperties[c][ExtendedRenderingState.LIGHT_K_SPECULAR]),
					(float)(channelProperties[c][ExtendedRenderingState.LIGHT_SHININESS]),
					(int)Math.round(channelProperties[c][ExtendedRenderingState.USE_LUT]),
			};
		}
		// TODO remove this line and set from GUI
//		Color bg = Toolbar.getBackgroundColor();
		Color bg = new Color(
				(int)Math.round(nonChannelProperties[ExtendedRenderingState.BG_COLOR_RED]),
				(int)Math.round(nonChannelProperties[ExtendedRenderingState.BG_COLOR_GREEN]),
				(int)Math.round(nonChannelProperties[ExtendedRenderingState.BG_COLOR_BLUE]));
		int t = (int)Math.round(nonChannelProperties[ExtendedRenderingState.TIMEPOINT]);
		t = Math.max(1, Math.min(image.getNFrames(), t));
		if(t != tIndex) {
			tIndex = t;
			image.setT(tIndex);
			setImage(image);
		}
		int[] result = cast(invTransform, alphacorr, channelSettings, bg.getRed(), bg.getGreen(), bg.getBlue());

		ColorProcessor ret = new ColorProcessor(wOut, hOut, result);

		kf.adjustBoundingbox(bbox);
		bbox.drawBoundingBox(ret, fwdTransform, invTransform);

		kf.adjustScalebar(sbar);
		sbar.drawScalebar(ret, fwdTransform, invTransform, transform.getOutputSpacing()[0] / transform.getScale());


		for(int c = 0; c < channelSettings.length; c++)
			bbox.drawFrontClippingPlane(ret, fwdTransform, invTransform,
					(cz * l + (float)channelProperties[c][ExtendedRenderingState.NEAR]) / l);
		return ret;
	}

	public void clear(int channel) {
		white(channel);
	}

	public synchronized void setTgtSize(int w, int h) {
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

	public void setProgram(String src) {
		setKernel(src);
	}

	public void crop(
			final ImagePlus in,
			final int channel,
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

		final int n = dIn;
		final AtomicInteger prog = new AtomicInteger(0);

//		for(int ic = 0; ic < nC; ic++) {
		final int ic = channel;
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
//		}
		IJ.showProgress(1);
		exec.shutdown();
		try {
			exec.awaitTermination(1, TimeUnit.DAYS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		setImage(in);
	}
}

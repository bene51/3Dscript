package animation2;

import java.awt.Color;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

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

	public static void main(String... args) {
		new ij.ImageJ();
		String dir = "D:\\VLanger\\20161205-Intravital-Darm\\";
		String name = "cy5-shg-2p-maus3919-gecleart-20x-big-stack1.resampled.tif";
		ImagePlus imp = IJ.openImage(dir + name);
		imp.show();

		int d = imp.getNSlices();

		RenderingSettings renderingSettings0 = new RenderingSettings(
				300,    // alphaMin,
				3000,   // float alphaMax,
				2,      // float alphaGamma,
				300,    // float colorMin,
				3000,   // float colorMax,
				1f);    // float colorGamma,

		RenderingSettings renderingSettings1 = new RenderingSettings(
				300,   // alphaMin,
				3000,  // float alphaMax,
				2,      // float alphaGamma,
				300,   // float colorMin,
				3000,  // float colorMax,
				1);    // float colorGamma,

		RenderingSettings[] renderingSettings = new RenderingSettings[] {renderingSettings0, renderingSettings1};

		CudaRaycaster raycaster = new CudaRaycaster(imp, imp.getWidth(), imp.getHeight(), 1);
		ImagePlus comp = raycaster.renderAndCompose(new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0}, renderingSettings, 0, 2 * d);

		new ImagePlus("", comp.getImage()).show();
	}

	private static native void initRaycaster8(
			int nChannels,
			int width, int height, int depth,
			int wOut, int hOut,
			float zStep, float alphastop);

	private static native void initRaycaster16(
			int nChannels,
			int width, int height, int depth,
			int wOut, int hOut,
			float zStep, float alphastop);

	public static native void close();

	private static native void setTexture8(int channel, byte[][] data);
	private static native void setTexture16(int channel, short[][] data);

	private static native void setTargetSize(int width, int height);

	private static native void setBoundingBox(int bx, int by, int bz, int bw, int bh, int pb);

	private static native byte[] cast(
			int channel,
			float[] inverseTransform,
			float near,
			float far,
			float alphamin, float alphamax, float alphagamma,
			float colormin, float colormax, float colorgamma);


	private ImagePlus image;
	private int wOut;
	private int hOut;

	public CudaRaycaster(ImagePlus imp, int wOut, int hOut, float zStep) {
		this(imp, wOut, hOut, zStep, 0.95f);
	}

	public CudaRaycaster(ImagePlus imp, int wOut, int hOut, float zStep, float alphastop) {
		this.image = imp;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int d = imp.getNSlices();
		this.wOut = wOut;
		this.hOut = hOut;
		int nChannels = imp.getNChannels();
		this.channelLUTs = getChannelLUTs();

		if(imp.getType() == ImagePlus.GRAY8) {
			initRaycaster8(nChannels, w, h, d, wOut, hOut, zStep, alphastop);
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
			initRaycaster16(nChannels, w, h, d, wOut, hOut, zStep, alphastop);
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

	public ImageProcessor project(
			int channel,
			float[] inverseTransform,
			RenderingSettings renderingSettings,
			float near,
			float far) {
		byte[] result = cast(
				channel,
				inverseTransform,
				near,
				far,
				renderingSettings.alphaMin,
				renderingSettings.alphaMax,
				renderingSettings.alphaGamma,
				renderingSettings.colorMin,
				renderingSettings.colorMax,
				renderingSettings.colorGamma);
		return new ByteProcessor(wOut, hOut, result, null);
	}

	public void setTgtSize(int w, int h) {
		wOut = w;
		hOut = h;
		setTargetSize(w, h);
	}

	public void setBBox(int bx0, int by0, int bz0, int bx1, int by1, int bz1) {
		setBoundingBox(bx0, by0, bz0, bx1 - bx0, by1 - by0, bz1 - bz0);
	}


	public ImagePlus renderAndCompose(float[] transform, RenderingSettings[] renderingSettings, float near, float far) {
		ImageStack stack = new ImageStack(wOut, hOut);
		for(int ch = 0; ch < image.getNChannels(); ch++) {
			ImageProcessor ip = project(ch, transform, renderingSettings[ch], near, far);
			stack.addSlice(ip);
		}

		ImagePlus iimp = new ImagePlus("", stack);
		iimp.setDimensions(image.getNChannels(), 1, 1);
		if(!image.isComposite()) {
			int t = image.getType();
			boolean grayscale = t == ImagePlus.GRAY8 || t == ImagePlus.GRAY16 || t == ImagePlus.GRAY32;
			if(channelLUTs[0] != null && !grayscale)
				iimp.getProcessor().setLut(channelLUTs[0]);
			return iimp;
		}

		CompositeImage comp = new CompositeImage(iimp, CompositeImage.COMPOSITE);
		for(int c = 0; c < image.getNChannels(); c++)
			comp.setChannelLut(channelLUTs[c], c + 1);

		return new ImagePlus("", comp.getImage());
	}

	private LUT[] channelLUTs;

	private LUT[] getChannelLUTs() {
		int nChannels = image.getNChannels();
		LUT[] channelLUTs = new LUT[nChannels];
		if(!image.isComposite()) {
			LUT lut = image.getProcessor().getLut();
			int t = image.getType();
			boolean grayscale = t == ImagePlus.GRAY8 || t == ImagePlus.GRAY16 || t == ImagePlus.GRAY32;
			if(lut != null && !grayscale) {
				channelLUTs[0] = LUT.createLutFromColor(getLUTColor(lut));
			} else {
				channelLUTs[0] = null;
			}
			return channelLUTs;
		}
		for(int c = 0; c < image.getNChannels(); c++) {
			image.setC(c + 1);
			Color col = ((CompositeImage)image).getChannelColor();
			if(col.equals(Color.BLACK))
				col = Color.white;
			channelLUTs[c] = LUT.createLutFromColor(col);
		}
		return channelLUTs;
	}

	public Color getLUTColor(LUT lut) {
		int index = lut.getMapSize() - 1;
		int r = lut.getRed(index);
		int g = lut.getGreen(index);
		int b = lut.getBlue(index);
		//IJ.log(index+" "+r+" "+g+" "+b);
		if (r<100 || g<100 || b<100)
			return new Color(r, g, b);
		else
			return Color.WHITE;
	}


//	/*
//	 * Just for reference
//	 */
//	public static ImageProcessor projectCPU(
//			ImageProcessor[] ips,
//			double[] dir,
//			double zStep,
//			double near, double far,
//			int wOut, int hOut,
//			byte[] colorlut, byte[] alphalut,
//			double alphaStop,
//			double dx, double dy, double dz,
//			float[] inv) {
//
//		final ImageProcessor ret = new ByteProcessor(wOut, hOut);
//		int w = ips[0].getWidth();
//		int h = ips[0].getHeight();
//		int d = ips.length;
//		for(int y = 0; y < hOut; y++) {
//			float[] r0 = new float[3];
//			double[] entryexit = new double[2];
//			for(int x = 0; x < wOut; x++) {
//				Transform.apply(inv, x, y, 0, r0);  // TODO near must at the moment be in pixels
//				boolean hits = Transform.intersect(w, h, d, r0, dir, entryexit);
//				if(!hits) {
//					System.out.println("ray " + x + ", "+ y + " does not hit");
//					continue;
//				}
//				if(entryexit[0] < near)
//					entryexit[0] = near;
//				if(entryexit[1] > far)
//					entryexit[1] = far;
//
//				int idx = y * wOut + x;
//				float color = 0;
//				float alpha = 0;
//				double x0 = r0[0] + entryexit[0] * dir[0]; // start from front
//				double y0 = r0[1] + entryexit[0] * dir[1]; // start from front
//				double z0 = r0[2] + entryexit[0] * dir[2]; // start from front
//				int n = (int)Math.floor(Math.abs(entryexit[1] - entryexit[0]) / zStep); // TODO floor?
//				for(int step = 0; step < n; step++) {
//					int c = 1; // Math.round(Transform.interpolateGray(imp, channel, imp.getFrame(),
//							   // ips, new float[] {(float) x0, (float) y0, (float) z0}));
//
//					double alphar = (alphalut[c] & 0xff) / 255.0;
//					double colorr = colorlut[c] & 0xff;
//					if(alphar > 0) {
//						color = (float)(color + (1 - alpha) * colorr * alphar);
//						alpha = (float)(alpha + (1 - alpha) * alphar);
//						if(alpha > alphaStop)
//							break;
//					}
//
//					x0 += dx;
//					y0 += dy;
//					z0 += dz;
//				}
//				ret.set(idx, roundAndClamp(color));
//			}
//		}
//		return ret;
//	}
//
//	private static int roundAndClamp(double c) {
//		int r = (int)Math.round(c);
//		if(r < 0) return 0;
//		if(r > 255) return 255;
//		return r;
//	}
}

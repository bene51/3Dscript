package animation2;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

public class CompositeRaycaster implements PlugInFilter {

	private boolean[] process;
	private byte[][] colorlut;
	private byte[][] alphalut;

	private double[] colormin, alphamin;
	private double[] colormax, alphamax;
	private double[] gammaForAlpha;
	private double[] gammaForColor;

	private double near = 0;
	private double far = Double.POSITIVE_INFINITY;
	private double zStep = 1;

	private ImagePlus image;

	public static void main(String... args) {
		new ij.ImageJ();
		// ImagePlus imp = IJ.openImage("D:\\20161121-Intravital\\darm1\\darm1.median.resampled.tif");
		ImagePlus imp = IJ.openImage("D:\\20161118-Intravital\\darm1\\darm1.median.resampled.tif");
		imp.show();
		CompositeRaycaster cr = new CompositeRaycaster();
		cr.setup("", imp);

		cr.run(null);
	}

	public CompositeRaycaster() {}

	public CompositeRaycaster(
			int bitdepth,
			double[] colorMin,
			double[] alphaMin,
			double[] colorMax,
			double[] alphaMax,
			double[] colorGamma,
			double[] alphaGamma) {

		int nC = colorMin.length;
		colormin = new double[nC];
		alphamin = new double[nC];
		colormax = new double[nC];
		alphamax = new double[nC];
		gammaForColor = new double[nC];
		gammaForAlpha = new double[nC];
		process = new boolean[nC];
		alphalut = new byte[nC][];
		colorlut = new byte[nC][];
		int nlevels = (int)Math.pow(2, bitdepth);

		for(int c = 0; c < nC; c++) {
			process[c] = true;
			colorlut[c] = new byte[nlevels];
			alphalut[c] = new byte[nlevels];
		}
		setParameters(colorMin, alphaMin, colorMax, alphaMax, colorGamma, alphaGamma);
	}

	public CompositeRaycaster(int bitdepth, int nChannels, double min, double max) {
		int nC = nChannels;
		colormin = new double[nC];
		alphamin = new double[nC];
		colormax = new double[nC];
		alphamax = new double[nC];
		gammaForColor = new double[nC];
		gammaForAlpha = new double[nC];
		process = new boolean[nC];
		alphalut = new byte[nC][];
		colorlut = new byte[nC][];
		int nlevels = (int)Math.pow(2, bitdepth);

		for(int c = 0; c < nC; c++) {
			colormin[c] = alphamin[c] = min;
			colormax[c] = alphamax[c] = max;
			gammaForAlpha[c] = 2.0;
			gammaForColor[c] = 1;
			process[c] = true;
			colorlut[c] = new byte[nlevels];
			alphalut[c] = new byte[nlevels];
		}

		updateLUTs();
	}

	@Override
	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_16;
	}

	@Override
	public void run(ImageProcessor ip) {
		LUT[] luts = null;
		if(image.isComposite())
			luts = image.getLuts();
		else
			luts = new LUT[] {image.getProcessor().getLut()};

		int nC = image.getNChannels();

		GenericDialog gd = new GenericDialog("");
		for(int c = 0; c < nC; c++) {
			gd.addMessage("channel " + (c + 1));
			gd.addStringField("Range_" + (c + 1) + "_color", Math.round(luts[c].min) + " - " + Math.round(luts[c].max));
			gd.addStringField("Range_" + (c + 1) + "_alpha", Math.round(luts[c].min) + " - " + Math.round(luts[c].max));
			gd.addNumericField("Gamma_" + (c + 1) + "_color", 1, 1);
			gd.addNumericField("Gamma_" + (c + 1) + "_alpha", 2, 1);
		}

		gd.showDialog();

		colormin = new double[nC];
		alphamin = new double[nC];
		colormax = new double[nC];
		alphamax = new double[nC];
		gammaForColor = new double[nC];
		gammaForAlpha = new double[nC];
		process = new boolean[nC];
		alphalut = new byte[nC][];
		colorlut = new byte[nC][];
		int nlevels = (int)Math.pow(2, image.getBitDepth());

		for(int c = 0; c < nC; c++) {
			process[c] = true;
			colorlut[c] = new byte[nlevels];
			alphalut[c] = new byte[nlevels];


			String rangesC = gd.getNextString();
			String rangesA = gd.getNextString();

			String[] toks = rangesC.split("-");
			colormin[c] = Double.parseDouble(toks[0].trim());
			colormax[c] = Double.parseDouble(toks[1].trim());

			toks = rangesA.split("-");
			alphamin[c] = Double.parseDouble(toks[0].trim());
			alphamax[c] = Double.parseDouble(toks[1].trim());

			gammaForColor[c] = gd.getNextNumber();
			gammaForAlpha[c] = gd.getNextNumber();
		}

		updateLUTs();

		float[] inv = new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
		ImageStack stack = new ImageStack(image.getWidth(), image.getHeight());

//		for(int z = 6; z < 11; z++) {
			near = 0;
			ImageProcessor res = project(image, image.getWidth(), image.getHeight(), inv, 0.95);
			stack.addSlice(res);
//		}
		new ImagePlus("", stack).show();
	}

	public void setParameters(
			double[] colorMin,
			double[] alphaMin,
			double[] colorMax,
			double[] alphaMax,
			double[] colorGamma,
			double[] alphaGamma) {
		int nC = colorlut.length;

		for(int c = 0; c < nC; c++) {
			colormin[c] = colorMin[c];
			colormax[c] = colorMax[c];

			alphamin[c] = alphaMin[c];
			alphamax[c] = alphaMax[c];

			gammaForColor[c] = colorGamma[c];
			gammaForAlpha[c] = alphaGamma[c];
		}
		updateLUTs();
	}

	/**
	 * Maps each value v to 255 * ((v - min) / (max - min)) ^ gamma
	 */
	public void updateLUTs() {
		int levels = colorlut[0].length;
		int nC = colorlut.length;
		for(int c = 0; c < nC; c++) {
			for(int i = 0; i < levels; i++) {
				double vc = (i - colormin[c]) / (colormax[c] - colormin[c]);
				vc = Math.max(0, Math.min(1, vc));
				vc = 255 * Math.pow(vc, gammaForColor[c]);
				int vci = roundAndClamp(vc);
				colorlut[c][i] = (byte)vci;

				double va = (i - alphamin[c]) / (alphamax[c] - alphamin[c]);
				va = Math.max(0,  Math.min(1, va));
				va = 255 * Math.pow(va, gammaForAlpha[c]);
				int vai = roundAndClamp(va);
				alphalut[c][i] = (byte)vai;
			}
		}
	}

	public ImageProcessor project(final ImagePlus imp, final int wOut, final int hOut, final float[] inv, final double alphaStop) {
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getNSlices();
		final int nC = imp.getNChannels();

		final double[] dir = new double[] {inv[2], inv[6], inv[10]};
		double len = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
		dir[0] /= len;
		dir[1] /= len;
		dir[2] /= len;

		final double dx = dir[0] * zStep; // TODO zStep must currently be in pixels
		final double dy = dir[1] * zStep;
		final double dz = dir[2] * zStep;

		final ImageProcessor[] ips = new ImageProcessor[imp.getStackSize()];
		for (int z = 0; z < ips.length; z++)
			ips[z] = imp.getStack().getProcessor(z + 1);

		int nThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService exec = Executors.newFixedThreadPool(nThreads);
		ImageStack stack = new ImageStack(wOut, hOut);
		final boolean backToFront = false;
		long t0 = System.currentTimeMillis();
		for(int ch = 0; ch < nC; ch++) {
			final int channel = ch;
			if(!process[channel])
				continue;

			final ImageProcessor ret = new ByteProcessor(wOut, hOut);
			for(int iy = 0; iy < hOut; iy++) {
				final int y = iy;
				exec.submit(new Runnable() {
					@Override
					public void run() {
						try {
							float[] r0 = new float[3];
							double[] entryexit = new double[2];
							for(int x = 0; x < wOut; x++) {
								Transform.apply(inv, x, y, 0, r0);  // TODO near must at the moment be in pixels
								boolean hits = Transform.intersect(w, h, d, r0, dir, entryexit);
								if(!hits) {
									System.out.println("ray " + x + ", "+ y + " does not hit");
									continue;
								}
								if(entryexit[0] < near)
									entryexit[0] = near;
								if(entryexit[1] > far)
									entryexit[1] = far;

								int idx = y * wOut + x;
								if(backToFront) {
									float color = 0;
									double x0 = r0[0] + entryexit[1] * dir[0]; // start from back
									double y0 = r0[1] + entryexit[1] * dir[1]; // start from back
									double z0 = r0[2] + entryexit[1] * dir[2]; // start from back
									int n = (int)Math.floor(Math.abs(entryexit[1] - entryexit[0]) / zStep); // TODO floor?
									for(int step = 0; step < n; step++) {
										int c = Math.round(Transform.interpolateGray(imp, channel, imp.getFrame(),
												ips, new float[] {(float) x0, (float) y0, (float) z0}));

										double alphar = (alphalut[channel][c] & 0xff) / 255.0;
										double colorr = colorlut[channel][c] & 0xff;
										if(alphar > 0)
											color   = (float)(color   * (1 - alphar) + colorr * alphar);

										x0 -= dx;
										y0 -= dy;
										z0 -= dz;
									}
									ret.set(idx, roundAndClamp(color));
								} else {
									float color = 0;
									float alpha = 0;
									double x0 = r0[0] + entryexit[0] * dir[0]; // start from front
									double y0 = r0[1] + entryexit[0] * dir[1]; // start from front
									double z0 = r0[2] + entryexit[0] * dir[2]; // start from front
									int n = (int)Math.floor(Math.abs(entryexit[1] - entryexit[0]) / zStep); // TODO floor?
									for(int step = 0; step < n; step++) {
										int c = Math.round(Transform.interpolateGray(imp, channel, imp.getFrame(),
												ips, new float[] {(float) x0, (float) y0, (float) z0}));

										double alphar = (alphalut[channel][c] & 0xff) / 255.0;
										double colorr = colorlut[channel][c] & 0xff;
										if(alphar > 0) {
											color = (float)(color + (1 - alpha) * colorr * alphar);
											alpha = (float)(alpha + (1 - alpha) * alphar);
											if(alpha > alphaStop)
												break;
										}

										x0 += dx;
										y0 += dy;
										z0 += dz;
									}
									ret.set(idx, roundAndClamp(color));
								}
							}
						} catch(Throwable t) {
							t.printStackTrace();
							System.exit(1);
						}
					}
				});
			}
			stack.addSlice(ret);
		}
		exec.shutdown();
		try {
			exec.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long t1 = System.currentTimeMillis();
		System.out.println("Needed " + (t1 - t0) + " ms");

		ImagePlus iimp = new ImagePlus("", stack);
		iimp.setDimensions(nC, 1, 1);
		if(!imp.isComposite())
			return iimp.getProcessor();

		CompositeImage comp = new CompositeImage(iimp, CompositeImage.COMPOSITE);
		for(int c = 0; c < nC; c++) {
			imp.setC(c + 1);
			Color col = ((CompositeImage)imp).getChannelColor();
			comp.setChannelLut(LUT.createLutFromColor(col), c + 1);
		}
		return new ImagePlus("", comp.getImage()).getProcessor();
	}

	private int roundAndClamp(double c) {
		int r = (int)Math.round(c);
		if(r < 0) return 0;
		if(r > 255) return 255;
		return r;
	}
}

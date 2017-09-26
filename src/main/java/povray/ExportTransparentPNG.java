package povray;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import ij.IJ;
import ij.ImagePlus;
import ij.process.StackConverter;

public class ExportTransparentPNG {

	public static void main(String...strings) throws IOException {
		new ij.ImageJ();

//		File outdir = new File("H:\\paper\\2017-Animation\\figures\\Fig3\\DD_lyse_1_still");
		File outdir = new File("d:\\povray\\transparencytest\\original");
		if(!outdir.exists())
			outdir.mkdirs();

		ImagePlus imp = IJ.openImage("d:\\povray\\transparencytest\\DD_lyse_1.subtracted.small.rgb.tif");
//Resizer resizer = new Resizer();
//resizer.setAverageWhenDownsizing(true);
//imp = resizer.zScale(imp, imp.getNSlices() / 10, ImageProcessor.BILINEAR);
//IJ.save(imp, new File(outdir, imp.getTitle()).getAbsolutePath());
		double gamma = 2; // 1.3;
//		ImagePlus imp = IJ.openImage("H:\\paper\\2017-Animation\\figures\\Fig3\\DD_lyse_1_still\\DD_lyse_1.subtracted.tif");
		imp.show();
		exportRGB(imp, new File(outdir, "xy"), gamma);

//		final ImagePlus yz = Turn.toZY(imp);
//		yz.show();
//		exportRGB(yz, new File(outdir, "zy"), gamma);
//
//		final ImagePlus xz = Turn.toXZ(imp);
//		xz.show();
//		exportRGB(xz, new File(outdir, "xz"), gamma);
	}

//	public static int[][] createRGB(ImagePlus imp,
//			float cmin, float cmax, float cgamma,
//			float amin, float amax, float agamma) {
//
//		int w = imp.getWidth();
//		int h = imp.getHeight();
//		int d = imp.getNSlices();
//		int[][] ret = new int[d][w * h];
//		for(int z = 0; z < d; z++) {
//			int idx = image.getStackIndex(image.getC(), z + 1, image.getT());
//			slices[z] = image.getStack().getProcessor(idx);
//		}
//	}

	public static void export(ImagePlus imp, File dir, double gamma) throws IOException {
		int type = imp.getType();
		if(type != ImagePlus.COLOR_RGB) {
			imp = imp.duplicate();
			new StackConverter(imp).convertToRGB();
		}
		exportRGB(imp, dir, gamma);
	}

	public static void exportRGB(final ImagePlus imp, final File dir, final double gamma) throws IOException {
		if(!dir.exists())
			dir.mkdirs();

		final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

		for(int iz = 0; iz < imp.getStackSize(); iz++) {
			final int z = iz;
			exec.submit(new Runnable() {
				@Override
				public void run() {
					int[] pixels = (int[])imp.getStack().getPixels(z + 1);
					int[] argb = new int[pixels.length];
					for(int i = 0; i < argb.length; i++) {
						int c = pixels[i];
						int r = (c&0xff0000)>>16;
						int g = (c&0xff00)>>8;
						int b = c&0xff;
						double v = (r + g + b) / 3.0;
						v = Math.pow(v / 255, gamma);
						// do opacity correction: https://stackoverflow.com/questions/12494439/opacity-correction-in-raycasting-volume-rendering
						// v = 1 - Math.pow(1 - v, 10);
						v = Math.max(0, Math.min(1, v));
						byte a = (byte)(255 * v);
						argb[i] = (((a&0xff) << 24) | ((r&0xff) << 16) | ((g&0xff) << 8) | (b&0xff));
					}
					BufferedImage bi = new BufferedImage(imp.getWidth(), imp.getHeight(), BufferedImage.TYPE_INT_ARGB);
					bi.getRaster().setDataElements(0, 0, imp.getWidth(), imp.getHeight(), argb);
					try {
						ImageIO.write(bi, "PNG", new File(dir, IJ.pad(z, 4) + ".png"));
					} catch(Exception e) {
						throw new RuntimeException("Cannot create RGB files", e);
					}
				}
			});
		}
		exec.shutdown();
		try {
			exec.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

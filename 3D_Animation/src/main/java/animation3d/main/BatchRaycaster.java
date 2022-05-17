package animation3d.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import animation3d.renderer3d.OpenCLRaycaster;
import animation3d.renderer3d.Renderer3D;
import animation3d.textanim.Animator;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class BatchRaycaster implements PlugInFilter {

	private ImagePlus image;

	public BatchRaycaster() {
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_16;
	}

	private static String loadText(String file) throws IOException {
		BufferedReader buf = new BufferedReader(new FileReader(file));
		String line;
		StringBuilder res = new StringBuilder();
		while((line = buf.readLine()) != null) {
			res.append(line).append("\n");
		}
		buf.close();
		return res.toString();
	}

	@Override
	public void run(ImageProcessor ip) {
		GenericDialogPlus gd = new GenericDialogPlus("");
		gd.addFileField("Animation file", "");
		gd.addNumericField("Output_width", image.getWidth(), 0);
		gd.addNumericField("Output_height", image.getHeight(), 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		String animationFile = gd.getNextString();
		int w = (int)gd.getNextNumber();
		int h = (int)gd.getNextNumber();


		render(image, animationFile, w, h, true);
	}

	public static ImagePlus render(ImagePlus input, String pathToAnimationScript, int outputWidth, int outputHeight) {
		return render(input, pathToAnimationScript, outputWidth, outputHeight, false);
	}

	public static ImagePlus render(ImagePlus input, String pathToAnimationScript, int outputWidth, int outputHeight, boolean autoShow) {
		String animation;
		try {
			animation = loadText(pathToAnimationScript);
		} catch(Exception e) {
			throw new RuntimeException("Error loading " + pathToAnimationScript, e);
		}

		Renderer3D renderer;
		try {
			renderer = new Renderer3D(input, input.getWidth(), input.getHeight());
			renderer.setTargetSize(outputWidth, outputHeight);
		} catch(UnsatisfiedLinkError e) {
			throw new RuntimeException("Either your graphics card doesn't support OpenCL "
					+ "or your drivers are not uptodate. Please install "
					+ "the newest drivers for your card and try again.", e);
		}

		Animator animator = new Animator(renderer, autoShow);
		try {
			animator.render(animation);
		} catch(Exception e) {
			throw new RuntimeException("Exception during rendering", e);
		}

		ImagePlus ret;
		try {
			ret = animator.waitForRendering(1, TimeUnit.HOURS);
		} catch (Exception e) {
			throw new RuntimeException("Exception during rendering", e);
		}
		OpenCLRaycaster.close();

		return ret;
	}

	public static void main(String... args) throws IOException {
		new ij.ImageJ();
		ImagePlus imp = IJ.openImage("D:\\flybrain.green.tif");
		imp.show();

		// render(imp, "d:/tmp.animation.txt", imp.getWidth(), imp.getHeight(), false).show();
		BatchRaycaster cr = new BatchRaycaster();
		cr.setup("", imp);
		cr.run(null);

	}
}

package povray;

import java.io.File;

import editor.AnimationEditor;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import textanim.Default3DRecordingProvider;

public class Main implements PlugInFilter {

	private ImagePlus image;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_ALL;
	}

	@Override
	public void run(ImageProcessor ip) {
		GenericDialogPlus gd = new GenericDialogPlus("Povray Animator");
		gd.addDirectoryField("Output_folder", "");
		gd.addNumericField("Target_width", 640, 0);
		gd.addNumericField("Target_height", 480, 0);
		gd.showDialog();

		if(gd.wasCanceled())
			return;

		String outputFolder = gd.getNextString();
		int tgtW = (int)gd.getNextNumber();
		int tgtH = (int)gd.getNextNumber();

		run(image, outputFolder, tgtW, tgtH);
	}

	public static void run(ImagePlus image, String outputFolder, int tgtW, int tgtH) {
		File outdir = new File(outputFolder);
		if(!outdir.exists())
			outdir.mkdirs();
		PovrayRenderer renderer = new PovrayRenderer(image, outdir, tgtW, tgtH);
		AnimationEditor editor = new AnimationEditor(renderer, Default3DRecordingProvider.getInstance());
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		new ij.ImageJ();
		ImagePlus imp = IJ.openImage("H:\\paper\\2017-Animation\\figures\\Fig3\\DD_lyse_1.subtracted.small.rgb.tif");
		imp.show();
		String outputFolder = "H:\\paper\\2017-Animation\\figures\\Fig3\\DD_lyse_1_movie";
		run(imp, outputFolder, 800, 600);
	}
}

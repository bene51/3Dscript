package animation3d.ext;

import animation3d.editor.AnimationEditor;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Main implements PlugInFilter {

	private ImagePlus image;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_ALL;
	}

	@Override
	public void run(ImageProcessor ip) {
		run(image);
	}

	public static void run(ImagePlus image) {
		ExtRenderer renderer = new ExtRenderer(image);
		AnimationEditor editor = new AnimationEditor(renderer, null);
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		new ij.ImageJ();
		ij.IJ.run("AuPbSn 40 (56K)");
		Main.run(ij.IJ.getImage());
	}
}

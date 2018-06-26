package animation3d.ij3dviewer;

import animation3d.editor.AnimationEditor;
import animation3d.textanim.Default3DRecordingProvider;
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
		IJ3DRenderer renderer = new IJ3DRenderer(image);
		AnimationEditor editor = new AnimationEditor(renderer, Default3DRecordingProvider.getInstance());
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		ImagePlus imp = ij.IJ.openImage("D:\\flybrain.green.tif");
		Main.run(imp);
	}
}

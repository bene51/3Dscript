package animation3d.bdv;

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
		return DOES_8G | DOES_16;
	}

	@Override
	public void run(ImageProcessor ip) {
		// TODO
	}

	public static void run(ImagePlus imp) {
		// File xmlFile = new File("/Users/bene/Downloads/HisYFP-SPIM/downsampled/dataset.xml");
		BDVRenderer renderer;
		renderer = new BDVRenderer(imp);
		AnimationEditor editor = new AnimationEditor(renderer, Default3DRecordingProvider.getInstance());
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		new ij.ImageJ();
//		ImagePlus imp = ij.IJ.openImage("/Users/bene/flybrain.tif");
		ImagePlus imp = ij.IJ.openImage("/Users/bene/head.tif");
		// ImagePlus imp = ij.IJ.openImage("/Users/bene/Downloads/HisYFP-SPIM/downsampled/fused.tif");
//		imp.getCalibration().pixelWidth = 2;
//		imp.getCalibration().pixelHeight = 2;
//		imp.getCalibration().pixelDepth = 4;
		imp.show();
		Main.run(imp);
	}
}

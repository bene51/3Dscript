package animation3d.bdv;

import java.io.File;

import animation3d.editor.AnimationEditor;
import animation3d.textanim.Default3DRecordingProvider;
import ij.IJ;
import ij.plugin.PlugIn;
import mpicbg.spim.data.SpimDataException;

public class Main implements PlugIn {

	@Override
	public void run(String arg) {
		if ( ij.Prefs.setIJMenuBar )
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );
	}

	public static void run(File xmlFile) {
		BDVRenderer renderer;
		try {
			renderer = new BDVRenderer(xmlFile);
			AnimationEditor editor = new AnimationEditor(renderer, Default3DRecordingProvider.getInstance());
			editor.setVisible(true);
		} catch (SpimDataException e) {
			e.printStackTrace();
			IJ.handleException(e);
		}
	}

	public static void main(String[] args) {
		if ( ij.Prefs.setIJMenuBar )
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		new ij.ImageJ();
//		ImagePlus imp = ij.IJ.openImage("/Users/bene/flybrain.tif");
//		imp.getCalibration().pixelWidth = 2;
//		imp.getCalibration().pixelHeight = 2;
//		imp.getCalibration().pixelDepth = 4;
//		imp.show();
//		ImagePlus imp = ij.IJ.openImage("/Users/bene/head.tif");
//		ImagePlus imp = ij.IJ.openImage("/Users/bene/Downloads/HisYFP-SPIM/downsampled/fused.tif");
		File xmlFile = new File("/Users/bene/Downloads/HisYFP-SPIM/downsampled/dataset.xml");
		// File xmlFile = new File("/Users/bene/fijiplugins/3Dscript/export.xml");
		Main.run(xmlFile);
	}
}

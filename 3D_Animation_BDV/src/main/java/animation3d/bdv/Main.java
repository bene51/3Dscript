package animation3d.bdv;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import animation3d.editor.AnimationEditor;
import animation3d.textanim.Default3DRecordingProvider;
import bdv.BigDataViewer;
import bdv.ij.util.ProgressWriterIJ;
import bdv.viewer.ViewerOptions;
import ij.IJ;
import ij.Prefs;
import ij.plugin.PlugIn;
import mpicbg.spim.data.SpimDataException;

public class Main implements PlugIn {

	static String lastDatasetPath = "./export.xml";

	// Parts are copied from https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/BigDataViewerPlugIn.java
	@Override
	public void run(String arg) {
		if (ij.Prefs.setIJMenuBar)
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		File file = null;

		if (Prefs.useJFileChooser) {
			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setSelectedFile(new File(lastDatasetPath));
			fileChooser.setFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "xml files";
				}

				@Override
				public boolean accept(final File f) {
					if (f.isDirectory())
						return true;
					if (f.isFile()) {
						final String s = f.getName();
						final int i = s.lastIndexOf('.');
						if (i > 0 && i < s.length() - 1) {
							final String ext = s.substring(i + 1).toLowerCase();
							return ext.equals("xml");
						}
					}
					return false;
				}
			});

			final int returnVal = fileChooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION)
				file = fileChooser.getSelectedFile();
		}
		else { // use FileDialog
			final FileDialog fd = new FileDialog((Frame) null, "Open", FileDialog.LOAD);
			fd.setDirectory(new File(lastDatasetPath).getParent());
			fd.setFile(new File(lastDatasetPath).getName());
			final AtomicBoolean workedWithFilenameFilter = new AtomicBoolean(false);
			fd.setFilenameFilter(new FilenameFilter() {
				private boolean firstTime = true;

				@Override
				public boolean accept(final File dir, final String name) {
					if (firstTime) {
						workedWithFilenameFilter.set(true);
						firstTime = false;
					}

					final int i = name.lastIndexOf('.');
					if (i > 0 && i < name.length() - 1) {
						final String ext = name.substring(i + 1).toLowerCase();
						return ext.equals("xml");
					}
					return false;
				}
			});
			fd.setVisible(true);
			if (isMac() && !workedWithFilenameFilter.get()) {
				fd.setFilenameFilter(null);
				fd.setVisible(true);
			}
			final String filename = fd.getFile();
			if (filename != null) {
				file = new File(fd.getDirectory() + filename);
			}
		}

		if (file != null) {
			try {
				lastDatasetPath = file.getAbsolutePath();
				BigDataViewer.open(file.getAbsolutePath(), file.getName(), new ProgressWriterIJ(),
						ViewerOptions.options());
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private boolean isMac() {
		final String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		return (OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0);
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

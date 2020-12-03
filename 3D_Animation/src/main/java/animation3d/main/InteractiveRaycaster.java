package animation3d.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import animation3d.editor.AnimationEditor;
import animation3d.gui.AnimationPanel;
import animation3d.gui.AnimatorDialog;
import animation3d.gui.Bookmark;
import animation3d.gui.BookmarkPanel;
import animation3d.gui.ContrastPanel;
import animation3d.gui.CroppingPanel;
import animation3d.gui.OutputPanel;
import animation3d.gui.RenderingThread;
import animation3d.gui.TransformationPanel;
import animation3d.renderer3d.ExtendedRenderingState;
import animation3d.renderer3d.RecordingProvider;
import animation3d.renderer3d.Renderer3D;
import animation3d.renderer3d.RenderingAlgorithm;
import animation3d.textanim.Animator;
import animation3d.textanim.CombinedTransform;
import animation3d.util.Transform;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/*
 * TODOs
 * - export mp4
 * - set current settings when importing an animation
 * - show progress when recording an animation
 * - increase near and far
 * - cancel recording
 * - record clipping in %
 * - if some property is not set, use the current setting (and not 0).
 *
 */
public class InteractiveRaycaster implements PlugInFilter {

	private static List<InteractiveRaycaster> instances;

	private ImagePlus image;
	private double[] min, max;
	private int[][] histo8;

	private AnimatorDialog dialog;
	private ContrastPanel contrastPanel;
	private TransformationPanel transformationPanel;
	private CroppingPanel croppingPanel;
	private BookmarkPanel bookmarkPanel;
	private OutputPanel outputPanel;
	private AnimationPanel animationPanel;

	private Renderer3D renderer;
	private RenderingThread worker;

	private ImagePlus outputImage;

	public InteractiveRaycaster() {
		if(instances == null)
			instances = new ArrayList<InteractiveRaycaster>(3);
		instances.add(0, this);
	}

	public static InteractiveRaycaster getActiveRaycaster() {
		return instances.get(0);
	}

	public ImagePlus getImage() {
		return image;
	}

	void setAsActiveRaycaster() {
		InteractiveRaycaster active = getActiveRaycaster();
		if(this.equals(active))
			return;
		if(active != null)
			instances.remove(this);
		instances.add(0, this);
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_16;
	}

	private void setLookAndFeel() {
		if ( IJ.isMacOSX() || IJ.isWindows() )
		{
			try
			{
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( final ClassNotFoundException e )
			{
				e.printStackTrace();
			}
			catch ( final InstantiationException e )
			{
				e.printStackTrace();
			}
			catch ( final IllegalAccessException e )
			{
				e.printStackTrace();
			}
			catch ( final UnsupportedLookAndFeelException e )
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run(ImageProcessor ip) {
		setLookAndFeel();
		calculateChannelMinAndMax();

		try {
			renderer = new Renderer3D(image, image.getWidth(), image.getHeight());
		} catch(UnsatisfiedLinkError e) {
			IJ.handleException(e);
			IJ.error("Either your graphics card doesn't support OpenCL "
					+ "or your drivers are not uptodate. Please install "
					+ "the newest drivers for your card and try again.");
			return;
		}
		ExtendedRenderingState rs = renderer.getRenderingState();
		worker = new RenderingThread(renderer);

		outputImage = worker.getOutputImage();

		dialog = new AnimatorDialog("Interactive Raycaster", null);
		WindowManager.addWindow(dialog);
		contrastPanel = dialog.addContrastPanel(
				histo8,
				min, max,
				rs.clone().getChannelProperties(),
				Color.BLACK);

		transformationPanel = dialog.addTransformationPanel(0, 0, 0, 0, 0, 0, 1);

		croppingPanel = dialog.addCroppingPanel(image);
		for(int c = 0; c < image.getNChannels(); c++) {
			rs.setChannelProperty(c, ExtendedRenderingState.NEAR, croppingPanel.getNear());
			rs.setChannelProperty(c, ExtendedRenderingState.FAR,  croppingPanel.getFar());
		}

		bookmarkPanel = dialog.addBookmarkPanel(renderer);

		outputPanel = dialog.addOutputPanel(
				outputImage.getWidth(), outputImage.getHeight(), 1,
				renderer.getBoundingBox(),
				renderer.getScalebar());

		animationPanel = dialog.addAnimationPanel();

		final Point mouseDown = new Point();
		final ExtendedRenderingState mouseDownFrame = rs.clone();
		final boolean[] isRotation = new boolean[] {false};

		final ImageCanvas canvas = outputImage.getCanvas();
		final MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setAsActiveRaycaster();
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				mouseDown.setLocation(e.getPoint());
				isRotation[0] = !e.isShiftDown();
				mouseDownFrame.setFrom(renderer.getRenderingState());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				setAsActiveRaycaster();
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				int dx = e.getX() - mouseDown.x;
				int dy = e.getY() - mouseDown.y;
				if(!isRotation[0]) {
					ExtendedRenderingState kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.translateBy(dx, dy, 0, true);
					push(kf);
					transformationPanel.setTransformation(t.guessEulerAnglesDegree(), t.getTranslation(), t.getScale());
				}
				else {
					float speed = 0.7f;
					if(e.isAltDown()) {
						if(Math.abs(dx) > Math.abs(dy))
							dy = 0;
						else
							dx = 0;
					}
					int ax = -Math.round(dx * speed);
					int ay =  Math.round(dy * speed);
					ExtendedRenderingState kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.rotateBy(ax, ay);
					push(kf);
					transformationPanel.setTransformation(t.guessEulerAnglesDegree(), t.getTranslation(), t.getScale());
				}
			}
		};
		canvas.addMouseListener(mouseListener);

		final MouseMotionListener mouseMotionListener = new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				setAsActiveRaycaster();
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;

				int dx = e.getX() - mouseDown.x;
				int dy = e.getY() - mouseDown.y;
				// translation
				if(!isRotation[0]) {
					ExtendedRenderingState kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.translateBy(dx, dy, 0, true);
					push(kf);
				}
				// rotation
				else {
					float speed = 0.7f;
					if(e.isAltDown()) {
						if(Math.abs(dx) > Math.abs(dy))
							dy = 0;
						else
							dx = 0;
					}
					int ax = -Math.round(dx * speed);
					int ay =  Math.round(dy * speed);
					ExtendedRenderingState kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.rotateBy(ax, ay);
					push(kf);

					IJ.showStatus(ax + "\u00B0" + ", " + ay + "\u00B0");
					// transformationPanel.setTransformation(t.guessEulerAnglesDegree(), t.getTranslation(), t.getScale());
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {}
		};
		canvas.addMouseMotionListener(mouseMotionListener);

		final MouseWheelListener mouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				setAsActiveRaycaster();
				int units = e.getWheelRotation();
				float factor = 1 + units * 0.2f;

				int ex = e.getX();
				int ey = e.getY();

				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				CombinedTransform t = kf.getFwdTransform();
				t.zoomInto(ex, ey, factor);
				push(kf);

				transformationPanel.setTransformation(t.guessEulerAnglesDegree(), t.getTranslation(), t.getScale());
			}
		};
		canvas.addMouseWheelListener(mouseWheelListener);

		contrastPanel.addContrastPanelListener(new ContrastPanel.Listener() {
			@Override
			public void intensityChanged(int channel, double min, double max, double gamma) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setIntensity(channel, min, max, gamma);
				push(kf, -1, -1);
			}

			@Override
			public void alphaChanged(int channel, double min, double max, double gamma) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setAlpha(channel, min, max, gamma);
				push(kf, -1, -1);
			}

			@Override
			public void lightsChanged(int channel, boolean use, double kObj, double kDiff, double kSpec, double shininess) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setLight(channel, use, kObj, kDiff, kSpec, shininess);
				push(kf, -1, -1);
			}

			@Override
			public void weightsChanged(int channel, double weight) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setChannelProperty(channel, ExtendedRenderingState.WEIGHT, weight);
				push(kf, -1, -1);
			}

			@Override
			public void colorChanged(int channel, Color color) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setColor(channel, color);
				push(kf, -1, -1);
			}

			@Override
			public void channelChanged() {
				setAsActiveRaycaster();
				int channel = contrastPanel.getChannel();
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				croppingPanel.setBoundingBox(
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_XMIN),
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_YMIN),
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_ZMIN),
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_XMAX),
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_YMAX),
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_ZMAX));
				croppingPanel.setNearAndFar(
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.NEAR),
						(int)kf.getChannelProperty(channel, ExtendedRenderingState.FAR));
			}

			@Override
			public void renderingSettingsReset() {
				resetRenderingSettings();
			}

			@Override
			public void renderingAlgorithmChanged(RenderingAlgorithm algorithm) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setRenderingAlgorithm(algorithm);
				push(kf);
			}

			@Override
			public void backgroundChanged(Color bg) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setBackgroundColor(bg.getRed(), bg.getGreen(), bg.getBlue());
				push(kf);
			}
		});

		transformationPanel.addTransformationPanelListener(new TransformationPanel.Listener() {
			@Override
			public void transformationChanged(float ax, float ay, float az, float dx, float dy, float dz, float s) {
				setTransformation(ax, ay, az, dx, dy, dz, s);
			}

			@Override
			public void resetTransformation() {
				InteractiveRaycaster.this.resetTransformation();
			}
		});

		croppingPanel.addCroppingPanelListener(new CroppingPanel.Listener() {
			private int[] getChannels() {
				boolean allChannels = croppingPanel.applyToAllChannels();
				if(!allChannels)
					return new int[] {contrastPanel.getChannel()};

				int[] channels = new int[image.getNChannels()];
				for(int i = 0; i < channels.length; i++)
					channels[i] = i;
				return channels;
			}

			@Override
			public void nearFarChanged(int near, int far) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				for(int channel : getChannels()) {
					kf.setChannelProperty(channel, ExtendedRenderingState.NEAR, near);
					kf.setChannelProperty(channel, ExtendedRenderingState.FAR,  far);
				}
				push(kf);
			}

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				for(int channel : getChannels()) {
					kf.setChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_XMIN, bbx0);
					kf.setChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_YMIN, bby0);
					kf.setChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_ZMIN, bbz0);
					kf.setChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_XMAX, bbx1);
					kf.setChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_YMAX, bby1);
					kf.setChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_ZMAX, bbz1);
				}
				push(kf);
			}

			@Override
			public void cutOffROI() {
				Roi roi = outputImage.getRoi();
				if(roi != null) {
					boolean allChannels = croppingPanel.applyToAllChannels();
					ByteProcessor mask = new ByteProcessor(outputImage.getWidth(), outputImage.getHeight());
					mask.setValue(255);
					if(roi instanceof TextRoi)
						mask.draw(roi);
					else
						mask.fill(roi);
					mask.resetRoi();
					mask.invert();

					ExtendedRenderingState kf = renderer.getRenderingState();
					float[] fwd = kf.getFwdTransform().calculateForwardTransform();
					if(allChannels) {
						for(int c = 0; c < image.getNChannels(); c++)
							renderer.crop(image, c, mask, fwd);
					}
					else
						renderer.crop(image, contrastPanel.getChannel(), mask, fwd);

					push(kf);
				}
				else {
					IJ.error("Selection required");
				}
			}
		});

		bookmarkPanel.addBookmarkPanelListener(new BookmarkPanel.Listener() {
			@Override
			public void gotoBookmark(Bookmark bookmark) {
				ExtendedRenderingState rs = bookmark.getRenderingState();
				push(rs);
				setGUIFromRenderingState(rs);
			}
		});

		outputPanel.addOutputPanelListener(new OutputPanel.Listener() {
			@Override
			public void outputWidthChanged(int tgtW) {
				int tgtH = renderer.getTargetHeight(); // tgtW * image.getHeight() / image.getWidth();
				setOutputSize(tgtW, tgtH);
				outputPanel.setOutputSize(tgtW, tgtH);
			}

			@Override
			public void outputHeightChanged(int tgtH) {
				int tgtW = renderer.getTargetWidth(); // tgtH * image.getWidth() / image.getHeight();
				setOutputSize(tgtW, tgtH);
				outputPanel.setOutputSize(tgtW, tgtH);
			}

			@Override
			public void boundingBoxChanged() {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setBoundingboxProperties(renderer.getBoundingBox());
				push(kf);
			}

			@Override
			public void scalebarChanged() {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setScalebarProperties(renderer.getScalebar());
				push(kf);
			}
		});

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("closing");
				worker.shutdown();
				canvas.removeMouseListener(mouseListener);
				canvas.removeMouseMotionListener(mouseMotionListener);
				canvas.removeMouseWheelListener(mouseWheelListener);
				WindowManager.removeWindow(dialog);
				instances.remove(this);
			}
		});

		animationPanel.addTimelineListener(new AnimationPanel.Listener() {
			@Override
			public void textBasedAnimation() {
				startTextBasedAnimation();
			}
		});


		dialog.setModal(false);
		dialog.pack();
		dialog.showDialog();

		Dimension outsize = new Dimension(image.getWidth(), image.getHeight());
		double mag = image.getCanvas().getMagnification();
		outsize.width = (int)Math.round(outsize.width * mag);
		outsize.height = (int)Math.round(outsize.height * mag);

		outputPanel.setOutputSize(outsize.width, outsize.height);

		setOutputSize(outsize.width, outsize.height);

		Toolbar.getInstance().setTool(Toolbar.HAND);

//		ExtendedRenderingState tmp = renderer.getRenderingState().clone();
//		tmp.setChannelProperty(1, ExtendedRenderingState.USE_LUT, 1);
//		push(tmp);

//		ImageProcessor bg = IJ.openImage("D:\\PSoteloHitschfeld\\cover\\bg2.tif").getProcessor();
//		ColorProcessor cp = bg.convertToColorProcessor();
//		renderer.setBackground(cp);
	}

	private void push() {
		push(renderer.getRenderingState(), -1, -1);
	}

	private void push(ExtendedRenderingState rs) {
		push(rs, -1, -1);
	}

	private void push(int w, int h) {
		push(renderer.getRenderingState(), w, h);
	}

	private void push(ExtendedRenderingState rs, int w, int h) {
		setAsActiveRaycaster();
		worker.push(rs, w, h);
	}

	public void setOutputSize(int tgtW, int tgtH) {
		renderer.setTargetSize(tgtW, tgtH);
		Calibration cal = outputImage.getCalibration();
		CombinedTransform trans = renderer.getRenderingState().getFwdTransform();
		trans.adjustOutputCalibration(cal);
		renderer.getScalebar().setDefaultLength(trans.getOutputSpacing()[0] / trans.getScale());
		push(tgtW, tgtH);
	}

	public void setZStep(float zStep) {
		renderer.getRenderingState().getFwdTransform().setZStep(zStep);
		push();
	}

	public void resetRenderingSettings() {
		ExtendedRenderingState rs = renderer.getRenderingState().clone();
		renderer.resetRenderingSettings(rs);
		contrastPanel.setChannel(contrastPanel.getChannel());
		contrastPanel.setRenderingSettings(rs.clone().getChannelProperties());
		contrastPanel.setRenderingAlgorithm(rs.getRenderingAlgorithm());
		push(rs, -1, -1);
	}

	public void setTransformation(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		ExtendedRenderingState kf = renderer.getRenderingState().clone();
		kf.getFwdTransform().setTransformation(ax, ay, az, dx, dy, dz, s);
		push(kf);
		transformationPanel.setTransformation(new float[] {ax, ay, az}, new float[] {dx, dy, dz}, s);
	}

	public void resetTransformation() {
		setTransformation(0, 0, 0, 0, 0, 0, 1);
	}

	public void setGUIFromRenderingState(ExtendedRenderingState rs) {
		// Contrast Panel
		this.contrastPanel.setRenderingSettings(rs.getChannelProperties());
		this.contrastPanel.setBackground(rs.getBackgroundColor());
		this.contrastPanel.setRenderingAlgorithm(rs.getRenderingAlgorithm());

		// Transformation Panel
		CombinedTransform t = rs.getFwdTransform();
		transformationPanel.setTransformation(t.guessEulerAnglesDegree(), t.getTranslation(), t.getScale());

		int channel = contrastPanel.getChannel();
		// Cropping Panel
		int bbx0 = (int)rs.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_XMIN);
		int bby0 = (int)rs.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_YMIN);
		int bbz0 = (int)rs.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_ZMIN);
		int bbx1 = (int)rs.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_XMAX);
		int bby1 = (int)rs.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_YMAX);
		int bbz1 = (int)rs.getChannelProperty(channel, ExtendedRenderingState.BOUNDINGBOX_ZMAX);
		croppingPanel.setBoundingBox(bbx0, bby0, bbz0, bbx1, bby1, bbz1);

		int near = (int)rs.getChannelProperty(channel, ExtendedRenderingState.NEAR);
		int far  = (int)rs.getChannelProperty(channel, ExtendedRenderingState.FAR);
		croppingPanel.setNearAndFar(near, far);

		// TODO Output Panel, once its options are included in the RenderingState
		outputPanel.updateGui();
	}

	public void startTextBasedAnimation() {
		setAsActiveRaycaster();
		Animator animator = new Animator(renderer);
		animator.addAnimationListener(new Animator.Listener() {
			@Override
			public void animationFinished(ImagePlus result) {
				setGUIFromRenderingState(renderer.getRenderingState());
			}
		});
		AnimationEditor editor = new AnimationEditor(renderer, animator, RecordingProvider.getInstance());
		JMenuBar mbar = editor.getJMenuBar();
		JMenu extrasMenu = new JMenu("Extras");
		extrasMenu.setMnemonic(KeyEvent.VK_E);
		JMenuItem createIJ1Macro = new JMenuItem("Create IJ1 Macro");
		createIJ1Macro.addActionListener(e -> {
			int ow = renderer.getTargetWidth();
			int oh = renderer.getTargetHeight();
			float scalebar = renderer.getScalebar().isVisible() ? renderer.getScalebar().getLength() : 0;
			String boundingbox = renderer.getBoundingBox().isVisible() ? "bounding_box" : "";
			RenderingAlgorithm ra = contrastPanel.getRenderingAlgorithm();
			String algo = null;
			switch(ra) {
			case COMBINED_TRANSPARENCY:    algo = "Combined transparency"; break;
			case INDEPENDENT_TRANSPARENCY: algo = "Independent transparency"; break;
			default: algo = "Maximum intensity projection"; break;
			}

			String text = editor.getTab().getEditorPane().getText();
			String[] lines = text.split("\\r?\\n");
			String macro =
"function makeAnimation() {\n" +
"	return \"\"";
for(String line : lines) {
	macro = macro + " +\n\"\t" + line + "\\n\"";
}
macro = macro +
";\n}\n" +
"path = getDirectory(\"temp\") + \"xyz.animation.txt\";\n" +
"File.saveString(makeAnimation(), path);\n" +
"run(\"Batch Animation\",\n" +
"	\"animation=\" + path + \" \" +\n" +
"	\"output_width=" + ow + " output_height=" + oh + "\");";
			Path tmp = null;
			try {
				tmp = Files.createTempFile("ijmacro", ".ijm");
				Files.write(tmp, macro.getBytes());
			} catch(Exception ex) {
				IJ.handleException(ex);
				return;
			}
			IJ.open(tmp.toString());
		});

		extrasMenu.add(createIJ1Macro);
		mbar.add(extrasMenu);
		editor.setVisible(true);
	}

	private void calculateChannelMinAndMax() {
		int nC = image.getNChannels();
		min = new double[nC];
		max = new double[nC];
		histo8 = new int[nC][];

		for(int c = 0; c < nC; c++) {
			min[c] = Double.POSITIVE_INFINITY;
			max[c] = Double.NEGATIVE_INFINITY;
			for(int z = 0; z < image.getNSlices(); z++) {
				int idx = image.getStackIndex(c + 1, z + 1, image.getT());
				ImageProcessor ip = image.getStack().getProcessor(idx);
				ImageStatistics stat = ImageStatistics.getStatistics(ip, ImageStatistics.MIN_MAX, null);
				min[c] = Math.min(min[c], stat.min);
				max[c] = Math.max(max[c], stat.max);
			}
			int wh = image.getWidth() * image.getHeight();
			int nBins = 256;
			histo8[c] = new int[nBins];
			double scale = nBins / (max[c] - min[c]);
			for(int z = 0; z < image.getNSlices(); z++) {
				int idx = image.getStackIndex(c + 1, z + 1, image.getT());
				ImageProcessor ip = image.getStack().getProcessor(idx);
				for(int i = 0; i < wh; i++) {
					float v = ip.getf(i);
					int index = (int)(scale * (v - min[c]));
					if(index >= nBins)
						index = nBins-1;
					histo8[c][index]++;
				}
			}
		}
	}

	public static void main(String... args) throws IOException {
//		new ij.ImageJ();
//		URL url = ClassLoader.getSystemClassLoader().getResource("eye.png");
//		Image image = ImageIO.read(url);
//		new ImagePlus("", image).show();
//		if(true)
//			return;

//		new ij.ImageJ();
//
//		for(int t = 1; t <= 12; t++) {
//			ImagePlus imp2 = IJ.openImage("D:\\PSoteloHitschfeld\\" + IJ.pad(t,  2) + ".tif");
//			imp2.duplicate().show();
//
//			for(int z = 0; z < imp2.getNSlices(); z++) {
//				int i = imp2.getStackIndex(1, z + 1, 1);
//				ImageProcessor ip = imp2.getStack().getProcessor(i);
//				for(int y = 0; y < ip.getHeight(); y++) {
//					for(int x = 0; x < ip.getWidth(); x++) {
//						float v = ip.getf(x, y);
//						v = v * (1 + x * 1f / 512f);
//						ip.setf(x, y, v);
//					}
//				}
//			}
//			IJ.save(imp2, "d:\\PSoteloHitschfeld\\new\\" + IJ.pad(t, 2) + ".tif");
//		}
//
//		if(true)
//			return;
		float[] m = new float[12];
		double[] p = new double[] {
				90 * Math.random(),
				90 * Math.random(),
				90 * Math.random(),
		};
		System.out.println(Arrays.toString(p));
		Transform.fromEulerAngles(m, new double[] {
				p[0] * Math.PI / 180,
				p[1] * Math.PI / 180,
				p[2] * Math.PI / 180});
		Transform.guessEulerAngles(m, p);
		for(int i = 0; i < 3; i++)
			p[i] = 180 * p[i] / Math.PI;
		System.out.println(Arrays.toString(p));
		new ij.ImageJ();
		ImagePlus imp = null;
		imp = IJ.openImage("d:\\flybrain.tif");
		imp.show();

		InteractiveRaycaster cr = new InteractiveRaycaster();
		cr.setup("", imp);
		cr.run(null);
	}
}

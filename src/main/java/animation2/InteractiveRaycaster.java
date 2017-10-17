package animation2;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;

import editor.AnimationEditor;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import renderer3d.ExtendedRenderingState;
import renderer3d.OpenCLProgram;
import renderer3d.RecordingProvider;
import renderer3d.Renderer3D;
import renderer3d.RenderingAlgorithm;
import renderer3d.Transform;
import textanim.CombinedTransform;

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

	private static InteractiveRaycaster instance;

	private ImagePlus image;
	private double[] min, max;
	private int[][] histo8;

	private AnimatorDialog dialog;
	private ContrastPanel contrastPanel;
	private TransformationPanel transformationPanel;
	private CroppingPanel croppingPanel;
	private OutputPanel outputPanel;
	private AnimationPanel animationPanel;

	private Renderer3D renderer;
	private RenderingThread worker;

	public InteractiveRaycaster() {
		instance = this;
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_16;
	}

	@Override
	public void run(ImageProcessor ip) {
		calculateChannelMinAndMax();

		renderer = new Renderer3D(image, image.getWidth(), image.getHeight());
		ExtendedRenderingState rs = renderer.getRenderingState();
		worker = new RenderingThread(renderer);

		dialog = new AnimatorDialog("Interactive Raycaster", worker.out.getWindow());
		contrastPanel = dialog.addContrastPanel(
				histo8,
				min, max,
				renderer.getRenderingState().getChannelProperties());

		transformationPanel = dialog.addTransformationPanel(0, 0, 0, 0, 0, 0, 1);

		croppingPanel = dialog.addCroppingPanel(image);
		rs.setNonchannelProperty(ExtendedRenderingState.NEAR, croppingPanel.getNear());
		rs.setNonchannelProperty(ExtendedRenderingState.FAR,  croppingPanel.getFar());

		outputPanel = dialog.addOutputPanel(worker.out.getWidth(), worker.out.getHeight(), 1, renderer.getBoundingBox());

		animationPanel = dialog.addAnimationPanel();

		final Point mouseDown = new Point();
		final ExtendedRenderingState mouseDownFrame = rs.clone();
		final boolean[] isRotation = new boolean[] {false};

		final ImageCanvas canvas = worker.out.getCanvas();
		final MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				mouseDown.setLocation(e.getPoint());
				isRotation[0] = !e.isShiftDown();
				mouseDownFrame.setFrom(renderer.getRenderingState());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				int dx = e.getX() - mouseDown.x;
				int dy = e.getY() - mouseDown.y;
				if(!isRotation[0]) {
					ExtendedRenderingState kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.translateBy(dx, dy, 0, true);
					push(kf);
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
				}
			}
		};
		canvas.addMouseListener(mouseListener);

		final MouseMotionListener mouseMotionListener = new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
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
					transformationPanel.setTransformation(t.guessEulerAnglesDegree(), t.getTranslation(), t.getScale());
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {}
		};
		canvas.addMouseMotionListener(mouseMotionListener);

		final MouseWheelListener mouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
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
			public void renderingSettingsChanged() {
				push();
			}

			@Override
			public void channelChanged() {}

			@Override
			public void renderingSettingsReset() {
				resetRenderingSettings();
			}

			@Override
			public void renderingAlgorithmChanged(RenderingAlgorithm algorithm) {
				int nChannels = renderer.getNChannels();
				switch(algorithm) {
				case INDEPENDENT_TRANSPARENCY:
					renderer.setProgram(OpenCLProgram.makeSource(nChannels, false, false));
					break;
				case COMBINED_TRANSPARENCY:
					renderer.setProgram(OpenCLProgram.makeSource(nChannels, false, true));
					break;
				case MAXIMUM_INTENSITY:
					renderer.setProgram(OpenCLProgram.makeSourceForMIP(nChannels, false));
					break;
				}
				push();
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
			@Override
			public void nearFarChanged(int near, int far) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setNonchannelProperty(ExtendedRenderingState.NEAR, near);
				kf.setNonchannelProperty(ExtendedRenderingState.FAR,  far);
				push(kf);
			}

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				ExtendedRenderingState kf = renderer.getRenderingState().clone();
				kf.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMIN, bbx0);
				kf.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMIN, bby0);
				kf.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMIN, bbz0);
				kf.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMAX, bbx1);
				kf.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMAX, bby1);
				kf.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMAX, bbz1);
				push(kf);
			}

			@Override
			public void cutOffROI() {
				Roi roi = worker.out.getRoi();
				if(roi != null) {
					ByteProcessor mask = new ByteProcessor(worker.out.getWidth(), worker.out.getHeight());
					mask.setValue(255);
					if(roi instanceof TextRoi)
						mask.draw(roi);
					else
						mask.fill(roi);
					mask.resetRoi();
					mask.invert();

					ExtendedRenderingState kf = renderer.getRenderingState();
					float[] fwd = kf.getFwdTransform().calculateForwardTransform();
					renderer.crop(image, mask, fwd);

					push(kf);
				}
				else {
					IJ.error("Selection required");
				}
			}
		});

		outputPanel.addOutputPanelListener(new OutputPanel.Listener() {
			@Override
			public void outputSizeChanged(int tgtW, int tgtH, float zStep) {
				setOutputSize(tgtW, tgtH);
				setZStep(zStep);
			}

			@Override
			public void boundingBoxChanged() {
				push();
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

		outputPanel.setOutputSize(outsize.width, outsize.height, 1);

		setOutputSize(outsize.width, outsize.height);

		Toolbar.getInstance().setTool(Toolbar.HAND);

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
		push(renderer.getRenderingState(), -1, -1);
	}

	private void push(ExtendedRenderingState rs, int w, int h) {
		worker.push(rs, w, h, -1);
	}

	public void setOutputSize(int tgtW, int tgtH) {
		renderer.setTargetSize(tgtW, tgtH);
		Calibration cal = worker.out.getCalibration();
		renderer.getRenderingState().getFwdTransform().adjustOutputCalibration(cal);
		push(tgtW, tgtH);
	}

	public void setZStep(float zStep) {
		renderer.getRenderingState().getFwdTransform().setZStep(zStep);
		push();
	}

	public void resetRenderingSettings() {
		renderer.resetRenderingSettings();
		push();
		contrastPanel.setChannel(contrastPanel.getChannel());
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

	public void startTextBasedAnimation() {
		AnimationEditor editor = new AnimationEditor(renderer, RecordingProvider.getInstance());
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
		String dir = "D:\\VLanger\\20161205-Intravital-Darm\\";
		String name = "cy5-shg-2p-maus3919-gecleart-20x-big-stack1.resampled.tif";
		// ImagePlus imp = IJ.openImage(dir + name);
		ImagePlus imp = null;
		// imp = IJ.openImage("/Users/bene/flybrain.tif");
		// imp = IJ.openImage("D:\\PTripal\\20161109\\wonderbear_C7-wt-PBS-Axiocam_2.background.resampled.8-bit.tif");
		// imp = IJ.openImage("D:\\CKersten\\20170714\\J936 Composite.resampled.resampled.tif");
		imp = IJ.openImage("D:\\aklingberg\\170828_testpfote_063zoom_10z_14-35-29.resampled.tif");
		// ImagePlus imp = IJ.openImage("D:\\MHoffmann\\20160126-Markus2.small.tif");
		// ImagePlus imp = IJ.openImage("/Users/bene/flybrain.tif");
		imp.show();

		InteractiveRaycaster cr = new InteractiveRaycaster();
		cr.setup("", imp);
		cr.run(null);
	}
}

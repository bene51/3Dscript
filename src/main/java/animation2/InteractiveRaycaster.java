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
import renderer3d.ExtendedKeyframe;
import renderer3d.RecordingProvider;
import renderer3d.Renderer3DAdapter;
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

	private Renderer3DAdapter renderer;
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

		final float zStep = 2;
		renderer = new Renderer3DAdapter(image, image.getWidth(), image.getHeight(), zStep);
		ExtendedKeyframe keyframe = renderer.getKeyframe();
		worker = new RenderingThread(renderer);

		dialog = new AnimatorDialog("Interactive Raycaster", worker.out.getWindow());
		contrastPanel = dialog.addContrastPanel(
				histo8,
				min, max,
				renderer.getKeyframe().getChannelProperties());

		transformationPanel = dialog.addTransformationPanel(0, 0, 0, 0, 0, 0, 1);

		croppingPanel = dialog.addCroppingPanel(image);
		keyframe.setNonchannelProperty(ExtendedKeyframe.NEAR, croppingPanel.getNear());
		keyframe.setNonchannelProperty(ExtendedKeyframe.FAR,  croppingPanel.getFar());

		boolean boundingBox = false; // TODO save in Prefs
		outputPanel = dialog.addOutputPanel(worker.out.getWidth(), worker.out.getHeight(), zStep, renderer.getBoundingBox());

		animationPanel = dialog.addAnimationPanel();

		final Point mouseDown = new Point();
		final ExtendedKeyframe mouseDownFrame = keyframe.clone();
		final boolean[] isRotation = new boolean[] {false};

		final ImageCanvas canvas = worker.out.getCanvas();
		final MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				mouseDown.setLocation(e.getPoint());
				isRotation[0] = !e.isShiftDown();
				mouseDownFrame.setFrom(renderer.getKeyframe());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				int dx = e.getX() - mouseDown.x;
				int dy = e.getY() - mouseDown.y;
				if(!isRotation[0]) {
					ExtendedKeyframe kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.translateBy(dx, dy, 0, true);
					worker.push(kf, -1, -1, -1);
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
					ExtendedKeyframe kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.rotateBy(ax, ay);
					worker.push(kf, -1, -1, -1);
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
					ExtendedKeyframe kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.translateBy(dx, dy, 0, true);
					worker.push(kf, -1, -1, -1);
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
					ExtendedKeyframe kf = mouseDownFrame.clone();
					CombinedTransform t = kf.getFwdTransform();
					t.rotateBy(ax, ay);
					worker.push(kf, -1, -1, -1);

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

				ExtendedKeyframe kf = renderer.getKeyframe().clone();
				CombinedTransform t = kf.getFwdTransform();
				t.zoomInto(ex, ey, factor);
				worker.push(kf, -1, -1, -1);

				transformationPanel.setTransformation(t.guessEulerAnglesDegree(), t.getTranslation(), t.getScale());
			}
		};
		canvas.addMouseWheelListener(mouseWheelListener);

		contrastPanel.addContrastPanelListener(new ContrastPanel.Listener() {
			@Override
			public void renderingSettingsChanged() {
				worker.push(renderer.getKeyframe(), -1, -1, -1);
			}

			@Override
			public void channelChanged() {}

			@Override
			public void renderingSettingsReset() {
				resetRenderingSettings();
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
				ExtendedKeyframe kf = renderer.getKeyframe().clone();
				kf.setNonchannelProperty(ExtendedKeyframe.NEAR, near);
				kf.setNonchannelProperty(ExtendedKeyframe.FAR,  far);
				worker.push(kf, -1, -1, -1);
			}

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				ExtendedKeyframe kf = renderer.getKeyframe().clone();
				kf.setNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_XMIN, bbx0);
				kf.setNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_YMIN, bby0);
				kf.setNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_ZMIN, bbz0);
				kf.setNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_XMAX, bbx1);
				kf.setNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_YMAX, bby1);
				kf.setNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_ZMAX, bbz1);
				worker.push(kf, -1, -1, -1);
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

					ExtendedKeyframe kf = renderer.getKeyframe();
					float[] fwd = kf.getFwdTransform().calculateForwardTransform();
					renderer.crop(image, mask, fwd);

					worker.push(kf, -1, -1, -1);
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
				worker.push(renderer.getKeyframe(), -1, -1, -1);
			}
		});

		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				System.out.println("closing");
				worker.shutdown(); // TODO check that this works
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

		outputPanel.setOutputSize(outsize.width, outsize.height, zStep);

		setOutputSize(outsize.width, outsize.height);
		setZStep(zStep);

		Toolbar.getInstance().setTool(Toolbar.HAND);

//		ImageProcessor bg = IJ.openImage("D:\\PSoteloHitschfeld\\cover\\bg2.tif").getProcessor();
//		ColorProcessor cp = bg.convertToColorProcessor();
//		renderer.setBackground(cp);
	}

	public void setOutputSize(int tgtW, int tgtH) {
		renderer.setTargetSize(tgtW, tgtH);

		Calibration cal = worker.out.getCalibration();
		renderer.getKeyframe().getFwdTransform().adjustOutputCalibration(cal);
		worker.push(renderer.getKeyframe(), tgtW, tgtH, -1);
	}

	public void setZStep(float zStep) {
		renderer.setTargetZStep(zStep);
		worker.push(renderer.getKeyframe(), -1, -1, -1);
	}

	public void resetRenderingSettings() {
		renderer.resetRenderingSettings();
		worker.push(renderer.getKeyframe(), -1, -1, -1);
		contrastPanel.setChannel(contrastPanel.getChannel());
	}

	public void setTransformation(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		ExtendedKeyframe kf = renderer.getKeyframe().clone();
		kf.getFwdTransform().setTransformation(ax, ay, az, dx, dy, dz, s);
		worker.push(kf, -1, -1, -1);
		transformationPanel.setTransformation(new float[] {ax, ay, az}, new float[] {dx, dy, dz}, s);
	}

	public void resetTransformation() {
		setTransformation(0, 0, 0, 0, 0, 0, 1);
	}

//	public void record(int from, int to, List<TransformationAnimation> animations, Timelines timelines) {
//		ImageStack stack = new ImageStack(worker.out.getWidth(), worker.out.getHeight());
//		ImagePlus anim = null;
//		Keyframe current = createKeyframe(from, croppingPanel, renderingSettings, rotation, translation, scale, nearfar);
//
//		for(int t = from; t <= to; t++) {
//			if(IJ.escapePressed())
//				break;
//			Keyframe k = timelines.getInterpolatedFrame(t, current);
//
//			float[] fwd = Transform.fromIdentity(null);
//			for(TransformationAnimation a : animations) {
//				float[] x = new float[12];
//				a.getTransformationAt(t, x);
//				fwd = Transform.mul(x, fwd);
//			}
//			fwd = Transform.mul(fwd, fromCalib);
//			fwd = Transform.mul(toTransform, fwd);
//
//			float[] inv = calculateInverseTransform(fwd);
//
//			worker.getRaycaster().setBBox(k.bbx0, k.bby0, k.bbz0, k.bbx1, k.bby1, k.bbz1);
//			if(image.getNFrames() > 1) {
//				int before = image.getT();
//				image.setT(t + 1);
//				if(image.getT() != before)
//					worker.getRaycaster().setImage(image);
//			}
//
//			stack.addSlice(worker.getRaycaster().renderAndCompose(fwd, inv, k.renderingSettings, k.near, k.far).getProcessor());
//			if(t == from + 1) {
//				anim = new ImagePlus(image.getTitle(), stack);
//				anim.setCalibration(worker.out.getCalibration().copy());
//				anim.show();
//			} else if(t > from + 1) {
//				anim.setSlice(t - from + 1);
//				anim.updateAndDraw();
//			}
//		}
//		IJ.resetEscape();
//	}

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

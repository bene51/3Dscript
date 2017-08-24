package animation2;

import java.awt.Color;
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
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import renderer3d.Keyframe;
import renderer3d.Transform;

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

	private RenderingThread worker;

	private float[] pdOut;

//	private float[] fromCalib;
//	private float[] toTransform;
//	private float[] nearfar;
//	private float[] scale;
//	private float[] translation;
//	private float[] rotation;
//	private float[] rotcenter;

//	private RenderingSettings[] renderingSettings;
//	private LUT[] luts;

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
//		luts = image.isComposite() ?
//				image.getLuts() : new LUT[] {image.getProcessor().getLut()};

//		final int nC = image.getNChannels();

		calculateChannelMinAndMax();

//		final float[] pd = new float[] {
//				(float)image.getCalibration().pixelWidth,
//				(float)image.getCalibration().pixelHeight,
//				(float)image.getCalibration().pixelDepth
//		};
//
//		fromCalib = Transform.fromCalibration(pd[0], pd[1], pd[2], 0, 0, 0, null);
//
//		pdOut = new float[] {pd[0], pd[0], pd[0]}; // TODO phOut
//
//		toTransform = Transform.fromCalibration(
//				pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
//		Transform.invert(toTransform);
//
//		nearfar = new float[] {0, 0};
//		scale = new float[] {1};
//		translation = new float[3];
//		rotation = Transform.fromIdentity(null);
//		rotcenter = new float[] {
//				image.getWidth()   * pd[0] / 2,
//				image.getHeight()  * pd[1] / 2,
//				image.getNSlices() * pd[2] / 2};
//
//		renderingSettings = new RenderingSettings[nC];
//		for(int c = 0; c < nC; c++) {
//			renderingSettings[c] = new RenderingSettings(
//					(float)luts[c].min, (float)luts[c].max, 1,
//					(float)luts[c].min, (float)luts[c].max, 2);
//		}
		final float zStep = 2;
		worker = new RenderingThread(
				image,
				renderingSettings,
				Transform.fromIdentity(null),
				Transform.fromIdentity(null), nearfar, zStep);

		dialog = new AnimatorDialog("Interactive Raycaster", worker.out.getWindow());
		contrastPanel = dialog.addContrastPanel(histo8, getLUTColors(luts), min, max, renderingSettings);

		transformationPanel = dialog.addTransformationPanel(0, 0, 0, 0, 0, 0, 1);

		croppingPanel = dialog.addCroppingPanel(image);

		nearfar[0] = croppingPanel.getNear();
		nearfar[1] = croppingPanel.getFar();

		outputPanel = dialog.addOutputPanel(worker.out.getWidth(), worker.out.getHeight(), zStep);

		animationPanel = dialog.addAnimationPanel();

//		Calibration cal = worker.out.getCalibration();
//		cal.pixelWidth = pdOut[0] / scale[0];
//		cal.pixelHeight = pdOut[1] / scale[0];
//		cal.setUnit(image.getCalibration().getUnit());

		// TODO shutdown

		final Point mouseDown = new Point();
		final boolean[] isRotation = new boolean[] {false};

		final ImageCanvas canvas = worker.out.getCanvas();
		final MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				mouseDown.setLocation(e.getPoint());
				isRotation[0] = !e.isShiftDown();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				int dx = e.getX() - mouseDown.x;
				int dy = e.getY() - mouseDown.y;
				if(!isRotation[0]) {
					renderer.translateBy(dx, dy, 0, true);
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
					renderer.rotateBy(ax, ay);
				}
			}
		};
		canvas.addMouseListener(mouseListener);

		final MouseMotionListener mouseMotionListener = new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if(Toolbar.getToolId() != Toolbar.HAND)
					return;
				// translation
				if(!isRotation[0]) {
					System.out.println(e.getX() + ", " + e.getY());
					int dx = e.getX() - mouseDown.x;
					int dy = e.getY() - mouseDown.y;
					float[] trans = new float[] {
							translation[0] + dx * pdOut[0],// / scale[0],
							translation[1] + dy * pdOut[1],// / scale[0],
							translation[2]};
					float[] fwd = calculateForwardTransform(
							scale[0],
							trans,
							rotation,
							rotcenter,
							fromCalib,
							toTransform);
					float[] inv = calculateInverseTransform(fwd);
					transformationPanel.setTransformation(guessEulerAnglesDegree(rotation), trans, scale[0]);
					worker.push(renderingSettings, fwd, inv, nearfar);
				}
				// rotation
				else {
					float speed = 0.7f;
					int dx = e.getX() - mouseDown.x;
					int dy = e.getY() - mouseDown.y;
					if(e.isAltDown()) {
						if(Math.abs(dx) > Math.abs(dy))
							dy = 0;
						else
							dx = 0;
					}
					int ax = -Math.round(dx * speed);
					int ay =  Math.round(dy * speed);

					IJ.showStatus(ax + "\u00B0" + ", " + ay + "\u00B0");

					float[] rx = Transform.fromAngleAxis(new float[] {0, 1, 0}, ax * (float)Math.PI / 180f, null);
					float[] ry = Transform.fromAngleAxis(new float[] {1, 0, 0}, ay * (float)Math.PI / 180f, null);

					float[] r = Transform.mul(rx, ry);
					float[] rot = Transform.mul(r, rotation);

					System.out.println(rot[3] + ", "+ rot[7] + ", " + rot[11]);
//					float[] cinv = Transform.fromTranslation(-rotcenter[0], -rotcenter[1], -rotcenter[2], null);
//					float[] c = Transform.fromTranslation(rotcenter[0], rotcenter[1], rotcenter[2], null);
//					float[] rot = Transform.mul(c, Transform.mul(r, Transform.mul(cinv, rotation)));
					float[] fwd = calculateForwardTransform(
							scale[0],
							translation,
							rot,
							rotcenter,
							fromCalib,
							toTransform);
					float[] inv = calculateInverseTransform(fwd);
					transformationPanel.setTransformation(guessEulerAnglesDegree(rot), translation, scale[0]);
					worker.push(renderingSettings, fwd, inv, nearfar);
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

				float[] transform = calculateForwardTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);

				// calculate the current output pixel coordinate of the rotation center
				// transform is the transformation that gets pixel coordinates as input
				// and transforms to pixel output
				float[] c = Transform.apply(transform, rotcenter[0] / pd[0], rotcenter[1] / pd[1], rotcenter[2] / pd[2], null);

				// dx and dy are the x- and y-distances of the mouse point to the rotation center
				// imagine a output size of 10x10, a rotation center at (5,5), the mouse at (1,1)
				// and a scale factor of 0.5
				// then dx = (4, 4)
				float dx = c[0] - ex;
				float dy = c[1] - ey;

				// calculate where the transformed (scaled) mouse point appears (using rotcenter as scaling
				// center)
				// in the example: p = (5,5) - 0.5*(4,4) = (3,3)
				float px = c[0] - factor * dx;
				float py = c[1] - factor * dy;

				// the transformed mouse point is at (px, py) (3,3), but should be at the original (untransformed)
				// mouse position (1, 1), therefore, we need to shift the image back
				translation[0] += (ex - px) * pdOut[0];
				translation[1] += (ey - py) * pdOut[1];

				scale[0] *= factor;

				float[] fwd = calculateForwardTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				float[] inv = calculateInverseTransform(fwd);
				transformationPanel.setTransformation(guessEulerAnglesDegree(rotation), translation, scale[0]);
				worker.push(renderingSettings, fwd, inv, nearfar);
				Calibration cal = worker.out.getCalibration();
				cal.pixelWidth = pdOut[0] / scale[0];
				cal.pixelHeight = pdOut[1] / scale[0];
			}
		};
		canvas.addMouseWheelListener(mouseWheelListener);

		contrastPanel.addContrastPanelListener(new ContrastPanel.Listener() {
			@Override
			public void renderingSettingsChanged() {
				render();
			}

			@Override
			public void channelChanged() {
				int c = contrastPanel.getChannel();
				contrastPanel.setChannel(c);
			}

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
				resetTransformation();
			}
		});

		croppingPanel.addCroppingPanelListener(new CroppingPanel.Listener() {
			@Override
			public void nearFarChanged(int near, int far) {
				nearfar[0] = near;
				nearfar[1] = far;
				render();
			}

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				float[] fwd = calculateForwardTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				float[] inv = calculateInverseTransform(fwd);
				worker.push(renderingSettings, fwd, inv, nearfar,  bbx0, bby0, bbz0, bbx1, bby1, bbz1);
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

					float[] fwd = calculateForwardTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
					worker.getRaycaster().crop(image, mask, fwd);
					float[] inv = calculateInverseTransform(fwd);
					worker.push(renderingSettings, fwd, inv, nearfar);
				}
				else {
					IJ.error("Selection required");
				}
			}
		});

		outputPanel.addOutputPanelListener(new OutputPanel.Listener() {
			@Override
			public void outputSizeChanged(int tgtW, int tgtH, float zStep) {
				pdOut[0] = image.getWidth() * pd[0] / tgtW;
				pdOut[1] = image.getHeight() * pd[1] / tgtH;

				final float[] tt = Transform.fromCalibration(
						pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
				Transform.invert(tt);
				System.arraycopy(tt, 0, toTransform, 0, 12);

				worker.getRaycaster().setTargetZStep(zStep);
				float[] fwd = calculateForwardTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				float[] inv = calculateInverseTransform(fwd);
				worker.push(renderingSettings, fwd, inv, nearfar, tgtW, tgtH);
				Calibration cal = worker.out.getCalibration();
				cal.pixelWidth = pdOut[0] / scale[0];
				cal.pixelHeight = pdOut[1] / scale[0];
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
		pdOut[0] = image.getWidth() * pd[0] / outsize.width;
		pdOut[1] = image.getHeight() * pd[1] / outsize.height;

		final float[] tt = Transform.fromCalibration(
				pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
		Transform.invert(tt);
		System.arraycopy(tt, 0, toTransform, 0, 12);

		float[] fwd = calculateForwardTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
		float[] inv = calculateInverseTransform(fwd);
		worker.getRaycaster().setTargetZStep(zStep);
		worker.push(renderingSettings, fwd, inv, nearfar, outsize.width, outsize.height);
		cal = worker.out.getCalibration();
		cal.pixelWidth = pdOut[0] / scale[0];
		cal.pixelHeight = pdOut[1] / scale[0];

		Toolbar.getInstance().setTool(Toolbar.HAND);

		ImageProcessor bg = IJ.openImage("D:\\PSoteloHitschfeld\\cover\\bg3.tif").getProcessor();
		ColorProcessor cp = bg.convertToColorProcessor();
		worker.getRaycaster().setBackground(cp);
	}

	public void render() {
		float[] fwd = calculateForwardTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
		float[] inv = calculateInverseTransform(fwd);
		worker.push(renderingSettings, fwd, inv, nearfar);
	}

	public float[] getRotationCenter() {
		return rotcenter;
	}

	public int getNChannels() {
		return luts.length;
	}

	public void resetRenderingSettings() {
		for(int c = 0; c < luts.length; c++) {
			renderingSettings[c].alphaMin = (float)luts[c].min;
			renderingSettings[c].alphaMax = (float)luts[c].max;
			renderingSettings[c].alphaGamma = 2;
			renderingSettings[c].colorMin = (float)luts[c].min;
			renderingSettings[c].colorMax = (float)luts[c].max;
			renderingSettings[c].colorGamma = 1;
			renderingSettings[c].weight = 1;
		}

		int c = contrastPanel.getChannel();
		contrastPanel.setChannel(c);
		render();
	}

	public void setTransformation(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		Transform.fromEulerAngles(rotation, new double[] {
				Math.PI * ax / 180,
				Math.PI * ay / 180,
				Math.PI * az / 180});

		scale[0] = s;

		translation[0] = dx;
		translation[1] = dy;
		translation[2] = dz;

		render();
		Calibration cal = worker.out.getCalibration();
		cal.pixelWidth = pdOut[0] / scale[0];
		cal.pixelHeight = pdOut[1] / scale[0];
	}

	public void resetTransformation() {
		scale[0] = 1;
		translation[0] = translation[1] = translation[2] = 0;
		Transform.fromIdentity(rotation);
		render();
		Calibration cal = worker.out.getCalibration();
		cal.pixelWidth = pdOut[0] / scale[0];
		cal.pixelHeight = pdOut[1] / scale[0];
		transformationPanel.setTransformation(guessEulerAnglesDegree(rotation), translation, scale[0]);
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
		AnimationEditor editor = new AnimationEditor(this);
		editor.setVisible(true);
	}

	private Keyframe createKeyframe(int frame,
			CroppingPanel croppingPanel,
			RenderingSettings[] renderingSettings,
			float[] rotation,
			float[] translation,
			float[] scale,
			float[] nearfar) {
		RenderingSettings[] rs = new RenderingSettings[renderingSettings.length];
		for(int i = 0; i < rs.length; i++)
			rs[i] = new RenderingSettings(renderingSettings[i]);
		int bbx0 = croppingPanel.getBBXMin();
		int bby0 = croppingPanel.getBBYMin();
		int bbz0 = croppingPanel.getBBZMin();
		int bbx1 = croppingPanel.getBBXMax();
		int bby1 = croppingPanel.getBBYMax();
		int bbz1 = croppingPanel.getBBZMax();

		double[] eulerAngles = new double[3];
		Transform.guessEulerAngles(rotation, eulerAngles);
		eulerAngles[0] = eulerAngles[0] * 180 / Math.PI;
		eulerAngles[1] = eulerAngles[1] * 180 / Math.PI;
		eulerAngles[2] = eulerAngles[2] * 180 / Math.PI;
		return new Keyframe(
				frame, rs,
				nearfar[0], nearfar[1],
				scale[0],
				translation[0], translation[1], translation[2],
				eulerAngles[0], eulerAngles[1], eulerAngles[2],
				bbx0, bby0, bbz0, bbx1, bby1, bbz1);
	}

	private Color getLUTColor(LUT lut) {
		int index = lut.getMapSize() - 1;
		int r = lut.getRed(index);
		int g = lut.getGreen(index);
		int b = lut.getBlue(index);
		//IJ.log(index+" "+r+" "+g+" "+b);
		if (r<100 || g<100 || b<100)
			return new Color(r, g, b);
		else
			return Color.black;
	}

	private Color[] getLUTColors(LUT[] lut) {
		Color[] colors = new Color[lut.length];
		for(int i = 0; i < lut.length; i++)
			colors[i] = getLUTColor(lut[i]);
		return colors;
	}

	private static float[] calculateForwardTransform(float scale, float[] translation, float[] rotation, float[] center, float[] fromCalib, float[] toTransform) {
		float[] scaleM = Transform.fromScale(scale, null);
		float[] transM = Transform.fromTranslation(translation[0], translation[1], translation[2], null);
		float[] centerM = Transform.fromTranslation(-center[0], -center[1], -center[2], null);

		float[] x = Transform.mul(scaleM, Transform.mul(rotation, centerM));
		Transform.applyTranslation(center[0], center[1], center[2], x);
		x = Transform.mul(transM, x);

		x = Transform.mul(x, fromCalib);
		x = Transform.mul(toTransform, x);

		return x;
	}

	private static float[] calculateInverseTransform(float[] fwd) {
		float[] copy = new float[12];
		System.arraycopy(fwd, 0, copy, 0, 12);
		Transform.invert(copy);
		return copy;
	}

	private float[] guessEulerAnglesDegree(float[] rotation) {
		float[] eulerAngles = new float[3];
		Transform.guessEulerAngles(rotation, eulerAngles);
		eulerAngles[0] = eulerAngles[0] * 180 / (float)Math.PI;
		eulerAngles[1] = eulerAngles[1] * 180 / (float)Math.PI;
		eulerAngles[2] = eulerAngles[2] * 180 / (float)Math.PI;
		return eulerAngles;
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

	public static void main(String... args) {

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
		ImagePlus imp = IJ.openImage("D:\\flybrain.tif");
		// ImagePlus imp = IJ.openImage("D:\\MHoffmann\\20160126-Markus2.small.tif");
		// ImagePlus imp = IJ.openImage("/Users/bene/flybrain.tif");
		imp.show();

		InteractiveRaycaster cr = new InteractiveRaycaster();
		cr.setup("", imp);
		cr.run(null);
	}
}

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
import java.io.File;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;

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

	private ImagePlus image;
	private double[] min, max;
	private int[][] histo8;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_16;
	}

	@Override
	public void run(ImageProcessor ip) {
		final LUT[] luts = image.isComposite() ?
				image.getLuts() : new LUT[] {image.getProcessor().getLut()};

		final int nC = image.getNChannels();

		calculateChannelMinAndMax();

		final float[] pd = new float[] {
				(float)image.getCalibration().pixelWidth,
				(float)image.getCalibration().pixelHeight,
				(float)image.getCalibration().pixelDepth
		};

		final float[] fromCalib = Transform.fromCalibration(pd[0], pd[1], pd[2], 0, 0, 0, null);

		final float[] pdOut = new float[] {pd[0], pd[0], pd[0]}; // TODO phOut

		final float[] toTransform = Transform.fromCalibration(
				pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
		Transform.invert(toTransform);

		final float[] nearfar = new float[] {0, 0};
		final float[] scale = new float[] {1};
		final float[] translation = new float[3];
		final float[] rotation = Transform.fromIdentity(null);

		final float[] rotcenter = new float[] {image.getWidth() * pd[0] / 2, image.getHeight() * pd[1] / 2, image.getNSlices() * pd[2] / 2};

		final RenderingSettings[] renderingSettings = new RenderingSettings[nC];
		for(int c = 0; c < nC; c++) {
			renderingSettings[c] = new RenderingSettings(
					(float)luts[c].min, (float)luts[c].max, 2,
					(float)luts[c].min, (float)luts[c].max, 1);
		}
		final float zStep = 2;
		final RenderingThread worker = new RenderingThread(image, renderingSettings, Transform.fromIdentity(null), nearfar, zStep);

		Color col = getLUTColor(luts[0]);

		final AnimatorDialog gd = new AnimatorDialog("Interactive Raycaster", worker.out.getWindow());
		final ContrastPanel contrastPanel = gd.addContrastPanel(histo8[0], col, min[0], max[0], renderingSettings[0], renderingSettings.length);

		final TransformationPanel transformationPanel = gd.addTransformationPanel(0, 0, 0, 0, 0, 0, 1);

		final CroppingPanel croppingPanel = gd.addCroppingPanel(image);

		nearfar[0] = croppingPanel.getNear();
		nearfar[1] = croppingPanel.getFar();

		final OutputPanel outputPanel = gd.addOutputPanel(worker.out.getWidth(), worker.out.getHeight(), zStep);

		final Timelines timelines = new Timelines(renderingSettings.length, 0, 99);
		final String[] timelineNames = new String[timelines.size()];
		for(int i = 0; i < timelineNames.length; i++)
			timelineNames[i] = Timelines.getName(i);
		final AnimationPanel animationPanel = gd.addAnimationPanel(timelineNames, timelines, 0, 0);

		Calibration cal = worker.out.getCalibration();
		cal.pixelWidth = pdOut[0] / scale[0];
		cal.pixelHeight = pdOut[1] / scale[0];
		cal.setUnit(image.getCalibration().getUnit());

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
					translation[0] += dx * pdOut[0]; // / scale[0];
					translation[1] += dy * pdOut[1]; // / scale[0];
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

					float[] rx = Transform.fromAngleAxis(new float[] {0, 1, 0}, ax * (float)Math.PI / 180f, null);
					float[] ry = Transform.fromAngleAxis(new float[] {1, 0, 0}, ay * (float)Math.PI / 180f, null);
					float[] r = Transform.mul(rx, ry);
//					float[] cinv = Transform.fromTranslation(-rotcenter[0], -rotcenter[1], -rotcenter[2], null);
//					float[] c = Transform.fromTranslation(rotcenter[0], rotcenter[1], rotcenter[2], null);
//					float[] rot = Transform.mul(c, Transform.mul(r, Transform.mul(cinv, rotation)));
					float[] rot = Transform.mul(r, rotation);

					System.arraycopy(rot, 0, rotation, 0, 12);
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
					float[] inverse = calculateInverseTransform(
							scale[0],
							trans,
							rotation,
							rotcenter,
							fromCalib,
							toTransform);
					transformationPanel.setTransformation(guessEulerAnglesDegree(rotation), trans, scale[0]);
					worker.push(renderingSettings, inverse, nearfar);
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
					float[] inverse = calculateInverseTransform(
							scale[0],
							translation,
							rot,
							rotcenter,
							fromCalib,
							toTransform);
					transformationPanel.setTransformation(guessEulerAnglesDegree(rot), translation, scale[0]);
					worker.push(renderingSettings, inverse, nearfar);
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

				float[] transform = calculateTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);

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

				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				transformationPanel.setTransformation(guessEulerAnglesDegree(rotation), translation, scale[0]);
				worker.push(renderingSettings, inverse, nearfar);
				Calibration cal = worker.out.getCalibration();
				cal.pixelWidth = pdOut[0] / scale[0];
				cal.pixelHeight = pdOut[1] / scale[0];
			}
		};
		canvas.addMouseWheelListener(mouseWheelListener);

		contrastPanel.addContrastPanelListener(new ContrastPanel.Listener() {
			@Override
			public void renderingSettingsChanged() {
				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				worker.push(renderingSettings, inverse, nearfar);
			}

			@Override
			public void channelChanged() {
				int c = contrastPanel.getChannel();
				Color col = getLUTColor(luts[c]);
				contrastPanel.set(histo8[c], col, min[c], max[c], renderingSettings[c]);
			}

			@Override
			public void renderingSettingsReset() {
				for(int c = 0; c < nC; c++) {
					renderingSettings[c].alphaMin = (float)luts[c].min;
					renderingSettings[c].alphaMax = (float)luts[c].max;
					renderingSettings[c].alphaGamma = 2;
					renderingSettings[c].colorMin = (float)luts[c].min;
					renderingSettings[c].colorMax = (float)luts[c].max;
					renderingSettings[c].colorGamma = 1;
				}

				int c = contrastPanel.getChannel();
				Color col = getLUTColor(luts[c]);
				contrastPanel.set(histo8[c], col, min[c], max[c], renderingSettings[c]);
				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				worker.push(renderingSettings, inverse, nearfar);
			}

			@Override
			public void record(NumberField src, String timelineName, boolean delete) {
				for(int i = 0; i < timelines.size(); i++) {
					if(Timelines.getName(i).equals(timelineName)) {
						int frame = animationPanel.getCurrentFrame();
						if(delete)
							timelines.get(i).removePointAt(frame);
						else
							timelines.get(i).add(frame, Double.parseDouble(src.getText()));
						animationPanel.repaint();
						break;
					}
				}
			}
		});

		transformationPanel.addTransformationPanelListener(new TransformationPanel.Listener() {
			@Override
			public void transformationChanged(float ax, float ay, float az, float dx, float dy, float dz, float s) {
				Transform.fromEulerAngles(rotation, new double[] {
						Math.PI * ax / 180,
						Math.PI * ay / 180,
						Math.PI * az / 180});

				scale[0] = s;

				translation[0] = dx;
				translation[1] = dy;
				translation[2] = dz;

				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				worker.push(renderingSettings, inverse, nearfar);
				Calibration cal = worker.out.getCalibration();
				cal.pixelWidth = pdOut[0] / scale[0];
				cal.pixelHeight = pdOut[1] / scale[0];
			}

			@Override
			public void record(NumberField src, String timelineName, boolean delete) {
				for(int i = 0; i < timelines.size(); i++) {
					if(Timelines.getName(i).equals(timelineName)) {
						int frame = animationPanel.getCurrentFrame();
						if(delete)
							timelines.get(i).removePointAt(frame);
						else
							timelines.get(i).add(frame, Double.parseDouble(src.getText()));
						animationPanel.repaint();
						break;
					}
				}
			}

			@Override
			public void resetTransformation() {
				scale[0] = 1;
				translation[0] = translation[1] = translation[2] = 0;
				Transform.fromIdentity(rotation);
				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				worker.push(renderingSettings, inverse, nearfar);
				transformationPanel.setTransformation(guessEulerAnglesDegree(rotation), translation, scale[0]);
			}
		});

		croppingPanel.addCroppingPanelListener(new CroppingPanel.Listener() {
			@Override
			public void nearFarChanged(int near, int far) {
				nearfar[0] = near;
				nearfar[1] = far;
				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				worker.push(renderingSettings, inverse, nearfar);
			}

			@Override
			public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				worker.push(renderingSettings, inverse, nearfar, bbx0, bby0, bbz0, bbx1, bby1, bbz1);
			}

			@Override
			public void record(NumberField src, String timelineName, boolean delete) {
				for(int i = 0; i < timelines.size(); i++) {
					if(Timelines.getName(i).equals(timelineName)) {
						int frame = animationPanel.getCurrentFrame();
						if(delete)
							timelines.get(i).removePointAt(frame);
						else
							timelines.get(i).add(frame, Double.parseDouble(src.getText()));
						animationPanel.repaint();
						break;
					}
				}
			}

			@Override
			public void cutOffROI() {
				float[] fwdTransform = calculateTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
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

					worker.getRaycaster().crop(image, mask, fwdTransform);
					float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
					worker.push(renderingSettings, inverse, nearfar);
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

				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				worker.getRaycaster().setTargetZStep(zStep);
				worker.push(renderingSettings, inverse, nearfar, tgtW, tgtH);
				Calibration cal = worker.out.getCalibration();
				cal.pixelWidth = pdOut[0] / scale[0];
				cal.pixelHeight = pdOut[1] / scale[0];
			}
		});

		gd.addWindowListener(new WindowAdapter() {
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
			public void currentTimepointChanged(int t) {
				if(timelines.isEmpty())
					return;
				Keyframe current = createKeyframe(t, croppingPanel, renderingSettings, rotation, translation, scale, nearfar);
				Keyframe k = timelines.getInterpolatedFrame(t, current);
				for(int i = 0; i < renderingSettings.length; i++) {
					renderingSettings[i].set(k.renderingSettings[i]);
				}

				int c = contrastPanel.getChannel();
				Color col = getLUTColor(luts[c]);
				contrastPanel.set(histo8[c], col, min[c], max[c], renderingSettings[c]);

				croppingPanel.setBoundingBox(k.bbx0, k.bby0, k.bbz0, k.bbx1, k.bby1, k.bbz1);

				translation[0] = k.dx;
				translation[1] = k.dy;
				translation[2] = k.dz;

				scale[0] = k.scale;

				nearfar[0] = k.near;
				nearfar[1] = k.far;

				croppingPanel.setNearAndFar(Math.round(k.near), Math.round(k.far));

				Transform.fromEulerAngles(rotation, new double[] {
						Math.PI * k.angleX / 180,
						Math.PI * k.angleY / 180,
						Math.PI * k.angleZ / 180});

				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
				transformationPanel.setTransformation(new float[] {
						(float)k.angleX,
						(float)k.angleY,
						(float)k.angleZ}, translation, scale[0]);
				worker.push(renderingSettings, inverse, nearfar, k.bbx0, k.bby0, k.bbz0, k.bbx1, k.bby1, k.bbz1, t + 1);
			}

			@Override
			public void recordKeyframe() {
				int t = animationPanel.getCurrentFrame();
				Keyframe previous = timelines.getKeyframeNoInterpol(t);
				Keyframe current  = createKeyframe(t,
						croppingPanel,
						renderingSettings,
						rotation,
						translation,
						scale,
						nearfar);
				KeyframePanel panel = new KeyframePanel(current, previous);
				GenericDialog gd = new GenericDialog("Record time point");
				gd.addPanel(panel);
				gd.showDialog();
				if(gd.wasCanceled())
					return;
				panel.apply();
				timelines.recordFrame(current);
				animationPanel.repaint();
			}

			@Override
			public void insertSpin() {
				int t = animationPanel.getCurrentFrame();
				GenericDialog gd = new GenericDialog("");
				gd.addNumericField("#frames", 180, 0);
				gd.addNumericField("angle", 360, 2);
				String[] axisChoice = new String[] {"x-axis", "y-axis"};
				gd.addChoice("axis", axisChoice, axisChoice[1]);
				gd.showDialog();
				if(gd.wasCanceled())
					return;

				int nFrames = (int)gd.getNextNumber();
				double angle = gd.getNextNumber();
				int axisI = gd.getNextChoiceIndex();
				float[] axis = axisI == 0 ? new float[] {1, 0, 0} : new float[] {0, 1, 0};

				// calculate euler angle increments for 1 degree
				double[] eulerAngles0 = new double[3];
				Transform.guessEulerAngles(rotation, eulerAngles0);
				float[] r = Transform.fromAngleAxis(axis, (float)(1 * Math.PI) / 180f, null);
				float[] rot = Transform.mul(r, rotation);
				double[] eulerAngles1 = new double[3];
				Transform.guessEulerAngles(rot, eulerAngles1);
				double dEx = eulerAngles1[0] - eulerAngles0[0];
				double dEy = eulerAngles1[1] - eulerAngles0[1];
				double dEz = eulerAngles1[2] - eulerAngles0[2];

				double a = 0;
				Keyframe kf = timelines.getKeyframeNoInterpol(t);
				if(kf.angleX == Keyframe.UNSET) kf.angleX = 180 * (eulerAngles0[0] + dEx * a) / Math.PI;
				if(kf.angleY == Keyframe.UNSET) kf.angleY = 180 * (eulerAngles0[1] + dEy * a) / Math.PI;
				if(kf.angleZ == Keyframe.UNSET) kf.angleZ = 180 * (eulerAngles0[2] + dEz * a) / Math.PI;
				timelines.recordFrame(kf);
				kf = timelines.getKeyframeNoInterpol(t + nFrames);
				kf.angleX = 180 * (eulerAngles0[0] + dEx * angle) / Math.PI;
				kf.angleY = 180 * (eulerAngles0[1] + dEy * angle) / Math.PI;
				kf.angleZ = 180 * (eulerAngles0[2] + dEz * angle) / Math.PI;
				timelines.recordFrame(kf);
			}

			@Override
			public void record(int from, int to) {
				ImageStack stack = new ImageStack(worker.out.getWidth(), worker.out.getHeight());
				ImagePlus anim = null;
				Keyframe current = createKeyframe(from, croppingPanel, renderingSettings, rotation, translation, scale, nearfar);

				for(int t = from; t <= to; t++) {
					if(IJ.escapePressed())
						break;
					Keyframe k = timelines.getInterpolatedFrame(t, current);

					float[] translation = new float[] {k.dx, k.dy, k.dz};
					float[] rotation = new float[12];
					Transform.fromEulerAngles(rotation, new double[] {
							Math.PI * k.angleX / 180,
							Math.PI * k.angleY / 180,
							Math.PI * k.angleZ / 180});

					float[] inverse = calculateInverseTransform(k.scale, translation, rotation, rotcenter, fromCalib, toTransform);

					worker.getRaycaster().setBBox(k.bbx0, k.bby0, k.bbz0, k.bbx1, k.bby1, k.bbz1);
					if(image.getNFrames() > 1) {
						int before = image.getT();
						image.setT(t + 1);
						if(image.getT() != before)
							worker.getRaycaster().setImage(image);
					}

					stack.addSlice(worker.getRaycaster().renderAndCompose(inverse, k.renderingSettings, k.near, k.far).getProcessor());
					if(t == from + 1) {
						anim = new ImagePlus(image.getTitle(), stack);
						anim.setCalibration(worker.out.getCalibration().copy());
						anim.show();
					} else if(t > from + 1) {
						anim.setSlice(t - from + 1);
						anim.updateAndDraw();
					}
				}
				IJ.resetEscape();
			}

			@Override
			public void exportJSON() {
				SaveDialog d = new SaveDialog("Save animation", "animation.json", ".json");
				String dir = d.getDirectory();
				String name = d.getFileName();
				if(dir != null && name != null) {
					try {
						JsonExporter.exportTimelines(timelines, new File(dir, name));
					} catch(Exception e) {
						IJ.handleException(e);
					}
				}
			}

			@Override
			public void importJSON() {
				OpenDialog d = new OpenDialog("Open animation", "animation.json", ".json");
				String dir = d.getDirectory();
				String name = d.getFileName();
				if(dir != null && name != null) {
					try {
						JsonExporter.importTimelines(timelines, new File(dir, name));
					} catch(Exception e) {
						IJ.handleException(e);
					}
				}
			}
		});




//		Panel p = new Panel(new FlowLayout(FlowLayout.RIGHT));
//		Button but = new Button("Animate");
//		but.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				GenericDialog gd = new GenericDialog("");
//				gd.addNumericField("#frames", 180, 0);
//				gd.addNumericField("y_angle_increment", 2, 0);
//				gd.addNumericField("x_angle_increment", 0, 0);
//				gd.addCheckbox("Scroll_through", false);
//				gd.addNumericField("Scroll_from", 0, 2);
//				gd.addNumericField("Scroll_to", 0, 2);
//				gd.addNumericField("dz", 1, 0);
//				gd.showDialog();
//				if(gd.wasCanceled())
//					return;
//
//				int nFrames = (int)gd.getNextNumber();
//				int ax = (int)gd.getNextNumber();
//				int ay = (int)gd.getNextNumber();
//				boolean scrollThrough = gd.getNextBoolean();
//				float scrollFrom = (float)gd.getNextNumber();
//				float scrollTo = (float)gd.getNextNumber();
//				float dz = (float)gd.getNextNumber();
//
//				ImageStack stack = new ImageStack(worker.out.getWidth(), worker.out.getHeight());
//				float[] inverse = null;
//				for(int i = 0; i < nFrames; i++) {
//					float[] rx = Transform.fromAngleAxis(new float[] {0, 1, 0}, ax * (float)Math.PI / 180f, null);
//					float[] ry = Transform.fromAngleAxis(new float[] {1, 0, 0}, ay * (float)Math.PI / 180f, null);
//					float[] r = Transform.mul(rx, ry);
////					float[] cinv = Transform.fromTranslation(-rotcenter[0], -rotcenter[1], -rotcenter[2], null);
////					float[] c = Transform.fromTranslation(rotcenter[0], rotcenter[1], rotcenter[2], null);
////					float[] rot = Transform.mul(c, Transform.mul(r, Transform.mul(cinv, rotation)));
//					float[] rot = Transform.mul(r, rotation);
//					System.arraycopy(rot, 0, rotation, 0, 12);
//
//					inverse = calculateInverseTransform(
//							scale[0],
//							translation,
//							rot,
//							rotcenter,
//							fromCalib,
//							toTransform);
//					stack.addSlice(worker.getRaycaster().renderAndCompose(inverse, renderingSettings, nearfar[0], nearfar[1]).getProcessor());
//					IJ.showProgress(i + 1, nFrames);
//				}
//				if(scrollThrough) {
//					int n = Math.round((scrollTo - scrollFrom) / dz) + 1;
//					for(int i = 0; i < n; i++) {
//						stack.addSlice(worker.getRaycaster().renderAndCompose(
//								inverse, renderingSettings, scrollFrom + i * dz, nearfar[1]).getProcessor());
//					}
//					for(int i = n - 1; i >= 0; i--) {
//						stack.addSlice(worker.getRaycaster().renderAndCompose(
//								inverse, renderingSettings, scrollFrom + i * dz, nearfar[1]).getProcessor());
//					}
//				}
//				ImagePlus anim = new ImagePlus(image.getTitle(), stack);
//				anim.setCalibration(worker.out.getCalibration().copy());
//				anim.show();
//			}
//		});
//		p.add(but);
//
//		Button but = new Button("Reset transformations");
//		but.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				scale[0] = 1;
//				translation[0] = translation[1] = translation[2] = 0;
//				Transform.fromIdentity(rotation);
//				float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
//				transformationPanel.setTransformation(guessEulerAnglesDegree(rotation), translation, scale[0]);
//				worker.push(renderingSettings, inverse, nearfar);
//			}
//		});
//		p.add(but);



//		gd.addPanel(p);

		gd.setModal(false);
		gd.pack();
		gd.showDialog();

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

		float[] inverse = calculateInverseTransform(scale[0], translation, rotation, rotcenter, fromCalib, toTransform);
		worker.getRaycaster().setTargetZStep(zStep);
		worker.push(renderingSettings, inverse, nearfar, outsize.width, outsize.height);
		cal = worker.out.getCalibration();
		cal.pixelWidth = pdOut[0] / scale[0];
		cal.pixelHeight = pdOut[1] / scale[0];

		Toolbar.getInstance().setTool(Toolbar.HAND);
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

	private Keyframe createUnsetKeyframe(int frame, int nChannels) {
		RenderingSettings[] rs = new RenderingSettings[nChannels];
		int us = Keyframe.UNSET;
		for(int i = 0; i < rs.length; i++)
			rs[i] = new RenderingSettings(us, us, us, us, us, us);

		int bbx0, bby0, bbz0, bbx1, bby1, bbz1;
		bbx0 = bby0 = bbz0 = bbx1 = bby1 = bbz1 = us;

		float near, far, scale, dx, dy, dz;
		near = far = scale = dx = dy = dz = us;

		double ax, ay, az;
		ax = ay = az = us;
		return new Keyframe(
				frame, rs,
				near, far,
				scale,
				dx, dy, dz,
				ax, ay, az,
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

	private static float[] calculateTransform(float scale, float[] translation, float[] rotation, float[] center, float[] fromCalib, float[] toTransform) {
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

	/**
	 * Calculates scale * translation * rotation
	 * @param scale
	 * @param translation
	 * @param rotation
	 */
	private static float[] calculateInverseTransform(float scale, float[] translation, float[] rotation, float[] center, float[] fromCalib, float[] toTransform) {
		float[] x = calculateTransform(scale, translation, rotation, center, fromCalib, toTransform);
		Transform.invert(x);
		return x;
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

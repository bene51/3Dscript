package renderer3d;

import java.awt.Color;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import textanim.Renderer3D;

public class Renderer3DAdapter extends CudaRaycaster implements Renderer3D  {

	private final Keyframe keyframe;

	private float near;
	private float far;

	private LUT[] luts;

	public Renderer3DAdapter(ImagePlus image, int wOut, int hOut, float zStep) {
		super(image, wOut, hOut, zStep);

		luts = image.isComposite() ?
				image.getLuts() : new LUT[] {image.getProcessor().getLut()};

		final int nC = image.getNChannels();

		float[] pdIn = new float[] {
				(float)image.getCalibration().pixelWidth,
				(float)image.getCalibration().pixelHeight,
				(float)image.getCalibration().pixelDepth
		};

		float[] pdOut = new float[] {pdIn[0], pdIn[0], pdIn[0]}; // TODO phOut


		near = 0;
		far = 0;
		float[] rotcenter = new float[] {
				image.getWidth()   * pdIn[0] / 2,
				image.getHeight()  * pdIn[1] / 2,
				image.getNSlices() * pdIn[2] / 2};

		RenderingSettings[] renderingSettings = new RenderingSettings[nC];
		for(int c = 0; c < nC; c++) {
			renderingSettings[c] = new RenderingSettings(
					(float)luts[c].min, (float)luts[c].max, 1,
					(float)luts[c].min, (float)luts[c].max, 2);
		}

		CombinedTransform transformation = new CombinedTransform(pdIn, pdOut, rotcenter);

		this.keyframe = new Keyframe(0, renderingSettings, near, far, transformation, 0, 0, 0, image.getWidth(), image.getHeight(), image.getNSlices());
	}

	public void resetRenderingSettings() {
		RenderingSettings[] renderingSettings = keyframe.renderingSettings;
		for(int c = 0; c < luts.length; c++) {
			renderingSettings[c].alphaMin = (float)luts[c].min;
			renderingSettings[c].alphaMax = (float)luts[c].max;
			renderingSettings[c].alphaGamma = 2;
			renderingSettings[c].colorMin = (float)luts[c].min;
			renderingSettings[c].colorMax = (float)luts[c].max;
			renderingSettings[c].colorGamma = 1;
			renderingSettings[c].weight = 1;
		}
	}

	public Keyframe getKeyframe() {
		return keyframe;
	}

	public Color[] getChannelColors() {
		return getLUTColors(luts);
	}

	private Color[] getLUTColors(LUT[] lut) {
		Color[] colors = new Color[lut.length];
		for(int i = 0; i < lut.length; i++)
			colors[i] = getLUTColor(lut[i]);
		return colors;
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

	@Override
	public ImageProcessor render(Keyframe kf) {
		if(kf.bbx0 != keyframe.bbx0 || kf.bbx1 != keyframe.bbx1 ||
				kf.bby0 != keyframe.bby0 || kf.bby1 != keyframe.bbz1 ||
				kf.bbz0 != keyframe.bbz0 || kf.bbz1 != keyframe.bby1) {
			super.setBBox(kf.bbx0, kf.bby0, kf.bbz0, kf.bbx1, kf.bby1, kf.bbz1);
			keyframe.bbx0 = kf.bbx0;
			keyframe.bbx1 = kf.bbx1;
			keyframe.bby0 = kf.bby0;
			keyframe.bby1 = kf.bby1;
			keyframe.bbz0 = kf.bbz0;
			keyframe.bbz1 = kf.bbz1;
		}

		CombinedTransform transform = kf.getFwdTransform();
		float[] fwd = transform.calculateForwardTransform();
		float[] inv = CombinedTransform.calculateInverseTransform(fwd);
		keyframe.setFrom(kf);
		return super.project(fwd, inv, kf.renderingSettings, kf.near, kf.far);
	}

	@Override
	public void setTargetSize(int w, int h) {
		super.setTgtSize(w, h);
		Calibration cal = image.getCalibration();
		float pwOut = (float)(image.getWidth()  * cal.pixelWidth  / w);
		float phOut = (float)(image.getHeight() * cal.pixelHeight / h);
		float pdOut = (float)cal.pixelDepth;
		float[] p = new float[] {pwOut, phOut, pdOut};

		keyframe.getFwdTransform().setOutputSpacing(p);
	}

	@Override
	public void setTimelapseIndex(int t) {
		if(image.getNFrames() > 1) {
			int before = image.getT();
			image.setT(t + 1);
			if(image.getT() != before)
				super.setImage(image);
		}
	}

	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
	}

	@Override
	public void setBackground(ColorProcessor bg) {
		super.setBackground(bg);

	}

	@Override
	public void setZStep(double zStep) {
		super.setTargetZStep((float)zStep);
	}

	public int getNChannels() {
		return luts.length;
	}
}

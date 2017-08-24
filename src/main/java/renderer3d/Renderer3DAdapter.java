package renderer3d;

import java.awt.Color;

import animation2.RenderingSettings;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

public class Renderer3DAdapter extends CudaRaycaster implements Renderer3D  {

	private final Keyframe keyframe;

	private float[] pdOut;

	private float[] fromCalib;
	private float[] toTransform;
	private float[] nearfar;
	private float[] scale;
	private float[] translation;
	private float[] rotation;
	private float[] rotcenter;

	private LUT[] luts;
	private RenderingSettings[] renderingSettings;
	private ImagePlus out;

	public Renderer3DAdapter(ImagePlus image, int wOut, int hOut, float zStep) {
		super(image, wOut, hOut, zStep);

		this.keyframe = Keyframe.createEmptyKeyframe(image.getNChannels());

		luts = image.isComposite() ?
				image.getLuts() : new LUT[] {image.getProcessor().getLut()};

		final int nC = image.getNChannels();

		final float[] pd = new float[] {
				(float)image.getCalibration().pixelWidth,
				(float)image.getCalibration().pixelHeight,
				(float)image.getCalibration().pixelDepth
		};

		fromCalib = Transform.fromCalibration(pd[0], pd[1], pd[2], 0, 0, 0, null);

		pdOut = new float[] {pd[0], pd[0], pd[0]}; // TODO phOut

		toTransform = Transform.fromCalibration(
				pdOut[0], pdOut[1], pdOut[2], 0, 0, 0, null);
		Transform.invert(toTransform);

		nearfar = new float[] {0, 0};
		scale = new float[] {1};
		translation = new float[3];
		rotation = Transform.fromIdentity(null);
		rotcenter = new float[] {
				image.getWidth()   * pd[0] / 2,
				image.getHeight()  * pd[1] / 2,
				image.getNSlices() * pd[2] / 2};

		renderingSettings = new RenderingSettings[nC];
		for(int c = 0; c < nC; c++) {
			renderingSettings[c] = new RenderingSettings(
					(float)luts[c].min, (float)luts[c].max, 1,
					(float)luts[c].min, (float)luts[c].max, 2);
		}

		out = renderAndCompose(Transform.fromIdentity(null), Transform.fromIdentity(null), renderingSettings, nearfar[0], nearfar[1]);

		Calibration cal = out.getCalibration();
		cal.pixelWidth = pdOut[0] / scale[0];
		cal.pixelHeight = pdOut[1] / scale[0];
		cal.setUnit(image.getCalibration().getUnit());

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
		if(kf.getFwdTransform()) == null

		return super.renderAndCompose(fwd, inv, kf.renderingSettings, kf.near, kf.far);
	}

	@Override
	public void setTargetSize(int w, int h) {
		super.setTgtSize(w, h);
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

	public void translateBy(float dx, float dy, float dz, boolean inOutputPixels) {
		if(inOutputPixels) {
			dx *= pdOut[0];
			dy *= pdOut[1];
			dz *= pdOut[2];
		}
		translation[0] += dx;
		translation[1] += dy;
		translation[2] += dz;
	}

	/**
	 * in degrees.
	 * @param ax
	 * @param ay
	 */
	public void rotateBy(float ax, float ay) {
		float[] rx = Transform.fromAngleAxis(new float[] {0, 1, 0}, ax * (float)Math.PI / 180f, null);
		float[] ry = Transform.fromAngleAxis(new float[] {1, 0, 0}, ay * (float)Math.PI / 180f, null);
		float[] r = Transform.mul(rx, ry);
		float[] rot = Transform.mul(r, rotation);

		System.arraycopy(rot, 0, rotation, 0, 12);
	}
}

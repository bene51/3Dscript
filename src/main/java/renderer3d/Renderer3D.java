package renderer3d;

import java.awt.Color;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import textanim.CombinedTransform;
import textanim.IKeywordFactory;
import textanim.IRenderer3D;
import textanim.RenderingState;

public class Renderer3D extends CudaRaycaster implements IRenderer3D  {

	private final ExtendedRenderingState rs;

	private float near;
	private float far;

	private final IKeywordFactory kwFactory = new ExtendedKeywordFactory();

	public Renderer3D(ImagePlus image, int wOut, int hOut, float zStep) {
		super(image, wOut, hOut, zStep);

		LUT[] luts = image.isComposite() ?
				image.getLuts() : new LUT[] {image.getProcessor().getLut()};

		final int nC = image.getNChannels();

		float[] pdIn = new float[] {
				(float)image.getCalibration().pixelWidth,
				(float)image.getCalibration().pixelHeight,
				(float)image.getCalibration().pixelDepth
		};

		float[] pdOut = new float[] {pdIn[0], pdIn[1], pdIn[2]};


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
		Color[] channelColors = calculateChannelColors();

		CombinedTransform transformation = new CombinedTransform(pdIn, pdOut, rotcenter);

		this.rs = new ExtendedRenderingState(0,
				renderingSettings,
				channelColors,
				near, far,
				transformation,
				0, 0, 0, image.getWidth(), image.getHeight(), image.getNSlices());
	}

	public void resetRenderingSettings() {
		LUT[] luts = image.isComposite() ?
				image.getLuts() : new LUT[] {image.getProcessor().getLut()};
//		RenderingSettings[] renderingSettings = rs.renderingSettings;
		for(int c = 0; c < luts.length; c++) {
//			renderingSettings[c].alphaMin = (float)luts[c].min;
//			renderingSettings[c].alphaMax = (float)luts[c].max;
//			renderingSettings[c].alphaGamma = 2;
//			renderingSettings[c].colorMin = (float)luts[c].min;
//			renderingSettings[c].colorMax = (float)luts[c].max;
//			renderingSettings[c].colorGamma = 1;
//			renderingSettings[c].weight = 1;

			rs.setChannelProperty(c, ExtendedRenderingState.COLOR_MIN,   luts[c].min);
			rs.setChannelProperty(c, ExtendedRenderingState.COLOR_MAX,   luts[c].max);
			rs.setChannelProperty(c, ExtendedRenderingState.COLOR_GAMMA, 1);
			rs.setChannelProperty(c, ExtendedRenderingState.ALPHA_MIN,   luts[c].min);
			rs.setChannelProperty(c, ExtendedRenderingState.ALPHA_MAX,   luts[c].max);
			rs.setChannelProperty(c, ExtendedRenderingState.ALPHA_GAMMA, 2);
		}
	}

	@Override
	public IKeywordFactory getKeywordFactory() {
		return kwFactory;
	}

	@Override
	public ExtendedRenderingState getRenderingState() {
		return rs;
	}

	@Override
	public ImageProcessor render(RenderingState kf2) {
		ExtendedRenderingState kf = (ExtendedRenderingState)kf2;
		int kfbbx0 = (int)kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMIN);
		int kfbby0 = (int)kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMIN);
		int kfbbz0 = (int)kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMIN);
		int kfbbx1 = (int)kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMAX);
		int kfbby1 = (int)kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMAX);
		int kfbbz1 = (int)kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMAX);
		if(kfbbx0 != (int)rs.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMIN) ||
				kfbby0 != (int)rs.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMIN) ||
				kfbbz0 != (int)rs.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMIN) ||
				kfbbx1 != (int)rs.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMAX) ||
				kfbby1 != (int)rs.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMAX) ||
				kfbbz1 != (int)rs.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMAX)) {

			super.setBBox(kfbbx0, kfbby0, kfbbz0, kfbbx1, kfbby1, kfbbz1);
			rs.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMIN, kfbbx0);
			rs.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMIN, kfbby0);
			rs.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMIN, kfbbz0);
			rs.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMAX, kfbbx1);
			rs.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMAX, kfbby1);
			rs.setNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMAX, kfbbz1);
		}

		CombinedTransform transform = kf.getFwdTransform();
		float[] fwd = transform.calculateForwardTransform();
		float[] inv = CombinedTransform.calculateInverseTransform(fwd);
		rs.setFrom(kf);
		return super.project(fwd, inv, kf.getChannelProperties(),
				(float)kf.getNonchannelProperty(ExtendedRenderingState.NEAR),
				(float)kf.getNonchannelProperty(ExtendedRenderingState.FAR));
	}

	@Override
	public void setTargetSize(int w, int h) {
		super.setTgtSize(w, h);
		Calibration cal = image.getCalibration();
		float pwOut = (float)(image.getWidth()  * cal.pixelWidth  / w);
		float phOut = (float)(image.getHeight() * cal.pixelHeight / h);
		float pdOut = (float)cal.pixelDepth;
		float[] p = new float[] {pwOut, phOut, pdOut};

		rs.getFwdTransform().setOutputSpacing(p);
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

	public int getNChannels() {
		return image.getNChannels();
	}

	private Color[] calculateChannelColors() {
		int nChannels = image.getNChannels();
		Color[] channelColors = new Color[nChannels];
		if(!image.isComposite()) {
			LUT lut = image.getProcessor().getLut();
			int t = image.getType();
			boolean grayscale = t == ImagePlus.GRAY8 || t == ImagePlus.GRAY16 || t == ImagePlus.GRAY32;
			if(lut != null && !grayscale) {
				channelColors[0] = getLUTColor(lut);
			} else {
				channelColors[0] = Color.WHITE;
			}
			return channelColors;
		}
		for(int c = 0; c < image.getNChannels(); c++) {
			image.setC(c + 1);
			Color col = ((CompositeImage)image).getChannelColor();
			if(col.equals(Color.BLACK))
				col = Color.white;
			channelColors[c] = col;
		}
		return channelColors;
	}

	private Color getLUTColor(LUT lut) {
		int index = lut.getMapSize() - 1;
		int r = lut.getRed(index);
		int g = lut.getGreen(index);
		int b = lut.getBlue(index);
		if (r<100 || g<100 || b<100)
			return new Color(r, g, b);
		else
			return Color.WHITE;
	}
}

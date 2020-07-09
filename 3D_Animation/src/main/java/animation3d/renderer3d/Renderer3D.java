package animation3d.renderer3d;

import java.awt.Color;
import java.util.Arrays;

import animation3d.gui.CroppingPanel;
import animation3d.textanim.CombinedTransform;
import animation3d.textanim.IKeywordFactory;
import animation3d.textanim.IRenderer3D;
import animation3d.textanim.RenderingState;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.LUT;

public class Renderer3D extends OpenCLRaycaster implements IRenderer3D  {

	private final ExtendedRenderingState rs;

	private final IKeywordFactory kwFactory = new KeywordFactory();

	public Renderer3D(ImagePlus image, int wOut, int hOut) {
		this(image, wOut, hOut, null);
	}

	public Renderer3D(ImagePlus image, int wOut, int hOut, Progress loadingProgress) {
		super(image, wOut, hOut, loadingProgress);
		this.rs = makeDefaultRenderingState(image);
	}

	public ExtendedRenderingState makeDefaultRenderingState(ImagePlus image) {
		LUT[] luts = image.isComposite() ?
				image.getLuts() : new LUT[] {image.getProcessor().getLut()};

		final int nC = image.getNChannels();

		float[] pdIn = new float[] {
				(float)image.getCalibration().pixelWidth,
				(float)image.getCalibration().pixelHeight,
				(float)image.getCalibration().pixelDepth
		};

		Calibration cal = image.getCalibration();
		float pwOut = (float)(image.getWidth()  * cal.pixelWidth  / wOut);
		float phOut = (float)(image.getHeight() * cal.pixelHeight / hOut);
		float pdOut = pdIn[2];
		float[] p = new float[] {pwOut, phOut, pdOut};

		float near = (float)CroppingPanel.getNear(image);
		float far  = (float)CroppingPanel.getFar(image);
		float[] rotcenter = new float[] {
				image.getWidth()   * pdIn[0] / 2,
				image.getHeight()  * pdIn[1] / 2,
				image.getNSlices() * pdIn[2] / 2};

		RenderingSettings[] renderingSettings = new RenderingSettings[nC];
		for(int c = 0; c < nC; c++) {
			renderingSettings[c] = new RenderingSettings(
					(float)luts[c].min, (float)luts[c].max, 1,
					(float)luts[c].min, (float)luts[c].max, 2,
					1,
					0, 0, 0,
					image.getWidth(), image.getHeight(), image.getNSlices(),
					near, far);
		}
		Color[] channelColors = calculateChannelColors();

		CombinedTransform transformation = new CombinedTransform(pdIn, p, rotcenter);

		ExtendedRenderingState rs = new ExtendedRenderingState(0,
				image.getT(),
				renderingSettings,
				channelColors,
				Color.BLACK,
				RenderingAlgorithm.INDEPENDENT_TRANSPARENCY,
				transformation);
		rs.setScalebarProperties(super.getScalebar());
		rs.setBoundingboxProperties(super.getBoundingBox());
		return rs;
	}

	public void reset() {
		rs.setFrom(makeDefaultRenderingState(image));
	}

	@Override
	public float[] getRotationCenter() {
		return new float[] {
				image.getWidth()   * (float)image.getCalibration().pixelWidth  / 2f,
				image.getHeight()  * (float)image.getCalibration().pixelHeight / 2f,
				image.getNSlices() * (float)image.getCalibration().pixelDepth  / 2f
		};
	}

	@Override
	public String getTitle() {
		return image.getTitle();
	}

	public void resetRenderingSettings(ExtendedRenderingState rs) {
		LUT[] luts = image.isComposite() ?
				image.getLuts() : new LUT[] {image.getProcessor().getLut()};
		Color[] channelColors = calculateChannelColors();
		for(int c = 0; c < luts.length; c++) {
			rs.setChannelProperty(c, ExtendedRenderingState.INTENSITY_MIN,   luts[c].min);
			rs.setChannelProperty(c, ExtendedRenderingState.INTENSITY_MAX,   luts[c].max);
			rs.setChannelProperty(c, ExtendedRenderingState.INTENSITY_GAMMA, 1);
			rs.setChannelProperty(c, ExtendedRenderingState.ALPHA_MIN,   luts[c].min);
			rs.setChannelProperty(c, ExtendedRenderingState.ALPHA_MAX,   luts[c].max);
			rs.setChannelProperty(c, ExtendedRenderingState.ALPHA_GAMMA, 2);
			rs.setChannelProperty(c, ExtendedRenderingState.WEIGHT, 1);
			rs.setChannelProperty(c, ExtendedRenderingState.CHANNEL_COLOR_RED,   channelColors[c].getRed());
			rs.setChannelProperty(c, ExtendedRenderingState.CHANNEL_COLOR_GREEN, channelColors[c].getGreen());
			rs.setChannelProperty(c, ExtendedRenderingState.CHANNEL_COLOR_BLUE,  channelColors[c].getBlue());
			rs.setChannelProperty(c, ExtendedRenderingState.USE_LIGHT, 0);
			rs.setChannelProperty(c, ExtendedRenderingState.LIGHT_K_OBJECT,   1);
			rs.setChannelProperty(c, ExtendedRenderingState.LIGHT_K_DIFFUSE,  0);
			rs.setChannelProperty(c, ExtendedRenderingState.LIGHT_K_SPECULAR, 0);
			rs.setChannelProperty(c, ExtendedRenderingState.LIGHT_SHININESS,  5);
			// rs.setChannelProperty(c, ExtendedRenderingState.USE_LUT, 0);
			// TODO useLUT (for now, just leave it as it is)
		}
		rs.setNonChannelProperty(ExtendedRenderingState.RENDERING_ALGORITHM, RenderingAlgorithm.INDEPENDENT_TRANSPARENCY.ordinal());
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
		adjustProgram(kf);
		rs.setFrom(kf);
		return super.project(kf);
	}

	private void adjustProgram(ExtendedRenderingState next) {
		int nChannels = getNChannels();
		boolean[] pUseLights = rs.useLights();
		boolean[] pUseLUT = rs.useLUT();
		RenderingAlgorithm pAlgorithm = rs.getRenderingAlgorithm();
		boolean[] nUseLights = next.useLights();
		boolean[] nUseLUT = next.useLUT();
		RenderingAlgorithm nAlgorithm = next.getRenderingAlgorithm();

		if(Arrays.equals(pUseLights, nUseLights) && Arrays.equals(pUseLUT, nUseLUT) && pAlgorithm.equals(nAlgorithm))
			return;

		String program = null;
		switch(nAlgorithm) {
		case INDEPENDENT_TRANSPARENCY:
			program = OpenCLProgram.makeSource(nChannels, false, false, false, nUseLights, nUseLUT);
			break;
		case COMBINED_TRANSPARENCY:
			program = OpenCLProgram.makeSource(nChannels, false, true, false, nUseLights, nUseLUT);
			break;
		case MAXIMUM_INTENSITY:
			program = OpenCLProgram.makeSource(nChannels, false, false, true, nUseLights, nUseLUT);
			break;
		}
		setProgram(program);

		for(int c = 0; c < nChannels; c++) {
			if(nUseLUT[c] && !pUseLUT[c]) // lut switched on for channel c
				setLookupTable(c, makeRandomLUT());
			else if(!nUseLUT[c] && pUseLUT[c]) // lut switched of for channel c
				setLookupTable(c, null);

			if(nUseLights[c] && !pUseLights[c]) // light switched on for channel c
				super.calculateGradients(c);
			else if(!nUseLights[c] && pUseLights[c]) // light switched of for channel c
				super.clearGradients(c);
		}
	}

	@Override
	public void setTargetSize(int w, int h) {
		super.setTgtSize(w, h);
		Calibration cal = image.getCalibration();
		float pwOut = (float)(image.getWidth()  * cal.pixelWidth  / w);
		float phOut = (float)(image.getHeight() * cal.pixelHeight / h);
		float pdOut = rs.getFwdTransform().getOutputSpacing()[2];

		int iw = super.wIn;
		int ih = super.hIn;

		float ox = 0;
		float oy = 0;
		float[] p = new float[] {pwOut, phOut, pdOut};

		if(w > h * (float)iw / ih) { // output width > expected output width
			ox = (w - h * (float)iw / ih) / 2f;
			p = new float[] {phOut, phOut, pdOut};
		} else if(h > (float)w * ih / iw) {
			oy = (h - w * (float)ih / iw) / 2f;
			p = new float[] {pwOut, pwOut, pdOut};
		}
		rs.getFwdTransform().setOutputSpacing(p);
		rs.getFwdTransform().setOffset(ox, oy);
	}

	@Override
	public int getNChannels() {
		return image.getNChannels();
	}

	private Color[] calculateChannelColors() {
		int nChannels = image.getNChannels();
		Color[] channelColors = new Color[nChannels];
		if(!image.isComposite()) {
			LUT lut = image.getProcessor().getLut();
			if(lut != null) {
				channelColors[0] = getLUTColor(lut);
			} else {
				channelColors[0] = Color.WHITE;
			}
			return channelColors;
		}
		for(int c = 0; c < image.getNChannels(); c++) {
			// image.setC(c + 1);
			channelColors[c] = getLUTColor(((CompositeImage)image).getChannelLut(c + 1));
		}
		return channelColors;
	}

	private Color getLUTColor(LUT lut) {
		int index = lut.getMapSize() - 1;
		int r = lut.getRed(index);
		int g = lut.getGreen(index);
		int b = lut.getBlue(index);
		return new Color(r, g, b);
	}
}

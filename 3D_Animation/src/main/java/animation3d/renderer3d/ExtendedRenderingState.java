package animation3d.renderer3d;

import java.awt.Color;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.RenderingState;

public class ExtendedRenderingState extends RenderingState {

	public static final int INTENSITY_MIN   = 0;
	public static final int INTENSITY_MAX   = 1;
	public static final int INTENSITY_GAMMA = 2;
	public static final int ALPHA_MIN   = 3;
	public static final int ALPHA_MAX   = 4;
	public static final int ALPHA_GAMMA = 5;
	public static final int WEIGHT      = 6;
	public static final int CHANNEL_COLOR_RED   = 7;
	public static final int CHANNEL_COLOR_GREEN = 8;
	public static final int CHANNEL_COLOR_BLUE  = 9;
	public static final int BOUNDINGBOX_XMIN    = 10;
	public static final int BOUNDINGBOX_YMIN    = 11;
	public static final int BOUNDINGBOX_ZMIN    = 12;
	public static final int BOUNDINGBOX_XMAX    = 13;
	public static final int BOUNDINGBOX_YMAX    = 14;
	public static final int BOUNDINGBOX_ZMAX    = 15;
	public static final int NEAR                = 16;
	public static final int FAR                 = 17;
	public static final int USE_LIGHT           = 18;
	public static final int LIGHT_K_OBJECT      = 19;
	public static final int LIGHT_K_DIFFUSE     = 20;
	public static final int LIGHT_K_SPECULAR    = 21;
	public static final int LIGHT_SHININESS     = 22;
	public static final int USE_LUT             = 23;

	public static final int BG_COLOR_RED        = 0;
	public static final int BG_COLOR_GREEN      = 1;
	public static final int BG_COLOR_BLUE       = 2;
	public static final int TIMEPOINT           = 3;
	public static final int RENDERING_ALGORITHM = 4;
	public static final int SHOW_SCALEBAR       = 5;
	public static final int SCALEBAR_RED        = 6;
	public static final int SCALEBAR_GREEN      = 7;
	public static final int SCALEBAR_BLUE       = 8;
	public static final int SCALEBAR_LENGTH     = 9;
	public static final int SCALEBAR_WIDTH      = 10;
	public static final int SCALEBAR_POSITION   = 11;
	public static final int SCALEBAR_OFFSET     = 12;
	public static final int SHOW_BOUNDINGBOX    = 13;
	public static final int BOUNDINGBOX_RED     = 14;
	public static final int BOUNDINGBOX_GREEN   = 15;
	public static final int BOUNDINGBOX_BLUE    = 16;
	public static final int BOUNDINGBOX_WIDTH   = 17;

	private static final String[] channelPropertyNames = {
			"INTENSITY_MIN",
			"INTENSITY_MAX",
			"INTENSITY_GAMMA",
			"ALPHA_MIN",
			"ALPHA_MAX",
			"ALPHA_GAMMA",
			"WEIGHT",
			"CHANNEL_COLOR_RED",
			"CHANNEL_COLOR_GREEN",
			"CHANNEL_COLOR_BLUE",
			"BOUNDINGBOX_XMIN",
			"BOUNDINGBOX_YMIN",
			"BOUNDINGBOX_ZMIN",
			"BOUNDINGBOX_XMAX",
			"BOUNDINGBOX_YMAX",
			"BOUNDINGBOX_ZMAX",
			"NEAR",
			"FAR",
			"USE_LIGHT",
			"LIGHT_K_OBJECT",
			"LIGHT_K_DIFFUSE",
			"LIGHT_K_SPECULAR",
			"LIGHT_SHININESS",
			"USE_LUT",
	};

	private static final String[] nonChannelPropertyNames = {
			"BG_COLOR_RED",
			"BG_COLOR_GREEN",
			"BG_COLOR_BLUE",
			"TIMEPOINT",
			"RENDERING_ALGORITHM",
			"SHOW_SCALEBAR",
			"SCALEBAR_RED",
			"SCALEBAR_GREEN",
			"SCALEBAR_BLUE",
			"SCALEBAR_LENGTH",
			"SCALEBAR_WIDTH",
			"SCALEBAR_POSITION",
			"SCALEBAR_OFFSET",
			"SHOW_BOUNDINGBOX",
			"BOUNDINGBOX_RED",
			"BOUNDINGBOX_GREEN",
			"BOUNDINGBOX_BLUE",
			"BOUNDINGBOX_WIDTH",
	};

	@Override
	public String[] getNonChannelPropertyNames() {
		return nonChannelPropertyNames;
	}

	@Override
	public String[] getChannelPropertyNames() {
		return channelPropertyNames;
	}

	public ExtendedRenderingState(int frame, CombinedTransform fwdTransform, int nChannels) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[18];
		channelProperties = new double[nChannels][24];
	}

	public ExtendedRenderingState(
			int frame,
			int timepoint,
			RenderingSettings[] renderingSettings,
			Color[] channelColors,
			Color bgColor,
			RenderingAlgorithm algorithm,
			CombinedTransform fwdTransform) {
		this(frame, fwdTransform, renderingSettings.length);

		nonChannelProperties[BG_COLOR_RED]    = bgColor.getRed();
		nonChannelProperties[BG_COLOR_GREEN]  = bgColor.getGreen();
		nonChannelProperties[BG_COLOR_BLUE]   = bgColor.getBlue();
		nonChannelProperties[TIMEPOINT]       = timepoint;
		nonChannelProperties[RENDERING_ALGORITHM] = algorithm.ordinal();

		for(int c = 0; c < renderingSettings.length; c++) {
			Color cC = channelColors[c];
			RenderingSettings rs = renderingSettings[c];
			channelProperties[c][INTENSITY_MIN]       = rs.colorMin;
			channelProperties[c][INTENSITY_MAX]       = rs.colorMax;
			channelProperties[c][INTENSITY_GAMMA]     = rs.colorGamma;
			channelProperties[c][ALPHA_MIN]           = rs.alphaMin;
			channelProperties[c][ALPHA_MAX]           = rs.alphaMax;
			channelProperties[c][ALPHA_GAMMA]         = rs.alphaGamma;
			channelProperties[c][WEIGHT]              = rs.weight;
			channelProperties[c][CHANNEL_COLOR_RED]   = cC.getRed();
			channelProperties[c][CHANNEL_COLOR_GREEN] = cC.getGreen();
			channelProperties[c][CHANNEL_COLOR_BLUE]  = cC.getBlue();
			channelProperties[c][BOUNDINGBOX_XMIN]    = rs.bbx0;
			channelProperties[c][BOUNDINGBOX_YMIN]    = rs.bby0;
			channelProperties[c][BOUNDINGBOX_ZMIN]    = rs.bbz0;
			channelProperties[c][BOUNDINGBOX_XMAX]    = rs.bbx1;
			channelProperties[c][BOUNDINGBOX_YMAX]    = rs.bby1;
			channelProperties[c][BOUNDINGBOX_ZMAX]    = rs.bbz1;
			channelProperties[c][NEAR]                = rs.near;
			channelProperties[c][FAR]                 = rs.far;
			channelProperties[c][USE_LIGHT]           = renderingSettings[c].useLight ? 1 : 0;
			channelProperties[c][LIGHT_K_OBJECT]      = renderingSettings[c].k_o;
			channelProperties[c][LIGHT_K_DIFFUSE]     = renderingSettings[c].k_d;
			channelProperties[c][LIGHT_K_SPECULAR]    = renderingSettings[c].k_s;
			channelProperties[c][LIGHT_SHININESS]     = renderingSettings[c].shininess;
			channelProperties[c][USE_LUT]             = renderingSettings[c].useLUT ? 1 : 0;
		}
	}

	public int getNChannels() {
		return channelProperties.length;
	}

	public double[][] getChannelProperties() {
		return channelProperties;
	}

	public double[] getNonChannelProperties() {
		return nonChannelProperties;
	}

	public Color[] getChannelColors() {
		Color[] c  = new Color[channelProperties.length];
		for(int i = 0; i < c.length; i++) {
			c[i] = new Color(
					(int)channelProperties[i][CHANNEL_COLOR_RED],
					(int)channelProperties[i][CHANNEL_COLOR_GREEN],
					(int)channelProperties[i][CHANNEL_COLOR_BLUE]);
		}
		return c;
	}

	public void setIntensity(int channel, double min, double max, double gamma) {
		channelProperties[channel][INTENSITY_MIN]   = min;
		channelProperties[channel][INTENSITY_MAX]   = max;
		channelProperties[channel][INTENSITY_GAMMA] = gamma;
	}

	public void setAlpha(int channel, double min, double max, double gamma) {
		channelProperties[channel][ALPHA_MIN]   = min;
		channelProperties[channel][ALPHA_MAX]   = max;
		channelProperties[channel][ALPHA_GAMMA] = gamma;
	}

	public void setColor(int channel, Color c) {
		channelProperties[channel][CHANNEL_COLOR_RED]   = c.getRed();
		channelProperties[channel][CHANNEL_COLOR_GREEN] = c.getGreen();
		channelProperties[channel][CHANNEL_COLOR_BLUE]  = c.getBlue();
	}

	public void setLight(int channel, boolean useLight, double kObj, double kDiff, double kSpec, double shininess) {
		channelProperties[channel][USE_LIGHT]        = useLight ? 1 : 0;
		channelProperties[channel][LIGHT_K_OBJECT]   = kObj;
		channelProperties[channel][LIGHT_K_DIFFUSE]  = kDiff;
		channelProperties[channel][LIGHT_K_SPECULAR] = kSpec;
		channelProperties[channel][LIGHT_SHININESS]  = shininess;
	}

	public Color getBackgroundColor() {
		int r = (int)nonChannelProperties[BG_COLOR_RED];
		int g = (int)nonChannelProperties[BG_COLOR_GREEN];
		int b = (int)nonChannelProperties[BG_COLOR_BLUE];
		return new Color(r, g, b);
	}

	public void setBackgroundColor(int r, int g, int b) {
		nonChannelProperties[BG_COLOR_RED]   = r;
		nonChannelProperties[BG_COLOR_GREEN] = g;
		nonChannelProperties[BG_COLOR_BLUE]  = b;
	}

	public void setScalebarProperties(Scalebar scalebar) {
		nonChannelProperties[SHOW_SCALEBAR]     = scalebar.isVisible() ? 1 : 0;
		nonChannelProperties[SCALEBAR_RED]      = scalebar.getColor().getRed();
		nonChannelProperties[SCALEBAR_GREEN]    = scalebar.getColor().getGreen();
		nonChannelProperties[SCALEBAR_BLUE]     = scalebar.getColor().getBlue();
		nonChannelProperties[SCALEBAR_LENGTH]   = scalebar.getLength();
		nonChannelProperties[SCALEBAR_WIDTH]    = scalebar.getWidth();
		nonChannelProperties[SCALEBAR_OFFSET]   = scalebar.getOffset();
		nonChannelProperties[SCALEBAR_POSITION] = scalebar.getPosition().ordinal();
	}

	public void adjustScalebar(Scalebar sbar) {
		Color c = new Color(
				(int)nonChannelProperties[SCALEBAR_RED],
				(int)nonChannelProperties[SCALEBAR_GREEN],
				(int)nonChannelProperties[SCALEBAR_BLUE]);
		int p = Math.min((int)nonChannelProperties[SCALEBAR_POSITION], Scalebar.Position.values().length - 1);
		sbar.setVisible(nonChannelProperties[ExtendedRenderingState.SHOW_SCALEBAR] > 0.5);
		sbar.setColor(c);
		sbar.setLength((float)nonChannelProperties[ExtendedRenderingState.SCALEBAR_LENGTH]);
		sbar.setWidth((float)nonChannelProperties[ExtendedRenderingState.SCALEBAR_WIDTH]);
		sbar.setPosition(Scalebar.Position.values()[p]);
		sbar.setOffset((float)nonChannelProperties[ExtendedRenderingState.SCALEBAR_OFFSET]);
	}

	public void setBoundingboxProperties(BoundingBox bb) {
		nonChannelProperties[SHOW_BOUNDINGBOX]     = bb.isVisible() ? 1 : 0;
		nonChannelProperties[BOUNDINGBOX_RED]      = bb.getColor().getRed();
		nonChannelProperties[BOUNDINGBOX_GREEN]    = bb.getColor().getGreen();
		nonChannelProperties[BOUNDINGBOX_BLUE]     = bb.getColor().getBlue();
		nonChannelProperties[BOUNDINGBOX_WIDTH]    = bb.getWidth();
	}

	public void adjustBoundingbox(BoundingBox bb) {
		Color c = new Color(
				(int)nonChannelProperties[BOUNDINGBOX_RED],
				(int)nonChannelProperties[BOUNDINGBOX_GREEN],
				(int)nonChannelProperties[BOUNDINGBOX_BLUE]);
		bb.setVisible(nonChannelProperties[ExtendedRenderingState.SHOW_BOUNDINGBOX] > 0.5);
		bb.setColor(c);
		bb.setWidth((float)nonChannelProperties[ExtendedRenderingState.BOUNDINGBOX_WIDTH]);
	}

	public RenderingAlgorithm getRenderingAlgorithm() {
		int algo = (int)nonChannelProperties[RENDERING_ALGORITHM];
		return RenderingAlgorithm.values()[algo];
	}

	public void setRenderingAlgorithm(RenderingAlgorithm algorithm) {
		nonChannelProperties[RENDERING_ALGORITHM] = algorithm.ordinal();
	}

	public boolean[] useLights() {
		boolean[] useLights = new boolean[channelProperties.length];
		for(int c = 0; c < channelProperties.length; c++)
			useLights[c] = channelProperties[c][USE_LIGHT] > 0;
		return useLights;
	}

	public boolean[] useLUT() {
		boolean[] useLUT = new boolean[channelProperties.length];
		for(int c = 0; c < channelProperties.length; c++)
			useLUT[c] = channelProperties[c][USE_LUT] > 0;
		return useLUT;
	}

	@Override
	public ExtendedRenderingState clone() {
		ExtendedRenderingState kf = new ExtendedRenderingState(0, null, channelProperties.length);
		kf.setFrom(this);
		return kf;
	}
}

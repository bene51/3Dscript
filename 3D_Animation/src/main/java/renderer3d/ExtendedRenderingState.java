package renderer3d;

import java.awt.Color;

import textanim.CombinedTransform;
import textanim.RenderingState;

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

	public static final int BG_COLOR_RED        = 0;
	public static final int BG_COLOR_GREEN      = 1;
	public static final int BG_COLOR_BLUE       = 2;
	public static final int TIMEPOINT           = 3;
	public static final int RENDERING_ALGORITHM = 4;

	public ExtendedRenderingState(int frame, CombinedTransform fwdTransform, int nChannels) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[5];
		channelProperties = new double[nChannels][23];
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

	@Override
	public ExtendedRenderingState clone() {
		ExtendedRenderingState kf = new ExtendedRenderingState(0, null, channelProperties.length);
		kf.setFrom(this);
		return kf;
	}
}

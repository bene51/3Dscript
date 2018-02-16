package renderer3d;

import java.awt.Color;

import textanim.CombinedTransform;
import textanim.RenderingState;

public class ExtendedRenderingState extends RenderingState {

	public static final int BOUNDINGBOX_XMIN  = 0;
	public static final int BOUNDINGBOX_YMIN  = 1;
	public static final int BOUNDINGBOX_ZMIN  = 2;
	public static final int BOUNDINGBOX_XMAX  = 3;
	public static final int BOUNDINGBOX_YMAX  = 4;
	public static final int BOUNDINGBOX_ZMAX  = 5;
	public static final int NEAR              = 6;
	public static final int FAR               = 7;

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
	public static final int USE_LIGHT           = 10;
	public static final int LIGHT_K_OBJECT      = 11;
	public static final int LIGHT_K_DIFFUSE     = 12;
	public static final int LIGHT_K_SPECULAR    = 13;
	public static final int LIGHT_SHININESS     = 14;


	public ExtendedRenderingState(int frame, CombinedTransform fwdTransform, int nChannels) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[8];
		channelProperties = new double[nChannels][15];
	}

	public ExtendedRenderingState(
			int frame,
			RenderingSettings[] renderingSettings,
			Color[] channelColors,
			float near, float far,
			CombinedTransform fwdTransform,
			int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		this(frame, fwdTransform, renderingSettings.length);

		nonChannelProperties[BOUNDINGBOX_XMIN] = bbx0;
		nonChannelProperties[BOUNDINGBOX_YMIN] = bby0;
		nonChannelProperties[BOUNDINGBOX_ZMIN] = bbz0;
		nonChannelProperties[BOUNDINGBOX_XMAX] = bbx1;
		nonChannelProperties[BOUNDINGBOX_YMAX] = bby1;
		nonChannelProperties[BOUNDINGBOX_ZMAX] = bbz1;
		nonChannelProperties[NEAR]             = near;
		nonChannelProperties[FAR]              = far;

		for(int c = 0; c < renderingSettings.length; c++) {
			Color cC = channelColors[c];
			channelProperties[c][INTENSITY_MIN]   = renderingSettings[c].colorMin;
			channelProperties[c][INTENSITY_MAX]   = renderingSettings[c].colorMax;
			channelProperties[c][INTENSITY_GAMMA] = renderingSettings[c].colorGamma;
			channelProperties[c][ALPHA_MIN]   = renderingSettings[c].alphaMin;
			channelProperties[c][ALPHA_MAX]   = renderingSettings[c].alphaMax;
			channelProperties[c][ALPHA_GAMMA] = renderingSettings[c].alphaGamma;
			channelProperties[c][WEIGHT]      = renderingSettings[c].weight;
			channelProperties[c][CHANNEL_COLOR_RED]   = cC.getRed();
			channelProperties[c][CHANNEL_COLOR_GREEN] = cC.getGreen();
			channelProperties[c][CHANNEL_COLOR_BLUE]  = cC.getBlue();
			channelProperties[c][USE_LIGHT]        = renderingSettings[c].useLight ? 1 : 0;
			channelProperties[c][LIGHT_K_OBJECT]   = renderingSettings[c].k_o;
			channelProperties[c][LIGHT_K_DIFFUSE]  = renderingSettings[c].k_d;
			channelProperties[c][LIGHT_K_SPECULAR] = renderingSettings[c].k_s;
			channelProperties[c][LIGHT_SHININESS]  = renderingSettings[c].shininess;
		}
	}

	public int getNChannels() {
		return channelProperties.length;
	}

	public double[][] getChannelProperties() {
		return channelProperties;
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

	@Override
	public ExtendedRenderingState clone() {
		ExtendedRenderingState kf = new ExtendedRenderingState(0, null, channelProperties.length);
		kf.setFrom(this);
		return kf;
	}
}

package renderer3d;

import textanim.CombinedTransform;
import textanim.Keyframe2;

public class ExtendedKeyframe extends Keyframe2 {

	public static final int BOUNDINGBOX_XMIN  = 0;
	public static final int BOUNDINGBOX_YMIN  = 1;
	public static final int BOUNDINGBOX_ZMIN  = 2;
	public static final int BOUNDINGBOX_XMAX  = 3;
	public static final int BOUNDINGBOX_YMAX  = 4;
	public static final int BOUNDINGBOX_ZMAX  = 5;
	public static final int NEAR              = 6;
	public static final int FAR               = 7;

	public static final int COLOR_MIN   = 0;
	public static final int COLOR_MAX   = 1;
	public static final int COLOR_GAMMA = 2;
	public static final int ALPHA_MIN   = 3;
	public static final int ALPHA_MAX   = 4;
	public static final int ALPHA_GAMMA = 5;
	public static final int WEIGHT      = 6;

	public ExtendedKeyframe(int frame, CombinedTransform fwdTransform, int nChannels) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[8];
		channelProperties = new double[nChannels][7];
	}

	public ExtendedKeyframe(
			int frame,
			RenderingSettings[] renderingSettings,
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
			channelProperties[c][COLOR_MIN]   = renderingSettings[c].colorMin;
			channelProperties[c][COLOR_MAX]   = renderingSettings[c].colorMax;
			channelProperties[c][COLOR_GAMMA] = renderingSettings[c].colorGamma;
			channelProperties[c][ALPHA_MIN]   = renderingSettings[c].alphaMin;
			channelProperties[c][ALPHA_MAX]   = renderingSettings[c].alphaMax;
			channelProperties[c][ALPHA_GAMMA] = renderingSettings[c].alphaGamma;
			channelProperties[c][WEIGHT]      = renderingSettings[c].weight;
		}
	}

	public int getNChannels() {
		return channelProperties.length;
	}

	public double[][] getChannelProperties() {
		return channelProperties;
	}

	@Override
	public ExtendedKeyframe clone() {
		ExtendedKeyframe kf = new ExtendedKeyframe(0, null, channelProperties.length);
		kf.setFrom(this);
		return kf;
	}
}

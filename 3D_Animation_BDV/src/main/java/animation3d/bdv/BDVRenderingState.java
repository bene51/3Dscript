package animation3d.bdv;

import java.awt.Color;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.RenderingState;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;

public class BDVRenderingState extends RenderingState {

	// global properties
	public static final int DISPLAY_MODE   = 0;
	public static final int TIMEPOINT      = 1;
	public static final int INTERPOLATION  = 2;
	public static final int CURRENT_SOURCE = 3;

	// channel properties
	public static final int INTENSITY_MIN       = 0;
	public static final int INTENSITY_MAX       = 1;
	public static final int CHANNEL_COLOR_RED   = 2;
	public static final int CHANNEL_COLOR_GREEN = 3;
	public static final int CHANNEL_COLOR_BLUE  = 4;

	public BDVRenderingState(int frame, CombinedTransform fwdTransform, int nChannels) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[4];
		channelProperties = new double[nChannels][5];
	}

	public BDVRenderingState(
			int frame,
			DisplayMode displaymode,
			int timepoint,
			Interpolation interpolation,
			int currentSource,
			CombinedTransform fwdTransform,
			Color[] channelColors,
			double[] channelMin,
			double[] channelMax) {
		this(frame, fwdTransform, channelColors.length);
		nonChannelProperties[DISPLAY_MODE]   = displaymode.ordinal();
		nonChannelProperties[TIMEPOINT]      = timepoint;
		nonChannelProperties[INTERPOLATION]  = interpolation.ordinal();
		nonChannelProperties[CURRENT_SOURCE] = currentSource;
		for(int c = 0; c < channelColors.length; c++) {
			Color cC = channelColors[c];
			channelProperties[c][INTENSITY_MIN]       = channelMin[c];
			channelProperties[c][INTENSITY_MAX]       = channelMax[c];
			channelProperties[c][CHANNEL_COLOR_RED]   = cC.getRed();
			channelProperties[c][CHANNEL_COLOR_GREEN] = cC.getGreen();
			channelProperties[c][CHANNEL_COLOR_BLUE]  = cC.getBlue();
		}
	}

	public DisplayMode getDisplayMode() {
		return DisplayMode.values()[(int)nonChannelProperties[DISPLAY_MODE]];
	}

	public int getTimepoint() {
		return (int)nonChannelProperties[TIMEPOINT];
	}

	public Interpolation getInterpolation() {
		return Interpolation.values()[(int)nonChannelProperties[INTERPOLATION]];
	}

	public int getCurrentSource() {
		return (int)nonChannelProperties[CURRENT_SOURCE];
	}

	public Color getChannelColor(int c) {
		int red   = (int)channelProperties[c][CHANNEL_COLOR_RED];
		int green = (int)channelProperties[c][CHANNEL_COLOR_GREEN];
		int blue  = (int)channelProperties[c][CHANNEL_COLOR_BLUE];
		return new Color(red, green, blue);
	}

	public void setChannelColor(int c, Color col) {
		channelProperties[c][CHANNEL_COLOR_RED]   = col.getRed();
		channelProperties[c][CHANNEL_COLOR_GREEN] = col.getGreen();
		channelProperties[c][CHANNEL_COLOR_BLUE]  = col.getBlue();
	}

	public double getChannelMin(int c) {
		return channelProperties[c][INTENSITY_MIN];
	}

	public double getChannelMax(int c) {
		return channelProperties[c][INTENSITY_MAX];
	}

	public void setChannelMinAndMax(int c, double min, double max) {
		channelProperties[c][INTENSITY_MIN] = min;
		channelProperties[c][INTENSITY_MAX] = max;
	}

	@Override
	public BDVRenderingState clone() {
		BDVRenderingState kf = new BDVRenderingState(0, null, channelProperties.length);
		kf.setFrom(this);
		return kf;
	}
}

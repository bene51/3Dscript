package animation3d.bdv;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.RenderingState;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;

public class BDVRenderingState extends RenderingState {

	public static final int DISPLAY_MODE   = 0;
	public static final int TIMEPOINT      = 1;
	public static final int INTERPOLATION  = 2;
	public static final int CURRENT_SOURCE = 3;

	public BDVRenderingState(int frame, CombinedTransform fwdTransform) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[4];
		channelProperties = null;
	}

	public BDVRenderingState(
			int frame,
			DisplayMode displaymode,
			int timepoint,
			Interpolation interpolation,
			int currentSource,
			CombinedTransform fwdTransform) {
		this(frame, fwdTransform);
		nonChannelProperties[DISPLAY_MODE]   = displaymode.ordinal();
		nonChannelProperties[TIMEPOINT]      = timepoint;
		nonChannelProperties[INTERPOLATION]  = interpolation.ordinal();
		nonChannelProperties[CURRENT_SOURCE] = currentSource;
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

	@Override
	public BDVRenderingState clone() {
		BDVRenderingState kf = new BDVRenderingState(0, null);
		kf.setFrom(this);
		return kf;
	}
}

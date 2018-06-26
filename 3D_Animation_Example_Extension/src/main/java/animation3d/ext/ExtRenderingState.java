package animation3d.ext;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.RenderingState;

public class ExtRenderingState extends RenderingState {

	public static final int BRIGHTNESS  = 0;
	public static final int COLOR_RED   = 1;
	public static final int COLOR_GREEN = 2;
	public static final int COLOR_BLUE  = 3;

	public ExtRenderingState(int frame, CombinedTransform fwdTransform) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[4];
		channelProperties = null;
	}

	public ExtRenderingState(
			int frame,
			double brightness,
			CombinedTransform fwdTransform) {
		this(frame, fwdTransform);

		nonChannelProperties[BRIGHTNESS] = brightness;
	}

	@Override
	public ExtRenderingState clone() {
		ExtRenderingState kf = new ExtRenderingState(0, null);
		kf.setFrom(this);
		return kf;
	}
}

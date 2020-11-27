package animation3d.ext;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.RenderingState;

public class ExtRenderingState extends RenderingState {

	public static final int BRIGHTNESS  = 0;
	public static final int SCALE_X     = 1;
	public static final int SCALE_Y     = 2;

	public ExtRenderingState(int frame) {
		super(frame, new CombinedTransform(new float[] {1, 1, 1}, new float[] {1, 1, 1}, new float[] {1, 1, 1}));
		nonChannelProperties = new double[3];
		channelProperties = null;
	}

	public ExtRenderingState(
			int frame,
			double brightness,
			double x,
			double y) {
		this(frame);

		nonChannelProperties[BRIGHTNESS] = brightness;
		nonChannelProperties[SCALE_X] = x;
		nonChannelProperties[SCALE_Y] = y;
	}

	@Override
	public ExtRenderingState clone() {
		ExtRenderingState kf = new ExtRenderingState(0);
		kf.setFrom(this);
		return kf;
	}
}

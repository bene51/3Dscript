package animation3d.povray;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.RenderingState;

public class PovrayRenderingState extends RenderingState {

	public static final int LENS_X  = 0;
	public static final int LENS_Y  = 1;
	public static final int LENS_Z  = 2;

	public PovrayRenderingState(int frame, CombinedTransform fwdTransform) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[3];
		channelProperties = null;
	}

	public PovrayRenderingState(
			int frame,
			CombinedTransform fwdTransform,
			float lensx, float lensy, float lensz) {
		this(frame, fwdTransform);

		nonChannelProperties[LENS_X] = lensx;
		nonChannelProperties[LENS_Y] = lensy;
		nonChannelProperties[LENS_Z] = lensz;
	}

	@Override
	public PovrayRenderingState clone() {
		PovrayRenderingState kf = new PovrayRenderingState(0, null);
		kf.setFrom(this);
		return kf;
	}
}

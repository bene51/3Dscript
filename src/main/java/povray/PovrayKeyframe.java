package povray;

import textanim.CombinedTransform;
import textanim.Keyframe2;

public class PovrayKeyframe extends Keyframe2 {

	public static final int LENS_X  = 0;
	public static final int LENS_Y  = 1;
	public static final int LENS_Z  = 2;

	public PovrayKeyframe(int frame, CombinedTransform fwdTransform) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[3];
		channelProperties = null;
	}

	public PovrayKeyframe(
			int frame,
			CombinedTransform fwdTransform,
			float lensx, float lensy, float lensz) {
		this(frame, fwdTransform);

		nonChannelProperties[LENS_X] = lensx;
		nonChannelProperties[LENS_Y] = lensy;
		nonChannelProperties[LENS_Z] = lensz;
	}

	@Override
	public PovrayKeyframe clone() {
		PovrayKeyframe kf = new PovrayKeyframe(0, null);
		kf.setFrom(this);
		return kf;
	}
}

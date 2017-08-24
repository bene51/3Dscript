package textanim;

import java.util.List;
import java.util.Map;

import parser.NoSuchMacroException;
import parser.NumberOrMacro;
import renderer3d.Keyframe;
import renderer3d.Transform;

public class ScaleAnimation extends TransformationAnimation {

	private NumberOrMacro byFactor;
	private float[] center;

	public ScaleAnimation(int fromFrame, int toFrame, NumberOrMacro byFactor, float[] center) {
		super(fromFrame, toFrame);
		this.byFactor = byFactor;
		this.center = new float[3];
		System.arraycopy(center, 0, this.center, 0, center.length);
	}

	@Override
	public void pickScripts(Map<String, String> scripts) throws NoSuchMacroException {
		pickScripts(scripts, byFactor);
	}

	@Override
	public void adjustKeyframe(Keyframe current, List<Keyframe> previous) {}


	private float evalOrInterpolate(int frame, NumberOrMacro n) {
		return n.isMacro() ? (float)n.evaluateMacro(frame) : (float)interpolate(frame, 1, n.getValue());
	}

	@Override
	public void getTransformationAt(int frame, float[] matrix) {
		float factor = evalOrInterpolate(frame, byFactor);
		float[] scale = Transform.fromScale(factor, null);
		scale = Transform.mul(scale, Transform.fromTranslation(-center[0], -center[1], -center[2], matrix));
		scale = Transform.mul(Transform.fromTranslation(center[0], center[1], center[2], null), scale);
		System.arraycopy(scale, 0, matrix, 0, 12);
	}
}

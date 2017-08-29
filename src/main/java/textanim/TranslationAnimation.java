package textanim;

import java.util.List;

import parser.NumberOrMacro;
import renderer3d.Keyframe;
import renderer3d.Transform;

public class TranslationAnimation extends TransformationAnimation {

	private NumberOrMacro byX;
	private NumberOrMacro byY;
	private NumberOrMacro byZ;

	public TranslationAnimation(int fromFrame, int toFrame, NumberOrMacro byX, NumberOrMacro byY, NumberOrMacro byZ) {
		super(fromFrame, toFrame);
		this.byX = byX;
		this.byY = byY;
		this.byZ = byZ;
	}

	@Override
	public NumberOrMacro[] getNumberOrMacros() {
		return new NumberOrMacro[] {byX, byY, byZ};
	}

	@Override
	public void adjustKeyframe(Keyframe current, List<Keyframe> previous) {}

	private float evalOrInterpolate(int frame, NumberOrMacro n) {
		return n.isMacro() ? (float)n.evaluateMacro(frame, fromFrame, toFrame) : (float)interpolate(frame, 0, n.getValue());
	}

	@Override
	public void getTransformationAt(int frame, float[] matrix) {
		float dx = evalOrInterpolate(frame, byX);
		float dy = evalOrInterpolate(frame, byY);
		float dz = evalOrInterpolate(frame, byZ);
		Transform.fromTranslation(dx, dy, dz, matrix);
	}
}

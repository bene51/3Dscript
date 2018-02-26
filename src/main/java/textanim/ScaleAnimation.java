package textanim;

import java.util.List;

import parser.NumberOrMacro;
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
	public NumberOrMacro[] getNumberOrMacros() {
		return new NumberOrMacro[] {byFactor};
	}

	@Override
	public void adjustRenderingState(RenderingState current, List<RenderingState> previous, int nChannels) {}


	private float evalOrInterpolate(int frame, NumberOrMacro n) {
		return n.isMacro() ?
				(float)n.evaluateMacro(frame, fromFrame, toFrame) :
				(float)interpolate(frame, 1, n.getValue());
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

package animation3d.textanim;

import java.util.List;

import animation3d.parser.NumberOrMacro;
import animation3d.util.Transform;

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
	public void adjustRenderingState(RenderingState current, List<RenderingState> previous, int nChannels) {
		super.adjustRenderingState(current, previous, nChannels);
	}

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

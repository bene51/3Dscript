package animation3d.textanim;

import java.util.List;

import animation3d.parser.NumberOrMacro;
import animation3d.util.Transform;

public class ResetTransformAnimation extends TransformationAnimation {

	private float[] center;

	public ResetTransformAnimation(int fromFrame, int toFrame, float[] center) {
		super(fromFrame, toFrame);
		this.center = new float[3];
		System.arraycopy(center, 0, this.center, 0, center.length);
	}

	@Override
	public NumberOrMacro[] getNumberOrMacros() {
		return new NumberOrMacro[] { };
	}

	@Override
	public void adjustRenderingState(RenderingState current, List<RenderingState> previous, int nChannels) {
		int frame = current.getFrame();
		if(frame >= fromFrame)
			current.getFwdTransform().setTransformation(Transform.fromIdentity(null));
	}

	@Override
	public void getTransformationAt(int frame, float[] matrix) {
		Transform.fromIdentity(matrix);
	}
}
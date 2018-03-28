package textanim;

import java.util.List;

import renderer3d.Transform;

public abstract class TransformationAnimation extends Animation {

	public static final int X_AXIS = 0;
	public static final int Y_AXIS = 1;
	public static final int Z_AXIS = 2;

	public static final float[][] AXIS_VECTOR = {
			{1, 0, 0},
			{0, 1, 0},
			{0, 0, 1},
	};

	public TransformationAnimation(int fromFrame, int toFrame) {
		super(fromFrame, toFrame);
	}

	@Override
	public void adjustRenderingState(RenderingState current, List<RenderingState> previous, int nChannels) {
		int frame = current.getFrame();
		float[] x = new float[12];
		getTransformationAt(frame, x);

		float[] fwd = current.getFwdTransform().calculateForwardTransformWithoutCalibration();
		fwd = Transform.mul(x, fwd);
		current.getFwdTransform().setTransformation(fwd);
	}

	public abstract void getTransformationAt(int frame, float[] matrix);

}

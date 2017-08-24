package textanim;

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

	public abstract void getTransformationAt(int frame, float[] matrix);

}

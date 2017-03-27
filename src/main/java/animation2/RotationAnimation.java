package animation2;

import java.util.HashMap;


public class RotationAnimation implements TransformationAnimation {

	private final Timeline timeline;
	private final float[] axis;
	private final float[] center;

	public RotationAnimation(float[] axis, float[] center) {
		timeline = new Timeline();
		this.axis = new float[3];
		this.center = new float[3];
		System.arraycopy(axis, 0, this.axis, 0, 3);
		System.arraycopy(center, 0, this.center, 0, 3);
	}

	@Override
	public HashMap<String, Timeline> getTimelines() {
		HashMap<String, Timeline> timelines = new HashMap<String, Timeline>();
		timelines.put("Rotation", timeline);
		return timelines;
	}

	public RotationAnimation(float[] axis, float[] center, int fromFrame, int angle0, int angle1, int nFrames, double smoothness) {
		this(axis, center);
		addSegment(fromFrame, angle0, angle1, nFrames, smoothness);
	}

	public void addSegment(int fromFrame, int angle0, int angle1, int nFrames, double smoothness) {
		timeline.addSegment(new Segment(fromFrame, angle0, fromFrame + nFrames - 1, angle1, smoothness));
	}

	@Override
	public void getTransformationAt(int frame, float[] matrix) {
		double angle = timeline.getInterpolatedValue(frame);
		float rad = (float)(Math.PI * angle / 180.0);
		float[] rot = Transform.fromAngleAxis(axis, rad, null);
		rot = Transform.mul(rot, Transform.fromTranslation(-center[0], -center[1], -center[2], matrix));
		rot = Transform.mul(Transform.fromTranslation(center[0], center[1], center[2], null), rot);
		System.arraycopy(rot, 0, matrix, 0, 12);
	}
}

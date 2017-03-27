package animation2;

import java.util.HashMap;


public class ScaleAnimation implements TransformationAnimation {

	private final Timeline timelineX, timelineY, timelineZ;
	private final float[] center;

	public ScaleAnimation(float[] center) {
		timelineX = new Timeline();
		timelineY = new Timeline();
		timelineZ = new Timeline();
		this.center = new float[3];
		System.arraycopy(center, 0, this.center, 0, 3);
	}

	public ScaleAnimation(float[] center, int fromFrame, double scale0, double scale1, int nFrames, double smoothness) {
		this(center);
		addSegment(fromFrame, scale0, scale1, nFrames, smoothness);
	}

	@Override
	public HashMap<String, Timeline> getTimelines() {
		HashMap<String, Timeline> timelines = new HashMap<String, Timeline>();
		timelines.put("Scale X", timelineX);
		timelines.put("Scale Y", timelineY);
		timelines.put("Scale Z", timelineZ);
		return timelines;
	}

	public void addSegment(
			int fromFrame,
			double scale0,
			double scale1,
			int nFrames,
			double smoothness) {
		timelineX.addSegment(new Segment(
				fromFrame, scale0, fromFrame + nFrames - 1, scale1, smoothness));
		timelineY.addSegment(new Segment(
				fromFrame, scale0, fromFrame + nFrames - 1, scale1, smoothness));
		timelineZ.addSegment(new Segment(
				fromFrame, scale0, fromFrame + nFrames - 1, scale1, smoothness));
	}

	public void addSegment(
			int fromFrame,
			double[] scale0,
			double[] scale1,
			int nFrames,
			double smoothness) {
		timelineX.addSegment(new Segment(
				fromFrame, scale0[0], fromFrame + nFrames - 1, scale1[0], smoothness));
		timelineY.addSegment(new Segment(
				fromFrame, scale0[1], fromFrame + nFrames - 1, scale1[1], smoothness));
		timelineZ.addSegment(new Segment(
				fromFrame, scale0[2], fromFrame + nFrames - 1, scale1[2], smoothness));
	}

	@Override
	public void getTransformationAt(int frame, float[] matrix) {
		double scaleX = timelineX.getInterpolatedValue(frame);
		double scaleY = timelineY.getInterpolatedValue(frame);
		double scaleZ = timelineZ.getInterpolatedValue(frame);
		float[] sc = Transform.fromScale((float)scaleX, (float)scaleY, (float)scaleZ, null);
		sc = Transform.mul(sc, Transform.fromTranslation(-center[0], -center[1], -center[2], matrix));
		sc = Transform.mul(Transform.fromTranslation(center[0], center[1], center[2], null), sc);
		System.arraycopy(sc, 0, matrix, 0, 12);
	}
}

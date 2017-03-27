package animation2;

import java.util.HashMap;


public class TranslationAnimation implements TransformationAnimation {

	private final Timeline timelineX;
	private final Timeline timelineY;
	private final Timeline timelineZ;

	public TranslationAnimation(int fromFrame, float[] pos0, float[] pos1, int nFrames, double smoothness) {
		timelineX = new Timeline();
		timelineY = new Timeline();
		timelineZ = new Timeline();
		addSegment(fromFrame, pos0, pos1, nFrames, smoothness);
	}

	public void addSegment(int fromFrame, float[] pos0, float[] pos1, int nFrames, double smoothness) {
		timelineX.addSegment(new Segment(fromFrame, pos0[0], fromFrame + nFrames - 1, pos1[0], smoothness));
		timelineY.addSegment(new Segment(fromFrame, pos0[1], fromFrame + nFrames - 1, pos1[1], smoothness));
		timelineZ.addSegment(new Segment(fromFrame, pos0[2], fromFrame + nFrames - 1, pos1[2], smoothness));
	}

	@Override
	public HashMap<String, Timeline> getTimelines() {
		HashMap<String, Timeline> timelines = new HashMap<String, Timeline>();
		timelines.put("Translation X", timelineX);
		timelines.put("Translation Y", timelineY);
		timelines.put("Translation Z", timelineZ);
		return timelines;
	}

	@Override
	public void getTransformationAt(int frame, float[] matrix) {
		double dx = timelineX.getInterpolatedValue(frame);
		double dy = timelineY.getInterpolatedValue(frame);
		double dz = timelineZ.getInterpolatedValue(frame);
		Transform.fromTranslation((float)dx, (float)dy, (float)dz, matrix);
	}
}

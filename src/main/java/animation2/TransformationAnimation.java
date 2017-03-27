package animation2;

import java.util.HashMap;


public interface TransformationAnimation {

	public void getTransformationAt(int frame, float[] matrix);

	public HashMap<String, Timeline> getTimelines();
}

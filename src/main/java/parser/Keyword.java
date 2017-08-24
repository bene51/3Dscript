package parser;

import renderer3d.Keyframe;

public interface Keyword {

	public String text();

	public int length();


	public enum GeneralKeyword implements Keyword {
		FROM_FRAME("From frame"),
		TO_FRAME("to frame"),
		AT_FRAME("At frame"),
		ROTATE("rotate by"),
		DEGREES("degrees"),
		AROUND("around"),
		TRANSLATE("translate"),
		BY("by"),
		ZOOM("zoom by a factor of"),
		CHANGE("change"),
		CHANNEL("channel"),
		FROM("from"),
		TO("to"),
		HORIZONTALLY("horizontally"),
		VERTICALLY("vertically");


		private final String text;

		private GeneralKeyword(String text) {
			this.text = text;
		}

		@Override
		public String text() {
			return text;
		}

		@Override
		public int length() {
			return text.length();
		}
	}


	public static enum NonchannelProperty implements Keyword {

		BOUNDING_BOX_X_MIN("bounding box min x", Keyframe.BOUNDINGBOX_XMIN),
		BOUNDING_BOX_Y_MIN("bounding box min y", Keyframe.BOUNDINGBOX_YMIN),
		BOUNDING_BOX_Z_MIN("bounding box min z", Keyframe.BOUNDINGBOX_ZMIN),
		BOUNDING_BOX_X_MAX("bounding box max x", Keyframe.BOUNDINGBOX_XMAX),
		BOUNDING_BOX_Y_MAX("bounding box max y", Keyframe.BOUNDINGBOX_YMAX),
		BOUNDING_BOX_Z_MAX("bounding box max z", Keyframe.BOUNDINGBOX_ZMAX),

		FRONT_CLIPPING("front clipping", Keyframe.NEAR),
		BACK_CLIPPING("back clipping", Keyframe.FAR);

		private final String text;
		private final int timelineIdx;

		private NonchannelProperty(String text, int timelineIdx) {
			this.text = text;
			this.timelineIdx = timelineIdx;
		}

		public int getTimelineIndex() {
			return timelineIdx;
		}

		@Override
		public String text() {
			return text;
		}

		@Override
		public int length() {
			return text.length();
		}
	}

	public static enum ChannelProperty implements Keyword {

		COLOR_MIN("min color",     Keyframe.COLOR_MIN),
		COLOR_MAX("max color",     Keyframe.COLOR_MAX),
		COLOR_GAMMA("color gamma", Keyframe.COLOR_GAMMA),

		ALPHA_MIN("min alpha",     Keyframe.ALPHA_MIN),
		ALPHA_MAX("max alpha",     Keyframe.ALPHA_MAX),
		ALPHA_GAMMA("alpha gamma", Keyframe.ALPHA_GAMMA),

		WEIGHT("weight",           Keyframe.WEIGHT);

		private final String text;
		private final int timelineIdx;

		private ChannelProperty(String text, int timelineIdx) {
			this.text = text;
			this.timelineIdx = timelineIdx;
		}

		public int getTimelineIndex(int channel) {
			return Keyframe.getNumberOfNonChannelProperties() + channel * Keyframe.getNumberOfChannelProperties() + timelineIdx;
		}

		@Override
		public String text() {
			return text;
		}

		@Override
		public int length() {
			return text.length();
		}
	}

	public static enum Transition implements Keyword {

		LINEAR("linear",           new float[] {0, 0, 1, 1}),
		EASE_IN_OUT("ease-in-out", new float[] {0.42f, 0, 0.58f, 1}),
		EASE_IN("ease-in",         new float[] {0.42f, 0, 1, 1}),
		EASE_OUT("ease-out",       new float[] {0, 0, 0.58f, 1}),
		EASE("ease",               new float[] {0.25f, 0.1f, 0.25f, 1});

		private final String text;

		private final float[] transition;

		private Transition(String text, float[] transition) {
			this.text = text;
			this.transition = transition;
		}

		@Override
		public String text() {
			return text;
		}

		@Override
		public int length() {
			return text.length();
		}

		public float[] getTransition() {
			return transition;
		}
	}
}
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

		BOUNDING_BOX_X_MIN("bounding box min x", new String[] {"<x>"}, Keyframe.BOUNDINGBOX_XMIN),
		BOUNDING_BOX_Y_MIN("bounding box min y", new String[] {"<y>"}, Keyframe.BOUNDINGBOX_YMIN),
		BOUNDING_BOX_Z_MIN("bounding box min z", new String[] {"<z>"}, Keyframe.BOUNDINGBOX_ZMIN),
		BOUNDING_BOX_X_MAX("bounding box max x", new String[] {"<x>"}, Keyframe.BOUNDINGBOX_XMAX),
		BOUNDING_BOX_Y_MAX("bounding box max y", new String[] {"<y>"}, Keyframe.BOUNDINGBOX_YMAX),
		BOUNDING_BOX_Z_MAX("bounding box max z", new String[] {"<z>"}, Keyframe.BOUNDINGBOX_ZMAX),

		BOUNDING_BOX_X("bounding box x", new String[] {"<xmin>", "<xmax>"}, Keyframe.BOUNDINGBOX_XMIN, Keyframe.BOUNDINGBOX_XMAX),
		BOUNDING_BOX_Y("bounding box y", new String[] {"<ymin>", "<ymax>"}, Keyframe.BOUNDINGBOX_YMIN, Keyframe.BOUNDINGBOX_YMAX),
		BOUNDING_BOX_Z("bounding box z", new String[] {"<zmin>", "<zmax>"}, Keyframe.BOUNDINGBOX_ZMIN, Keyframe.BOUNDINGBOX_ZMAX),

		FRONT_CLIPPING("front clipping", new String[] {"<front>"}, Keyframe.NEAR),
		BACK_CLIPPING("back clipping",   new String[] {"<back>"},  Keyframe.FAR),

		FRONT_BACK_CLIPPING("front/back clipping", new String[] {"<front>", "<back>"}, Keyframe.NEAR, Keyframe.FAR);

		private final String text;
		private final String[] autocompletionDesc;
		private final int[] timelineIdcs;

		private NonchannelProperty(String text, String[] autocompletionDesc, int... timelineIdcs) {
			this.text = text;
			this.autocompletionDesc = autocompletionDesc;
			this.timelineIdcs = timelineIdcs;
		}

		public int[] getTimelineIndices() {
			return timelineIdcs;
		}

		public String[] getAutocompletionDescriptions() {
			return autocompletionDesc;
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

		COLOR_MIN("min color",     new String[] {"<min>"},    Keyframe.COLOR_MIN),
		COLOR_MAX("max color",     new String[] {"<max>"},    Keyframe.COLOR_MAX),
		COLOR_GAMMA("color gamma", new String[] {"<gamma>"},  Keyframe.COLOR_GAMMA),

		ALPHA_MIN("min alpha",     new String[] {"<min>"},    Keyframe.ALPHA_MIN),
		ALPHA_MAX("max alpha",     new String[] {"<max>"},    Keyframe.ALPHA_MAX),
		ALPHA_GAMMA("alpha gamma", new String[] {"<gamma>"},  Keyframe.ALPHA_GAMMA),

		COLOR("color",             new String[] {"<min>", "<max>", "<gamma>"},  Keyframe.COLOR_MIN, Keyframe.COLOR_MAX, Keyframe.COLOR_GAMMA),
		ALPHA("alpha",             new String[] {"<min>", "<max>", "<gamma>"},  Keyframe.ALPHA_MIN, Keyframe.ALPHA_MAX, Keyframe.ALPHA_GAMMA),

		WEIGHT("weight",           new String[] {"<weight>"},  Keyframe.WEIGHT);

		private final String text;
		private final String[] autocompletionDesc;
		private final int[] timelineIdcs;

		private ChannelProperty(String text, String[] autocompletionDesc, int... timelineIdcs) {
			this.text = text;
			this.autocompletionDesc = autocompletionDesc;
			this.timelineIdcs = timelineIdcs;
		}

		private int getTimelineIndex(int channel, int timelineIdx) {
			return Keyframe.getNumberOfNonChannelProperties() + channel * Keyframe.getNumberOfChannelProperties() + timelineIdx;
		}

		public int[] getTimelineIndices(int channel) {
			int[] indices = new int[timelineIdcs.length];
			for(int i = 0; i < indices.length; i++)
				indices[i] = getTimelineIndex(channel, timelineIdcs[i]);
			return indices;
		}

		public String[] getAutocompletionDescriptions() {
			return autocompletionDesc;
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

		NONE("(none)",             new float[] {0, 0, 1, 1}),
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
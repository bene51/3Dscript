package renderer3d;

import parser.Keyword2;
import textanim.KeywordFactory;

public class ExtendedKeywordFactory implements KeywordFactory {

	@Override
	public Keyword2[] getNonChannelKeywords() {
		return NonChannelKeyword.values();
	}

	@Override
	public Keyword2[] getChannelKeywords() {
		return ChannelKeyword.values();
	}

	public static enum ChannelKeyword implements Keyword2 {

		COLOR_MIN("min color",     new String[] {"<min>"},    ExtendedKeyframe.COLOR_MIN),
		COLOR_MAX("max color",     new String[] {"<max>"},    ExtendedKeyframe.COLOR_MAX),
		COLOR_GAMMA("color gamma", new String[] {"<gamma>"},  ExtendedKeyframe.COLOR_GAMMA),

		ALPHA_MIN("min alpha",     new String[] {"<min>"},    ExtendedKeyframe.ALPHA_MIN),
		ALPHA_MAX("max alpha",     new String[] {"<max>"},    ExtendedKeyframe.ALPHA_MAX),
		ALPHA_GAMMA("alpha gamma", new String[] {"<gamma>"},  ExtendedKeyframe.ALPHA_GAMMA),

		COLOR("color",             new String[] {"<min>", "<max>", "<gamma>"},  ExtendedKeyframe.COLOR_MIN, ExtendedKeyframe.COLOR_MAX, ExtendedKeyframe.COLOR_GAMMA),
		ALPHA("alpha",             new String[] {"<min>", "<max>", "<gamma>"},  ExtendedKeyframe.ALPHA_MIN, ExtendedKeyframe.ALPHA_MAX, ExtendedKeyframe.ALPHA_GAMMA),

		WEIGHT("weight",           new String[] {"<weight>"},  ExtendedKeyframe.WEIGHT);

		private final String keyword;
		private final String[] autocompletionDesc;
		private final int[] keyframeProperties;

		private ChannelKeyword(String text, String[] autocompletionDesc, int... keyframeProperties) {
			this.keyword = text;
			this.autocompletionDesc = autocompletionDesc;
			this.keyframeProperties = keyframeProperties;
		}

		@Override
		public int[] getKeyframeProperties() {
			return keyframeProperties;
		}

		@Override
		public String[] getAutocompletionDescriptions() {
			return autocompletionDesc;
		}

		@Override
		public String getKeyword() {
			return keyword;
		}

		@Override
		public int length() {
			return keyword.length();
		}
	}

	public static enum NonChannelKeyword implements Keyword2 {

		BOUNDING_BOX_X_MIN("bounding box min x", new String[] {"<x>"}, ExtendedKeyframe.BOUNDINGBOX_XMIN),
		BOUNDING_BOX_Y_MIN("bounding box min y", new String[] {"<y>"}, ExtendedKeyframe.BOUNDINGBOX_YMIN),
		BOUNDING_BOX_Z_MIN("bounding box min z", new String[] {"<z>"}, ExtendedKeyframe.BOUNDINGBOX_ZMIN),
		BOUNDING_BOX_X_MAX("bounding box max x", new String[] {"<x>"}, ExtendedKeyframe.BOUNDINGBOX_XMAX),
		BOUNDING_BOX_Y_MAX("bounding box max y", new String[] {"<y>"}, ExtendedKeyframe.BOUNDINGBOX_YMAX),
		BOUNDING_BOX_Z_MAX("bounding box max z", new String[] {"<z>"}, ExtendedKeyframe.BOUNDINGBOX_ZMAX),

		BOUNDING_BOX_X("bounding box x", new String[] {"<xmin>", "<xmax>"}, ExtendedKeyframe.BOUNDINGBOX_XMIN, ExtendedKeyframe.BOUNDINGBOX_XMAX),
		BOUNDING_BOX_Y("bounding box y", new String[] {"<ymin>", "<ymax>"}, ExtendedKeyframe.BOUNDINGBOX_YMIN, ExtendedKeyframe.BOUNDINGBOX_YMAX),
		BOUNDING_BOX_Z("bounding box z", new String[] {"<zmin>", "<zmax>"}, ExtendedKeyframe.BOUNDINGBOX_ZMIN, ExtendedKeyframe.BOUNDINGBOX_ZMAX),

		FRONT_CLIPPING("front clipping", new String[] {"<front>"}, ExtendedKeyframe.NEAR),
		BACK_CLIPPING("back clipping",   new String[] {"<back>"},  ExtendedKeyframe.FAR),

		FRONT_BACK_CLIPPING("front/back clipping", new String[] {"<front>", "<back>"}, ExtendedKeyframe.NEAR, ExtendedKeyframe.FAR);

		private final String keyword;
		private final String[] autocompletionDesc;
		private final int[] keyframeProperties;

		private NonChannelKeyword(String text, String[] autocompletionDesc, int... keyframeProperties) {
			this.keyword = text;
			this.autocompletionDesc = autocompletionDesc;
			this.keyframeProperties = keyframeProperties;
		}

		@Override
		public int[] getKeyframeProperties() {
			return keyframeProperties;
		}

		@Override
		public String[] getAutocompletionDescriptions() {
			return autocompletionDesc;
		}

		@Override
		public String getKeyword() {
			return keyword;
		}

		@Override
		public int length() {
			return keyword.length();
		}
	}
}

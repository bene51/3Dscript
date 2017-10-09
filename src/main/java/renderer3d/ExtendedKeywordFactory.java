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

		COLOR_MIN("min color",     new String[] {"<min>"},    ExtendedRenderingState.COLOR_MIN),
		COLOR_MAX("max color",     new String[] {"<max>"},    ExtendedRenderingState.COLOR_MAX),
		COLOR_GAMMA("color gamma", new String[] {"<gamma>"},  ExtendedRenderingState.COLOR_GAMMA),

		ALPHA_MIN("min alpha",     new String[] {"<min>"},    ExtendedRenderingState.ALPHA_MIN),
		ALPHA_MAX("max alpha",     new String[] {"<max>"},    ExtendedRenderingState.ALPHA_MAX),
		ALPHA_GAMMA("alpha gamma", new String[] {"<gamma>"},  ExtendedRenderingState.ALPHA_GAMMA),

		COLOR("color",             new String[] {"<min>", "<max>", "<gamma>"},  ExtendedRenderingState.COLOR_MIN, ExtendedRenderingState.COLOR_MAX, ExtendedRenderingState.COLOR_GAMMA),
		ALPHA("alpha",             new String[] {"<min>", "<max>", "<gamma>"},  ExtendedRenderingState.ALPHA_MIN, ExtendedRenderingState.ALPHA_MAX, ExtendedRenderingState.ALPHA_GAMMA),

		WEIGHT("weight",           new String[] {"<weight>"},  ExtendedRenderingState.WEIGHT);

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

		BOUNDING_BOX_X_MIN("bounding box min x", new String[] {"<x>"}, ExtendedRenderingState.BOUNDINGBOX_XMIN),
		BOUNDING_BOX_Y_MIN("bounding box min y", new String[] {"<y>"}, ExtendedRenderingState.BOUNDINGBOX_YMIN),
		BOUNDING_BOX_Z_MIN("bounding box min z", new String[] {"<z>"}, ExtendedRenderingState.BOUNDINGBOX_ZMIN),
		BOUNDING_BOX_X_MAX("bounding box max x", new String[] {"<x>"}, ExtendedRenderingState.BOUNDINGBOX_XMAX),
		BOUNDING_BOX_Y_MAX("bounding box max y", new String[] {"<y>"}, ExtendedRenderingState.BOUNDINGBOX_YMAX),
		BOUNDING_BOX_Z_MAX("bounding box max z", new String[] {"<z>"}, ExtendedRenderingState.BOUNDINGBOX_ZMAX),

		BOUNDING_BOX_X("bounding box x", new String[] {"<xmin>", "<xmax>"}, ExtendedRenderingState.BOUNDINGBOX_XMIN, ExtendedRenderingState.BOUNDINGBOX_XMAX),
		BOUNDING_BOX_Y("bounding box y", new String[] {"<ymin>", "<ymax>"}, ExtendedRenderingState.BOUNDINGBOX_YMIN, ExtendedRenderingState.BOUNDINGBOX_YMAX),
		BOUNDING_BOX_Z("bounding box z", new String[] {"<zmin>", "<zmax>"}, ExtendedRenderingState.BOUNDINGBOX_ZMIN, ExtendedRenderingState.BOUNDINGBOX_ZMAX),

		FRONT_CLIPPING("front clipping", new String[] {"<front>"}, ExtendedRenderingState.NEAR),
		BACK_CLIPPING("back clipping",   new String[] {"<back>"},  ExtendedRenderingState.FAR),

		FRONT_BACK_CLIPPING("front/back clipping", new String[] {"<front>", "<back>"}, ExtendedRenderingState.NEAR, ExtendedRenderingState.FAR);

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

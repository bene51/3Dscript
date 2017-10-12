package renderer3d;

import parser.Keyword;
import textanim.IKeywordFactory;

public class KeywordFactory implements IKeywordFactory {

	@Override
	public Keyword[] getNonChannelKeywords() {
		return NonChannelKeyword.values();
	}

	@Override
	public Keyword[] getChannelKeywords() {
		return ChannelKeyword.values();
	}

	public static enum ChannelKeyword implements Keyword {

		INTENSITY_MIN("min intensity",     new String[] {"<min>"},    ExtendedRenderingState.INTENSITY_MIN),
		INTENSITY_MAX("max intensity",     new String[] {"<max>"},    ExtendedRenderingState.INTENSITY_MAX),
		INTENSITY_GAMMA("intensity gamma", new String[] {"<gamma>"},  ExtendedRenderingState.INTENSITY_GAMMA),

		ALPHA_MIN("min alpha",     new String[] {"<min>"},    ExtendedRenderingState.ALPHA_MIN),
		ALPHA_MAX("max alpha",     new String[] {"<max>"},    ExtendedRenderingState.ALPHA_MAX),
		ALPHA_GAMMA("alpha gamma", new String[] {"<gamma>"},  ExtendedRenderingState.ALPHA_GAMMA),

		INTENSITY("intensity",     new String[] {"<min>", "<max>", "<gamma>"},  ExtendedRenderingState.INTENSITY_MIN, ExtendedRenderingState.INTENSITY_MAX, ExtendedRenderingState.INTENSITY_GAMMA),
		ALPHA("alpha",             new String[] {"<min>", "<max>", "<gamma>"},  ExtendedRenderingState.ALPHA_MIN, ExtendedRenderingState.ALPHA_MAX, ExtendedRenderingState.ALPHA_GAMMA),

		WEIGHT("weight",           new String[] {"<weight>"},  ExtendedRenderingState.WEIGHT);

		private final String keyword;
		private final String[] autocompletionDesc;
		private final int[] rsProperties;

		private ChannelKeyword(String text, String[] autocompletionDesc, int... rsProperties) {
			this.keyword = text;
			this.autocompletionDesc = autocompletionDesc;
			this.rsProperties = rsProperties;
		}

		@Override
		public int[] getRenderingStateProperties() {
			return rsProperties;
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

	public static enum NonChannelKeyword implements Keyword {

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
		private final int[] rsProperties;

		private NonChannelKeyword(String text, String[] autocompletionDesc, int... rsProperties) {
			this.keyword = text;
			this.autocompletionDesc = autocompletionDesc;
			this.rsProperties = rsProperties;
		}

		@Override
		public int[] getRenderingStateProperties() {
			return rsProperties;
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

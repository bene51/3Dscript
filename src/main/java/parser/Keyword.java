package parser;

import java.util.HashMap;
import java.util.Map;

public interface Keyword {

	/**
	 * The actual keyword text.
	 */
	public String getKeyword();

	/**
	 * A list of strings that describe each expected value
	 */
	public String[] getAutocompletionDescriptions();

	/**
	 * Returns an array with the indices of the rendering state properties
	 * (i.e. the indices in the nonchannelproperties and channelproperties
	 * arrays defined in RenderingState).
	 */
	public int[] getRenderingStateProperties();

	/**
	 * Returns the length of the keyword.
	 */
	public int length();

	/**
	 * Returns a map that assigns pre-defined strings to values.
	 */
	public Map<String, double[]> getReplacementMap();


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
		public String getKeyword() {
			return text;
		}

		@Override
		public int length() {
			return text.length();
		}

		@Override
		public String[] getAutocompletionDescriptions() {
			return null;
		}

		@Override
		public int[] getRenderingStateProperties() {
			return null;
		}

		@Override
		public Map<String, double[]> getReplacementMap() {
			return new HashMap<String, double[]>();
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
		public String getKeyword() {
			return text;
		}

		@Override
		public int length() {
			return text.length();
		}

		public float[] getTransition() {
			return transition;
		}

		@Override
		public String[] getAutocompletionDescriptions() {
			return null;
		}

		@Override
		public int[] getRenderingStateProperties() {
			return null;
		}

		@Override
		public Map<String, double[]> getReplacementMap() {
			return new HashMap<String, double[]>();
		}
	}
}

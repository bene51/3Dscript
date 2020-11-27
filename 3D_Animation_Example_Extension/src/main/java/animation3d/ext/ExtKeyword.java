package animation3d.ext;

import java.util.HashMap;
import java.util.Map;

import animation3d.parser.Keyword;

public enum ExtKeyword implements Keyword {

	BRIGHTNESS("brightness",
			new String[] {"<brightness>"},
			makeBrightnessReplacementMap(),
			ExtRenderingState.BRIGHTNESS),
	POSITION("scale",
			new String[] {"<sx>", "<sy>"},
			ExtRenderingState.SCALE_X,
			ExtRenderingState.SCALE_Y);

	private final String keyword;
	private final int[] rsProperties;
	private final String[] autocompletionDesc;
	private final Map<String, double[]> replacementMap;

	private ExtKeyword(String text, String[] autocompletionDesc, int... rsProperties) {
		this(text, autocompletionDesc, new HashMap<String, double[]>(), rsProperties);
	}

	private ExtKeyword(String text, String[] autocompletionDesc, Map<String, double[]> replacementMap, int... rsProperties) {
		this.keyword = text;
		this.rsProperties = rsProperties;
		this.autocompletionDesc = autocompletionDesc;
		this.replacementMap = replacementMap;
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

	@Override
	public Map<String, double[]> getReplacementMap() {
		return replacementMap;
	}

	private static Map<String, double[]> makeBrightnessReplacementMap() {
		HashMap<String, double[]> map = new HashMap<String, double[]>();
		map.put("dark",   new double[] {0.3});
		map.put("normal", new double[] {0.5});
		map.put("bright", new double[] {1.0});
		return map;
	}
}
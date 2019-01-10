package animation3d.bdv;

import java.util.HashMap;
import java.util.Map;

import animation3d.parser.Keyword;

public enum BDVChannelKeyword implements Keyword {

	INTENSITY_MIN("min intensity",     new String[] {"<min>"},    BDVRenderingState.INTENSITY_MIN),
	INTENSITY_MAX("max intensity",     new String[] {"<max>"},    BDVRenderingState.INTENSITY_MAX),

	INTENSITY("intensity",     new String[] {"<min>", "<max>"},  BDVRenderingState.INTENSITY_MIN, BDVRenderingState.INTENSITY_MAX),

	COLOR("color", new String[] {"<red>", "<green>", "<blue>"}, makeColorMap(), BDVRenderingState.CHANNEL_COLOR_RED, BDVRenderingState.CHANNEL_COLOR_GREEN, BDVRenderingState.CHANNEL_COLOR_BLUE);


	private final String keyword;
	private final String[] autocompletionDesc;
	private final int[] rsProperties;
	private final Map<String, double[]> replacementMap;

	private BDVChannelKeyword(String text, String[] autocompletionDesc, int... rsProperties) {
		this(text, autocompletionDesc, null, rsProperties);
	}

	private BDVChannelKeyword(String text, String[] autocompletionDesc, Map<String, double[]> replacementMap, int... rsProperties) {
		this.keyword = text;
		this.autocompletionDesc = autocompletionDesc;
		this.rsProperties = rsProperties;
		if(replacementMap == null)
			this.replacementMap = new HashMap<String, double[]>();
		else
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

	private static Map<String, double[]> makeColorMap() {
		HashMap<String, double[]> map = new HashMap<String, double[]>();
		map.put("red",     new double[] {255.0, 0.0, 0.0});
		map.put("green",   new double[] {0.0, 255.0, 0.0});
		map.put("blue",    new double[] {0.0, 0.0, 255.0});
		map.put("yellow",  new double[] {255.0, 255.0, 0.0});
		map.put("cyan",    new double[] {0.0, 255.0, 255.0});
		map.put("magenta", new double[] {255.0, 0.0, 255.0});
		return map;
	}
}
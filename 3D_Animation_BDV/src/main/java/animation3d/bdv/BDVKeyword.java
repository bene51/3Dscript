package animation3d.bdv;

import java.util.HashMap;
import java.util.Map;

import animation3d.parser.Keyword;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;

public enum BDVKeyword implements Keyword {

	DISPLAY_MODE ("display mode",   new String[] {"<mode>"},          makeDisplayModeMap(),   BDVRenderingState.DISPLAY_MODE),
	TIMEPOINT    ("timepoint",      new String[] {"<timepoint>"},                             BDVRenderingState.TIMEPOINT),
	INTERPOLATION("interpolation",  new String[] {"<interpolation>"}, makeInterpolationMap(), BDVRenderingState.INTERPOLATION),
	SOURCE       ("current source", new String[] {"<source>"},                                BDVRenderingState.CURRENT_SOURCE);

	private final String keyword;
	private final String[] autocompletionDesc;
	private final int[] rsProperties;
	private final Map<String, double[]> replacementMap;

	private BDVKeyword(String text, String[] autocompletionDesc, int... rsProperties) {
		this(text, autocompletionDesc, null, rsProperties);
	}

	private BDVKeyword(String text, String[] autocompletionDesc, Map<String, double[]> replacementMap, int... rsProperties) {
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

	private static Map<String, double[]> makeInterpolationMap() {
		HashMap<String, double[]> map = new HashMap<String, double[]>();
		map.put("nearest neighbor",     new double[] {Interpolation.NEARESTNEIGHBOR.ordinal()});
		map.put("linear",               new double[] {Interpolation.NLINEAR.ordinal()});
		return map;
	}

	private static Map<String, double[]> makeDisplayModeMap() {
		HashMap<String, double[]> map = new HashMap<String, double[]>();
		for(DisplayMode dm : DisplayMode.values())
			map.put(dm.getName(), new double[] {dm.ordinal()});
		return map;
	}
}
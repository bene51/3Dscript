package animation3d.ij3dviewer;

import java.util.HashMap;
import java.util.Map;

import parser.Keyword;

public enum IJ3DKeyword implements Keyword {

	DISPLAY_MODE("display mode", new String[] {"<mode>"},         IJ3DRenderingState.DISPLAY_MODE),
	COLOR       ("color",        new String[] {"<red>", "<green>", "<blue>"},
			makeColorMap(),
			IJ3DRenderingState.COLOR_RED,
			IJ3DRenderingState.COLOR_GREEN,
			IJ3DRenderingState.COLOR_BLUE),
	TRANSPARENCY("transparency", new String[] {"<transparency>"}, IJ3DRenderingState.TRANSPARENCY),
	THRESHOLD   ("threshold",    new String[] {"<threshold>"},    IJ3DRenderingState.THRESHOLD);


	private final String keyword;
	private final String[] autocompletionDesc;
	private final int[] rsProperties;
	private final Map<String, double[]> replacementMap;

	private IJ3DKeyword(String text, String[] autocompletionDesc, int... rsProperties) {
		this(text, autocompletionDesc, null, rsProperties);
	}

	private IJ3DKeyword(String text, String[] autocompletionDesc, Map<String, double[]> replacementMap, int... rsProperties) {
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
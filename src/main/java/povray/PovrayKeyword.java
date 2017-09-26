package povray;

import parser.Keyword2;

public enum PovrayKeyword implements Keyword2 {

	LENS_X("lens x", new String[] {"<x>"},               PovrayKeyframe.LENS_X),
	LENS_Y("lens y", new String[] {"<y>"},               PovrayKeyframe.LENS_Y),
	LENS_Z("lens z", new String[] {"<z>"},               PovrayKeyframe.LENS_Z),
	LENS  ("lens",   new String[] {"<x>", "<y>", "<z>"}, PovrayKeyframe.LENS_X, PovrayKeyframe.LENS_Y, PovrayKeyframe.LENS_Z);

	private final String keyword;
	private final String[] autocompletionDesc;
	private final int[] keyframeProperties;

	private PovrayKeyword(String text, String[] autocompletionDesc, int... keyframeProperties) {
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
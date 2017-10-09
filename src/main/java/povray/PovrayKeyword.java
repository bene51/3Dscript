package povray;

import parser.Keyword2;

public enum PovrayKeyword implements Keyword2 {

	LENS_X("lens x", new String[] {"<x>"},               PovrayRenderingState.LENS_X),
	LENS_Y("lens y", new String[] {"<y>"},               PovrayRenderingState.LENS_Y),
	LENS_Z("lens z", new String[] {"<z>"},               PovrayRenderingState.LENS_Z),
	LENS  ("lens",   new String[] {"<x>", "<y>", "<z>"}, PovrayRenderingState.LENS_X, PovrayRenderingState.LENS_Y, PovrayRenderingState.LENS_Z);

	private final String keyword;
	private final String[] autocompletionDesc;
	private final int[] rsProperties;

	private PovrayKeyword(String text, String[] autocompletionDesc, int... rsProperties) {
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
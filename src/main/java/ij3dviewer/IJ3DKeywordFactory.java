package ij3dviewer;

import parser.Keyword;
import textanim.IKeywordFactory;

public class IJ3DKeywordFactory implements IKeywordFactory {

	// TODO make singleton

	@Override
	public Keyword[] getNonChannelKeywords() {
		return IJ3DKeyword.values();
	}

	@Override
	public Keyword[] getChannelKeywords() {
		return null;
	}
}

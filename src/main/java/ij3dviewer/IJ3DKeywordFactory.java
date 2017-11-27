package ij3dviewer;

import parser.Keyword;
import textanim.IKeywordFactory;

public class IJ3DKeywordFactory implements IKeywordFactory {

	private static IJ3DKeywordFactory instance = null;

	private IJ3DKeywordFactory() {}

	public static IJ3DKeywordFactory getInstance() {
		if(instance == null)
			instance = new IJ3DKeywordFactory();
		return instance;
	}

	@Override
	public Keyword[] getNonChannelKeywords() {
		return IJ3DKeyword.values();
	}

	@Override
	public Keyword[] getChannelKeywords() {
		return null;
	}
}

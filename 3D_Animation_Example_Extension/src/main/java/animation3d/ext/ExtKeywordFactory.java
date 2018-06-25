package animation3d.ext;

import parser.Keyword;
import textanim.IKeywordFactory;

public class ExtKeywordFactory implements IKeywordFactory {

	private static ExtKeywordFactory instance = null;

	private ExtKeywordFactory() {}

	public static ExtKeywordFactory getInstance() {
		if(instance == null)
			instance = new ExtKeywordFactory();
		return instance;
	}

	@Override
	public Keyword[] getNonChannelKeywords() {
		return ExtKeyword.values();
	}

	@Override
	public Keyword[] getChannelKeywords() {
		return null;
	}
}

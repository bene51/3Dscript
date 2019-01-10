package animation3d.bdv;

import animation3d.parser.Keyword;
import animation3d.textanim.IKeywordFactory;

public class BDVKeywordFactory implements IKeywordFactory {

	private static BDVKeywordFactory instance = null;

	private BDVKeywordFactory() {}

	public static BDVKeywordFactory getInstance() {
		if(instance == null)
			instance = new BDVKeywordFactory();
		return instance;
	}

	@Override
	public Keyword[] getNonChannelKeywords() {
		return BDVKeyword.values();
	}

	@Override
	public Keyword[] getChannelKeywords() {
		return BDVChannelKeyword.values();
	}
}

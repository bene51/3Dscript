package textanim;

import parser.Keyword;

public interface IKeywordFactory {

	public Keyword[] getChannelKeywords();

	public Keyword[] getNonChannelKeywords();
}

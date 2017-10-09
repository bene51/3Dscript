package textanim;

import parser.Keyword2;

public interface IKeywordFactory {

	public Keyword2[] getChannelKeywords();

	public Keyword2[] getNonChannelKeywords();
}

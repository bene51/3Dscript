package textanim;

import parser.Keyword2;

public interface KeywordFactory {

	public Keyword2[] getChannelKeywords();

	public Keyword2[] getNonChannelKeywords();
}

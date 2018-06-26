package animation3d.textanim;

import animation3d.parser.Keyword;

public interface IKeywordFactory {

	public Keyword[] getChannelKeywords();

	public Keyword[] getNonChannelKeywords();
}

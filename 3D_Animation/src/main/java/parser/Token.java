package parser;

public class Token {

	public Token(String text, TokenType type, int offset) {
        this.text = text;
        this.type = type;
        this.offset = offset;
    }

	public int length() {
		return text.length();
	}

    public final String text;
    public final TokenType type;
    public final int offset;
}

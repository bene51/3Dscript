package animation3d.parser;

public enum TokenType {
    DIGIT("a digit (0-9)"),
    DOT("'.'"),
    SIGN("'+' or '-'"),
    LPAREN("'('"),
    RPAREN("')'"),
    COMMA("','"),
    KEYWORD("a keyword"),
    SPACE("a space character"),
    LETTER("a letter (a-z)"),
    UNDERSCORE("'_'"),
    EOF("end-of-file");

    private final String niceString;

	TokenType(String niceString) {
		this.niceString = niceString;
	}

	@Override
	public String toString() {
		return niceString;
	}
}
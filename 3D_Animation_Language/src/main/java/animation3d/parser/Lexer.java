package animation3d.parser;

import java.util.ArrayList;

public class Lexer {

	private final String input;
	private int index;

	public Lexer(String input) {
		this.input = input;
		this.index = 0;
	}

	public Token getNextToken(Iterable<String> tokens, boolean optional) {
		for(String token : tokens) {
			if(input.regionMatches(true, index, token, 0, token.length())) {
				int pos = index;
				index += token.length();
				return new Token(token, TokenType.KEYWORD, pos);
			}
		}
		if(optional)
			return null; // without increasing index;
		throw new RuntimeException("Error at position " + index + ": Expected one of " + tokens.toString() + " but found end of line.");
	}

	public Token getNextToken(Keyword keyword, boolean optional) {
		if(input.regionMatches(true, index, keyword.getKeyword(), 0, keyword.length())) {
			int pos = index;
			index += keyword.length();
			return new Token(keyword.getKeyword(), TokenType.KEYWORD, pos);
		}
		if(optional)
			return null; // without increasing index;
		throw new RuntimeException("Error at position " + index + ": Expected " + keyword + " but found end of line.");
	}

	public Token getNextToken(TokenType token, boolean optional) {
		if(index >= input.length()) {
			if(token == TokenType.EOF)
				return new Token("", TokenType.EOF, index);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found end of line.");
		}

		char c = input.charAt(index);
		if(token == TokenType.DIGIT) {
			if(Character.isDigit(c))
				return new Token(Character.toString(c), token, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.LETTER) {
			if(Character.isLetter(c))
				return new Token(Character.toString(c), token, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.UNDERSCORE) {
			if(c == '_')
				return new Token(Character.toString(c), token, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.SPACE) {
			if(Character.isWhitespace(c) && c != '\r' && c != '\n')
				return new Token(Character.toString(c), TokenType.SPACE, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.DOT) {
			if(c == '.')
				return new Token(Character.toString(c), TokenType.DOT, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.SIGN) {
			if(c == '+' || c == '-')
				return new Token(Character.toString(c), TokenType.SIGN, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.LPAREN) {
			if(c == '(')
				return new Token(Character.toString(c), TokenType.LPAREN, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.RPAREN) {
			if(c == ')')
				return new Token(Character.toString(c), TokenType.RPAREN, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.COMMA) {
			if(c == ',')
				return new Token(Character.toString(c), TokenType.COMMA, index++);
			if(optional)
				return null; // without increasing index;
			throw new RuntimeException("Error at position " + index + ": Expected " + token + " but found " + c);
		}

		else if(token == TokenType.KEYWORD) {
			throw new RuntimeException("Should not call getToken() with TokenType==KEYWORD");
		}

		else {
			throw new RuntimeException("Unknow token type: " + token);
		}
	}

	public int getIndex() {
		return index;
	}

	/**
	 * Checks from the characters between the current index and
	 * <code>cursorpos</code> which keywords are possible.
	 * @param cursorpos
	 * @return
	 */
	public String[] getAutocompletionList(int cursorpos, Keyword...keywords) {
		String prefix = input.substring(index, cursorpos);
		ArrayList<String> list = new ArrayList<String>();
		System.out.println("*" + prefix + "*");
		for(Keyword kw : keywords)
			if(kw.getKeyword().regionMatches(true, 0, prefix, 0, prefix.length()))
				list.add(kw.getKeyword());

		String[] ret = new String[list.size()];
		list.toArray(ret);
		return ret;
	}

	public String[] getAutocompletionList(int cursorpos, String...keywords) {
		String prefix = input.substring(index, cursorpos);
		ArrayList<String> list = new ArrayList<String>();
		System.out.println("*" + prefix + "*");
		for(String kw : keywords)
			if(kw.regionMatches(true, 0, prefix, 0, prefix.length()))
				list.add(kw);

		String[] ret = new String[list.size()];
		list.toArray(ret);
		return ret;
	}

	public String getAutocompletionString(int cursorpos, Keyword kw) {
		String prefix = input.substring(index, cursorpos);
		if(kw.getKeyword().regionMatches(true, 0, prefix, 0, prefix.length()))
			return kw.getKeyword();

		return null;
	}



//	public Token getNextToken() {
//		if(index >= input.length())
//			return new Token("", TokenType.EOF, index);
//
//		String keyword = keywordAt(index);
//		if(keyword != null) {
//			index += keyword.length();
//			return new Token(keyword, TokenType.KEYWORD, index - keyword.length());
//		}
//
//		char c = input.charAt(index);
//		index++;
//
//		if(Character.isDigit(c))
//			return new Token(Character.toString(c), TokenType.DIGIT, index - 1);
//
//		if(Character.isWhitespace(c) && c != '\r' && c != '\n')
//			return new Token(Character.toString(c), TokenType.SPACE, index - 1);
//
//		if(c == '.')
//			return new Token(Character.toString(c), TokenType.DOT, index - 1);
//
//		if(c == '+' || c == '-')
//			return new Token(Character.toString(c), TokenType.SIGN, index - 1);
//
//		if(c == '(')
//			return new Token(Character.toString(c), TokenType.LPAREN, index - 1);
//
//		if(c == ')')
//			return new Token(Character.toString(c), TokenType.RPAREN, index - 1);
//
//		if(c == ',')
//			return new Token(Character.toString(c), TokenType.COMMA, index - 1);
//
//		throw new RuntimeException("Error at index " + (index - 1));
//	}
}

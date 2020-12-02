package animation3d.parser;

public class ParsingException extends Exception {

	private final int pos;

	private int line = -1;

	public ParsingException(int pos, String msg) {
		super(msg);
		this.pos = pos;
	}

	public int getPos() {
		return pos;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getLine() {
		return line;
	}
}

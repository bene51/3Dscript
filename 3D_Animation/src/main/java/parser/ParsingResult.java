package parser;

import textanim.Animation;

public class ParsingResult {

	private Animation result = null;

	private int fromFrame;
	private int toFrame;

	private Autocompletion autocompletion;

	public void setResult(Animation ta) {
		this.result = ta;
	}

	public Animation getResult() {
		return result;
	}

	public void setAutocompletion(Autocompletion autocompletion) {
		this.autocompletion = autocompletion;
	}

	public Autocompletion getAutocompletion() {
		return autocompletion;
	}

	public void setFromTo(int from, int to) {
		this.fromFrame = from;
		this.toFrame = to;
	}

	public int getFrom() {
		return fromFrame;
	}

	public int getTo() {
		return toFrame;
	}
}

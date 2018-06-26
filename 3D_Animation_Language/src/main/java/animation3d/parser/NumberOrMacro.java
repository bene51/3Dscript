package animation3d.parser;

import ij.IJ;

public class NumberOrMacro {

	final double v;
	final String functionName;
	String macro;

	public NumberOrMacro(double v) {
		this.v = v;
		this.functionName = null;
	}

	public NumberOrMacro(String functionName) {
		this.v = -1;
		this.functionName = functionName;
	}

	public void setMacro(String macro) {
		this.macro = macro;
	}

	public boolean isMacro() {
		return functionName != null;
	}

	public double getValue() {
		return v;
	}

	public String getFunctionName() {
		return functionName;
	}

	public double evaluateMacro(int t, int from, int to) {
		t = Math.min(to, Math.max(from, t));
		return Double.parseDouble(IJ.runMacro(macro + "\nreturn d2s(" + functionName + "(" + t + "), 5);"));
	}
}

package animation2;

public class CtrlPoint extends Point {

	public static final boolean isCtrlPoint = true;

	public final LinePoint parent;

	public CtrlPoint(int x, double y, LinePoint parent) {
		super(x, y);
		this.parent = parent;
	}
}

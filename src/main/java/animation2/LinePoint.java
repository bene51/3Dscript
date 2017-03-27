package animation2;

public class LinePoint extends Point {

	public static final boolean isLinePoint = true;

	public final CtrlPoint c1;
	public final CtrlPoint c2;

	public LinePoint(int x, double y) {
		super(x, y);
		c1 = new CtrlPoint(x, y, this);
		c2 = new CtrlPoint(x, y, this);
	}

	@Override
	public void moveTo(int x, double y, Point lower, Point upper) {
		int px = this.x;
		double py = this.y;

		super.moveTo(x, y, lower, upper);

		int dx = this.x - px;
		double dy = this.y - py;

		c1.moveBy(dx, dy);
		c2.moveBy(dx, dy);
	}
}

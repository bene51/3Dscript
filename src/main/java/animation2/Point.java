package animation2;

public class Point implements Comparable<Point> {
	protected double x;
	protected double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void set(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public int compareTo(Point o) {
		if(o.x < x) return +1;
		if(o.x > x) return -1;
		return 0;
	}

	public void moveTo(double x, double y, Point lower, Point upper) {
		x = Math.max(lower.x, Math.min(upper.x, x));
		set(x, y);
	}

	public void moveBy(double dx, double dy) {
		this.x += dx;
		this.y += dy;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}

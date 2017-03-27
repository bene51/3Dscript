package animation2;

public class Segment {

	public final double p1x, p1y, p2x, p2y;
	private double smoothness = 0;

	public Segment(double p1x, double p1y, double p2x, double p2y) {
		this(p1x, p1y, p2x, p2y, 0);
	}

	public Segment(double p1x, double p1y, double p2x, double p2y,
			double smoothness) {
		this.p1x = p1x;
		this.p1y = p1y;
		this.p2x = p2x;
		this.p2y = p2y;
		this.smoothness = smoothness;
	}

	public double getInterpolatedValue(double x) {
		if (x < p1x)
			return p1y;
		if (x > p2x)
			return p2y;

		if (smoothness == 0)
			smoothness = 1;
		// P(t) = (1-t)^3P0 + 3(1-t)^2tP1 + 3(1-t)t^2P2 + t^3P3 with t running
		// from 0 to 1.
		double x1 = p1x, x2 = p1x + smoothness, x3 = p2x - smoothness, x4 = p2x;
		double a = -1 * x1 + 3 * x2 - 3 * x3 + x4;
		double b = +3 * x1 - 6 * x2 + 3 * x3;
		double c = -3 * x1 + 3 * x2;
		double d = +1 * x1 - 1 * x;
		Cubic cubic = new Cubic();
		cubic.solve(a, b, c, d);
		System.out.println("t0 = " + cubic.x1);
		System.out.println("t1 = " + cubic.x2);
		System.out.println("t2 = " + cubic.x3);

		double t = cubic.x1;
		if (Double.isNaN(t) || t < -1e-6 || t > 1 + 1e-6)
			t = cubic.x2;
		if (Double.isNaN(t) || t < -1e-6 || t > 1 + 1e-6)
			t = cubic.x3;
		if (Double.isNaN(t) || t < -1e-6 || t > 1 + 1e-6)
			throw new RuntimeException(
					"Could not find solution for cubic equation");

		if (t < 0)
			t = 0;
		else if (t > 1)
			t = 1;

		// TODO just for control: ix should match x
		double ix = Math.pow(1 - t, 3) * x1 + 3 * Math.pow(1 - t, 2) * t * x2
				+ 3 * (1 - t) * t * t * x3 + t * t * t * x4;
		System.out.println("x = " + x + " t = " + t + " ix = " + ix);

		double y1 = p1y, y2 = p1y, y3 = p2y, y4 = p2y;
		double iy = Math.pow(1 - t, 3) * y1 + 3 * Math.pow(1 - t, 2) * t * y2
				+ 3 * (1 - t) * t * t * y3 + t * t * t * y4;
		return iy;
	}

	public double getSmoothness() {
		return smoothness;
	}

	public void setSmoothness(double smoothness) {
		this.smoothness = smoothness;
	}

	@Override
	public String toString() {
		return "(" + p1x + ", " + p1y + ") -> (" + p2x + ", " + p2y + ") (" + smoothness + ")";
	}
}
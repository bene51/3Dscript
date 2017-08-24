package textanim;

import java.util.List;
import java.util.Map;

import parser.NoSuchMacroException;
import parser.NumberOrMacro;
import renderer3d.Keyframe;
import util.Cubic;

public abstract class Animation {

	protected final int fromFrame;
	protected final int toFrame;

	public Animation(int fromFrame, int toFrame) {
		this.fromFrame = fromFrame;
		this.toFrame = toFrame;
	}

	protected void pickScripts(Map<String, String> scripts, NumberOrMacro... noms) throws NoSuchMacroException {
		for(NumberOrMacro nom : noms) {
			if(nom.isMacro()) {
				String fName = nom.getFunctionName();
				if(!scripts.containsKey(fName))
					throw new NoSuchMacroException("No macro for " + fName);
				nom.setMacro(scripts.get(fName));
			}
		}
	}

	public abstract void pickScripts(Map<String, String> scripts) throws NoSuchMacroException;

	public abstract void adjustKeyframe(Keyframe current, List<Keyframe> previous);

	public double interpolate(int t, double vFrom, double vTo) {
		if(t >= toFrame)
			return vTo;
		if(t <= fromFrame)
			return vFrom;
		// return vFrom + (t - fromFrame) * (vTo - vFrom) / (toFrame - fromFrame);

		if(x2 == -1) x2 = 0;
		if(y2 == -1) y2 = 0;
		if(x3 == -1) x3 = 1;
		if(y3 == -1) y3 = 1;

		double dx = toFrame - fromFrame;
		double dy = vTo - vFrom;

		double px1 = fromFrame;
		double px2 = fromFrame + x2 * dx;
		double px3 = fromFrame + x3 * dx;
		double px4 = toFrame;

		double py1 = vFrom;
		double py2 = vFrom + y2 * dy;
		double py3 = vFrom + y3 * dy;
		double py4 = vTo;

		return getInterpolatedValue(t, px1, py1, px2, py2, px3, py3, px4, py4);
	}

	private double x2 = -1, y2 = -1, x3 = -1, y3 = -1;

	public void setBezierControls(double x2, double y2, double x3, double y3) {
		this.x2 = x2;
		this.y2 = y2;
		this.x3 = x3;
		this.y3 = y3;
	}

	private static double getInterpolatedValue(
			double x,
			double x1, double y1,
			double x2, double y2,
			double x3, double y3,
			double x4, double y4) {

		if (x < x1)
			return y1;
		if (x > x4)
			return y4;

		// P(t) = (1-t)^3P0 + 3(1-t)^2tP1 + 3(1-t)t^2P2 + t^3P3 with t running
		// from 0 to 1.
		double a = -1 * x1 + 3 * x2 - 3 * x3 + x4;
		double b = +3 * x1 - 6 * x2 + 3 * x3;
		double c = -3 * x1 + 3 * x2;
		double d = +1 * x1 - 1 * x;
		Cubic cubic = new Cubic();
		cubic.solve(a, b, c, d);

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

		double iy = Math.pow(1 - t, 3) * y1 + 3 * Math.pow(1 - t, 2) * t * y2
				+ 3 * (1 - t) * t * t * y3 + t * t * t * y4;
		return iy;
	}
}

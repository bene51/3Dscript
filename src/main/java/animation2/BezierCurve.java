package animation2;

import java.util.ArrayList;

public class BezierCurve {

	public static class Point {
		double x;
		double y;
		boolean isCtrlPoint = false;

		public Point(double x, double y) {
			this(x, y, false);
		}

		public Point(double x, double y, boolean isCtrl) {
			this.x = x;
			this.y = y;
			this.isCtrlPoint = isCtrl;
		}

		public void set(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	private ArrayList<Point> ctrlPoints = new ArrayList<Point>();

}

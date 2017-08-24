package animation2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CtrlPoints implements Iterable<LinePoint> {

	/**
	 * The points are ordered as left control point - actual point - right control point
	 */
	private List<LinePoint> list = new ArrayList<LinePoint>();

	public CtrlPoints() {}

	public CtrlPoints(LinePoint... points) {
		for(LinePoint lp : points) {
			list.add(lp);
		}
		sort();
	}

	public CtrlPoints(CtrlPoints p) {
		list = new ArrayList<LinePoint>(p.list);
	}

	public void clear() {
		list.clear();
	}

	@Override
	public Iterator<LinePoint> iterator() {
		return list.listIterator();
	}

	/**
	 * Add a new LinePoint at the specified position; if there exists already a LinePoint at
	 * the given x coordinate, move the existing point to the given position
	 * @param x
	 * @param y
	 * @return
	 */
	public LinePoint add(int x, double y) {
		int e = getIndexAt(x);
		if(e == -1)
			return add(new LinePoint(x, y));
		LinePoint existing = get(e);
		Point ll = new Point(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY);
		Point ur = new Point(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
		existing.moveTo(x, y, ll, ur);
		return existing;
	}

	public LinePoint add(LinePoint c) {
		list.add(c);
		Collections.sort(list);
		return c;
	}

	/**
	 * Removes the given point, CtrlPoint or LinePoint
	 * @param cp
	 */
	public void remove(Point cp) {
		for(int i = 0; i < list.size(); i++) {
			LinePoint lp = list.get(i);
			if(lp == cp) {
				list.remove(i);
				return;
			}

			if(lp.c1 == cp) {
				lp.c1.moveTo(lp.x, lp.y, lp, lp);
				return;
			}
			if(lp.c2 == cp) {
				lp.c2.moveTo(lp.x, lp.y, lp, lp);
				return;
			}
		}
		System.out.println("Could not remove " + cp);
	}

	public void removePointAt(int x) {
		int toRemove = getIndexAt(x);
		if(toRemove != -1)
			list.remove(toRemove);
	}

	public int getIndexAt(int x) {
		for(int i = 0; i < list.size(); i++) {  // TODO replace with binary search
			LinePoint lp = list.get(i);
			if(lp.x == x)
				return i;
		}
		return -1;
	}

	public LinePoint getPointAt(int x) {
		int i = getIndexAt(x);
		if(i == -1)
			return null;
		return get(i);
	}

	public LinePoint get(int index) {
		return list.get(index);
	}

	public int indexOf(LinePoint lp) {
		return list.indexOf(lp);
	}

	public int size() {
		return list.size();
	}

	public void sort() {
		Collections.sort(list);
	}

	public double getLinearInterpolatedValue(double x) {
		Iterator<LinePoint> it = iterator();
		LinePoint p = it.next();
		if(x <= p.getX())
			return p.getY();
		for(int i = 1; i < size(); i++) {
			LinePoint p2 = it.next();
			if(x == p2.getX())
				return p2.getY();
			if(x < p2.getX()) {
				return p.getY() + (x - p.getX()) * (p2.getY() - p.getY()) / (p2.getX() - p.getX());
			}
			p = p2;
		}
		return p.getY();
	}

	public double getInterpolatedValue(double x) {
		if(size() == 0)
			return Keyframe.UNSET;
		if(size() == 1)
			return list.get(0).getY();
		Iterator<LinePoint> it = iterator();
		LinePoint p1 = it.next();
		if(x <= p1.getX())
			return p1.getY();
		for(int i = 1; i < size(); i++) {
			LinePoint p2 = it.next();
			if(x == p2.getX())
				return p2.getY();
			if(x < p2.getX())
				return getInterpolatedValue(x, p1, p1.c2, p2.c1, p2);

			p1 = p2;
		}
		return p1.getY();
	}

	public static double getInterpolatedValue(double x, Point p1, Point p2, Point p3, Point p4) {
		// P(t) = (1-t)^3P0 + 3(1-t)^2tP1 + 3(1-t)t^2P2 + t^3P3 with t running
		// from 0 to 1.
		double x1 = p1.x, x2 = p2.x, x3 = p3.x, x4 = p4.x;
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

		// TODO just for control: ix should match x
		double ix = Math.pow(1 - t, 3) * x1 + 3 * Math.pow(1 - t, 2) * t * x2
				+ 3 * (1 - t) * t * t * x3 + t * t * t * x4;
		System.out.println("x = " + x + " t = " + t + " ix = " + ix);

		double y1 = p1.y, y2 = p2.y, y3 = p3.y, y4 = p4.y;
		double iy = Math.pow(1 - t, 3) * y1 + 3 * Math.pow(1 - t, 2) * t * y2
				+ 3 * (1 - t) * t * t * y3 + t * t * t * y4;
		return iy;
	}

	public void getBoundingBox(Point lowerleft, Point upperright) {
		if(list.size() == 0) {
			return;
		}

		double xmin = lowerleft.x;
		double xmax = upperright.x;
		double ymin = lowerleft.y;
		double ymax = upperright.y;

		for(LinePoint lp : this) {
			if(lp.x < xmin) xmin = lp.x;
			if(lp.y < ymin) ymin = lp.y;
			if(lp.x > xmax) xmax = lp.x;
			if(lp.y > ymax) ymax = lp.y;

			if(lp.c1.x < xmin) xmin = lp.c1.x;
			if(lp.c1.y < ymin) ymin = lp.c1.y;
			if(lp.c1.x > xmax) xmax = lp.c1.x;
			if(lp.c1.y > ymax) ymax = lp.c1.y;

			if(lp.c2.x < xmin) xmin = lp.c2.x;
			if(lp.c2.y < ymin) ymin = lp.c2.y;
			if(lp.c2.x > xmax) xmax = lp.c2.x;
			if(lp.c2.y > ymax) ymax = lp.c2.y;
		}

		lowerleft.set((int)Math.floor(xmin), ymin);
		upperright.set((int)Math.ceil(xmax), ymax);
	}

	public ClosestPoint getClosestPoint(double x, Point exclude) {
		double d = Double.POSITIVE_INFINITY;
		Point closest = null;
		int index = -1;
		for(int i = 0; i < list.size(); i++) {
			LinePoint p = list.get(i);
			if(p != exclude) {
				double dx = Math.abs(p.getX() - x);
				if(dx <= d) {
					d = dx;
					closest = p;
					index = i;
				}
			}
			if(p.c1 != exclude) {
				double dx = Math.abs(p.c1.getX() - x);
				if(dx <= d) {
					d = dx;
					closest = p.c1;
					index = i;
				}
			}
			if(p.c2 != exclude) {
				double dx = Math.abs(p.c2.getX() - x);
				if(dx <= d) {
					d = dx;
					closest = p.c2;
					index = i;
				}
			}
		}
		return new ClosestPoint(closest, index);
	}

	public ClosestPoint getClosestPoint(double x, double y, Point exclude, boolean linepointFirst) {
		double d = Double.POSITIVE_INFINITY;
		Point closest = null;
		int index = -1;
		for(int i = 0; i < size(); i++) {
			LinePoint p = list.get(i);
			if(!linepointFirst && p != exclude) {
				double dx = Math.pow(p.getX() - x, 2) + Math.pow(p.getY() - y, 2);
				if(dx <= d) {
					d = dx;
					closest = p;
					index = i;
				}
			}
			if(p.c1 != exclude && i > 0) {
				double dx = Math.pow(p.c1.getX() - x, 2) + Math.pow(p.c1.getY() - y, 2);
				if(dx <= d) {
					d = dx;
					closest = p.c1;
					index = i;
				}
			}
			if(p.c2 != exclude && i < size() - 1) {
				double dx = Math.pow(p.c2.getX() - x, 2) + Math.pow(p.c2.getY() - y, 2);
				if(dx <= d) {
					d = dx;
					closest = p.c2;
					index = i;
				}
			}
			if(linepointFirst && p != exclude) {
				double dx = Math.pow(p.getX() - x, 2) + Math.pow(p.getY() - y, 2);
				if(dx <= d) {
					d = dx;
					closest = p;
					index = i;
				}
			}
		}
		return new ClosestPoint(closest, index);
	}

	public static class ClosestPoint {
		Point p;
		int linepointIndex;

		ClosestPoint(Point p, int linepointIndex) {
			this.p = p;
			this.linepointIndex = linepointIndex;
		}
	}

	public ClosestPoint getClosestPoint(double x, double y, boolean linepointFirst) {
		return getClosestPoint(x, y, null, linepointFirst);
	}

	public ClosestPoint getClosestPoint(double x) {
		return getClosestPoint(x, null);
	}

	public ClosestPoint getClosestPointWithin(double x, double diff) {
		return getClosestPointWithin(x, diff, null);
	}

	public ClosestPoint getClosestPointWithin(double x, double diff, Point exclude) {
		ClosestPoint closest = getClosestPoint(x, exclude);
		if(Math.abs(x - closest.p.getX()) > diff)
			return null;
		return closest;
	}

	public ClosestPoint getClosestPointWithin(double x, double y, double pw, double ph, double diff, boolean linepointFirst) {
		return getClosestPointWithin(x, y, pw, ph, diff, null, linepointFirst);
	}

	public ClosestPoint getClosestPointWithin(double x, double y, double pw, double ph, double diff, Point exclude, boolean linepointFirst) {
		ClosestPoint closest = getClosestPoint(x, y, exclude, linepointFirst);
		if(Math.pow((x - closest.p.getX()) / pw, 2) + Math.pow((y - closest.p.getY()) / ph, 2) > diff * diff)
			return null;
		return closest;
	}

	@Override
	public String toString() {
		String ret = "\n\n";
		for(LinePoint p : this) {
			ret += (p + " - " + p.c1 + " - " + p.c2 + "\n");
		}
		return ret;
	}

//	public CtrlPoint getPointAtPlane(int plane) {
//		CtrlPoint cp = null;
//		Iterator<CtrlPoint> it = iterator();
//		while(it.hasNext()) {
//			cp = it.next();
//			if(cp.getPlane() == plane)
//				return cp;
//			if(cp.getPlane() > plane)
//				return null;
//		}
//		return null;
//	}
//
//	public CtrlPoint getPointAtPlane(int plane, CtrlPoint exclude) {
//		CtrlPoint cp = null;
//		Iterator<CtrlPoint> it = iterator();
//		while(it.hasNext()) {
//			cp = it.next();
//			if(cp.getPlane() == plane && cp != exclude)
//				return cp;
//			if(cp.getPlane() > plane)
//				return null;
//		}
//		return null;
//	}
}

package animation2;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;

import animation2.CtrlPoints.ClosestPoint;

	public class TimelineOverview implements MouseMotionListener, MouseListener {

		private static final int R = 10;

		private Timelines timelines;

		private final DiagramCanvas diagram;

		public static interface Listener {
			public void curveChanged(boolean boundingBoxChanged);
		}

		private ArrayList<Listener> listeners = new ArrayList<Listener>();

		public void addCurveChangeListener(Listener l) {
			listeners.add(l);
		}

		public void removeCurveChangeListener(Listener l) {
			listeners.remove(l);
		}

		private void fireCurveChanged(boolean boundingBoxChanged) {
			for(Listener l : listeners)
				l.curveChanged(boundingBoxChanged);
		}

		public TimelineOverview(final DiagramCanvas diagram, Timelines timelines) {
			this.timelines = timelines;
			this.diagram = diagram;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if(e.isConsumed())
				return;
			if(e.getX() < diagram.getLeftPixel() - 2 || e.getX() > diagram.getRightPixel() + 2)
				return;
			if(e.getY() < diagram.getTopPixel() - 2 || e.getY() > diagram.getBottomPixel() + 2)
				return;

			// TODO implement:
			// * shift-click to remove
			// * dragging to shift in time
			// * (no need for double-click)

//			if(e.isShiftDown()) {
//				// check if there's already a control point
//				double pw = diagram.pw();
//				double ph = diagram.ph();
//				ClosestPoint p = ctrls.getClosestPointWithin(diagram.realX(e.getX()), diagram.realY(e.getY()), pw, ph, R, true);
//				if(p != null) {
//					ctrls.remove(p.p);
//					fireCurveChanged(true);
//				}
//			}
//			else if(e.getClickCount() == 2) {
//				System.out.println("double-clicked");
//				double pw = diagram.pw();
//				double ph = diagram.ph();
//				ClosestPoint cl = ctrls.getClosestPointWithin(diagram.realX(e.getX()), diagram.realY(e.getY()), pw, ph, R, true);
//				Point cp = cl == null ? null : cl.p;
//
//				boolean addedAPoint = cp == null;
//				int x = (int)Math.round(diagram.realX(e.getX()));
//				double y = diagram.realY(e.getY());
//				if(!addedAPoint) {
//					x = cp.x;
//					y = cp.y;
//				}
//
//				GenericDialog gd = new GenericDialog("");
//				gd.addNumericField("x", x, 0);
//				gd.addNumericField("y", y, 3);
//				gd.showDialog();
//				if(gd.wasCanceled())
//					return;
//				x = (int)gd.getNextNumber();
//				y = gd.getNextNumber();
//				// if it's a line point and we move it to another line point, remove the old one
//				// TODO implement this
////					Point tmp = ctrls.getClosestPointWithin(x, inc, cp);
////					if(tmp != null)
////						ctrls.remove(tmp);
//
//				if(addedAPoint)
//					cp = ctrls.add(x, y);
//				else {
//					Point ll = new Point(0, 0);
//					Point ur = new Point(0, 0);
//					ctrls.getBoundingBox(ll, ur); // TODO really getBoundingBox? Not getLowerAndUpper?
//					cp.moveTo(x, y, ll, ur);
//				}
//				fireCurveChanged(true);
//			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

		private ClosestPoint draggedCtrl = null;

		@Override
		public void mousePressed(MouseEvent e) {
//			System.out.println("mousePressed");
//			if(e.isConsumed())
//				return;
//
//			if(e.getX() < diagram.getLeftPixel() - 2 || e.getX() > diagram.getRightPixel() + 2)
//				return;
//			if(e.getY() < diagram.getTopPixel() - 2 || e.getY() > diagram.getBottomPixel() + 2)
//				return;
//
//			double x = diagram.realX(e.getX());
//			double y = diagram.realY(e.getY());
//
//			double pw = diagram.pw();
//			double ph = diagram.ph();
//			ClosestPoint tmp = ctrls.getClosestPointWithin(x, y, pw, ph, R, !e.isControlDown());
////			Point lower = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY); // new Point(diagram.getXMin(), diagram.getYMin());
////			Point upper = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY); // new Point(diagram.getXMax(), diagram.getYMax());
////			getLowerAndUpper(tmp, lower, upper);
//
//			if(tmp != null) {
//				// tmp.p.moveTo(tmp.p.getX(), y, lower, upper);
//				draggedCtrl = tmp;
//			} else {
////				LinePoint lp = ctrls.add(x, y);
////				int idx = ctrls.indexOf(lp);
////				draggedCtrl = new ClosestPoint(lp, idx);
//			}
//			fireCurveChanged(false);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			draggedCtrl = null;
			temporarilyReplaced = null;
			fireCurveChanged(true);
		}

		private LinePoint temporarilyReplaced = null;

		@Override
		public void mouseDragged(MouseEvent e) {
//			if(e.isConsumed())
//				return;
//			if(draggedCtrl == null)
//				return;
//
//			// if a point was temporarily removed previously while dragging,
//			// add it again
//			if(temporarilyReplaced != null)
//				ctrls.add(temporarilyReplaced);
//			temporarilyReplaced = null;
//
//			int x = (int)Math.round(diagram.realX(e.getX()));
////			int x = clamp((int)Math.round(x), 1, values.length - 2);
////			if(draggedCtrl.getX() == 0 || draggedCtrl.getX() == values.length - 1)
////				x = draggedCtrl.getPlane();
//			double y = diagram.realY(e.getY());
////			y = clamp(y, 0, maxv);
//
//			// check if there's already a point with that plane, if that's the case
//			// remove the existing one temporarily
//			if(draggedCtrl.p instanceof LinePoint) {
//				ClosestPoint closest = ctrls.getClosestPointWithin(x, 1 / diagram.pw(), draggedCtrl.p);
//				if(closest != null && (closest.p instanceof LinePoint)) {
//					temporarilyReplaced = (LinePoint)closest.p;
//					ctrls.remove(temporarilyReplaced);
//				}
//			}
//
//			Point lower = new Point(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY); // new Point(diagram.getXMin(), diagram.getYMin());
//			Point upper = new Point(Integer.MAX_VALUE, Double.POSITIVE_INFINITY); // new Point(diagram.getXMax(), diagram.getYMax());
//			getLowerAndUpper(draggedCtrl, lower, upper);
//			System.out.println(lower);
//			System.out.println(upper);
//
//			draggedCtrl.p.moveTo(x, y, lower, upper);
////			ctrls.sort();
//			fireCurveChanged(false);
		}

//		private void getLowerAndUpper(ClosestPoint cp, Point lower, Point upper) {
//			if(cp.p instanceof LinePoint) {
//				LinePoint lp = (LinePoint)cp.p;
//				if(cp.linepointIndex > 0) {
//					lower.x = ctrls.get(cp.linepointIndex - 1).c2.x;
//					lower.y = 0;
//					double dx = Math.abs(lp.x - lp.c1.x);
//					lower.x += dx;
//				}
//				if(cp.linepointIndex < ctrls.size() - 1) {
//					upper.x = ctrls.get(cp.linepointIndex + 1).c1.x;
//					upper.y = 0;
//					double dx = Math.abs(lp.x - lp.c2.x);
//					upper.x -= dx;
//				}
//			} else {
//				CtrlPoint p = (CtrlPoint)cp.p;
//				if(p == p.parent.c1) { // the left one
//					upper.set(p.parent.x, p.parent.y);
//					if(cp.linepointIndex > 0)
//						lower.x = ctrls.get(cp.linepointIndex - 1).c2.x;
//				}
//				else if(p == p.parent.c2) { // the left one
//					lower.set(p.parent.x, p.parent.y);
//					if(cp.linepointIndex < ctrls.size() - 1)
//						upper.x = ctrls.get(cp.linepointIndex + 1).c1.x;
//				}
//			}
//		}

		@Override
		public void mouseMoved(MouseEvent e) {}

		double getMaxV(double[] values) {
			double max = 0;
			for(int i = 0; i < values.length; i++)
				if(values[i] > max)
					max = values[i];
			return max;
		}

		public void paint(Graphics gx) {
			Graphics2D g = (Graphics2D)gx;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			// g.clearRect(0, 0, w, h);

			int nLines = timelines.size();
			Font font = new Font("Helvetica", Font.PLAIN, 8);
			gx.setFont(font);
			FontMetrics fm = gx.getFontMetrics();
			g.setColor(Color.ORANGE);
			for(int l = 0; l < nLines; l++) {
				CtrlPoints ctrls = timelines.get(l);
				int nPoints = ctrls.size();
				if(nPoints == 0)
					continue;
				g.setColor(Color.GRAY);
				int y = diagram.canvasY(nLines - 1 - l + 0.5);
				g.drawLine(diagram.getLeftPixel(), y, diagram.getRightPixel(), y);
				Iterator<LinePoint> it = ctrls.iterator();
				String name = timelines.getName(l);
				int sy = y - fm.getHeight() / 2 + fm.getAscent();
				g.drawString(name, 0, sy);
				g.setColor(Color.ORANGE);
				for(int i = 0; i < nPoints; i++) {
					LinePoint p = it.next();
					int x = diagram.canvasX(p.getX());
					// int y = diagram.canvasY(p.getY());
					g.fillOval(x - 3, y - 3, 7, 7);
				}
			}
			g.setColor(Color.BLACK);
		}
	}


package animation2;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Iterator;

import animation2.CtrlPoints.ClosestPoint;
import ij.gui.GenericDialog;

public class BezierCurveEditor extends Panel {

	public static void main(String[] args) {
		Frame frame = new Frame();
		double[] histo = new double[100];
		for(int i = 0; i < 100; i++)
			histo[i] = 50 + i * 0.5;
		CtrlPoints ctrls = new CtrlPoints();
		ctrls.add(0, 0);
		ctrls.add(99, 99);
		BezierCurveEditor slider = new BezierCurveEditor(histo, ctrls);
		frame.add(slider);
		frame.pack();
		frame.setVisible(true);
	}

	private static final int R = 10;

	private static final long serialVersionUID = 1L;

	private CurveChangerCanvas slider;

	public BezierCurveEditor(double[] histogram, CtrlPoints ctrls) {
		this(histogram, ctrls, new Color(255, 0, 0, 100));
	}

	public BezierCurveEditor(double[] histogram, CtrlPoints ctrls, Color color) {
		super();
//
//		this.slider = new CurveChangerCanvas(histogram, color, ctrls, this);
//		GridBagLayout gridbag = new GridBagLayout();
//		GridBagConstraints c = new GridBagConstraints();
//		setLayout(gridbag);
//
//		c.gridx = c.gridy = 0;
//		c.fill = GridBagConstraints.HORIZONTAL;
//		c.gridwidth = GridBagConstraints.REMAINDER;
//		c.insets = new Insets(0, 2, 0, 5);
//		c.weightx = 1.0;
//		add(slider, c);
	}

//	public void set(Color color, CtrlPoints ctrls) {
//		slider.set(color, ctrls);
//	}



	public static class CurveChangerCanvas implements MouseMotionListener, MouseListener {

//		private static final long serialVersionUID = 1L;
//		private Color color;
		private CtrlPoints ctrls;

		private final DiagramCanvas diagram;

//		private BezierCurveEditor slider;

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

//		public CurveChangerCanvas(final DiagramCanvas diagram, Color color, CtrlPoints ctrls, BezierCurveEditor slider) {
		public CurveChangerCanvas(final DiagramCanvas diagram, CtrlPoints ctrls) {
			this.ctrls = ctrls;
			this.diagram = diagram;
//			canvas.addMouseMotionListener(this);
//			canvas.addMouseListener(this);
		}

//		void set(Color color, CtrlPoints ctrls) {
//			this.ctrls = ctrls;
//			canvas.repaint();
//		}

		public void setControls(CtrlPoints ctrls) {
			this.ctrls = ctrls;
		}
		
		public CtrlPoints getControls() {
			return ctrls;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if(e.isConsumed())
				return;
			if(e.getX() < diagram.getLeftPixel() - 2 || e.getX() > diagram.getRightPixel() + 2)
				return;
			if(e.getY() < diagram.getTopPixel() - 2 || e.getY() > diagram.getBottomPixel() + 2)
				return;

			if(e.isShiftDown()) {
				// check if there's already a control point
				double pw = diagram.pw();
				double ph = diagram.ph();
				ClosestPoint p = ctrls.getClosestPointWithin(diagram.realX(e.getX()), diagram.realY(e.getY()), pw, ph, R, true);
				if(p != null) {
					ctrls.remove(p.p);
					fireCurveChanged(true);
				}
			}
			else if(e.getClickCount() == 2) {
				System.out.println("double-clicked");
				double pw = diagram.pw();
				double ph = diagram.ph();
				ClosestPoint cl = ctrls.getClosestPointWithin(diagram.realX(e.getX()), diagram.realY(e.getY()), pw, ph, R, true);
				Point cp = cl == null ? null : cl.p;

				boolean addedAPoint = cp == null;
				double x = diagram.realX(e.getX());
				double y = diagram.realY(e.getY());
				if(!addedAPoint) {
					x = cp.x;
					y = cp.y;
//					cp = ctrls.add(x, y);
				}

				GenericDialog gd = new GenericDialog("");
				gd.addNumericField("x", x, 3);
				gd.addNumericField("y", y, 3);
				gd.showDialog();
				if(gd.wasCanceled()) {
//					if(addedAPoint)
//						ctrls.remove(cp);
				} else {
					x = gd.getNextNumber();
					y = gd.getNextNumber();
					// if it's a line point and we move it to another line point, remove the old one
					// TODO implement this
//					Point tmp = ctrls.getClosestPointWithin(x, inc, cp);
//					if(tmp != null)
//						ctrls.remove(tmp);
					
					if(addedAPoint)
						cp = ctrls.add(x, y);
					else {
						Point ll = new Point(0, 0);
						Point ur = new Point(0, 0);
						ctrls.getBoundingBox(ll, ur);
						cp.moveTo(x, y, ll, ur);
					}
				}
				fireCurveChanged(true);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

		private ClosestPoint draggedCtrl = null;

		@Override
		public void mousePressed(MouseEvent e) {
			System.out.println("mousePressed");
			if(e.isConsumed())
				return;

			if(e.getX() < diagram.getLeftPixel() - 2 || e.getX() > diagram.getRightPixel() + 2)
				return;
			if(e.getY() < diagram.getTopPixel() - 2 || e.getY() > diagram.getBottomPixel() + 2)
				return;

			double x = diagram.realX(e.getX());
			double y = diagram.realY(e.getY());

			double pw = diagram.pw();
			double ph = diagram.ph();
			ClosestPoint tmp = ctrls.getClosestPointWithin(x, y, pw, ph, R, !e.isControlDown());
//			Point lower = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY); // new Point(diagram.getXMin(), diagram.getYMin());
//			Point upper = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY); // new Point(diagram.getXMax(), diagram.getYMax());
//			getLowerAndUpper(tmp, lower, upper);

			if(tmp != null) {
				// tmp.p.moveTo(tmp.p.getX(), y, lower, upper);
				draggedCtrl = tmp;
			} else {
//				LinePoint lp = ctrls.add(x, y);
//				int idx = ctrls.indexOf(lp);
//				draggedCtrl = new ClosestPoint(lp, idx);
			}
			fireCurveChanged(false);
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
			if(e.isConsumed())
				return;
			if(draggedCtrl == null)
				return;

			// if a point was temporarily removed previously while dragging,
			// add it again
			if(temporarilyReplaced != null)
				ctrls.add(temporarilyReplaced);
			temporarilyReplaced = null;

			double x = diagram.realX(e.getX());
//			int x = clamp((int)Math.round(x), 1, values.length - 2);
//			if(draggedCtrl.getX() == 0 || draggedCtrl.getX() == values.length - 1)
//				x = draggedCtrl.getPlane();
			double y = diagram.realY(e.getY());
//			y = clamp(y, 0, maxv);

			// check if there's already a point with that plane, if that's the case
			// remove the existing one temporarily
			if(draggedCtrl.p instanceof LinePoint) {
				ClosestPoint closest = ctrls.getClosestPointWithin(x, 1 / diagram.pw(), draggedCtrl.p);
				if(closest != null && (closest.p instanceof LinePoint)) {
					temporarilyReplaced = (LinePoint)closest.p;
					ctrls.remove(temporarilyReplaced);
				}
			}

			Point lower = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY); // new Point(diagram.getXMin(), diagram.getYMin());
			Point upper = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY); // new Point(diagram.getXMax(), diagram.getYMax());
			getLowerAndUpper(draggedCtrl, lower, upper);
			System.out.println(lower);
			System.out.println(upper);

			draggedCtrl.p.moveTo(x, y, lower, upper);
//			ctrls.sort();
			fireCurveChanged(false);
		}

		private void getLowerAndUpper(ClosestPoint cp, Point lower, Point upper) {
			if(cp.p instanceof LinePoint) {
				LinePoint lp = (LinePoint)cp.p;
				if(cp.linepointIndex > 0) {
					lower.x = ctrls.get(cp.linepointIndex - 1).c2.x;
					lower.y = 0;
					double dx = Math.abs(lp.x - lp.c1.x);
					lower.x += dx;
				}
				if(cp.linepointIndex < ctrls.size() - 1) {
					upper.x = ctrls.get(cp.linepointIndex + 1).c1.x;
					upper.y = 0;
					double dx = Math.abs(lp.x - lp.c2.x);
					upper.x -= dx;
				}
			} else {
				CtrlPoint p = (CtrlPoint)cp.p;
				if(p == p.parent.c1) { // the left one
					upper.set(p.parent.x, p.parent.y);
					if(cp.linepointIndex > 0)
						lower.x = ctrls.get(cp.linepointIndex - 1).c2.x;
				}
				else if(p == p.parent.c2) { // the left one
					lower.set(p.parent.x, p.parent.y);
					if(cp.linepointIndex < ctrls.size() - 1)
						upper.x = ctrls.get(cp.linepointIndex + 1).c1.x;
				}
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {}

		double getMaxV(double[] values) {
			double max = 0;
			for(int i = 0; i < values.length; i++)
				if(values[i] > max)
					max = values[i];
			return max;
		}

		double getInterpolated(double[] values, double realx) {
			if(realx <= 0)
				return values[0];
			if(realx >= values.length - 1)
				return values[values.length - 1];

			int l = (int)Math.floor(realx);
			int u = l + 1; // (int)Math.ceil(realx);
			return values[l] + (realx - l) * (values[u] - values[l]) / (u - l);
		}

		public void paint(Graphics gx) {
			Graphics2D g = (Graphics2D)gx;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			// g.clearRect(0, 0, w, h);

			int nPoints = ctrls.size();
			g.setColor(Color.DARK_GRAY);
			Iterator<LinePoint> it = ctrls.iterator();
			GeneralPath path = new GeneralPath();
			int c2x = -1;
			int c2y = -1;
			for(int i = 0; i < nPoints; i++) {
				LinePoint p = it.next();
				int x = diagram.canvasX(p.getX());
				int y = diagram.canvasY(p.getY());
				g.fillOval(x - 3, y - 3, 7, 7);
				int c1x = diagram.canvasX(p.c1.getX());
				int c1y = diagram.canvasY(p.c1.getY());

				if(i == 0)
					path.moveTo(x, y);
				else
					path.curveTo(c2x, c2y, c1x, c1y, x, y);

				c2x = diagram.canvasX(p.c2.getX());
				c2y = diagram.canvasY(p.c2.getY());

//				if(i > 0) { // TODO remove comments
					g.drawOval(c1x - 3, c1y - 3, 7, 7);
					g.drawLine(c1x, c1y, x, y);
//				}
//
//				if(i < nPoints - 1) {
					g.drawOval(c2x - 3, c2y - 3, 7, 7);
					g.drawLine(x, y, c2x, c2y);
//				}
			}
			LinePoint first = ctrls.get(0);
			int cy = diagram.canvasY(first.y);
			if(first.x > diagram.getXMin())
				g.drawLine(diagram.getLeftPixel(), cy, diagram.canvasX(first.x), cy);
			LinePoint last = ctrls.get(ctrls.size() - 1);
			cy = diagram.canvasY(last.y);
			if(last.x < diagram.getXMax())
				g.drawLine(diagram.canvasX(last.x), cy, diagram.getRightPixel(), cy);
			g.draw(path);

//			paintControl(g);

//			g.setColor(Color.BLACK);
//			g.drawRect(MARGIN_LEFT - 1, MARGIN_TOP - 1, w-MARGIN_LEFT-MARGIN_RIGHT, h-MARGIN_TOP-MARGIN_BOTTOM);
		}

		private void paintControl(Graphics g) {
			g.setColor(Color.BLUE);
			int cxp = -1;
			int cyp = -1;
			for(int i = diagram.getLeftPixel(); i < diagram.getRightPixel(); i++) {
				double rx = diagram.realX(i);
				double ry = ctrls.getInterpolatedValue(rx);
				int cy = diagram.canvasY(ry);
				if(i > 0) {
					g.drawLine(cxp, cyp, i, cy);
				}
				cxp = i;
				cyp = cy;
			}
		}
	}
}
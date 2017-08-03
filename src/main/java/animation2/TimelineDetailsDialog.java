package animation2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;

import ij.gui.GenericDialog;

public class TimelineDetailsDialog {

	public static void main(String...args) throws IOException {
		Timelines lines = new Timelines(1);
		JsonExporter.importTimelines(lines, new File("H:\\170704_test_gut_14-34-13-2.json"));
		showTimelineDialog(lines, 3);
	}

	private static class Dialog extends GenericDialog {

		public Dialog(String title) {
			super(title);
		}

		protected GridBagConstraints getConstraints() {
			GridBagLayout layout = (GridBagLayout) getLayout();
			Panel panel = new Panel();
			addPanel(panel);
			GridBagConstraints constraints = layout.getConstraints(panel);
			remove(panel);
			return constraints;
		}

		public void addTimelinePanel(Panel p) {
			GridBagConstraints c = getConstraints();
			GridBagLayout layout = (GridBagLayout)getLayout();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			layout.setConstraints(p, c);
			add(p);
		}
	}

	public static void showTimelineDialog(final Timelines timelines, final int timeline) {
		Dialog gd = new Dialog(Timelines.getName(timeline));
		TimelinePanel p = new TimelinePanel(timelines, timeline);
		gd.addTimelinePanel(p);
		gd.showDialog();
	}

	private static class TimelinePanel extends DoubleBuffer {

		private static final long serialVersionUID = 1L;
		private int timeline;

		private DiagramCanvas diagram;

		private BezierCurveEditor.CurveChangerCanvas bezier;
//		private TimelineOverview timelineOverview;

		private Timelines timelines;

		private void getBoundingBox(int tl, Point ll, Point ur) {
			final Point ll1 = new Point(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
			final Point ur1 = new Point(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY);
			timelines.getBoundingBox(ll1, ur1);
			final Point ll2 = new Point(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
			final Point ur2 = new Point(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY);
			timelines.get(tl).getBoundingBox(ll2, ur2);
			if(ll2.y == Double.POSITIVE_INFINITY)
				ll2.y = 0;
			if(ur2.y == Double.NEGATIVE_INFINITY)
				ur2.y = ll.y + 1;
			if(ur2.y == ll2.y)
				ur2.y += 1;

			ll.set(ll1.x, ll2.y);
			ur.set(ur1.x, ur2.y);
		}

		public TimelinePanel(final Timelines ctrls, final int timeline) {
			this.timelines = ctrls;
			this.timeline = timeline;
			this.diagram = new DiagramCanvas();
			diagram.addListenersTo(this);

			this.setBackground(Color.WHITE);
			bezier = new BezierCurveEditor.CurveChangerCanvas(diagram, ctrls.get(timeline));
//			timelineOverview = new TimelineOverview(diagram, ctrls);
			final Point ll = new Point(0, 0);
			final Point ur = new Point(0, 0);
			bezier.addCurveChangeListener(new BezierCurveEditor.CurveChangerCanvas.Listener() {
				@Override
				public void curveChanged(boolean boundingBoxChanged) {
					if(boundingBoxChanged) {
						getBoundingBox(timeline, ll, ur);
						diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
					}
					repaint();
				}
			});
			this.addMouseListener(bezier);
			this.addMouseMotionListener(bezier);

			int marginLeft = getMaximumStringWidth(ctrls) + 5;
			getBoundingBox(timeline, ll, ur);
			this.diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
			this.diagram.setMargins(2, marginLeft, 30, 2);
			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					diagram.setSizes(getWidth(), getHeight());
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(500, 300);
		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(0, 300);
		}

		private int getMaximumStringWidth(Timelines timelines) {
			int max = 0;
			setFont(new Font("Helvetica", Font.PLAIN, 8));
			for(int i = 0; i < timelines.size(); i++) {
				String s = Timelines.getName(i);
				int w = getFontMetrics(getFont()).stringWidth(s);
				if(w > max)
					max = w;
			}
			return max;
		}

		@Override
		public void paintBuffer(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			int w = getWidth();
			int h = getHeight();
			System.out.println(w + ", " + h);

			g.clearRect(0, 0, w, h);

			g.setColor(Color.BLACK);
			g2d.setStroke(new BasicStroke(1));

			g2d.setClip(diagram.getClippingRect());
			bezier.paint(g);
			g2d.setClip(0, 0, w, h);

			diagram.drawFrame(g2d);
			diagram.drawXMinMax(g2d);
			diagram.drawYMinMax(g2d);
		}
	}
}

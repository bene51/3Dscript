package animation2;

import java.awt.BasicStroke;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

public class AnimationPanel extends Panel implements NumberField.Listener, FocusListener {

	private static final long serialVersionUID = 1L;

	private NumberField currentTimepointTF = new NumberField(3);
	private Choice timelineChoice;
	private DoubleSliderCanvas slider;
	private Timelines timelines;
	private int timeline;

	public interface Listener {
		public void currentTimepointChanged(int t);
		public void recordKeyframe();
		public void insertSpin();
		public void record(int from, int to);
		public void exportJSON();
		public void importJSON();
	}

	public AnimationPanel(String[] timelineNames, Timelines ctrls, int timeline, int frame) {
		super();
		this.timelines = ctrls;
		this.timeline = timeline;
		currentTimepointTF.setIntegersOnly(true);
		currentTimepointTF.addListener(this);
		currentTimepointTF.addFocusListener(this);

		this.slider = new DoubleSliderCanvas(ctrls, this, frame);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		timelineChoice = new Choice();
		for(String s : timelineNames)
			timelineChoice.add(s);

		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 2, 10, 5);
		c.weightx = 1.0;
		add(slider, c);

		Panel buttons = new Panel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(currentTimepointTF);

		Button but = new Button("Set");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireRecordKeyframe();
			}
		});
		buttons.add(but);
		but = new Button("Spin");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireInsertSpin();
				updateBoundingBox();
				repaint();
			}
		});
		buttons.add(but);
		but = new Button("Record");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireRecord();
			}
		});
		buttons.add(but);
		but = new Button("Export");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireExportJSON();
			}
		});
		buttons.add(but);
		but = new Button("Import");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireImportJSON();
			}
		});
		buttons.add(but);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, -3, 10, 5);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 0;
		c.gridy++;
		add(buttons, c);

		slider.setCurrentFrame(frame);
		valueChanged();
	}

	public void updateBoundingBox() {
		Point ll = new Point(0, 0);
		Point ur = new Point(0, 0);
		slider.getBoundingBox(timeline, ll, ur);
		slider.diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
		slider.repaint();
	}

	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	public void addTimelineListener(Listener l) {
		listeners.add(l);
	}

	public void removeTimelineListener(Listener l) {
		listeners.remove(l);
	}

	private void fireCurrentTimepointChanged(int i) {
		for(Listener l : listeners)
			l.currentTimepointChanged(i);
	}

	private void fireRecordKeyframe() {
		for(Listener l : listeners)
			l.recordKeyframe();
	}

	private void fireInsertSpin() {
		for(Listener l : listeners)
			l.insertSpin();
	}

	private void fireRecord() {
		int from = (int)Math.round(slider.diagram.getXMin());
		int to = (int)Math.round(slider.diagram.getXMax());
		for(Listener l : listeners)
			l.record(from, to);
	}

	private void fireExportJSON() {
		for(Listener l : listeners)
			l.exportJSON();
	}

	private void fireImportJSON() {
		for(Listener l : listeners)
			l.importJSON();
	}

	public int getCurrentFrame() {
		return Integer.parseInt(currentTimepointTF.getText());
	}

	@Override
	public void focusGained(FocusEvent e) {
		TextField tf = (TextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {
		valueChanged(Double.parseDouble(currentTimepointTF.getText()));
	}

	@Override
	public void valueChanged(double v) {
		try {
			int t = Integer.parseInt(currentTimepointTF.getText());
			slider.setCurrentFrame(t);
			fireCurrentTimepointChanged(t);
		} catch(Exception ex) {
		}
	}

	public void valueChanged() {
		currentTimepointTF.setText(Integer.toString(slider.currentFrame));
		fireCurrentTimepointChanged(slider.currentFrame);
	}

	private static class DoubleSliderCanvas extends DoubleBuffer implements MouseMotionListener, MouseListener {

		private static final long serialVersionUID = 1L;
		private Color color = new Color(0, 127, 0, 200);
		private int currentFrame;
		private int currentDrawn;

		private DiagramCanvas diagram;

		private boolean dragging = false;

		private AnimationPanel slider;

		private TimelineOverview timelineOverview;

		private Timelines timelines;

		private void getBoundingBox(int tl, Point ll, Point ur) {
			final Point ll1 = new Point(Integer.MAX_VALUE, Double.POSITIVE_INFINITY);
			final Point ur1 = new Point(Integer.MIN_VALUE, Double.NEGATIVE_INFINITY);
			slider.timelines.getBoundingBox(ll1, ur1);
			ll.set(ll1.x, 0);
			ur.set(ur1.x, timelines.size());
		}

		public DoubleSliderCanvas(final Timelines ctrls, final AnimationPanel slider, final int currentFrame) {
			this.timelines = ctrls;
			this.slider = slider;
			this.diagram = new DiagramCanvas();
			this.currentFrame = currentFrame;
			diagram.addListenersTo(this);

			this.addMouseMotionListener(this);
			this.addMouseListener(this);
			this.setBackground(Color.WHITE);
			this.setPreferredSize(new Dimension(258, 2 + 30 + (ctrls.size() + 1) * 10));
			// bezier = new BezierCurveEditor.CurveChangerCanvas(diagram, ctrls);
			timelineOverview = new TimelineOverview(diagram, ctrls);
			final Point ll = new Point(0, 0);
			final Point ur = new Point(0, 0);
			timelineOverview.addCurveChangeListener(new TimelineOverview.Listener() {
				@Override
				public void curveChanged(boolean boundingBoxChanged) {
					if(boundingBoxChanged) {
						getBoundingBox(slider.timeline, ll, ur);
						diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
					}
					repaint();
				}
			});
			this.addMouseListener(timelineOverview);
			this.addMouseMotionListener(timelineOverview);

			int marginLeft = getMaximumStringWidth(ctrls) + 5;
			getBoundingBox(slider.timeline, ll, ur);
			this.diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
			this.diagram.setMargins(2, marginLeft, 30, 2);
			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					diagram.setSizes(getWidth(), getHeight());
				}
			});
		}

		private int getMaximumStringWidth(Timelines timelines) {
			int max = 0;
			setFont(new Font("Helvetica", Font.PLAIN, 8));
			for(String s : timelines.getNames()) {
				int w = getFontMetrics(getFont()).stringWidth(s);
				if(w > max)
					max = w;
			}
			return max;
		}

		public void setCurrentFrame(int frame) {
			this.currentFrame = frame;
			repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {
			if(dragging)
				e.consume();
		}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseDragged(MouseEvent e) {
			int canvasx = diagram.clampCanvasX(e.getX());
			int newx = (int)Math.round(diagram.realX(canvasx));
			if(dragging) {
				currentFrame = newx;
				repaint();
				slider.valueChanged();
				e.consume();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			if(Math.abs(x - currentDrawn) < 5) {
				setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
				dragging = true;
				return;
			} else {
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				dragging = false;
			}
			e.consume();
		}

		@Override
		public void paintBuffer(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			int w = getWidth();
			int h = getHeight();

			g.clearRect(0, 0, w, h);

			g2d.setStroke(new BasicStroke(2));
			g.setColor(color);

			g.setColor(Color.BLACK);
			g2d.setStroke(new BasicStroke(1));
			int kx = diagram.canvasX(currentFrame);
			if(kx >= diagram.getLeftPixel() && kx <= diagram.getRightPixel()) {
				g.drawLine(kx, diagram.getTopPixel(), kx, diagram.getBottomPixel());
				currentDrawn = kx;
			} else {
				currentDrawn = -Integer.MAX_VALUE;
			}

			// g2d.setClip(diagram.getClippingRect());
			timelineOverview.paint(g);
			g2d.setClip(0, 0, w, h);

			diagram.drawFrame(g2d);
			diagram.drawXMinMax(g2d);
		}
	}
}
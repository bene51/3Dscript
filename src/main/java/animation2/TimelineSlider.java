package animation2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

public class TimelineSlider extends Panel implements NumberField.Listener, FocusListener {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final Frame frame = new Frame();
				frame.setSize(500, 300);

				CtrlPoints ctrls = new CtrlPoints();
				ctrls.add(0, 0);
				ctrls.add(50, 50);

				TimelineSlider slider = new TimelineSlider(ctrls, 0);
				frame.add(slider);
				frame.setVisible(true);
			}
		});
	}

	private static final long serialVersionUID = 1L;


	private NumberField currentTimepointTF = new NumberField(3);

	private DoubleSliderCanvas slider;

	public interface Listener {
		public void currentTimepointChanged(int t);
	}

//	private static CtrlPoints makeDefault(TreeMap<Integer, Keyframe> keyframes) {
//		CtrlPoints ctrls = new CtrlPoints();
//		ctrls.add(keyframes.firstKey(), 0);
//		ctrls.add(keyframes.lastKey(), 0);
//		return ctrls;
//	}
//
//	/**
//	 * TODO temporary, remove again
//	 * @param keyframes
//	 * @param slider
//	 */
//	public TimelineSlider(TreeMap<Integer, Keyframe> keyframes, int currentTimepoint) {
//		this(keyframes, makeDefault(keyframes), currentTimepoint);
//	}

	public TimelineSlider(CtrlPoints ctrls, int currentTimepoint) {
		super();
		currentTimepointTF.setIntegersOnly(true);
		currentTimepointTF.addListener(this);
		currentTimepointTF.addFocusListener(this);

		this.slider = new DoubleSliderCanvas(ctrls, this);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		c.gridx = c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 2, 0, 5);
		c.weightx = 1.0;
		add(slider, c);

		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		c.insets = new Insets(3, 3, 0, 3);
		c.gridy++;

		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.insets = new Insets(3, 3, 0, 3);
		add(currentTimepointTF, c);
//		c.gridx++;
//		c.anchor = GridBagConstraints.CENTER;
//		add(gammaCTF, c);
//		c.gridx++;
//		c.anchor = GridBagConstraints.EAST;
//		add(maxCTF, c);

		slider.setCurrentFrame(currentTimepoint);
		valueChanged();
	}

	public CtrlPoints getControls() {
		return this.slider.bezier.getControls();
	}

	public void set(CtrlPoints ctrls) {
		this.slider.set(ctrls);
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

	public int getCurrentFrame() {
		return Integer.parseInt(currentTimepointTF.getText());
	}

	@Override
	public void focusGained(FocusEvent e) {
		TextField tf = (TextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {}

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

		private TimelineSlider slider;

		private BezierCurveEditor.CurveChangerCanvas bezier;

		public DoubleSliderCanvas(final CtrlPoints ctrls, TimelineSlider slider) {
			this.slider = slider;
			this.diagram = new DiagramCanvas();
			diagram.addListenersTo(this);

			this.addMouseMotionListener(this);
			this.addMouseListener(this);
			this.setBackground(Color.WHITE);
			this.setPreferredSize(new Dimension(200, 128));
			bezier = new BezierCurveEditor.CurveChangerCanvas(diagram, ctrls);
			final Point ll = new Point(0, 0);
			final Point ur = new Point(0, 0);
			bezier.addCurveChangeListener(new BezierCurveEditor.CurveChangerCanvas.Listener() {
				@Override
				public void curveChanged(boolean boundingBoxChanged) {
					if(boundingBoxChanged) {
						bezier.getControls().getBoundingBox(ll, ur);
						diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
					}
					repaint();
				}
			});
			this.addMouseListener(bezier);
			this.addMouseMotionListener(bezier);

			setFont(new Font("Helvetica", Font.PLAIN, 10));
			int marginLeft = 2 * getFontMetrics(getFont()).stringWidth(Double.toString(100)) + 5;
			ctrls.getBoundingBox(ll, ur);
			this.diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
			this.diagram.setMargins(2, marginLeft, 30, 2);
			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					diagram.setSizes(getWidth(), getHeight());
				}
			});
		}

		public void set(final CtrlPoints ctrls) {
			this.bezier.setControls(ctrls);
			Point ll = new Point(0, 0);
			Point ur = new Point(0, 0);
			ctrls.getBoundingBox(ll, ur);
			this.diagram.setBoundingBox(ll.x, ll.y, ur.x, ur.y);
			repaint();
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

			g2d.setClip(diagram.getClippingRect());
			bezier.paint(g);
			g2d.setClip(0, 0, w, h);

			diagram.drawFrame(g2d);
			diagram.drawXMinMax(g2d);
			diagram.drawYMinMax(g2d);
		}
	}
}
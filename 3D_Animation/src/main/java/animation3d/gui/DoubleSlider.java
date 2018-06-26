package animation3d.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DoubleSlider extends JPanel implements FocusListener, NumberField.Listener {

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		DoubleSlider slider = new DoubleSlider(new int[] {-100, 100}, new int[] {20, 50}, new Color(255, 0, 0, 100));
		frame.getContentPane().add(slider);
		frame.pack();
		frame.setVisible(true);
	}

	public static interface Listener {
		public void sliderChanged();
	}

	private static final long serialVersionUID = 1L;

	private DoubleSliderCanvas slider;
	private NumberField minTF = new NumberField(4);
	private NumberField maxTF = new NumberField(4);

	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	public DoubleSlider(int[] realMinMax, int[] setMinMax, Color color) {
		super();

		minTF.setIntegersOnly(true);
		minTF.addListener(this);
		minTF.addNumberFieldFocusListener(this);
		maxTF.setIntegersOnly(true);
		maxTF.addListener(this);
		maxTF.addNumberFieldFocusListener(this);

		this.slider = new DoubleSliderCanvas(realMinMax, setMinMax, color, this);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.insets = new Insets(0, 2, 0, 5);
		c.weightx = 1.0;
		add(slider, c);

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.gridwidth = 1;
		c.insets = new Insets(3, 3, 0, 3);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		add(minTF, c);
		c.gridx = 2;
		c.anchor = GridBagConstraints.EAST;
		add(maxTF, c);

		updateTextfieldsFromSliders();
	}

	public NumberField getMinField() {
		return minTF;
	}

	public NumberField getMaxField() {
		return maxTF;
	}

	public int getMin() {
		return slider.setMinMax[0];
	}

	public int getMax() {
		return slider.setMinMax[1];
	}

	public void setMinAndMax(int min, int max) {
		slider.setMinMax[0] = min;
		minTF.setText(Integer.toString(min));

		slider.setMinMax[1] = max;
		maxTF.setText(Integer.toString(max));

		slider.repaint();
	}

	@Override
	public void focusGained(FocusEvent e) {
		JTextField tf = (JTextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {
		valueChanged(0);
	}

	@Override
	public void valueChanged(double v) {
		try {
			slider.setMinMax[0] = Integer.parseInt(minTF.getText());
			slider.setMinMax[1] = Integer.parseInt(maxTF.getText());
			slider.repaint();
			fireSliderChanged();
		} catch(Exception ex) {
		}
	}

	private void updateTextfieldsFromSliders() {
		minTF.setText(Integer.toString(slider.setMinMax[0]));
		maxTF.setText(Integer.toString(slider.setMinMax[1]));
		fireSliderChanged();
	}

	public void set(final int[] realMinMax, final int[] setMinMax, Color color) {
		slider.set(realMinMax, setMinMax, color);
	}

	public void addSliderChangeListener(Listener l) {
		listeners.add(l);
	}

	public void removeSliderChangeListener(Listener l) {
		listeners.remove(l);
	}

	private void fireSliderChanged() {
		for(Listener l : listeners)
			l.sliderChanged();
	}

	private static class DoubleSliderCanvas extends DoubleBuffer implements MouseMotionListener, MouseListener {

		private static final long serialVersionUID = 1L;
		private Color color;
		private int[] realMinMax;
		private int[] setMinMax;

		private DiagramCanvas diagram;

		private DoubleSlider slider;

		public DoubleSliderCanvas(final int[] realMinMax, int[] setMinMax, Color color, DoubleSlider slider) {
			this.color = color;
			this.slider = slider;
			this.setMinMax = setMinMax;
			this.realMinMax = realMinMax;

			this.diagram = new DiagramCanvas();
			diagram.setMargins(2, 2, 2, 2);
			diagram.setBoundingBox(realMinMax[0], 0, realMinMax[1], 5);

			this.addMouseMotionListener(this);
			this.addMouseListener(this);
			this.setBackground(Color.WHITE);
			this.setFont(new Font("Helvetica", Font.PLAIN, 10));
			this.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					diagram.setSizes(getWidth(), getHeight());
					repaint();
				}
			});
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(0, 12);

		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(0, 12);

		}

		void set(final int[] realMinMax, final int[] setMinMax, Color color) {
			this.color = color;
			this.setMinMax = setMinMax;
			this.realMinMax = realMinMax;
			diagram.setBoundingBox(realMinMax[0], 0, realMinMax[1], 5);
			repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

		private final int DRAG_NONE  = 0;
		private final int DRAG_LEFT  = 1;
		private final int DRAG_RIGHT = 2;

		private int drag = DRAG_NONE;

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX();

			if(Math.abs(x - diagram.canvasX(setMinMax[0])) < 3) {
				drag = DRAG_LEFT;
			}
			else if(Math.abs(x - diagram.canvasX(setMinMax[1])) < 3) {
				drag = DRAG_RIGHT;
			}
			else {
				drag = DRAG_NONE;
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			drag = DRAG_NONE;
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		private final int clamp(int v, int min, int max) {
			return Math.max(min, Math.min(max, v));
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if(drag == DRAG_NONE)
				return;

			if(drag == DRAG_LEFT) {
				setMinMax[0] = clamp((int)Math.round(diagram.realX(e.getX())), realMinMax[0], realMinMax[1]);
			}
			else if(drag == DRAG_RIGHT) {
				setMinMax[1] = clamp((int)Math.round(diagram.realX(e.getX())), realMinMax[0], realMinMax[1]);
			}
			slider.updateTextfieldsFromSliders();
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int x = e.getX();

			if(Math.abs(x - diagram.canvasX(setMinMax[0])) < 3) {
				setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
			}
			else if(Math.abs(x - diagram.canvasX(setMinMax[1])) < 3) {
				setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
			}
			else {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}

		@Override
		public void paintBuffer(Graphics gx) {
			Graphics2D g = (Graphics2D)gx;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.clearRect(0, 0, getWidth(), getHeight());

			g.setColor(color);
			int x0 = diagram.canvasX(setMinMax[0]);
			int x1 = diagram.canvasX(setMinMax[1]);
			int wi = x1 - x0;
			g.fillRect(x0, diagram.getTopPixel() - 1, wi, diagram.getAvailableHeight() + 1);

			g.setColor(Color.BLACK);
			g.drawRect(x0, diagram.getTopPixel() - 1, wi, diagram.getAvailableHeight() + 1);

			g.setColor(Color.BLACK);
			diagram.drawFrame(gx);
		}
	}
}

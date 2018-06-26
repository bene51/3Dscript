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

public class SingleSlider extends JPanel implements FocusListener, NumberField.Listener {

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		SingleSlider slider = new SingleSlider(100, 75, new Color(255, 0, 0, 100));
		frame.getContentPane().add(slider);
		frame.pack();
		frame.setVisible(true);
	}

	public static interface Listener {
		public void sliderChanged();
	}

	private static final long serialVersionUID = 1L;

	private SingleSliderCanvas slider;
	private NumberField maxTF = new NumberField(4);

	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	public SingleSlider(int realMax, int setMax, Color color) {
		super();

		maxTF.setIntegersOnly(true);
		maxTF.addListener(this);
		maxTF.addNumberFieldFocusListener(this);

		this.slider = new SingleSliderCanvas(realMax, setMax, color, this);
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
		add(maxTF, c);
//		c.gridx = 2;
//		c.anchor = GridBagConstraints.EAST;
//		add(maxTF, c);

		updateTextfieldsFromSliders();
	}

	public void setColor(Color c) {
		slider.setColor(c);
	}

	public SingleSliderCanvas getCanvas() {
		return slider;
	}

	public NumberField getMaxField() {
		return maxTF;
	}

	public int getMax() {
		return slider.setMax;
	}

	public void setMax(int max) {
		slider.setMax = max;
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
			slider.setMax = Integer.parseInt(maxTF.getText());
			slider.repaint();
			fireSliderChanged();
		} catch(Exception ex) {
		}
	}

	private void updateTextfieldsFromSliders() {
		maxTF.setText(Integer.toString(slider.setMax));
		fireSliderChanged();
	}

	public void set(final int realMax, final int setMax, Color color) {
		slider.set(realMax, setMax, color);
		maxTF.setText(Integer.toString(slider.setMax));
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

	static class SingleSliderCanvas extends DoubleBuffer implements MouseMotionListener, MouseListener {

		private static final long serialVersionUID = 1L;
		private Color color;
		private int realMax;
		private int setMax;

		private DiagramCanvas diagram;

		private SingleSlider slider;

		public SingleSliderCanvas(final int realMax, int setMax, Color color, SingleSlider slider) {
			this.color = color;
			this.slider = slider;
			this.setMax = setMax;
			this.realMax = realMax;

			this.diagram = new DiagramCanvas();
			diagram.setMargins(2, 2, 2, 2);
			diagram.setBoundingBox(0, 0, realMax, 5);

			this.addMouseMotionListener(this);
			this.addMouseListener(this);
			this.setBackground(Color.WHITE);
			this.setFont(new Font("Helvetica", Font.PLAIN, 10));
			this.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					diagram.setSizes(getWidth(), getHeight());
				}
			});
		}

		public void setColor(Color c) {
			this.color = c;
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(0, 12);

		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(0, 12);

		}

		void set(final int realMax, final int setMax, Color color) {
			this.color = color;
			this.setMax = setMax;
			this.realMax = realMax;
			diagram.setBoundingBox(0, 0, realMax, 5);
			repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

		private final int DRAG_NONE  = 0;
		private final int DRAG_RIGHT = 2;

		private int drag = DRAG_NONE;

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX();

			if(Math.abs(x - diagram.canvasX(setMax)) < 3) {
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

			if(drag == DRAG_RIGHT) {
				setMax = clamp((int)Math.round(diagram.realX(e.getX())), 0, realMax);
			}
			slider.updateTextfieldsFromSliders();
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int x = e.getX();

			if(Math.abs(x - diagram.canvasX(setMax)) < 3) {
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
			int x1 = diagram.canvasX(setMax);
			int wi = x1 - 0;
			g.fillRect(0, diagram.getTopPixel() - 1, wi, diagram.getAvailableHeight() + 1);

			g.setColor(Color.BLACK);
			g.drawRect(0, diagram.getTopPixel() - 1, wi, diagram.getAvailableHeight() + 1);

			g.setColor(Color.BLACK);
			diagram.drawFrame(gx);
		}
	}
}
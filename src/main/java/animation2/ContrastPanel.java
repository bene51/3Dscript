package animation2;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

public class ContrastPanel extends Panel implements NumberField.Listener, FocusListener {

	public static void main(String[] args) {
		Frame frame = new Frame();
		int[] histo = new int[256];
		for(int i = 0; i < 256; i++)
			histo[i] = i;
		RenderingSettings r = new RenderingSettings(0, 255, 1, 0, 255, 1);
		ContrastPanel slider = new ContrastPanel(histo, Color.RED, 0, 255, r, 2);
		frame.add(slider);
		frame.pack();
		frame.setVisible(true);
	}

	private static final long serialVersionUID = 1L;


	private NumberField minCTF = new NumberField(4, true);
	private NumberField maxCTF = new NumberField(4, true);
	private NumberField gammaCTF = new NumberField(4, true);

	private NumberField minATF = new NumberField(4, true);
	private NumberField maxATF = new NumberField(4, true);
	private NumberField gammaATF = new NumberField(4, true);

	private Choice channelChoice;

	private DoubleSliderCanvas slider;

	public static interface Listener {
		public void renderingSettingsChanged();
		public void channelChanged();
		public void renderingSettingsReset();
		public void record(NumberField src, String timelineName, boolean delete);
	}

	private ArrayList<Listener> listeners =
			new ArrayList<Listener>();

	public ContrastPanel(int[] histogram, Color color, double min, double max, RenderingSettings r, int nChannels) {
		super();
		minCTF.addListener(this);
		minCTF.addNumberFieldFocusListener(this);
		maxCTF.addListener(this);
		maxCTF.addNumberFieldFocusListener(this);
		gammaCTF.addListener(this);
		gammaCTF.addNumberFieldFocusListener(this);

		minATF.addListener(this);
		minATF.addNumberFieldFocusListener(this);
		maxATF.addListener(this);
		maxATF.addNumberFieldFocusListener(this);
		gammaATF.addListener(this);
		gammaATF.addNumberFieldFocusListener(this);

		gammaCTF.setText(Double.toString(r.colorGamma));
		gammaATF.setText(Double.toString(r.alphaGamma));

		this.slider = new DoubleSliderCanvas(histogram, color, min, max, r, this);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		c.gridx = c.gridy = 0;
		c.insets = new Insets(5, 2, 10, 5);
		channelChoice = new Choice();
		for(int i = 0; i < nChannels; i++)
			channelChoice.add("Channel " + (i + 1));
		add(channelChoice, c);
		channelChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				fireChannelChanged();
			}
		});

		c.gridx = 2;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		Button but = new Button("Reset rendering settings");
		but.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireRenderingSettingsReset();
			}
		});
		add(but, c);

		c.gridx = 0;
		c.gridy++;
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
		c.insets = new Insets(3, 3, 0, 0);
		add(new Label("color"), c);
		c.insets = new Insets(3, 3, 0, 3);
		c.weightx = 1;
		c.gridx++;
		add(minCTF, c);
		c.gridx++;
		c.anchor = GridBagConstraints.CENTER;
		add(gammaCTF, c);
		c.gridx++;
		c.anchor = GridBagConstraints.EAST;
		add(maxCTF, c);

		c.gridy++;

		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.insets = new Insets(3, 3, 0, 0);
		add(new Label("alpha"), c);
		c.insets = new Insets(3, 3, 0, 3);
		c.weightx = 1;
		c.gridx++;
		add(minATF, c);
		c.gridx++;
		c.anchor = GridBagConstraints.CENTER;
		add(gammaATF, c);
		c.gridx++;
		c.anchor = GridBagConstraints.EAST;
		add(maxATF, c);

		updateTextfieldsFromSliders();
	}

	public int getChannel() {
		return channelChoice.getSelectedIndex();
	}

	public void set(int[] histogram, Color color, double min, double max, RenderingSettings r) {
		gammaATF.setText(Double.toString(r.alphaGamma));
		gammaCTF.setText(Double.toString(r.colorGamma));
		slider.set(histogram, color, min, max, r);
		updateTextfieldsFromSliders();
		repaint();
	}

	public void addContrastPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireRenderingSettingsChanged() {
		for(Listener l : listeners)
			l.renderingSettingsChanged();
	}

	private void fireRenderingSettingsReset() {
		for(Listener l : listeners)
			l.renderingSettingsReset();
	}

	private void fireChannelChanged() {
		for(Listener l : listeners)
			l.channelChanged();
	}

	private void fireRecord(NumberField src, String timelineName, boolean delete) {
		for(Listener l : listeners)
			l.record(src, timelineName, delete);
	}

	@Override
	public void focusGained(FocusEvent e) {
		TextField tf = (TextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {
		valueChanged(0);
	}

	@Override
	public void record(NumberField src, boolean delete) {
		String timelineName = null;
		int c = channelChoice.getSelectedIndex();
		if(src == minCTF)
			timelineName = "Channel " + (c + 1) + " color min";
		else if(src == maxCTF)
			timelineName = "Channel " + (c + 1) + " color max";
		else if(src == gammaCTF)
			timelineName = "Channel " + (c + 1) + " color gamma";
		else if(src == minATF)
			timelineName = "Channel " + (c + 1) + " alpha min";
		else if(src == maxATF)
			timelineName = "Channel " + (c + 1) + " alpha max";
		else if(src == gammaATF)
			timelineName = "Channel " + (c + 1) + " alpha gamma";
		fireRecord(src, timelineName, delete);
	}

	@Override
	public void valueChanged(double v) {
		try {
			slider.renderingSettings.colorMin = (float)Double.parseDouble(minCTF.getText());
			slider.renderingSettings.colorMax = (float)Double.parseDouble(maxCTF.getText());
			slider.renderingSettings.colorGamma = (float)Double.parseDouble(gammaCTF.getText());
			slider.renderingSettings.alphaMin = (float)Double.parseDouble(minATF.getText());
			slider.renderingSettings.alphaMax = (float)Double.parseDouble(maxATF.getText());
			slider.renderingSettings.alphaGamma = (float)Double.parseDouble(gammaATF.getText());
			slider.update();
			fireRenderingSettingsChanged();
		} catch(Exception ex) {
		}
	}

	private void updateTextfieldsFromSliders() {
		minCTF.setText(Double.toString(slider.renderingSettings.colorMin));
		maxCTF.setText(Double.toString(slider.renderingSettings.colorMax));
		minATF.setText(Double.toString(slider.renderingSettings.alphaMin));
		maxATF.setText(Double.toString(slider.renderingSettings.alphaMax));
		fireRenderingSettingsChanged();
	}

	private static class DoubleSliderCanvas extends DoubleBuffer implements MouseMotionListener, MouseListener {

		private static final long serialVersionUID = 1L;
		private int[] histogram;
		private double min, max;
		private Color color;
		private RenderingSettings renderingSettings;

		private int drawnColorMin, drawnColorMax, drawnAlphaMin, drawnAlphaMax;

		private int dragging = DRAGGING_NONE;

		private static final int DRAGGING_NONE  = 0;
		private static final int DRAGGING_COLOR_LEFT  = 1;
		private static final int DRAGGING_COLOR_RIGHT = 2;
		private static final int DRAGGING_ALPHA_LEFT  = 3;
		private static final int DRAGGING_ALPHA_RIGHT = 4;

		private ContrastPanel slider;

		public DoubleSliderCanvas(int[] histogram, Color color, double min, double max, RenderingSettings r, ContrastPanel slider) {
			set(histogram, color, min, max, r);
			this.slider = slider;
			this.addMouseMotionListener(this);
			this.addMouseListener(this);
			this.setPreferredSize(new Dimension(258, 128));
			this.setMinimumSize(new Dimension(0, 0));
			this.setBackground(Color.WHITE);
		}

		public void set(int[] histogram, Color color, double min, double max, RenderingSettings r) {
			this.histogram = histogram;
			this.color = color;
			this.min = min;
			this.max = max;
			this.renderingSettings = r;
		}

		public void update() {
			repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void mouseDragged(MouseEvent e) {
			double inc = getWidth() / (max - min + 1); // TODO fix the width
			int newx = (int)Math.round(e.getX() / inc + min);
			switch(dragging) {
				case DRAGGING_ALPHA_LEFT:
					float tmp = Math.max((float)min, Math.min(renderingSettings.alphaMax, newx));
					if(tmp != renderingSettings.alphaMin) {
						renderingSettings.alphaMin = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
					}
					break;
				case DRAGGING_ALPHA_RIGHT:
					tmp = Math.min((float)max, Math.max(renderingSettings.alphaMin, newx - 1));
					if(tmp != renderingSettings.alphaMax) {
						renderingSettings.alphaMax = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
					}
					break;
				case DRAGGING_COLOR_LEFT:
					tmp = Math.max((float)min, Math.min(renderingSettings.colorMax, newx));
					if(tmp != renderingSettings.colorMin) {
						renderingSettings.colorMin = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
					}
					break;
				case DRAGGING_COLOR_RIGHT:
					tmp = Math.min((float)max, Math.max(renderingSettings.colorMin, newx - 1));
					if(tmp != renderingSettings.colorMax) {
						renderingSettings.colorMax = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
					}
					break;
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			if(Math.abs(x - drawnAlphaMin) < 5) {
				setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
				dragging = DRAGGING_ALPHA_LEFT;
			} else if(Math.abs(x - drawnAlphaMax) < 5) {
				setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
				dragging = DRAGGING_ALPHA_RIGHT;
			} else if(Math.abs(x - drawnColorMin) < 5) {
				setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
				dragging = DRAGGING_COLOR_LEFT;
			} else if(Math.abs(x - drawnColorMax) < 5) {
				setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
				dragging = DRAGGING_COLOR_RIGHT;
			} else {
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				dragging = DRAGGING_NONE;
			}
		}

		int getMax(int[] histo) {
			return getMax(histo, -1);
		}

		int getMax(int[] histo, int notUsingIndex) {
			long max = 0;
			int maxi = 0;
			for(int i = 0; i < histo.length; i++) {
				if(histo[i] > max && i != notUsingIndex) {
					max = histo[i];
					maxi = i;
				}
			}
			return maxi;
		}

		@Override
		public void paintBuffer(Graphics g) {
			int w = getWidth();
			int h = getHeight();

			double bw = (w - 2.0) / histogram.length;
			int maxi1 = getMax(histogram);
			int maxn1 = histogram[maxi1];
			int maxi2 = getMax(histogram, maxi1); // second hightest value
			int maxn2 = histogram[maxi2];

			if(maxn1 > 2 * maxn2)
				maxn1 = (int)(1.5 * maxn2);

			g.clearRect(0, 0, w, h);

			g.setColor(color);
			for(int i = 0; i < histogram.length; i++) {
				int bx = (int)Math.round(i * bw);
				int bh = Math.round(histogram[i] * h / maxn1);
				int by = h - bh;
				g.drawLine(1 + bx, by, 1 + bx, by + bh);
			}

			// color transfer function
			g.setColor(Color.BLACK);
			int xprev = 0;
			int yprev = 0;
			for(int bx = 0; bx < w; bx++) {
				int x = (int)Math.round(min + bx * (max - min) / w);
				double ly = (x - renderingSettings.colorMin) / (renderingSettings.colorMax - renderingSettings.colorMin);
				ly = Math.max(0, Math.min(1, ly));
				double y = h * Math.pow(ly, renderingSettings.colorGamma);
				// print(x + ": " + y);
				if(bx != 0)
					g.drawLine(xprev, yprev, bx + 1, h - 1 - (int)Math.round(y));
				xprev = bx + 1;
				yprev = h - 1 - (int)Math.round(y);
			}
			drawnColorMin = (int)Math.round((renderingSettings.colorMin - min) * w / (max - min));
			drawnColorMax = (int)Math.round((renderingSettings.colorMax - min) * w / (max - min));

			g.drawLine(drawnColorMin, 0, drawnColorMin, h);
			g.drawLine(drawnColorMax, 0, drawnColorMax, h);

			// alpha transfer function
			g.setColor(Color.BLUE);
			xprev = 0;
			yprev = 0;
			for(int bx = 0; bx < w; bx++) {
				int x = (int)Math.round(min + bx * (max - min) / w);
				double ly = (x - renderingSettings.alphaMin) / (renderingSettings.alphaMax - renderingSettings.alphaMin);
				ly = Math.max(0, Math.min(1, ly));
				double y = h * Math.pow(ly, renderingSettings.alphaGamma);
				// print(x + ": " + y);
				if(bx != 0)
					g.drawLine(xprev, yprev, bx + 1, h - 1 - (int)Math.round(y));
				xprev = bx + 1;
				yprev = h - 1 - (int)Math.round(y);
			}
			drawnAlphaMin = (int)Math.round((renderingSettings.alphaMin - min) * w / (max - min));
			drawnAlphaMax = (int)Math.round((renderingSettings.alphaMax - min) * w / (max - min));

			g.drawLine(drawnAlphaMin, 0, drawnAlphaMin, h);
			g.drawLine(drawnAlphaMax, 0, drawnAlphaMax, h);

			g.setColor(Color.BLACK);
			g.drawRect(0, 0, w-1, h-1);
		}
	}
}
package animation3d.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import animation3d.renderer3d.ExtendedRenderingState;
import animation3d.renderer3d.RenderingAlgorithm;
import animation3d.textanim.CustomDecimalFormat;

public class ContrastPanel extends JPanel implements NumberField.Listener, FocusListener {

	public static void main(String[] args) {
		JFrame frame = new JFrame("");
		int[] histo = new int[256];
		for(int i = 0; i < 256; i++)
			histo[i] = i;
		ContrastPanel slider = new ContrastPanel(
				new int[][] { histo },
				new double[] { 0 }, // min
				new double[] { 255 }, // max
				new double[][] {{0, 255, 1, 0, 255, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}},
				Color.BLACK);
		frame.getContentPane().add(slider);
		frame.setSize(600, 400);
		frame.setVisible(true);
	}

	private static final long serialVersionUID = 1L;


	private NumberField minCTF = new NumberField(4);
	private NumberField maxCTF = new NumberField(4);
	private NumberField gammaCTF = new NumberField(4);

	private NumberField minATF = new NumberField(4);
	private NumberField maxATF = new NumberField(4);
	private NumberField gammaATF = new NumberField(4);

	private NumberField koTF = new NumberField(4);
	private NumberField kdTF = new NumberField(4);
	private NumberField ksTF = new NumberField(4);
	private NumberField shininessTF = new NumberField(4);

	private JPanel lightPanel;

	private JCheckBox useLightCB;

	private JComboBox<String> channelChoice;

	final JComboBox<String> renderingAlgorithm;

	private DoubleSliderCanvas slider;

	private SingleSlider[] weightSliders;

	private boolean dontFire = false;

	public static interface Listener {
		// public void renderingSettingsChanged(boolean lightsChanged);
		public void intensityChanged(int channel, double min, double max, double gamma);
		public void alphaChanged(int channel, double min, double max, double gamma);
		public void lightsChanged(int channel, boolean useLight, double kObj, double kDiff, double kSpec, double shininess);
		public void weightsChanged(int channel, double weight);
		public void colorChanged(int channel, Color c);
		public void backgroundChanged(Color bg);
		public void channelChanged();
		public void renderingSettingsReset();
		public void renderingAlgorithmChanged(RenderingAlgorithm algorithm);
	}

	private int[][] histogram;
	private double[] min;
	private double[] max;
	private double[][] renderingSettings;
	private Color background;

	private int channel = 0;

	private ArrayList<Listener> listeners =
			new ArrayList<Listener>();

	public ContrastPanel(int[][] histogram, double[] min, double max[], final double[][] r, final Color background) {
		super();

		this.background = background;

		this.histogram = histogram;
		this.min = min;
		this.max = max;
		this.renderingSettings = r;

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

		koTF.addListener(this);
		koTF.addNumberFieldFocusListener(this);
		kdTF.addListener(this);
		kdTF.addNumberFieldFocusListener(this);
		ksTF.addListener(this);
		ksTF.addNumberFieldFocusListener(this);
		shininessTF.addListener(this);
		shininessTF.addNumberFieldFocusListener(this);

		gammaCTF.setText(CustomDecimalFormat.format(r[channel][ExtendedRenderingState.INTENSITY_GAMMA], 1));
		gammaATF.setText(CustomDecimalFormat.format(r[channel][ExtendedRenderingState.ALPHA_GAMMA], 1));

		// TODO light checkbox

		koTF.setLimits(0, 1);
		koTF.setText(CustomDecimalFormat.format(r[channel][ExtendedRenderingState.LIGHT_K_OBJECT], 1));
		kdTF.setLimits(0, 1);
		kdTF.setText(CustomDecimalFormat.format(r[channel][ExtendedRenderingState.LIGHT_K_DIFFUSE], 1));
		ksTF.setLimits(0, 1);
		ksTF.setText(CustomDecimalFormat.format(r[channel][ExtendedRenderingState.LIGHT_K_SPECULAR], 1));
		ksTF.setLimits(0, 5);
		shininessTF.setText(CustomDecimalFormat.format(r[channel][ExtendedRenderingState.LIGHT_SHININESS], 1));

		this.slider = new DoubleSliderCanvas(histogram[channel], min[channel], max[channel], r[channel], this);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		c.gridx = c.gridy = 0;
		c.insets = new Insets(5, 2, 10, 5);
		channelChoice = new JComboBox<String>();
		for(int i = 0; i < renderingSettings.length; i++)
			channelChoice.addItem("Channel " + (i + 1));
		add(channelChoice, c);
		channelChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					setChannel(channelChoice.getSelectedIndex());
					fireChannelChanged();
				}
			}
		});

		c.gridx = 2;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		JButton but = new JButton("Reset rendering settings");
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
		add(new JLabel("intensity"), c);
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
		add(new JLabel("alpha"), c);
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


		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 0, 0, 0);
		this.useLightCB = new JCheckBox("Enable light", r[channel][ExtendedRenderingState.USE_LIGHT] > 0);
		add(useLightCB, c);

		c.gridy++;
		c.insets = new Insets(0, 20, 10, 0);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;

		lightPanel = new JPanel(new GridLayout(4, 2, 3, 3));
		lightPanel.add(new JLabel("object"));
		lightPanel.add(koTF);
		lightPanel.add(new JLabel("diffuse"));
		lightPanel.add(kdTF);
		lightPanel.add(new JLabel("specular"));
		lightPanel.add(ksTF);
		lightPanel.add(new JLabel("shininess"));
		lightPanel.add(shininessTF);
		add(lightPanel, c);
		lightPanel.setVisible(useLightCB.isSelected());

		useLightCB.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean use = useLightCB.isSelected();
				lightPanel.setVisible(use);
				renderingSettings[channel][ExtendedRenderingState.USE_LIGHT] = use ? 1 : 0;
				fireLightsChanged();
			}
		});


		weightSliders = new SingleSlider[renderingSettings.length];
		c.insets = new Insets(3, 3, 0, 3);

		for(int i = 0; i < renderingSettings.length; i++) {
			final int ch = i;
			int red   = (int)renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_RED];
			int green = (int)renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_GREEN];
			int blue  = (int)renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_BLUE];
			Color color = new Color(red, green, blue);
			if (red >= 100 && green >= 100 && blue >= 100)
				color = Color.black;

			final SingleSlider wslider = addSingleSlider(
					"Channel " + (i + 1) + " weight",
					100,
					100,
					color,
					c);
			wslider.addSliderChangeListener(new SingleSlider.Listener() {
				@Override
				public void sliderChanged() {
					double w = wslider.getMax() / 100f;
					renderingSettings[ch][ExtendedRenderingState.WEIGHT] = w;
					fireWeightsChanged(ch, w);
				}
			});

			wslider.getCanvas().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 2)
						return;
					// Color c = ColorPicker.pick();
					Color fg = new Color(
							(int)renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_RED],
							(int)renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_GREEN],
							(int)renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_BLUE]
							);
					ColorPicker cp = new ColorPicker(fg, ContrastPanel.this.background);
					cp.addForegroundColorListener(c -> {
						int red   = c.getRed();
						int green = c.getGreen();
						int blue  = c.getBlue();
						renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_RED]   = red;
						renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_GREEN] = green;
						renderingSettings[ch][ExtendedRenderingState.CHANNEL_COLOR_BLUE]  = blue;
						if(red < 100 || green < 100 || blue < 100)
							wslider.setColor(c);
						else
							wslider.setColor(Color.BLACK);
						fireColorChanged(ch, c);
						setChannel(channel);
					});
					cp.addBackgroundColorListener(c -> {
						int red   = c.getRed();
						int green = c.getGreen();
						int blue  = c.getBlue();
						Color bg = new Color(red, green, blue);
						fireBackgroundColorChanged(bg);
						ContrastPanel.this.background = bg;
					});
				}
			});

			weightSliders[i] = wslider;
		}

		c.insets.top = 20;
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.weightx = 0;
		add(new JLabel("Rendering algorithm"), c);

		renderingAlgorithm = new JComboBox<String>();
		renderingAlgorithm.addItem("Independent transparency");
		renderingAlgorithm.addItem("Combined transparency");
		renderingAlgorithm.addItem("Maximum intensity projection");
		renderingAlgorithm.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					switch(renderingAlgorithm.getSelectedIndex()) {
					case 0: fireRenderingAlgorithmChanged(RenderingAlgorithm.INDEPENDENT_TRANSPARENCY); break;
					case 1: fireRenderingAlgorithmChanged(RenderingAlgorithm.COMBINED_TRANSPARENCY); break;
					case 2: fireRenderingAlgorithmChanged(RenderingAlgorithm.MAXIMUM_INTENSITY); break;
					}
				}
			}
		});
		c.gridx++;
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(renderingAlgorithm, c);

		updateTextfieldsFromSliders();
	}

	@Override
	public void setBackground(Color bg) {
		this.background = bg;
	}

	public void setRenderingSettings(double[][] rs) {
		dontFire = true;
		this.renderingSettings = rs;
		setChannel(channel);
		for(int i = 0; i < renderingSettings.length; i++) {
			final int ch = i;
			int red   = (int)rs[ch][ExtendedRenderingState.CHANNEL_COLOR_RED];
			int green = (int)rs[ch][ExtendedRenderingState.CHANNEL_COLOR_GREEN];
			int blue  = (int)rs[ch][ExtendedRenderingState.CHANNEL_COLOR_BLUE];
			Color color = new Color(red, green, blue);
			if (red >= 100 && green >= 100 && blue >= 100)
				color = Color.black;

			double weight = rs[ch][ExtendedRenderingState.WEIGHT];

			SingleSlider wslider = weightSliders[i];
			wslider.set(100, (int)Math.round(100 * weight), color);
		}
		koTF.setText(CustomDecimalFormat.format(rs[channel][ExtendedRenderingState.LIGHT_K_OBJECT], 1));
		kdTF.setText(CustomDecimalFormat.format(rs[channel][ExtendedRenderingState.LIGHT_K_DIFFUSE], 1));
		ksTF.setText(CustomDecimalFormat.format(rs[channel][ExtendedRenderingState.LIGHT_K_SPECULAR], 1));
		shininessTF.setText(CustomDecimalFormat.format(rs[channel][ExtendedRenderingState.LIGHT_SHININESS], 1));
		boolean useLight = rs[channel][ExtendedRenderingState.USE_LIGHT] > 0;
		lightPanel.setVisible(useLight);
		useLightCB.setSelected(useLight);
		dontFire = false;
	}

	public RenderingAlgorithm getRenderingAlgorithm() {
		switch(renderingAlgorithm.getSelectedIndex()) {
		case 0: return RenderingAlgorithm.INDEPENDENT_TRANSPARENCY;
		case 1: return RenderingAlgorithm.COMBINED_TRANSPARENCY;
		case 2: return RenderingAlgorithm.MAXIMUM_INTENSITY;
		}
		return null;
	}

	public void setRenderingAlgorithm(RenderingAlgorithm algo) {
		renderingAlgorithm.setSelectedIndex(algo.ordinal());
	}

	private SingleSlider addSingleSlider(String label, int realMax, int setMax, Color color, GridBagConstraints c) {
		SingleSlider slider = new SingleSlider(realMax, setMax, color);

		GridBagLayout layout = (GridBagLayout)getLayout();
		c.gridy++;

		c.gridx = 0;
		if(label != null) {
			JLabel theLabel = new JLabel(label);
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.EAST;
			c.gridwidth = 1;
			c.weightx = 0;
			layout.setConstraints(theLabel, c);
			add(theLabel);
			c.gridx++;
		}
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		layout.setConstraints(slider, c);
		add(slider);
		return slider;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int c) {
		this.channel = c;
		gammaATF.setText(CustomDecimalFormat.format(renderingSettings[c][ExtendedRenderingState.ALPHA_GAMMA], 1));
		gammaCTF.setText(CustomDecimalFormat.format(renderingSettings[c][ExtendedRenderingState.INTENSITY_GAMMA], 1));
		koTF.setText(CustomDecimalFormat.format(renderingSettings[c][ExtendedRenderingState.LIGHT_K_OBJECT], 1));
		kdTF.setText(CustomDecimalFormat.format(renderingSettings[c][ExtendedRenderingState.LIGHT_K_DIFFUSE], 1));
		ksTF.setText(CustomDecimalFormat.format(renderingSettings[c][ExtendedRenderingState.LIGHT_K_SPECULAR], 1));
		shininessTF.setText(CustomDecimalFormat.format(renderingSettings[c][ExtendedRenderingState.LIGHT_SHININESS], 1));
		boolean useLight = renderingSettings[c][ExtendedRenderingState.USE_LIGHT] > 0;
		lightPanel.setVisible(useLight);
		useLightCB.setSelected(useLight);
		slider.set(histogram[c], min[c], max[c], renderingSettings[c]);
		updateTextfieldsFromSliders();
		slider.repaint();
		repaint();
	}

	public void addContrastPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireColorChanged(int channel, Color c) {
		for(Listener l : listeners)
			l.colorChanged(channel, c);
	}

	private void fireLightsChanged() {
		if(dontFire)
			return;
		double[] rs = renderingSettings[channel];
		for(Listener l : listeners)
			l.lightsChanged(channel,
					rs[ExtendedRenderingState.USE_LIGHT] > 0,
					rs[ExtendedRenderingState.LIGHT_K_OBJECT],
					rs[ExtendedRenderingState.LIGHT_K_DIFFUSE],
					rs[ExtendedRenderingState.LIGHT_K_SPECULAR],
					rs[ExtendedRenderingState.LIGHT_SHININESS]);
	}

	private void fireWeightsChanged(int channel, double weight) {
		if(dontFire)
			return;
		for(Listener l : listeners)
			l.weightsChanged(channel, weight);
	}

	private void fireIntensityChanged() {
		if(dontFire)
			return;
		double[] rs = renderingSettings[channel];
		for(Listener l : listeners)
			l.intensityChanged(channel,
					rs[ExtendedRenderingState.INTENSITY_MIN],
					rs[ExtendedRenderingState.INTENSITY_MAX],
					rs[ExtendedRenderingState.INTENSITY_GAMMA]);
	}

	private void fireAlphaChanged() {
		if(dontFire)
			return;
		double[] rs = renderingSettings[channel];
		for(Listener l : listeners)
			l.alphaChanged(channel,
					rs[ExtendedRenderingState.ALPHA_MIN],
					rs[ExtendedRenderingState.ALPHA_MAX],
					rs[ExtendedRenderingState.ALPHA_GAMMA]);
	}

	private void fireRenderingSettingsReset() {
		if(dontFire)
			return;
		for(Listener l : listeners)
			l.renderingSettingsReset();
	}

	private void fireBackgroundColorChanged(Color bg) {
		if(dontFire)
			return;
		for(Listener l : listeners)
			l.backgroundChanged(bg);
	}

	private void fireChannelChanged() {
		if(dontFire)
			return;
		for(Listener l : listeners)
			l.channelChanged();
	}

	private void fireRenderingAlgorithmChanged(RenderingAlgorithm algorithm) {
		if(dontFire)
			return;
		for(Listener l : listeners)
			l.renderingAlgorithmChanged(algorithm);
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

	private static boolean replaceIfUnequal(double[] array, int idx, double v) {
		if(array[idx] != v) {
			array[idx] = v;
			return true;
		}
		return false;
	}

	@Override
	public void valueChanged(double v) {
		try {
			double[] rs = slider.renderingSettings;
			boolean intensityChanged = false;
			intensityChanged = replaceIfUnequal(rs, ExtendedRenderingState.INTENSITY_MIN,   (float)Double.parseDouble(minCTF.getText()))   || intensityChanged;
			intensityChanged = replaceIfUnequal(rs, ExtendedRenderingState.INTENSITY_MAX,   (float)Double.parseDouble(maxCTF.getText()))   || intensityChanged;
			intensityChanged = replaceIfUnequal(rs, ExtendedRenderingState.INTENSITY_GAMMA, (float)Double.parseDouble(gammaCTF.getText())) || intensityChanged;;

			boolean alphaChanged = false;
			alphaChanged = replaceIfUnequal(rs, ExtendedRenderingState.ALPHA_MIN,   (float)Double.parseDouble(minATF.getText()))   || alphaChanged;
			alphaChanged = replaceIfUnequal(rs, ExtendedRenderingState.ALPHA_MAX,   (float)Double.parseDouble(maxATF.getText()))   || alphaChanged;
			alphaChanged = replaceIfUnequal(rs, ExtendedRenderingState.ALPHA_GAMMA, (float)Double.parseDouble(gammaATF.getText())) || alphaChanged;

			boolean lightsChanged = false;
			lightsChanged = replaceIfUnequal(rs, ExtendedRenderingState.LIGHT_K_OBJECT,   (float)Double.parseDouble(koTF.getText()))        || lightsChanged;
			lightsChanged = replaceIfUnequal(rs, ExtendedRenderingState.LIGHT_K_DIFFUSE,  (float)Double.parseDouble(kdTF.getText()))        || lightsChanged;
			lightsChanged = replaceIfUnequal(rs, ExtendedRenderingState.LIGHT_K_SPECULAR, (float)Double.parseDouble(ksTF.getText()))        || lightsChanged;
			lightsChanged = replaceIfUnequal(rs, ExtendedRenderingState.LIGHT_SHININESS,  (float)Double.parseDouble(shininessTF.getText())) || lightsChanged;

			slider.update();

			if(intensityChanged)
				fireIntensityChanged();
			if(alphaChanged)
				fireAlphaChanged();
			if(lightsChanged)
				fireLightsChanged();
		} catch(Exception ex) {
		}
	}

	private void updateTextfieldsFromSliders() {
		minCTF.setText(CustomDecimalFormat.format(slider.renderingSettings[ExtendedRenderingState.INTENSITY_MIN], 1));
		maxCTF.setText(CustomDecimalFormat.format(slider.renderingSettings[ExtendedRenderingState.INTENSITY_MAX], 1));
		minATF.setText(CustomDecimalFormat.format(slider.renderingSettings[ExtendedRenderingState.ALPHA_MIN], 1));
		maxATF.setText(CustomDecimalFormat.format(slider.renderingSettings[ExtendedRenderingState.ALPHA_MAX], 1));
	}

	private static class DoubleSliderCanvas extends DoubleBuffer implements MouseMotionListener, MouseListener {

		private static final long serialVersionUID = 1L;
		private int[] histogram;
		private double min, max;
		private Color color;
		private double[] renderingSettings;

		private int drawnColorMin, drawnColorMax, drawnAlphaMin, drawnAlphaMax;

		private int dragging = DRAGGING_NONE;

		private static final int DRAGGING_NONE  = 0;
		private static final int DRAGGING_COLOR_LEFT  = 1;
		private static final int DRAGGING_COLOR_RIGHT = 2;
		private static final int DRAGGING_ALPHA_LEFT  = 3;
		private static final int DRAGGING_ALPHA_RIGHT = 4;

		private ContrastPanel slider;

		public DoubleSliderCanvas(int[] histogram, double min, double max, double[] r, ContrastPanel slider) {
			set(histogram, min, max, r);
			this.slider = slider;
			this.addMouseMotionListener(this);
			this.addMouseListener(this);
			this.setBackground(Color.WHITE);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(0, 80);
		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(0, 80);
		}

		public void set(int[] histogram, double min, double max, double[] r) {
			this.histogram = histogram;
			this.min = min;
			this.max = max;
			this.renderingSettings = r;
			int red   = (int)r[ExtendedRenderingState.CHANNEL_COLOR_RED];
			int green = (int)r[ExtendedRenderingState.CHANNEL_COLOR_GREEN];
			int blue  = (int)r[ExtendedRenderingState.CHANNEL_COLOR_BLUE];
			if (red < 100 || green < 100 || blue < 100)
				this.color = new Color(red, green, blue);
			else
				this.color = Color.black;

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
					float tmp = (float)Math.max((float)min, Math.min(renderingSettings[ExtendedRenderingState.ALPHA_MAX], newx));
					if(tmp != renderingSettings[ExtendedRenderingState.ALPHA_MIN]) {
						renderingSettings[ExtendedRenderingState.ALPHA_MIN] = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
						slider.fireAlphaChanged();
					}
					break;
				case DRAGGING_ALPHA_RIGHT:
					tmp = (float)Math.min((float)max, Math.max(renderingSettings[ExtendedRenderingState.ALPHA_MIN], newx - 1));
					if(tmp != renderingSettings[ExtendedRenderingState.ALPHA_MAX]) {
						renderingSettings[ExtendedRenderingState.ALPHA_MAX] = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
						slider.fireAlphaChanged();
					}
					break;
				case DRAGGING_COLOR_LEFT:
					tmp = (float)Math.max((float)min, Math.min(renderingSettings[ExtendedRenderingState.INTENSITY_MAX], newx));
					if(tmp != renderingSettings[ExtendedRenderingState.INTENSITY_MIN]) {
						renderingSettings[ExtendedRenderingState.INTENSITY_MIN] = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
						slider.fireIntensityChanged();
					}
					break;
				case DRAGGING_COLOR_RIGHT:
					tmp = (float)Math.min((float)max, Math.max(renderingSettings[ExtendedRenderingState.INTENSITY_MIN], newx - 1));
					if(tmp != renderingSettings[ExtendedRenderingState.INTENSITY_MAX]) {
						renderingSettings[ExtendedRenderingState.INTENSITY_MAX] = tmp;
						repaint();
						slider.updateTextfieldsFromSliders();
						slider.fireIntensityChanged();
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

			double cmin   = renderingSettings[ExtendedRenderingState.INTENSITY_MIN];
			double cmax   = renderingSettings[ExtendedRenderingState.INTENSITY_MAX];
			double cgamma = renderingSettings[ExtendedRenderingState.INTENSITY_GAMMA];
			double amin   = renderingSettings[ExtendedRenderingState.ALPHA_MIN];
			double amax   = renderingSettings[ExtendedRenderingState.ALPHA_MAX];
			double agamma = renderingSettings[ExtendedRenderingState.ALPHA_GAMMA];

			// color transfer function
			g.setColor(Color.BLACK);
			int xprev = 0;
			int yprev = 0;
			for(int bx = 0; bx < w; bx++) {
				double x = min + bx * (max - min) / w;
				double ly = (x - cmin) / (cmax - cmin);
				ly = Math.max(0, Math.min(1, ly));
				double y = h * Math.pow(ly, cgamma);
				// print(x + ": " + y);
				if(bx != 0)
					g.drawLine(xprev, yprev, bx + 1, h - 1 - (int)Math.round(y));
				xprev = bx + 1;
				yprev = h - 1 - (int)Math.round(y);
			}
			drawnColorMin = (int)Math.round((cmin - min) * w / (max - min));
			drawnColorMax = (int)Math.round((cmax - min) * w / (max - min));

			g.drawLine(drawnColorMin, 0, drawnColorMin, h);
			g.drawLine(drawnColorMax, 0, drawnColorMax, h);

			// alpha transfer function
			g.setColor(Color.BLUE);
			xprev = 0;
			yprev = 0;
			for(int bx = 0; bx < w; bx++) {
				double x = min + bx * (max - min) / w;
				double ly = (x - amin) / (amax - amin);
				ly = Math.max(0, Math.min(1, ly));
				double y = h * Math.pow(ly, agamma);
				// print(x + ": " + y);
				if(bx != 0)
					g.drawLine(xprev, yprev, bx + 1, h - 1 - (int)Math.round(y));
				xprev = bx + 1;
				yprev = h - 1 - (int)Math.round(y);
			}
			drawnAlphaMin = (int)Math.round((amin - min) * w / (max - min));
			drawnAlphaMax = (int)Math.round((amax - min) * w / (max - min));

			g.drawLine(drawnAlphaMin, 0, drawnAlphaMin, h);
			g.drawLine(drawnAlphaMax, 0, drawnAlphaMax, h);

			g.setColor(Color.BLACK);
			g.drawRect(0, 0, w-1, h-1);
		}
	}
}
package animation3d.gui;

import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import animation3d.renderer3d.BoundingBox;
import animation3d.renderer3d.Scalebar;
import animation3d.renderer3d.Scalebar.Position;
import ij.gui.GenericDialog;

public class OutputPanel extends JPanel {

	public static void main(String...strings) {
		JFrame f = new JFrame("");
		f.getContentPane().add(new OutputPanel(
				200, 200, 1,
				new BoundingBox(100, 100, 100, 1, 1, 1),
				new Scalebar(100, 100, 100, 1, 1, 1)));
		f.pack();
		f.setVisible(true);
	}

	private static final long serialVersionUID = 1L;


	private NumberField widthTF, heightTF; // , zStepTF;
	private final JCheckBox sbBox, bbBox;

	public static interface Listener {
		public void outputWidthChanged(int w);
		public void outputHeightChanged(int h);
		public void boundingBoxChanged();
		public void scalebarChanged();
	}

	private final ArrayList<Listener> listeners =	new ArrayList<Listener>();

	private final BoundingBox boundingBox;
	private final Scalebar scaleBar;

	public void updateGui() {
		sbBox.setSelected(scaleBar.isVisible());
		bbBox.setSelected(boundingBox.isVisible());
	}

	public OutputPanel(int w, int h, float zStep, final BoundingBox boundingBox, final Scalebar scaleBar) {
		super(new GridLayout(2, 1));

		this.boundingBox = boundingBox;
		this.scaleBar = scaleBar;

		widthTF = new NumberField(4);
		widthTF.setFocusable(true);
		widthTF.setText(Integer.toString(w));
		widthTF.setIntegersOnly(true);
		widthTF.setLimits(0, 5000);

		heightTF = new NumberField(4);
		heightTF.setText(Integer.toString(h));
		heightTF.setIntegersOnly(true);
		heightTF.setLimits(0, 5000);

		JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));

		sizePanel.add(widthTF);
		sizePanel.add(new JLabel("   x "));
		sizePanel.add(heightTF);

		add(sizePanel);

		widthTF.addListener((v) -> fireOutputWidthChanged((int)v));
		heightTF.addListener((v) -> fireOutputHeightChanged((int)v));
		widthTF.addNumberFieldFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				JTextField tf = (JTextField)e.getSource();
				tf.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				fireOutputWidthChanged((int)Double.parseDouble(widthTF.getText()));
			}
		});
		heightTF.addNumberFieldFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				JTextField tf = (JTextField)e.getSource();
				tf.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				fireOutputHeightChanged((int)Double.parseDouble(heightTF.getText()));
			}
		});

		JPanel propertiesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		bbBox = new JCheckBox("Bounding Box", boundingBox.isVisible());
		final JButton bbProperties = new JButton("Properties");

		c.gridx = c.gridy = 0;
		c.insets = new Insets(0, 10, 0, 0);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		propertiesPanel.add(bbBox, c);
		c.gridx++;
		c.weightx = 1;
		propertiesPanel.add(bbProperties, c);

		sbBox = new JCheckBox("Scalebar", scaleBar.isVisible());
		final JButton sbProperties = new JButton("Properties");

		c.gridx = 0; c.gridy = 1;
		c.weightx = 0;
		propertiesPanel.add(sbBox, c);
		c.gridx++;
		c.weightx = 1;
		propertiesPanel.add(sbProperties, c);

		add(propertiesPanel);

		bbBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boundingBox.setVisible(bbBox.isSelected());
				fireBoundingBoxChanged();
			}
		});

		bbProperties.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color color = boundingBox.getColor();
				float width = boundingBox.getWidth();

				final GenericDialog gd = new GenericDialog("");
				gd.addNumericField("line width", width, 2);
				final TextField lwTF = (TextField)gd.getNumericFields().lastElement();
				gd.addSlider("red", 0, 255, color.getRed());
				final Scrollbar redSlider = (Scrollbar)gd.getSliders().lastElement();
				gd.addSlider("green", 0, 255, color.getGreen());
				final Scrollbar greenSlider = (Scrollbar)gd.getSliders().lastElement();
				gd.addSlider("blue", 0, 255, color.getBlue());
				final Scrollbar blueSlider = (Scrollbar)gd.getSliders().lastElement();

				lwTF.addTextListener(new TextListener() {
					@Override
					public void textValueChanged(TextEvent e) {
						String s = lwTF.getText();
						try {
							float f = (float)Double.parseDouble(s);
							boundingBox.setWidth(f);
							fireBoundingBoxChanged();
						} catch(Exception ex) {}
					}
				});
				AdjustmentListener li = new AdjustmentListener() {
					@Override
					public void adjustmentValueChanged(AdjustmentEvent e) {
						int r = redSlider.getValue();
						int g = greenSlider.getValue();
						int b = blueSlider.getValue();
						Color c = new Color(r, g, b);
						boundingBox.setColor(c);
						fireBoundingBoxChanged();
					}
				};
				redSlider.addAdjustmentListener(li);
				greenSlider.addAdjustmentListener(li);
				blueSlider.addAdjustmentListener(li);
				gd.showDialog();
				if(gd.wasCanceled()) {
					boundingBox.setColor(color);
					boundingBox.setWidth(width);
				}
			}
		});

		sbBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				scaleBar.setVisible(sbBox.isSelected());
				fireScalebarChanged();
			}
		});

		sbProperties.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color color = scaleBar.getColor();
				float width = scaleBar.getWidth();
				Position position = scaleBar.getPosition();
				float offset = scaleBar.getOffset();
				float length = scaleBar.getLength();

				String[] positions = Scalebar.Position.getNames();

				final GenericDialog gd = new GenericDialog("");
				gd.addChoice("Position", positions, position.toString());
				final Choice pChoice = (Choice)gd.getChoices().lastElement();
				gd.addNumericField("length", length, 0);
				final TextField lTF = (TextField)gd.getNumericFields().lastElement();
				gd.addNumericField("line width", width, 2);
				final TextField lwTF = (TextField)gd.getNumericFields().lastElement();
				gd.addNumericField("offset", offset, 0);
				final TextField oTF = (TextField)gd.getNumericFields().lastElement();
				gd.addSlider("red", 0, 255, color.getRed());
				final Scrollbar redSlider = (Scrollbar)gd.getSliders().lastElement();
				gd.addSlider("green", 0, 255, color.getGreen());
				final Scrollbar greenSlider = (Scrollbar)gd.getSliders().lastElement();
				gd.addSlider("blue", 0, 255, color.getBlue());
				final Scrollbar blueSlider = (Scrollbar)gd.getSliders().lastElement();

				pChoice.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						scaleBar.setPosition(Scalebar.Position.values()[pChoice.getSelectedIndex()]);
						fireScalebarChanged();
					}
				});
				lTF.addTextListener(new TextListener() {
					@Override
					public void textValueChanged(TextEvent e) {
						String s = lTF.getText();
						try {
							float f = (float)Double.parseDouble(s);
							scaleBar.setLength(f);
							fireScalebarChanged();
						} catch(Exception ex) {}
					}
				});
				oTF.addTextListener(new TextListener() {
					@Override
					public void textValueChanged(TextEvent e) {
						String s = oTF.getText();
						try {
							float f = (float)Double.parseDouble(s);
							scaleBar.setOffset(f);
							fireScalebarChanged();
						} catch(Exception ex) {}
					}
				});
				lwTF.addTextListener(new TextListener() {
					@Override
					public void textValueChanged(TextEvent e) {
						String s = lwTF.getText();
						try {
							float f = (float)Double.parseDouble(s);
							scaleBar.setWidth(f);
							fireScalebarChanged();
						} catch(Exception ex) {}
					}
				});
				AdjustmentListener li = new AdjustmentListener() {
					@Override
					public void adjustmentValueChanged(AdjustmentEvent e) {
						int r = redSlider.getValue();
						int g = greenSlider.getValue();
						int b = blueSlider.getValue();
						Color c = new Color(r, g, b);
						scaleBar.setColor(c);
						fireScalebarChanged();
					}
				};
				redSlider.addAdjustmentListener(li);
				greenSlider.addAdjustmentListener(li);
				blueSlider.addAdjustmentListener(li);
				gd.showDialog();
				if(gd.wasCanceled()) {
					scaleBar.setColor(color);
					scaleBar.setWidth(width);
					scaleBar.setPosition(position);
					scaleBar.setLength(length);
					scaleBar.setOffset(offset);
				}
			}
		});
	}

	public void setOutputSize(int width, int height) {
		widthTF.setText(Integer.toString(width));
		heightTF.setText(Integer.toString(height));
	}

	public void addOutputPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireOutputWidthChanged(int w) {
		for(Listener l : listeners)
			l.outputWidthChanged(w);
	}

	private void fireOutputHeightChanged(int h) {
		for(Listener l : listeners)
			l.outputHeightChanged(h);
	}

	private void fireBoundingBoxChanged() {
		for(Listener l : listeners)
			l.boundingBoxChanged();
	}

	private void fireScalebarChanged() {
		for(Listener l : listeners)
			l.scalebarChanged();
	}
}
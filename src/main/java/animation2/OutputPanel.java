package animation2;

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

import fiji.util.gui.GenericDialogPlus;
import renderer3d.BoundingBox;

public class OutputPanel extends JPanel implements FocusListener, NumberField.Listener {

	public static void main(String...strings) {
		JFrame f = new JFrame("");
		f.getContentPane().add(new OutputPanel(200, 200, 1, new BoundingBox(100, 100, 100, 1, 1, 1)));
		f.pack();
		f.setVisible(true);
	}

	private static final long serialVersionUID = 1L;


	private NumberField widthTF, heightTF, zStepTF;

	public static interface Listener {
		public void outputSizeChanged(int w, int h, float zStep);
		public void boundingBoxChanged();
	}

	private final ArrayList<Listener> listeners =	new ArrayList<Listener>();

	private final BoundingBox boundingBox;

	public OutputPanel(int w, int h, float zStep, final BoundingBox boundingBox) {
		super(new GridLayout(2, 1));

		this.boundingBox = boundingBox;

		widthTF = new NumberField(4);
		widthTF.setFocusable(true);
		widthTF.setText(Integer.toString(w));
		widthTF.setIntegersOnly(true);
		widthTF.setLimits(0, 5000);

		heightTF = new NumberField(4);
		heightTF.setText(Integer.toString(h));
		heightTF.setIntegersOnly(true);
		heightTF.setLimits(0, 5000);

		zStepTF = new NumberField(4);
		zStepTF.setText(Float.toString(zStep));
		zStepTF.setLimits(1, 5);

		JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));

		sizePanel.add(widthTF);
		sizePanel.add(new JLabel("   x "));
		sizePanel.add(heightTF);
		sizePanel.add(new JLabel("     zStep:"));
		sizePanel.add(zStepTF);

		add(sizePanel);

		widthTF.addListener(this);
		widthTF.addNumberFieldFocusListener(this);
		heightTF.addListener(this);
		heightTF.addNumberFieldFocusListener(this);
		zStepTF.addListener(this);
		zStepTF.addNumberFieldFocusListener(this);

		JPanel propertiesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		final JCheckBox bbBox = new JCheckBox("Bounding Box", boundingBox.isVisible());
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

				final GenericDialogPlus gd = new GenericDialogPlus("");
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
	}

	@Override
	public void focusGained(FocusEvent e) {
		JTextField tf = (JTextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {
		fireOutputSizeChanged();
	}

	@Override
	public void valueChanged(double v) {
		fireOutputSizeChanged();
	}

	public int getOutputWidth() {
		return (int)Double.parseDouble(widthTF.getText());
	}

	public int getOutputHeight() {
		return (int)Double.parseDouble(heightTF.getText());
	}

	public float getZStep() {
		return (float)Double.parseDouble(zStepTF.getText());
	}

	public void setOutputSize(int width, int height, float zStep) {
		widthTF.setText(Integer.toString(width));
		heightTF.setText(Integer.toString(height));
		zStepTF.setText(Float.toString(zStep));
	}

	public void addOutputPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireOutputSizeChanged() {
		for(Listener l : listeners)
			l.outputSizeChanged(getOutputWidth(), getOutputHeight(), getZStep());
	}

	private void fireBoundingBoxChanged() {
		for(Listener l : listeners)
			l.boundingBoxChanged();
	}
}
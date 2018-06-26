package animation3d.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import animation3d.textanim.CustomDecimalFormat;

public class TransformationPanel extends JPanel implements FocusListener, NumberField.Listener {

	public static void main(String[] args) {
		JFrame f = new JFrame();
		TransformationPanel tp = new TransformationPanel(0, 0, 0, 0, 0, 0, 0);
		f.getContentPane().add(tp);
		f.pack();
		f.setVisible(true);
	}

	private static final long serialVersionUID = 1L;

	private NumberField angleX, angleY, angleZ;
	private NumberField dX, dY, dZ;
	private NumberField scale;

	public static interface Listener {
		public void transformationChanged(float ax, float ay, float az, float dx, float dy, float dz, float s);
		public void resetTransformation();
	}

	private ArrayList<Listener> listeners =	new ArrayList<Listener>();

	public TransformationPanel(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		super();
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		angleX = makeNumberfield();
		angleY = makeNumberfield();
		angleZ = makeNumberfield();
		dX = makeNumberfield();
		dY = makeNumberfield();
		dZ = makeNumberfield();
		scale = makeNumberfield();

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 2, 2, 2);

		c.gridx = c.gridy = 0;
		c.gridwidth = 2;
		add(new JLabel("Rotation"), c);
		c.gridwidth = 1;
		c.gridy++;
		add(new JLabel("X"), c);
		c.gridy++;
		add(new JLabel("Y"), c);
		c.gridy++;
		add(new JLabel("Z"), c);

		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 2;
		add(new JLabel("Translation"), c);
		c.gridwidth = 1;
		c.gridy++;
		add(new JLabel("X"), c);
		c.gridy++;
		add(new JLabel("Y"), c);
		c.gridy++;
		add(new JLabel("Z"), c);

		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 2;
		add(new JLabel("Scale"), c);
		c.gridwidth = 1;

		c.insets = new Insets(2, 0, 2, 30);

		c.gridx = 1;
		c.gridy = 1;
		add(angleX, c);
		c.gridy++;
		add(angleY, c);
		c.gridy++;
		add(angleZ, c);

		c.gridx = 3;
		c.gridy = 1;
		add(dX, c);
		c.gridy++;
		add(dY, c);
		c.gridy++;
		add(dZ, c);

		c.gridx = 5;
		c.gridy = 1;
		add(scale, c);

		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 4;
		c.gridy = 2;
		c.gridwidth = 2;
		JButton reset = new JButton("Reset");
		reset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireResetTransformation();
			}
		});
		add(reset, c);

		setTransformation(ax, ay, az, dx, dy, dz, s);
	}

	private NumberField makeNumberfield() {
		NumberField nf = new NumberField(4);
		nf.addListener(this);
		nf.addNumberFieldFocusListener(this);
		return nf;
	}

	@Override
	public void focusGained(FocusEvent e) {
		JTextField tf = (JTextField)e.getSource();
		tf.selectAll();
	}

	@Override
	public void focusLost(FocusEvent e) {
		fireTransformationChanged();
	}

	@Override
	public void valueChanged(double v) {
		fireTransformationChanged();
	}

	public float getAngleX() {
		return (int)Double.parseDouble(angleX.getText());
	}

	public float getAngleY() {
		return (float)Double.parseDouble(angleY.getText());
	}

	public float getAngleZ() {
		return (float)Double.parseDouble(angleZ.getText());
	}

	public float getTranslationX() {
		return (float)Double.parseDouble(dX.getText());
	}

	public float getTranslationY() {
		return (float)Double.parseDouble(dY.getText());
	}

	public float getTranslationZ() {
		return (float)Double.parseDouble(dZ.getText());
	}

	public float getScale() {
		return (float)Double.parseDouble(scale.getText());
	}

	public void setTransformation(float[] rotation, float[] translation, float scale) {
		setTransformation(
				rotation[0], rotation[1], rotation[2],
				translation[0], translation[1], translation[2],
				scale);
	}

	public void setTransformation(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		angleX.setText(CustomDecimalFormat.format(ax,  1));
		angleY.setText(CustomDecimalFormat.format(ay,  1));
		angleZ.setText(CustomDecimalFormat.format(az,  1));
		dX.setText(CustomDecimalFormat.format(dx,  1));
		dY.setText(CustomDecimalFormat.format(dy,  1));
		dZ.setText(CustomDecimalFormat.format(dz,  1));
		scale.setText(CustomDecimalFormat.format(s, 1));
	}

	public void addTransformationPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireTransformationChanged() {
		for(Listener l : listeners)
			l.transformationChanged(
					getAngleX(),
					getAngleY(),
					getAngleZ(),
					getTranslationX(),
					getTranslationY(),
					getTranslationZ(),
					getScale());
	}

	private void fireResetTransformation() {
		for(Listener l : listeners)
			l.resetTransformation();
	}
}
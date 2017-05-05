package animation2;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

public class TransformationPanel extends Panel implements FocusListener, NumberField.Listener {

	public static void main(String[] args) {
		Frame f = new Frame();
		TransformationPanel tp = new TransformationPanel(0, 0, 0, 0, 0, 0, 0);
		f.add(tp);
		f.pack();
		f.show();
	}

	private static final long serialVersionUID = 1L;

	private NumberField angleX, angleZ, angleY;
	private NumberField dX, dY, dZ;
	private NumberField scale;

	public static interface Listener {
		public void transformationChanged(float ax, float ay, float az, float dx, float dy, float dz, float s);
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
		add(new Label("Angle X"), c);
		c.gridy++;
		add(new Label("Angle Y"), c);
		c.gridy++;
		add(new Label("Angle Z"), c);

		c.gridx = 2;
		c.gridy = 0;
		add(new Label("Translation X"), c);
		c.gridy++;
		add(new Label("Translation Y"), c);
		c.gridy++;
		add(new Label("Translation Z"), c);

		c.gridx = 4;
		c.gridy = 0;
		add(new Label("Scale"), c);

		c.insets = new Insets(2, 0, 2, 30);

		c.gridx = 1;
		c.gridy = 0;
		add(angleX, c);
		c.gridy++;
		add(angleY, c);
		c.gridy++;
		add(angleZ, c);

		c.gridx = 3;
		c.gridy = 0;
		add(dX, c);
		c.gridy++;
		add(dY, c);
		c.gridy++;
		add(dZ, c);

		c.gridx = 5;
		c.gridy = 0;
		add(scale, c);

		setTransformation(ax, ay, az, dx, dy, dz, s);
	}

	private NumberField makeNumberfield() {
		NumberField nf = new NumberField(4, true);
		nf.addListener(this);
		nf.addFocusListener(this);
		return nf;
	}

	@Override
	public void focusGained(FocusEvent e) {
		TextField tf = (TextField)e.getSource();
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

	@Override
	public void record(NumberField src) {

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
		angleX.setText(Double.toString(ax));
		angleY.setText(Double.toString(ay));
		angleZ.setText(Double.toString(az));
		dX.setText(Double.toString(dx));
		dY.setText(Double.toString(dy));
		dZ.setText(Double.toString(dz));
		scale.setText(Double.toString(s));
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
}
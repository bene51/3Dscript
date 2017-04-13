package animation2;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ArrayList;

public class OutputPanel extends Panel {

	private static final long serialVersionUID = 1L;


	private NumberField widthTF, heightTF;

	public static interface Listener {
		public void outputSizeChanged(int w, int h);
	}

	private ArrayList<Listener> listeners =	new ArrayList<Listener>();

	public OutputPanel(int w, int h) {
		super();
		setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));

		widthTF = new NumberField(4);
		widthTF.setText(Integer.toString(w));
		widthTF.setIntegersOnly(true);
		widthTF.setLimits(0, 5000);

		heightTF = new NumberField(4);
		heightTF.setText(Integer.toString(h));
		heightTF.setIntegersOnly(true);
		heightTF.setLimits(0, 5000);

		add(widthTF);
		add(new Label("   x "));
		add(heightTF);

		widthTF.addListener(new NumberField.Listener() {
			@Override
			public void valueChanged(double v) {
				fireOutputSizeChanged();
			}
		});

		heightTF.addListener(new NumberField.Listener() {
			@Override
			public void valueChanged(double v) {
				fireOutputSizeChanged();
			}
		});
	}

	public int getOutputWidth() {
		return (int)Double.parseDouble(widthTF.getText());
	}

	public int getOutputHeight() {
		return (int)Double.parseDouble(heightTF.getText());
	}

	public void setOutputSize(int width, int height) {
		widthTF.setText(Integer.toString(width));
		heightTF.setText(Integer.toString(height));
	}

	public void addOutputPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireOutputSizeChanged() {
		for(Listener l : listeners)
			l.outputSizeChanged(getOutputWidth(), getOutputHeight());
	}
}
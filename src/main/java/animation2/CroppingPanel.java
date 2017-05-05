package animation2;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;

public class CroppingPanel extends Panel {

	public static void main(String[] args) {
		ImagePlus imp = IJ.openImage("D:\\flybrain.tif");
		Frame frame = new Frame();

		CroppingPanel slider = new CroppingPanel(imp);
		frame.add(slider);
		frame.pack();
		frame.setVisible(true);
	}

	private static final long serialVersionUID = 1L;


	private DoubleSlider nearfar;
	private DoubleSlider bbX;
	private DoubleSlider bbY;
	private DoubleSlider bbZ;

	public static interface Listener {
		public void nearFarChanged(int near, int far);
		public void boundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1);
		public void record(NumberField src, String timelineName);
	}

	private ArrayList<Listener> listeners =	new ArrayList<Listener>();

	private DoubleSlider addDoubleSlider(String label, int[] realMinMax, int[] setMinMax, Color color, GridBagConstraints c) {
		DoubleSlider slider = new DoubleSlider(realMinMax, setMinMax, color);

		GridBagLayout layout = (GridBagLayout)getLayout();

		if(label != null) {
			Label theLabel = new Label(label);
			c.gridx = 0;
			c.anchor = GridBagConstraints.EAST;
			c.gridwidth = 1;
			layout.setConstraints(theLabel, c);
			add(theLabel);
			c.gridx++;
		}
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		layout.setConstraints(slider, c);
		add(slider);
		return slider;
	}

	public CroppingPanel(ImagePlus image) {
		super();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		int d = image.getNSlices();
		nearfar = addDoubleSlider(
				"near/far",
				new int[] {-5 * d, 5 * d},
				new int[] {-5 * d, 5 * d},
				new Color(255, 0, 0, 100),
				c);

		bbX = addDoubleSlider(
				"x_range",
				new int[] {0, image.getWidth()},
				new int[] {0, image.getWidth()},
				new Color(255, 0, 0, 100),
				c);
		bbY = addDoubleSlider(
				"y_range",
				new int[] {0, image.getHeight()},
				new int[] {0, image.getHeight()},
				new Color(255, 0, 0, 100),
				c);
		bbZ = addDoubleSlider(
				"z_range",
				new int[] {0, image.getNSlices()},
				new int[] {0, image.getNSlices()},
				new Color(255, 0, 0, 100),
				c);

		nearfar.addSliderChangeListener(new DoubleSlider.Listener() {
			@Override
			public void sliderChanged() {
				fireNearFarChanged(nearfar.getMin(), nearfar.getMax());
			}
		});

		DoubleSlider.Listener bbListener = new DoubleSlider.Listener() {
			@Override
			public void sliderChanged() {
				fireBoundingBoxChanged(
						bbX.getMin(),
						bbY.getMin(),
						bbZ.getMin(),
						bbX.getMax(),
						bbY.getMax(),
						bbZ.getMax());
			}
		};
		bbX.addSliderChangeListener(bbListener);
		bbY.addSliderChangeListener(bbListener);
		bbZ.addSliderChangeListener(bbListener);

		addNumberFieldListener(nearfar.getMinField(), "Near");
		addNumberFieldListener(nearfar.getMaxField(), "Far");
		addNumberFieldListener(bbX.getMinField(), "Bounding Box X Min");
		addNumberFieldListener(bbX.getMaxField(), "Bounding Box X Max");
		addNumberFieldListener(bbY.getMinField(), "Bounding Box Y Min");
		addNumberFieldListener(bbY.getMaxField(), "Bounding Box Y Max");
		addNumberFieldListener(bbZ.getMinField(), "Bounding Box Z Min");
		addNumberFieldListener(bbZ.getMaxField(), "Bounding Box Z Max");
	}

	private void addNumberFieldListener(NumberField nf, final String timelineName) {
		nf.addListener(new NumberField.Listener() {
			@Override public void valueChanged(double v) {}

			@Override
			public void record(NumberField src) {
				fireRecord(src, timelineName);
			}
		});
	}

	public int getBBXMin() {
		return bbX.getMin();
	}

	public int getBBYMin() {
		return bbY.getMin();
	}

	public int getBBZMin() {
		return bbZ.getMin();
	}

	public int getBBXMax() {
		return bbX.getMax();
	}

	public int getBBYMax() {
		return bbY.getMax();
	}

	public int getBBZMax() {
		return bbZ.getMax();
	}

	public int getNear() {
		return nearfar.getMin();
	}

	public int getFar() {
		return nearfar.getMax();
	}

	public void setBoundingBox(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		bbX.setMinAndMax(bbx0, bbx1);
		bbY.setMinAndMax(bby0, bby1);
		bbZ.setMinAndMax(bbz0, bbz1);
	}

	public void setNearAndFar(int near, int far) {
		nearfar.setMinAndMax(near, far);
	}

	public void addCroppingPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireBoundingBoxChanged(int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		for(Listener l : listeners)
			l.boundingBoxChanged(bbx0, bby0, bbz0, bbx1, bby1, bbz1);
	}

	private void fireNearFarChanged(int near, int far) {
		for(Listener l : listeners)
			l.nearFarChanged(near, far);
	}

	private void fireRecord(NumberField src, String timelineName) {
		for(Listener l : listeners)
			l.record(src, timelineName);
	}
}
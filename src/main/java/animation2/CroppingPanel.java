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
		public void boundingBoxChanged(int bbx, int bby, int bbz, int bbw, int bbh, int bbd);
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
				new int[] {Math.round(-5 * d), Math.round(5 * d)},
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
						bbX.getMax() - bbX.getMin(),
						bbY.getMax() - bbY.getMin(),
						bbZ.getMax() - bbZ.getMin());
			}
		};
	}

	public int getBBX() {
		return bbX.getMin();
	}

	public int getBBY() {
		return bbY.getMin();
	}

	public int getBBZ() {
		return bbZ.getMin();
	}

	public int getBBW() {
		return bbX.getMax() - bbX.getMin();
	}

	public int getBBH() {
		return bbY.getMax() - bbY.getMin();
	}

	public int getBBD() {
		return bbZ.getMax() - bbZ.getMin();
	}

	public int getNear() {
		return nearfar.getMin();
	}

	public int getFar() {
		return nearfar.getMax();
	}

	public void setBoundingBox(int bbx, int bby, int bbz, int bbw, int bbh, int bbd) {
		bbX.setMinAndMax(bbx, bbx + bbw);
		bbY.setMinAndMax(bby, bby + bbh);
		bbZ.setMinAndMax(bbz, bbz + bbd);
	}

	public void setNearAndFar(int near, int far) {
		nearfar.setMinAndMax(near, far);
	}

	public void addCroppingPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireBoundingBoxChanged(int bbx, int bby, int bbz, int bbw, int bbh, int bbd) {
		for(Listener l : listeners)
			l.boundingBoxChanged(bbx, bby, bbz, bbw, bbh, bbd);
	}

	private void fireNearFarChanged(int near, int far) {
		for(Listener l : listeners)
			l.nearFarChanged(near, far);
	}
}
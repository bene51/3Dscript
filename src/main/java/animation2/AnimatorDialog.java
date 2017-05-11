package animation2;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.ImagePlus;
import ij.gui.GenericDialog;


public class AnimatorDialog extends GenericDialog {

	private static final long serialVersionUID = 1L;

	private ActionListener listener;
	private Button okButton;

	public static interface Listener {
		public void renderingSettingsChanged();
		public void nearfarChanged();
		public void boundingBoxChanged();
	}

	public AnimatorDialog(String title) {
		super(title);
	}

	public AnimatorDialog(String title, Frame parent) {
		super(title, parent);
	}

	public void setActionListener(ActionListener l) {
		this.listener = l;
	}

	@Override
	public void showDialog() {
		super.showDialog();
		Button[] buttons = getButtons();
		okButton = buttons[0];
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == okButton && listener != null)
			listener.actionPerformed(e);
		else
			super.actionPerformed(e);
	}

	public void addChoice(String label, String[] choice) {
		super.addChoice(label, choice, choice[0]);
	}

	public ContrastPanel addContrastPanel(int[] histo8, Color color, double min, double max, RenderingSettings r, int nChannels) {
		ContrastPanel slider = new ContrastPanel(histo8, color, min, max, r, nChannels);

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = getConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		SwitchablePanel sp = new SwitchablePanel("Contrast", slider);
		layout.setConstraints(sp, c);
		add(sp);
		return slider;
	}

	public CroppingPanel addCroppingPanel(ImagePlus imp) {
		CroppingPanel slider = new CroppingPanel(imp);

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = getConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		SwitchablePanel sp = new SwitchablePanel("Cropping", slider);
		sp.switchOff();
		layout.setConstraints(sp, c);
		add(sp);
		return slider;
	}

	public OutputPanel addOutputPanel(int width, int height, float zStep) {
		OutputPanel outputPanel = new OutputPanel(width, height, zStep);

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = getConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		SwitchablePanel sp = new SwitchablePanel("Output", outputPanel);
		sp.switchOff();
		layout.setConstraints(sp, c);
		add(sp);
		return outputPanel;
	}

	public TransformationPanel addTransformationPanel(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		TransformationPanel panel = new TransformationPanel(ax, ay, az, dx, dy, dz, s);

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = getConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		SwitchablePanel sp = new SwitchablePanel("Transformation", panel);
		sp.switchOff();
		layout.setConstraints(sp, c);
		add(sp);
		return panel;
	}

	public DoubleSlider addDoubleSlider(String label, int[] realMinMax, int[] setMinMax, Color color) {
		DoubleSlider slider = new DoubleSlider(realMinMax, setMinMax, color);

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = getConstraints();

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

	public AnimationPanel addAnimationPanel(String[] timelineNames, Timelines ctrls, int timeline, int frame) {
		final AnimationPanel panel = new AnimationPanel(timelineNames, ctrls, timeline, frame);

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = getConstraints();

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		SwitchablePanel sp = new SwitchablePanel("Animation", panel);
		sp.switchOff();
		layout.setConstraints(sp, c);
		add(sp);
		return panel;
	}

	protected GridBagConstraints getConstraints() {
		GridBagLayout layout = (GridBagLayout)getLayout();
		Panel panel = new Panel();
		addPanel(panel);
		GridBagConstraints constraints = layout.getConstraints(panel);
		remove(panel);
		return constraints;
	}

	public static void main(String[] args) {
//		int[] histo = new int[256];
//		for(int i = 0; i < 256; i++)
//			histo[i] = i;
//		AnimatorDialog gd = new AnimatorDialog("GenericDialogOpener Test");
//		gd.addHistogramSlider("range", histo, Color.GREEN, 1, 4, 1, 4, 1);
//		gd.addNumericField("lkjl", 0, 3);
//		gd.showDialog();
	}
}
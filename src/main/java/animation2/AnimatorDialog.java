package animation2;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;


public class AnimatorDialog extends GenericDialog {

	private static final long serialVersionUID = 1L;

	private ActionListener listener;
	private Button okButton;
	private Panel contents;
	private GridBagConstraints c;

	public static interface Listener {
		public void renderingSettingsChanged();
		public void nearfarChanged();
		public void boundingBoxChanged();
	}

	private void initContents() {
		contents = new Panel(new GridBagLayout());
		c = new GridBagConstraints();
	}

	public AnimatorDialog(String title) {
		super(title);
		initContents();
	}

	public AnimatorDialog(String title, Frame parent) {
		super(title, parent);
		initContents();
	}

	public void setActionListener(ActionListener l) {
		this.listener = l;
	}

	@Override
	public void showDialog() {
		Panel dummy = new Panel();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridy++;
        contents.add(dummy, c);

        ScrollPane scroll = new ScrollPane();
        scroll.add(contents);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;

        add(scroll, c);
        // workaround:
        // need to change the y position of the next element, but
        // GenericDialog.y is not accessible
        Panel tmp = new Panel();
        addPanel(tmp);
        remove(tmp);

        super.showDialog();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
		int avHeight = screenSize.height - scnMax.top - scnMax.bottom;

		setSize(400, avHeight);
		setLocation(screenSize.width - scnMax.right - 400, scnMax.top);

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

	public ContrastPanel addContrastPanel(int[] histo8, Color color, double min, double max, RenderingSettings r, int nChannels) {
		ContrastPanel slider = new ContrastPanel(histo8, color, min, max, r, nChannels);

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		c.insets = new Insets(0, 0, 5, 0);
		SwitchablePanel sp = new SwitchablePanel("Contrast", slider);
		contents.add(sp, c);
		return slider;
	}

	public CroppingPanel addCroppingPanel(ImagePlus imp) {
		CroppingPanel slider = new CroppingPanel(imp);

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		SwitchablePanel sp = new SwitchablePanel("Cropping", slider);
		sp.switchOff();
		contents.add(sp, c);
		return slider;
	}

	public OutputPanel addOutputPanel(int width, int height, float zStep) {
		OutputPanel outputPanel = new OutputPanel(width, height, zStep);

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		SwitchablePanel sp = new SwitchablePanel("Output", outputPanel);
		sp.switchOff();
		contents.add(sp, c);
		return outputPanel;
	}

	public TransformationPanel addTransformationPanel(float ax, float ay, float az, float dx, float dy, float dz, float s) {
		TransformationPanel panel = new TransformationPanel(ax, ay, az, dx, dy, dz, s);

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		SwitchablePanel sp = new SwitchablePanel("Transformation", panel);
		sp.switchOff();
		contents.add(sp, c);
		return panel;
	}

	public DoubleSlider addDoubleSlider(String label, int[] realMinMax, int[] setMinMax, Color color) {
		DoubleSlider slider = new DoubleSlider(realMinMax, setMinMax, color);

		if(label != null) {
			Label theLabel = new Label(label);
			c.gridx = 0;
			c.anchor = GridBagConstraints.EAST;
			c.gridwidth = 1;
			contents.add(theLabel, c);
			c.gridx++;
		}
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		contents.add(slider, c);
		return slider;
	}

	public AnimationPanel addAnimationPanel(String[] timelineNames, Timelines ctrls, int timeline, int frame) {
		final AnimationPanel panel = new AnimationPanel(timelineNames, ctrls, timeline, frame);

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		SwitchablePanel sp = new SwitchablePanel("Animation", panel);
		sp.switchOff();
		contents.add(sp, c);
		return panel;
	}

	public static void main(String[] args) {
		int[] histo = new int[256];
		for (int i = 0; i < 256; i++)
			histo[i] = i;

		RenderingSettings rs = new RenderingSettings(0, 255, 2, 0, 255, 1);

		final AnimatorDialog gd = new AnimatorDialog("Interactive Raycaster", null);
		gd.addContrastPanel(histo, Color.RED, 0, 255, rs, 1);

		gd.addTransformationPanel(0, 0, 0, 0, 0, 0, 1);

		ImagePlus image = IJ.createImage("", 256, 256, 57, 8);
		gd.addCroppingPanel(image);

		gd.addOutputPanel(255, 255, 1);

		final Timelines timelines = new Timelines(1);
		final String[] timelineNames = new String[timelines.size()];
		for (int i = 0; i < timelineNames.length; i++)
			timelineNames[i] = Timelines.getName(i);
		gd.addAnimationPanel(timelineNames, timelines, 0, 0);

		gd.setModal(false);
		gd.showDialog();
    }
}

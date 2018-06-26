package animation3d.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import animation3d.renderer3d.BoundingBox;
import animation3d.renderer3d.Renderer3D;
import animation3d.renderer3d.Scalebar;
import ij.ImagePlus;


public class AnimatorDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel contents;
	private GridBagConstraints c;

	private void initContents() {
		contents = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
	}

	public AnimatorDialog(String title, Window parent) {
		super(parent, title);
		initContents();
	}

	public void showDialog() {
		JPanel dummy = new JPanel();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridy++;
        contents.add(dummy, c);

        JScrollPane scroll = new JScrollPane(contents);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;

        getContentPane().setLayout(new GridBagLayout());
        getContentPane().add(scroll, c);

        setVisible(true);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
		int avHeight = screenSize.height - scnMax.top - scnMax.bottom;

		setSize(400, avHeight);
		setLocation(screenSize.width - scnMax.right - 400, scnMax.top);
	}

	public ContrastPanel addContrastPanel(int[][] histo8, double min[], double max[], double[][] channelProperties, Color bg) {
		ContrastPanel slider = new ContrastPanel(histo8, min, max, channelProperties, bg);

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

	public BookmarkPanel addBookmarkPanel(Renderer3D renderer) {
		BookmarkPanel bookmarks = new BookmarkPanel(renderer);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		SwitchablePanel sp = new SwitchablePanel("Bookmarks", bookmarks);
		sp.switchOff();
		contents.add(sp, c);
		return bookmarks;
	}

	public OutputPanel addOutputPanel(int width, int height, float zStep, BoundingBox bb, Scalebar sb) {
		OutputPanel outputPanel = new OutputPanel(width, height, zStep, bb, sb);

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
			JLabel theLabel = new JLabel(label);
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

	public AnimationPanel addAnimationPanel() {
		final AnimationPanel panel = new AnimationPanel();

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.gridy++;
		SwitchablePanel sp = new SwitchablePanel("Animation", panel);
		sp.switchOff();
		contents.add(sp, c);
		return panel;
	}
}

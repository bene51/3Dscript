package animation3d.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;

public class AnimationPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public interface Listener {
		public void textBasedAnimation();
	}

	public AnimationPanel() {
		super();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		JButton textbased = new JButton("Start text-based animation editor");
		textbased.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireTextBasedAnimation();
			}
		});
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 2, 10, 5);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy++;
		add(textbased, c);
	}

	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	public void addTimelineListener(Listener l) {
		listeners.add(l);
	}

	public void removeTimelineListener(Listener l) {
		listeners.remove(l);
	}

	private void fireTextBasedAnimation() {
		for(Listener l : listeners)
			l.textBasedAnimation();
	}
}
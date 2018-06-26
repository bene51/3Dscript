package animation3d.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SwitchablePanel extends JPanel {

	private static final long serialVersionUID = -6728763866288689347L;

	private JLabel label;
	private JLabel label2;
	private JPanel labels;
	private JPanel panel;

	public SwitchablePanel(final String title, final JPanel panel) {
		this.panel = panel;
		labels = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		this.label = new JLabel(title);
		label.setBackground(new Color(100, 140, 200));
		label.setForeground(Color.WHITE);
		label.setFont(new Font("Helvetica", Font.BOLD, 14));
		labels.add(label);

		label2 = new JLabel("hide");


		Font font = new Font("Helvetica", Font.ITALIC, 12);
		Map<TextAttribute, Object>  attributes = new HashMap<TextAttribute, Object>();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

		label2.setFont(font.deriveFont(attributes));
		label2.setBackground(new Color(100, 140, 200));
		label2.setForeground(Color.WHITE);

		labels.add(label2);

		labels.setBackground(new Color(100, 140, 200));


		setLayout(new BorderLayout());
		add(labels, BorderLayout.NORTH);
		add(panel, BorderLayout.CENTER);

		this.label2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println(getParent());
				boolean visible = !panel.isVisible();
				panel.setVisible(visible);
				String sign = visible ? "hide" : "show";
				label2.setText(sign);
				revalidate();
			}
		});
	}

	public void switchOn() {
		panel.setVisible(true);
		label2.setText("hide");
		revalidate();
	}

	public void switchOff() {
		panel.setVisible(false);
		label2.setText("show");
		revalidate();
	}

	public static void main(String[] args) {
		JFrame f = new JFrame();
		f.setLayout(new GridLayout(2, 1));
		JPanel panel = new JPanel();
		panel.add(new JLabel("lkj lkj"));
		f.add(new SwitchablePanel("bla", panel));
		panel = new JPanel();
		panel.add(new JLabel("lkj lkj"));
		f.getContentPane().add(new SwitchablePanel("bla", panel));
		f.pack();
		f.setVisible(true);
	}
}

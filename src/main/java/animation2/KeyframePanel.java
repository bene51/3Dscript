package animation2;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class KeyframePanel extends Panel {

	private Keyframe kf;

	private GridBagLayout layout;
	private GridBagConstraints c;

	private static class Item {
		TextField tf;
		Choice cb;
		Field f;
		Object o;

		public Item(TextField tf, Choice cb, Field f, Object o) {
			super();
			this.tf = tf;
			this.cb = cb;
			this.f = f;
			this.o = o;
		}

		public void apply() {
			try {
				if(cb.getSelectedItem().equals("keep previous"))
					return;
				double v = Keyframe.UNSET;
				if(cb.getSelectedItem().equals("replace")) {
					v = Double.parseDouble(tf.getText()); // TODO check parsing exception
//					if(f.getName().startsWith("angle"))
//						v = v * Math.PI / 180.0;
				}

				if(f.getType() == Integer.TYPE)
					f.setInt(o, (int)Math.round(v));
				else if(f.getType() == Double.TYPE)
					f.setDouble(o, v);
				else if(f.getType() == Float.TYPE)
					f.setFloat(o, (float)v);
				else
					throw new RuntimeException("Unexpected type for " + f.getName());
			} catch(Exception e) {
				throw new RuntimeException("Cannot set value for " + f.getName(), e);
			}
		}
	}

	private ArrayList<Item> items = new ArrayList<Item>();

	/**
	 * TextField is always set to <code>value</code>, the Checkbox is checked if the
	 * value of <code>previous</code> is <b>not</b> Keyframe.UNSET.
	 */
	public KeyframePanel(Keyframe current, Keyframe previous) {
		super();
		this.kf = current;

		layout = new GridBagLayout();
		c = new GridBagConstraints();
		setLayout(layout);

		add("Frame", 0, 0, current.getFrame(), previous.getFrame(), null, null);
		Choice mmChoice = new Choice();
		mmChoice.add("<unset>");
		mmChoice.add("keep previous");
		mmChoice.add("replace");
		c.gridx++;
		layout.setConstraints(mmChoice, c);
		add(mmChoice);

		int nChannels = current.renderingSettings.length;
		try {
			for(int i = 0; i < nChannels; i++) {
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.gridx = 0;
				c.gridy++;
				c.anchor = GridBagConstraints.WEST;
				c.insets = new Insets(15, 0, 0, 0);
				Label label = new Label("Channel " + (i + 1));
				layout.setConstraints(label, c);
				add(label);

				final Choice choice = new Choice();
				choice.add("<unset>");
				choice.add("keep previous");
				choice.add("replace");
				c.gridx++;
				layout.setConstraints(choice, c);
				add(choice);

				c.insets = new Insets(0, 0, 0, 0);
				c.gridwidth = 1;

				RenderingSettings rs1 = current.renderingSettings[i];
				RenderingSettings rs2 = previous.renderingSettings[i];
				Choice c1 = add("color min", ++c.gridy, 0, rs1.colorMin,   rs2.colorMin,   RenderingSettings.class.getField("colorMin"),   rs1);
				Choice c2 = add("color max",   c.gridy, 3, rs1.colorMax,   rs2.colorMax,   RenderingSettings.class.getField("colorMax"),   rs1);
				Choice c3 = add("color gamma", c.gridy, 6, rs1.colorGamma, rs2.colorGamma, RenderingSettings.class.getField("colorGamma"), rs1);
				Choice c4 = add("alpha min", ++c.gridy, 0, rs1.alphaMin,   rs2.alphaMin,   RenderingSettings.class.getField("alphaMin"),   rs1);
				Choice c5 = add("alpha max",   c.gridy, 3, rs1.alphaMax,   rs2.alphaMax,   RenderingSettings.class.getField("alphaMax"),   rs1);
				Choice c6 = add("alpha gamma", c.gridy, 6, rs1.alphaGamma, rs2.alphaGamma, RenderingSettings.class.getField("alphaGamma"), rs1);

				broadcast(choice, c1, c2, c3, c4, c5, c6);
			}

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridx = 0;
			c.gridy++;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(15, 0, 0, 0);
			Label label = new Label("Clipping and Bounding Box");
			layout.setConstraints(label, c);
			add(label);

			Choice choice = new Choice();
			choice.add("<unset>");
			choice.add("keep previous");
			choice.add("replace");
			c.gridx++;
			layout.setConstraints(choice, c);
			add(choice);
			c.insets = new Insets(0, 0, 0, 0);


			Choice c1 = add("near", ++c.gridy, 0, current.near, previous.near, Keyframe.class.getField("near"), current);
			Choice c2 = add("far",    c.gridy, 3, current.far,  previous.far,  Keyframe.class.getField("far"),  current);

			c.gridwidth = 1;
			Choice c3 = add("x", ++c.gridy, 0, current.bbx, previous.bbx, Keyframe.class.getField("bbx"),  current);
			Choice c4 = add("y",   c.gridy, 3, current.bby, previous.bby, Keyframe.class.getField("bby"),  current);
			Choice c5 = add("z",   c.gridy, 6, current.bbz, previous.bbz, Keyframe.class.getField("bbz"),  current);
			Choice c6 = add("w", ++c.gridy, 0, current.bbw, previous.bbw, Keyframe.class.getField("bbw"),  current);
			Choice c7 = add("h",   c.gridy, 3, current.bbh, previous.bbh, Keyframe.class.getField("bbh"),  current);
			Choice c8 = add("d",   c.gridy, 6, current.bbd, previous.bbd, Keyframe.class.getField("bbd"),  current);
			broadcast(choice, c1, c2, c3, c4, c5, c6, c7, c8);

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridx = 0;
			c.gridy++;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(15, 0, 0, 0);
			label = new Label("Transformation");
			layout.setConstraints(label, c);
			add(label);
			choice = new Choice();
			choice.add("<unset>");
			choice.add("keep previous");
			choice.add("replace");
			c.gridx++;
			layout.setConstraints(choice, c);
			add(choice);
			c.insets = new Insets(0, 0, 0, 0);

			c.gridwidth = 1;
			c1 = add("Translate x", ++c.gridy, 0, current.dx, previous.dx, Keyframe.class.getField("dx"),  current);
			c2 = add("Translate y",   c.gridy, 3, current.dy, previous.dy, Keyframe.class.getField("dy"),  current);
			c3 = add("Translate z",   c.gridy, 6, current.dz, previous.dz, Keyframe.class.getField("dz"),  current);

			c4 = add("Angle x",     ++c.gridy, 0, current.angleX, previous.angleX, Keyframe.class.getField("angleX"),  current);
			c5 = add("Angle y",       c.gridy, 3, current.angleY, previous.angleY, Keyframe.class.getField("angleY"),  current);
			c6 = add("Angle z",       c.gridy, 6, current.angleZ, previous.angleZ, Keyframe.class.getField("angleZ"),  current);

			c7 = add("Scale",       ++c.gridy, 0, current.scale, previous.scale, Keyframe.class.getField("scale"),  current);
			broadcast(choice, c1, c2, c3, c4, c5, c6, c7);

			ArrayList<Choice> allChoices = new ArrayList<Choice>();
			for(Component c : getComponents()) {
				if(c instanceof Choice && c != mmChoice)
					allChoices.add((Choice)c);
			}
			Choice[] choices = new Choice[allChoices.size()];
			allChoices.toArray(choices);
			broadcast(mmChoice, choices);
		} catch(NoSuchFieldException e) {
			throw new RuntimeException("", e);
		}
	}

	private void broadcast(final Choice master, final Choice... slaves) {
		master.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				for(Choice slave : slaves) {
					slave.select(master.getSelectedItem());
					for(ItemListener l : slave.getItemListeners())
						l.itemStateChanged(e);
				}
			}
		});
	}

	public void apply() {
		for(Item i : items) {
			i.apply();
		}
	}

	/**
	 * TextField is always set to <code>value</code>, the Checkbox is checked if the
	 * value of <code>previous</code> is <b>not</b> Keyframe.UNSET.
	 * @param name
	 * @param gy
	 * @param gx
	 * @param value
	 * @param previous
	 * @param f
	 * @param o
	 */
	private void addWithCheckbox(String name, int gy, int gx, double value, double previous, Field f, Object o) {
		boolean checked = true;
		if(previous == Keyframe.UNSET)
			checked = false;

		c.gridx = gx;
		c.gridy = gy;
		c.anchor = GridBagConstraints.WEST;
		Checkbox cb = null;
		if(name.equals("Frame")) {
			Label l = new Label(name);
			layout.setConstraints(l, c);
			add(l);
		}
		else {
			cb = new Checkbox(name, checked);
			layout.setConstraints(cb, c);
			add(cb);
		}

		c.gridx++;
		c.insets.right = 15;
		TextField tf = new TextField(5);
		tf.setText(Double.toString(value));
		if(name.equals("Frame")) {
			tf.setFocusable(false);
			tf.setEditable(false);
		}
		layout.setConstraints(tf, c);
		c.insets.right = 0;
		add(tf);

//		if(!name.equals("Frame"))
//			items.add(new Item(tf, cb, f, o));
	}

	private Choice add(String name, int gy, int gx, double value, double previous, Field f, Object o) {
		boolean checked = true;
		if(previous == Keyframe.UNSET)
			checked = false;

//		if(name.startsWith("Angle")) {
//			if(previous != Keyframe.UNSET) previous = 180 * previous / Math.PI;
//			if(value != Keyframe.UNSET) value = 180 * value / Math.PI;
//		}

		final double prevValue = previous;
		final double currValue = value;

		c.gridx = gx;
		c.gridy = gy;
		c.anchor = GridBagConstraints.WEST;


		Label l = new Label(name);
		layout.setConstraints(l, c);
		add(l);

		c.gridx++;
		final TextField tf = new TextField(5);
		tf.setText(Double.toString(value));
		if(name.equals("Frame")) {
			tf.setFocusable(false);
			tf.setEditable(false);
		}

		layout.setConstraints(tf, c);
		add(tf);

		if(!name.equals("Frame")) {
			final Choice choice = new Choice();
			choice.add("<unset>");
			if(previous != Keyframe.UNSET)
				choice.add("keep previous");
			choice.add("replace");

			if(previous != Keyframe.UNSET) {
				if(previous != value) {
					choice.select("replace");
					tf.setForeground(Color.BLUE);
				} else {
					choice.select("keep previous");
				}
			}

			tf.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					choice.select("replace");
				}
			});

			choice.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent arg0) {
					if(choice.getSelectedItem().equals("<unset>")) {
						tf.setText("");
						tf.setEditable(false);
					}
					else if(choice.getSelectedItem().equals("keep previous")) {
						tf.setEditable(true);
						tf.setText(Double.toString(prevValue));
					}
					else if(choice.getSelectedItem().equals("replace")) {
						tf.setEditable(true);
						tf.setText(Double.toString(currValue));
					}
				}
			});
			c.gridx++;
			c.insets.right = 15;
			layout.setConstraints(choice, c);
			add(choice);
			c.insets.right = 0;

			items.add(new Item(tf, choice, f, o));
			return choice;
		}

		return null;
	}

	public static void main(String...strings) {
		Frame f = new Frame();
		Keyframe kf = new Keyframe(2);
		kf.renderingSettings = new RenderingSettings[] {
				new RenderingSettings(0, 0, 0, 0, 0, 0),
				new RenderingSettings(0, 0, 0, 0, 0, 0),
		};
		KeyframePanel panel = new KeyframePanel(kf, kf);
		f.add(panel);
		f.pack();
		f.show();

	}
}

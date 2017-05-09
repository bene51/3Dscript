package animation2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.TextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class NumberField extends Panel {

	private double min = Double.NEGATIVE_INFINITY;
	private double max = Double.POSITIVE_INFINITY;

	private boolean integersOnly = false;

	private boolean isRecordable = false;

	private TextField textfield;

	private ArrayList<Listener> listener = new ArrayList<Listener>();

	public static interface Listener {
		public void valueChanged(double v);
		public void record(NumberField src);
	}

	public void addListener(Listener l) {
		listener.add(l);
	}

	public void removeListener(Listener l) {
		listener.remove(l);
	}

	public String getText() {
		return textfield.getText();
	}

	public void setText(String s) {
		textfield.setText(s);
	}

	@Override
	public Dimension getPreferredSize() {
		if(!isRecordable)
			return super.getPreferredSize();
		Dimension d = textfield.getPreferredSize();
		int r = (textfield.getHeight() - 6) / 2;
		return new Dimension(d.width + 2 * r + 2, d.height);
	}

	private void fireValueChanged(double v) {
		System.out.println("fire");
		for(Listener l : listener)
			l.valueChanged(v);
	}

	private void fireRecord(NumberField src) {
		for(Listener l : listener)
			l.record(src);
	}

	public static void main(String[] args) {
		NumberField nf = new NumberField(8);
		nf.setText("5.88");
		nf.setLimits(-1000, 1000);
		// nf.setIntegersOnly(true);
		nf.addListener(new Listener() {
			@Override
			public void valueChanged(double v) {
				System.out.println("value changed to " + v);
			}
			@Override
			public void record(NumberField src) {
				System.out.println("record");
			}
		});
		Frame frame = new Frame("");
		frame.add(nf);
		frame.pack();
		frame.setVisible(true);
	}

	public void setLimits(double min, double max) {
		this.min = min;
		this.max = max;
	}

	public void setIntegersOnly(boolean b) {
		this.integersOnly = b;
	}

	private void setTextAndFire(String text) {
		System.out.println("setTextAndFire");
		if(getText().equals(text))
			return;
		setText(text);
		fireValueChanged(Double.parseDouble(text));
	}

	void handleKeyUp() {
		StringBuffer text = new StringBuffer(getText());
		int car = textfield.getCaretPosition();
		int originalCar = car;
		if(car == text.length())
			car--;

		int firstdig = 0;
		for(int i = 0; i < text.length(); i++) {
			if(Character.isDigit(text.charAt(i))) {
				firstdig = i;
				break;
			}
		}

		for(; car >= firstdig; car--) {
			char ch = text.charAt(car);
			if(!Character.isDigit(ch))
				continue;
			int digit = Integer.parseInt(Character.toString(ch));
			if(digit < 9) {
				text.setCharAt(car, Integer.toString(digit + 1).charAt(0));
				setTextAndFire(text.toString());
				textfield.setCaretPosition(originalCar);
				break;
			}
			else if(digit == 9) {
				text.setCharAt(car, '0');
				if(car == firstdig) {
					text.insert(firstdig, '1');
					setTextAndFire(text.toString());
					textfield.setCaretPosition(originalCar + 1);
				}
			}
		}
		double val = Double.parseDouble(getText());
		if(val < min)
			setTextAndFire(Double.toString(min));
		if(val > max)
			setTextAndFire(Double.toString(max));
		if(integersOnly && getText().contains(".")) {
			int intVal = (int)Math.round(Double.parseDouble(getText()));
			setTextAndFire(Integer.toString(intVal));
		}
	}

	void handleKeyDown() {
		StringBuffer text = new StringBuffer(getText());
		int car = textfield.getCaretPosition();
		int originalCar = car;
		if(car == text.length())
			car--;

		int firstdig = 0;
		for(int i = 0; i < text.length(); i++) {
			if(Character.isDigit(text.charAt(i))) {
				firstdig = i;
				break;
			}
		}
		for(; car >= firstdig; car--) {
			char ch = text.charAt(car);
			if(!Character.isDigit(ch))
				continue;
			int digit = Integer.parseInt(Character.toString(ch));
			if(digit > 0) {
				int carP = 0;
				// make 1 -> 0, and 14 -> 4, but only for integers
				if(car == firstdig && digit == 1 && (text.length() > firstdig + 1 && text.charAt(firstdig + 1) != '.')) {
					text.deleteCharAt(firstdig);
					carP = Math.max(0, originalCar - 1);
				}
				// normal case, e.g. 9 -> 8
				else {
					text.setCharAt(car, Integer.toString(digit - 1).charAt(0));
					carP = originalCar;
				}
				if(Double.parseDouble(text.toString()) == 0 && text.charAt(0) == '-') {
					text.deleteCharAt(0);
					carP = Math.max(0, carP - 1);
				}
				setTextAndFire(text.toString());
				textfield.setCaretPosition(carP);
				break;
			}
			else if(digit == 0) {
				// 210 -> 209
				if(car != firstdig)
					text.setCharAt(car, '9');
				// 010 -> 10 (or 010.1 -> 10.1, but not 0.1 -> .1)
				if(car == firstdig && text.length() > firstdig + 1 && text.charAt(firstdig + 1) != '.') {
					text.deleteCharAt(firstdig);
					setTextAndFire(text.toString());
					textfield.setCaretPosition(Math.max(0, originalCar - 1));
					break;
				} else if(car == firstdig && Double.parseDouble(getText()) == 0) {
					String s = getText();
					int cp = originalCar;
					if(s.charAt(0) == '-') {
						s = s.substring(1);
						cp--;
					}
					setText(s);
					textfield.setCaretPosition(cp);
					handleKeyUp();
					setTextAndFire("-" + getText());
					cp++;
					textfield.setCaretPosition(cp);
				} else if(car == firstdig && text.charAt(0) != '-') {
					setTextAndFire("-" + getText());
					textfield.setCaretPosition(originalCar + 1);
				} else if(car == firstdig && text.charAt(0) == '-') {
					setTextAndFire(getText().substring(1));
					textfield.setCaretPosition(Math.max(0, originalCar - 1));
				}
			}
		}
		double val = Double.parseDouble(getText());
		if(val < min)
			setTextAndFire(Double.toString(min));
		if(val > max)
			setTextAndFire(Double.toString(max));
		if(integersOnly && getText().contains(".")) {
			int intVal = (int)Math.round(Double.parseDouble(getText()));
			setTextAndFire(Integer.toString(intVal));
		}
	}

	public NumberField(int n) {
		this(n, false);
	}

	public NumberField(int n, final boolean isRecordable) {
		super(new FlowLayout(0, 0, 0));
		this.isRecordable = isRecordable;
		textfield = new TextField(n);
		add(textfield);
//		InputMap im = getInputMap();
//		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "bla");
//		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "bla");

		textfield.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int units = e.getWheelRotation();
				double d = Double.parseDouble(getText());
				boolean neg = d < 0;
				if((units > 0 && !neg) || (units < 0 && neg))
					handleKeyUp();
				else if((units < 0 && !neg) || (units > 0 && neg))
					handleKeyDown();
			}
		});

		textfield.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int kc = e.getKeyCode();
				if(kc == KeyEvent.VK_UP) {
					boolean neg = Double.parseDouble(getText()) < 0;
					if(!neg)
						handleKeyUp();
					else
						handleKeyDown();
					e.consume();
				} // VK_UP
				else if(kc == KeyEvent.VK_DOWN) {
					boolean neg = Double.parseDouble(getText()) < 0;
					if(!neg)
						handleKeyDown();
					else
						handleKeyUp();
					e.consume();
				} // VK_DOWN
				else if(kc == KeyEvent.VK_ENTER)
					fireValueChanged(Double.parseDouble(getText()));
				// fireKeyPressed(e);
			} // keyPressed

			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if((c == '-' && min < 0) || Character.isDigit(c) || (c == '.' && !integersOnly)) {
					; // fireValueChanged(Double.parseDouble(getText() + c));
				} else {
					e.consume();
				}
			}
		});

		if(isRecordable) {
			super.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					int x = e.getX();
					int y = e.getY();
					if(within(x, y)) {
						recordButtonColor = Color.ORANGE;
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if(recordButtonColor.equals(Color.ORANGE)) {
						recordButtonColor = defaultRecordButtonColor;
						repaint();
						fireRecord(NumberField.this);
					}
				}

				public boolean within(int x, int y) {
					int r = (textfield.getHeight() - 6) / 2;
					int x0 = textfield.getX() + textfield.getWidth() + 2;
					int y0 = textfield.getY() + textfield.getHeight()/2 - r;
					return x >= x0 && x <= x0 + 2 * r && y >= y0 && y <= y0 + 2 * r;
				}
			});
		}
	}

	private Color defaultRecordButtonColor = new Color(80, 80, 80);
	private Color recordButtonColor = defaultRecordButtonColor;


	@Override
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		super.paint(g);
		if(!isRecordable)
			return;
		int r = (textfield.getHeight() - 6) / 2;
		int x = textfield.getX() + textfield.getWidth() + 2;
		int y = textfield.getY() + textfield.getHeight()/2 - r;
		g.setColor(recordButtonColor);
		g.fillOval(x, y, 2 * r, 2 * r);
		g.setColor(Color.WHITE);
		FontMetrics fm = g2d.getFontMetrics();
		g2d.drawString("R",
				x + r - fm.stringWidth("R") / 2f,
				y + r + fm.getAscent() - (float)(fm.getHeight() / 2));
	}
}
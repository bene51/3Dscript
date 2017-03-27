package animation2;

import java.awt.Frame;
import java.awt.TextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class NumberField extends TextField {

	private double min = Double.NEGATIVE_INFINITY;
	private double max = Double.POSITIVE_INFINITY;

	private boolean integersOnly = false;

	private ArrayList<Listener> listener = new ArrayList<Listener>();

	public static interface Listener {
		public void valueChanged(double v);
	}

	public void addListener(Listener l) {
		listener.add(l);
	}

	public void removeListener(Listener l) {
		listener.remove(l);
	}

	private void fireValueChanged(double v) {
		System.out.println("fire");
		for(Listener l : listener)
			l.valueChanged(v);
	}

	public static void main(String[] args) {
		NumberField nf = new NumberField(8);
		nf.setText("5.88");
		nf.setLimits(0, 10);
		// nf.setIntegersOnly(true);
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
		if(getText().equals(text))
			return;
		super.setText(text);
		fireValueChanged(Double.parseDouble(text));
	}

	void handleKeyUp() {
		StringBuffer text = new StringBuffer(getText());
		int car = getCaretPosition();
		int originalCar = car;
		if(car == text.length())
			car--;

		for(; car >= 0; car--) {
			char ch = text.charAt(car);
			if(ch == '.')
				continue;
			int digit = Integer.parseInt(Character.toString(ch));
			if(digit < 9) {
				text.setCharAt(car, Integer.toString(digit + 1).charAt(0));
				setTextAndFire(text.toString());
				setCaretPosition(originalCar);
				break;
			}
			text.setCharAt(car, '0');
			if(car == 0) {
				text.insert(0, '1');
				setTextAndFire(text.toString());
				setCaretPosition(originalCar + 1);
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
		int car = getCaretPosition();
		int originalCar = car;
		if(car == text.length())
			car--;

		for(; car >= 0; car--) {
			char ch = text.charAt(car);
			if(ch == '.')
				continue;
			int digit = Integer.parseInt(Character.toString(ch));
			if(digit > 0) {
				int carP = 0;
				if(car == 0 && digit == 1 && (text.length() > 1 && text.charAt(1) != '.')) {
					text.deleteCharAt(0);
					carP = Math.max(0, originalCar - 1);
				} else {
					text.setCharAt(car, Integer.toString(digit - 1).charAt(0));
					carP = originalCar;
				}
				setTextAndFire(text.toString());
				setCaretPosition(carP);
				break;
			}
			text.setCharAt(car, '9');
			if(car == 0 && text.length() > 1 && text.charAt(1) != '.') {
				text.deleteCharAt(0);
				setTextAndFire(text.toString());
				setCaretPosition(Math.max(0, originalCar - 1));
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
		super(n);
//		InputMap im = getInputMap();
//		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "bla");
//		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "bla");

		super.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int units = e.getWheelRotation();
				if(units > 0)
					handleKeyUp();
				else if(units < 0)
					handleKeyDown();
			}
		});

		super.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_UP) {
					handleKeyUp();
					e.consume();
				} // VK_UP
				else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
					handleKeyDown();
					e.consume();
				} // VK_DOWN
				// fireKeyPressed(e);
			} // keyPressed

			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if(Character.isDigit(c) || (c == '.' && !integersOnly)) {
					;
				} else {
					e.consume();
				}
			}
		});
	}
}
package animation3d.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class BasicTextField implements MouseListener, KeyListener {

	public static final int JUSTIFY_LEFT = 0;
	public static final int JUSTIFY_RIGHT = 1;
	public static final int JUSTIFY_CENTER = 2;
	public static final int JUSTIFY_TOP = 3;
	public static final int JUSTIFY_BOTTOM = 4;

	public static interface Listener {
		public void textFieldChanged();
	}

	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	public void addTextFieldListener(Listener l) {
		listeners.add(l);
	}

	public void removeTextFieldListener(Listener l) {
		listeners.remove(l);
	}

	private void fireTextFieldChanged() {
		for(Listener l : listeners)
			l.textFieldChanged();
	}

	private Font font = new Font("Helvetica", Font.PLAIN, 12);

	private Rectangle rectangle = new Rectangle();

	private String text = "";

	private int x = 0;

	private int y = 0;

	private int justifyx = JUSTIFY_LEFT;

	private int justifyy = JUSTIFY_TOP;

	private boolean selectall = false;

	private boolean editing = false;

	private int padding = 3;

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return this.text;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void setJustification(int x, int y) {
		this.justifyx = x;
		this.justifyy = y;
	}

	public void paint(Graphics g) {
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);;
		g.setFont(font);
		Rectangle2D r = g.getFontMetrics().getStringBounds(text, g);
		int px = 0, py = 0;
		switch(justifyx) {
		case JUSTIFY_LEFT:   px = x + padding; break;
		case JUSTIFY_RIGHT:  px = x - padding - (int)Math.round(r.getWidth()); break;
		case JUSTIFY_CENTER: px = x - (int)Math.round(r.getWidth() / 2); break;
		}

		switch(justifyy) {
		case JUSTIFY_TOP:       py = y + padding; break;
		case JUSTIFY_BOTTOM:    py = y - padding - (int)Math.round(r.getHeight()); break;
		case JUSTIFY_CENTER:    py = y - (int)Math.round(r.getHeight() / 2); break;
		}

		rectangle.x = px;
		rectangle.width = (int)Math.round(r.getWidth());
		rectangle.y = py;
		rectangle.height = (int)Math.round(r.getHeight());

		g.setColor(selectall ? Color.BLACK : Color.WHITE);
		((Graphics2D)g).fill(rectangle);
		g.setColor(selectall ? Color.WHITE : Color.BLACK);
		g.drawString(text, px, py - (int)Math.round(r.getY()));

		if(editing) {
			g.drawRect(rectangle.x - 2, rectangle.y - 2, rectangle.width + 4, rectangle.height + 4);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(!e.isConsumed() && rectangle.contains(e.getPoint())) {
			selectall = true;
			editing = true;
			fireTextFieldChanged();
			e.getComponent().repaint();
			e.consume();
		}
		else {
			selectall = false;
			editing = false;
			e.getComponent().repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {
		if(editing) {
			char c = e.getKeyChar();
			if(Character.isDigit(c) || c == '-' || c == '.') {
				if(selectall) {
					text = Character.toString(c);
					selectall = false;
				}
				else
					text = text += c;
				fireTextFieldChanged();
				e.getComponent().repaint();
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ENTER) {
			editing = false;
			selectall = false;
			fireTextFieldChanged();
			e.getComponent().repaint();
		} else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
			selectall = false;
			fireTextFieldChanged();
			e.getComponent().repaint();
		} else if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			if(selectall)
				text = "";
			else
				if(text.length() > 0)
					text = text.substring(0, text.length() - 1);
			selectall = false;
			fireTextFieldChanged();
			e.getComponent().repaint();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	public static void main(String... args) {
		final int x = 50;
		final int y = 50;
		JFrame f = new JFrame();

		final BasicTextField tf1 = new BasicTextField();
		tf1.setText("111");
		tf1.setPosition(x,  y);
		tf1.setJustification(JUSTIFY_RIGHT, JUSTIFY_BOTTOM);

		final BasicTextField tf2 = new BasicTextField();
		tf2.setText("222");
		tf2.setPosition(x,  y);
		tf2.setJustification(JUSTIFY_LEFT, JUSTIFY_BOTTOM);

		final BasicTextField tf3 = new BasicTextField();
		tf3.setText("333");
		tf3.setPosition(x,  y);
		tf3.setJustification(JUSTIFY_RIGHT, JUSTIFY_TOP);

		final BasicTextField tf4 = new BasicTextField();
		tf4.setText("444");
		tf4.setPosition(x,  y);
		tf4.setJustification(JUSTIFY_LEFT, JUSTIFY_TOP);
		JPanel p = new JPanel() {
			private static final long serialVersionUID = 1420261164629448825L;

			@Override
			public void paintComponent(Graphics g) {
				g.drawLine(0, y, getWidth(), y);
				g.drawLine(x, 0, x, getHeight());
				tf1.paint(g);
				tf2.paint(g);
				tf3.paint(g);
				tf4.paint(g);
			}
		};
		p.addMouseListener(tf1);
		p.addMouseListener(tf2);
		p.addMouseListener(tf3);
		p.addMouseListener(tf4);
		p.addKeyListener(tf1);
		p.addKeyListener(tf2);
		p.addKeyListener(tf3);
		p.addKeyListener(tf4);
		p.setPreferredSize(new Dimension(300, 300));
		f.getContentPane().add(p);
		f.pack();
		f.setVisible(true);
	}
}

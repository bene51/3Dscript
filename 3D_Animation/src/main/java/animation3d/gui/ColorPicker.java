package animation3d.gui;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Vector;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ColorChooser;
import ij.gui.GUI;
import ij.plugin.Colors;
import ij.plugin.frame.PlugInDialog;
import ij.process.ColorProcessor;

public class ColorPicker extends PlugInDialog {

	static final String LOC_KEY = "cp.loc";

	private Color foreground = Color.WHITE;
	private Color background = Color.BLACK;
	private boolean useImageLUT = false;

	private final Checkbox imageLUTCheckBox;

	public interface BackgroundColorListener {
		public void backgroundColorChanged(Color bg);
	}

	public interface ForegroundColorListener {
		public void foregroundColorChanged(Color fg);
	}

	public interface LUTListener {
		void useImageLUTChanged(boolean useImageLUT);
	}

	private ArrayList<BackgroundColorListener> bgListener = new ArrayList<BackgroundColorListener>();
	private ArrayList<ForegroundColorListener> fgListener = new ArrayList<ForegroundColorListener>();
	private ArrayList<LUTListener>            lutListener = new ArrayList<>();

	private void fireForegroundColorChanged(Color c) {
		for(ForegroundColorListener l : fgListener)
			l.foregroundColorChanged(c);
	}

	private void fireBackgroundColorChanged(Color c) {
		for(BackgroundColorListener l : bgListener)
			l.backgroundColorChanged(c);
	}

	private void fireUseImageLUTChanged(boolean useImageLUT) {
		for(LUTListener l : lutListener)
			l.useImageLUTChanged(useImageLUT);
	}

	public void addForegroundColorListener(ForegroundColorListener l) {
		fgListener.add(l);
	}

	public void addBackgroundColorListener(BackgroundColorListener l) {
		bgListener.add(l);
	}

	public void addLUTListener(LUTListener l) {
		lutListener.add(l);
	}

	public ColorPicker(Color foreground, Color background, boolean useImageLUT) {
		super("CP");
		this.foreground = foreground;
		this.background = background;
		this.useImageLUT = useImageLUT;
		WindowManager.addWindow(this);
		int colorWidth = 22;
		int colorHeight = 16;
		int columns = 5;
		int rows = 20;
		int width = columns*colorWidth;
		int height = rows*colorHeight;
		addKeyListener(IJ.getInstance());
		setLayout(new BorderLayout());
		ColorGenerator cg = new ColorGenerator(width, height, new int[width*height]);
		cg.drawColors(colorWidth, colorHeight, columns, rows);
		Canvas colorCanvas = new ColorCanvas(width, height, null, cg);
		Panel panel = new Panel();
		GridBagLayout gridbag = new GridBagLayout();
		panel.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(3, 3, 3, 3);
		imageLUTCheckBox = new Checkbox("Use image LUT", useImageLUT);
		imageLUTCheckBox.addItemListener(e -> {
			setUseImageLUT(imageLUTCheckBox.getState());
		});
		panel.add(imageLUTCheckBox, c);
		c.gridy++;
		panel.add(colorCanvas, c);
		add(panel);
		setResizable(false);
		pack();
		Point loc = Prefs.getLocation(LOC_KEY);
		if (loc!=null)
			setLocation(loc);
		else
			GUI.center(this);
		show();
	}

	private void setForegroundColor(Color c) {
		if(useImageLUT) {
			setUseImageLUT(false);
			imageLUTCheckBox.setState(false);
		}
		foreground = c;
		fireForegroundColorChanged(c);
	}

	private void setBackgroundColor(Color c) {
		background = c;
		fireBackgroundColorChanged(c);
	}

	private void setUseImageLUT(boolean useImageLUT) {
		this.useImageLUT = useImageLUT;
		fireUseImageLUTChanged(useImageLUT);
	}

	public Color getForegroundColor() {
		return foreground;
	}

	public Color getBackgroundColor() {
		return background;
	}

	@Override
	public void close() {
		super.close();
		Prefs.saveLocation(LOC_KEY, getLocation());
	}

	class ColorGenerator extends ColorProcessor {
		private int w, h;
		private int[] colors = {0xff0000, 0x00ff00, 0x0000ff, 0xffffff, 0x00ffff, 0xff00ff, 0xffff00, 0x000000};

		public ColorGenerator(int width, int height, int[] pixels) {
			super(width, height, pixels);
			setAntialiasedText(true);
		}

		void drawColors(int colorWidth, int colorHeight, int columns, int rows) {
			w = colorWidth;
			h = colorHeight;
			setColor(0xffffff);
			setRoi(0, 0, 110, 320);
			fill();
			drawRamp();
			resetBW();
			flipper();
			drawLine(0, 256, 110, 256);

			int x = 1;
			int y = 0;
			refreshBackground(false);
			refreshForeground(false);

			Color c;
			float hue, saturation=1f, brightness=1f;
			double w=colorWidth, h=colorHeight;
			for ( x=2; x<10; x++) {
				for ( y=0; y<32; y++) {
					hue = (float)(y/(2*h)-.15);
					if (x<6) {
						saturation = 1f;
						brightness = (float)(x*4/w);
					} else {
						saturation = 1f - ((float)((5-x)*-4/w));
						brightness = 1f;
					}
					c = Color.getHSBColor(hue, saturation, brightness);
					setRoi(x*(int)(w/2), y*(int)(h/2), (int)w/2, (int)h/2);
					setColor(c);
					fill();
				}
			}
			drawSpectrum(h);
			resetRoi();
		}

		void drawColor(int x, int y, Color c) {
			setRoi(x*w, y*h, w, h);
			setColor(c);
			fill();
		}

		public void refreshBackground(boolean backgroundInFront) {
			//Boundary for Background Selection
			setColor(0x444444);
			drawRect((w*2)-12, 276, (w*2)+4, (h*2)+4);
			setColor(0x999999);
			drawRect((w*2)-11, 277, (w*2)+2, (h*2)+2);
			setRoi((w*2)-10, 278, w*2, h*2);//Paints the Background Color
			Color bg = getBackgroundColor();
			setColor(bg);
			fill();
			if (backgroundInFront)
				drawLabel("B", bg, w*4-18, 278+h*2);
		}

		public void refreshForeground(boolean backgroundInFront) {
			//Boundary for Foreground Selection
			setColor(0x444444);
			drawRect(8, 266, (w*2)+4, (h*2)+4);
			setColor(0x999999);
			drawRect(9, 267, (w*2)+2, (h*2)+2);
			setRoi(10, 268, w*2, h*2); //Paints the Foreground Color
			Color fg = getForegroundColor();
			setColor(fg);
			fill();
			if (backgroundInFront)
				drawLabel("F", fg, 12, 268+14);
		}

		private void drawLabel(String label, Color c, int x, int y) {
			int intensity = (c.getRed()+c.getGreen()+c.getBlue())/3;
			c = intensity<128?Color.white:Color.black;
			setColor(c);
			drawString(label, x, y);
		}

		void drawSpectrum(double h) {
			Color c;
			for ( int x=5; x<7; x++) {
				for ( int y=0; y<32; y++) {
					float hue = (float)(y/(2*h)-.15);
					c = Color.getHSBColor(hue, 1f, 1f);
					setRoi(x*(w/2), y*(int)(h/2), w/2, (int)h/2);
					setColor(c);
					fill();
				}
			}
			setRoi(55, 32, 22, 16); //Solid red
			setColor(0xff0000);
			fill();
			setRoi(55, 120, 22, 16); //Solid green
			setColor(0x00ff00);
			fill();
			setRoi(55, 208, 22, 16); //Solid blue
			setColor(0x0000ff);
			fill();
			setRoi(55, 80, 22, 8); //Solid yellow
			setColor(0xffff00);
			fill();
			setRoi(55, 168, 22, 8); //Solid cyan
			setColor(0x00ffff);
			fill();
			setRoi(55, 248, 22, 8); //Solid magenta
			setColor(0xff00ff);
			fill();
		}

		void drawRamp() {
			int r,g,b;
			for (int x=0; x<w; x++) {
				 for (double y=0; y<(h*16); y++) {
					r = g = b = (byte)y;
					pixels[(int)y*width+x] = 0xff000000 | ((r<<16)&0xff0000) | ((g<<8)&0xff00) | (b&0xff);
				}
			}
		}

		void resetBW() {   //Paints the Color Reset Button
			setColor(0x000000);
			drawRect(92, 300, 9, 7);
			setColor(0x000000);
			setRoi(88, 297, 9, 7);
			fill();
		}

		void flipper() {   //Paints the Flipper Button
			int xa = 90;
			int ya = 272;
			setColor(0x000000);
			drawLine(xa, ya, xa+9, ya+9);//Main Body
			drawLine(xa+1, ya, xa+9, ya+8);
			drawLine(xa, ya+1, xa+8, ya+9);
			drawLine(xa, ya, xa, ya+5);//Upper Arrow
			drawLine(xa+1, ya+1, xa+1, ya+6);
			drawLine(xa, ya, xa+5, ya);
			drawLine(xa+1, ya+1, xa+6, ya+1);
			drawLine(xa+9, ya+9, xa+9, ya+4);//Lower Arrow
			drawLine(xa+8, ya+8, xa+8, ya+3);
			drawLine(xa+9, ya+9, xa+4, ya+9);
			drawLine(xa+8, ya+8, xa+3, ya+8);
		}

	}

	class ColorCanvas extends Canvas implements MouseListener, MouseMotionListener{
		int width, height;
		Vector colors;
		boolean background;
		long mouseDownTime;
		ColorGenerator ip;
		Frame frame;

		public ColorCanvas(int width, int height, Frame frame, ColorGenerator ip) {
			this.width=width; this.height=height;
			this.ip = ip;
			addMouseListener(this);
			addMouseMotionListener(this);
			addKeyListener(IJ.getInstance());
			setSize(width, height);
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(width, height);
		}

		@Override
		public void update(Graphics g) {
			paint(g);
		}

		@Override
		public void paint(Graphics g) {
			g.drawImage(ip.createImage(), 0, 0, null);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			//IJ.log("mousePressed "+e);
			ip.setLineWidth(1);
			Rectangle flipperRect = new Rectangle(86, 268, 18, 18);
			Rectangle resetRect = new Rectangle(86, 294, 18, 18);
			Rectangle foreground1Rect = new Rectangle(9, 266, 45, 10);
			Rectangle foreground2Rect = new Rectangle(9, 276, 23, 25);
			Rectangle background1Rect = new Rectangle(33, 302, 45, 10);
			Rectangle background2Rect = new Rectangle(56, 277, 23, 25);
			int x = e.getX();
			int y = e.getY();
			long difference = System.currentTimeMillis()-mouseDownTime;
			boolean doubleClick = (difference<=250);
			mouseDownTime = System.currentTimeMillis();
			if (flipperRect.contains(x, y)) {
				Color c = getBackgroundColor();
				setBackgroundColor(getForegroundColor());
				setForegroundColor(c);
			} else if(resetRect.contains(x,y)) {
				setForegroundColor(new Color(0x000000));
				setBackgroundColor(new Color(0xffffff));
			} else if ((background1Rect.contains(x,y)) || (background2Rect.contains(x,y))) {
				background = true;
				if (doubleClick) editColor();
				ip.refreshForeground(background);
				ip.refreshBackground(background);
			} else if ((foreground1Rect.contains(x,y)) || (foreground2Rect.contains(x,y))) {
				background = false;
				if (doubleClick) editColor();
				ip.refreshBackground(background);
				ip.refreshForeground(background);
			} else {
				//IJ.log(" " + difference + " " + doubleClick);
				if (doubleClick)
					editColor();
				else
					setDrawingColor(x, y, background);
			}
			if (background) {
				ip.refreshForeground(background);
				ip.refreshBackground(background);
			} else {
				ip.refreshBackground(background);
				ip.refreshForeground(background);
			}
			repaint();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			int p = ip.getPixel(x, y);
			int r = (p&0xff0000)>>16;
			int g = (p&0xff00)>>8;
			int b = p&0xff;
			String hex = Colors.colorToString(new Color(r,g,b));
			IJ.showStatus("red="+pad(r)+", green="+pad(g)+", blue="+pad(b)+" ("+hex+")");

		}

		String pad(int n) {
			String str = ""+n;
			while (str.length()<3)
			str = "0" + str;
			return str;
		}

		void setDrawingColor(int x, int y, boolean setBackground) {
			int p = ip.getPixel(x, y);
			int r = (p&0xff0000)>>16;
			int g = (p&0xff00)>>8;
			int b = p&0xff;
			Color c = new Color(r, g, b);
			if (setBackground) {
				setBackgroundColor(c);
			} else {
				setForegroundColor(c);
			}
		}

		void editColor() {
			Color c  = background?getBackgroundColor():getForegroundColor();
			ColorChooser cc = new ColorChooser((background?"Background":"Foreground")+" Color", c, false);
			c = cc.getColor();
			if (background)
				setBackgroundColor(c);
			else
				setForegroundColor(c);
		}

		public void refreshColors() {
			ip.refreshBackground(false);
			ip.refreshForeground(false);
			repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseDragged(MouseEvent e) {}

	}
}


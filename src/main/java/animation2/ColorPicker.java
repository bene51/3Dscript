package animation2;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import ij.IJ;
import ij.plugin.frame.PlugInDialog;
import ij.process.ColorProcessor;

public class ColorPicker extends PlugInDialog {

	private static final long serialVersionUID = -9171495632005451201L;

	public static void main(String...strings) {
		new ij.ImageJ();
		Color c = ColorPicker.pick();
		System.out.println(c);
	}

	public static Color pick() {
		return new ColorPicker().color;
	}

	private Color color = Color.WHITE;

    public ColorPicker() {
		super("CP");
        int colorWidth = 5;
        int colorHeight = 5;
        int columns = 80;
        int rows = 20;
        addKeyListener(IJ.getInstance());
		setLayout(new BorderLayout());
        ColorProcessor cg = drawColors(colorWidth, colorHeight, columns, rows);

        Canvas colorCanvas = new ColorCanvas(cg);
        Panel panel = new Panel();
        panel.add(colorCanvas);
        add(panel);
		setResizable(false);
		pack();
		setModal(true);
		setVisible(true);
    }

    private void setColor(Color c ) {
    	this.color = c;
    }

    @Override
	public void close() {
	 	super.close();
	}

    public ColorProcessor drawColors(int colorWidth, int colorHeight, int columns, int rows) {
    	int width = columns*colorWidth;
        int height = rows*colorHeight;
        int[] pixels = new int[width * height];
        ColorProcessor cp = new ColorProcessor(width, height, pixels);
        float hue, saturation = 1f, brightness = 1f;
        int w = colorWidth, h = colorHeight;
        for (int x = 0; x < columns; x++) {
            hue = (float)x / columns;
            for (int y = 0; y < rows; y++) {
            	saturation = (float)(rows - y - 1) / rows;
            	Color c = Color.getHSBColor(hue, saturation, brightness);
                cp.setRoi(x * w, y * h, w, h);
                cp.setColor(c);
                cp.fill();
            }
        }
        cp.resetRoi();
        return cp;
    }

	class ColorCanvas extends Canvas implements MouseListener, MouseMotionListener{

		private static final long serialVersionUID = -410864712196020014L;

		private ColorProcessor ip;

		public ColorCanvas(ColorProcessor ip) {
			this.ip = ip;
			addMouseListener(this);
	 		addMouseMotionListener(this);
	        addKeyListener(IJ.getInstance());
			setSize(ip.getWidth(), ip.getHeight());
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(ip.getWidth(), ip.getHeight());
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
			int x = e.getX();
			int y = e.getY();
			setDrawingColor(x, y);
			Component parent = getParent();
			if(parent != null) {
				parent = parent.getParent();
				if(parent != null)
					((ColorPicker)parent).close();
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			int p = ip.getPixel(x, y);
			int r = (p&0xff0000)>>16;
			int g = (p&0xff00)>>8;
			int b = p&0xff;
			IJ.showStatus("red="+pad(r)+", green="+pad(g)+", blue="+pad(b));

		}

		String pad(int n) {
			String str = ""+n;
			while (str.length()<3)
			str = "0" + str;
			return str;
		}

		void setDrawingColor(int x, int y) {
			int p = ip.getPixel(x, y);
			int r = (p&0xff0000)>>16;
			int g = (p&0xff00)>>8;
			int b = p&0xff;
			setColor(new Color(r, g, b));
		}

		public void refreshColors() {
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


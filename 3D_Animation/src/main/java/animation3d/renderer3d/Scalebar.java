package animation3d.renderer3d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import animation3d.util.Transform;
import ij.process.ImageProcessor;

public class Scalebar {

	public enum Position {
		OBJECT_LOWER_LEFT_FRONT ("object - lower left front",  new float[] {-1, +1, -1}),
		OBJECT_LOWER_RIGHT_FRONT("object - lower right front", new float[] {+1, +1, -1}),
		OBJECT_UPPER_LEFT_FRONT ("object - upper left front",  new float[] {-1, -1, -1}),
		OBJECT_UPPER_RIGHT_FRONT("object - upper right front", new float[] {+1, -1, -1}),
		OBJECT_LOWER_LEFT_BACK  ("object - lower left back",   new float[] {-1, +1, +1}),
		OBJECT_LOWER_RIGHT_BACK ("object - lower right back",  new float[] {+1, +1, +1}),
		OBJECT_UPPER_LEFT_BACK  ("object - upper left back",   new float[] {-1, -1, +1}),
		OBJECT_UPPER_RIGHT_BACK ("object - upper right back",  new float[] {+1, -1, +1}),

		VIEW_LOWER_LEFT ("view - lower left",  new float[] {-1, +1, 0}),
		VIEW_LOWER_RIGHT("view - lower right", new float[] {+1, +1, 0}),
		VIEW_UPPER_LEFT ("view - upper left",  new float[] {-1, -1, 0}),
		VIEW_UPPER_RIGHT("view - upper right", new float[] {+1, -1, 0});

		private String name;

		private float[] mul;

		private Position(String name, float[] mul) {
			this.name = name;
			this.mul = mul;
		}

		@Override
		public String toString() {
			return name;
		}

		public static Position fromName(String name) {
			for(Position sb : values())
				if(sb.name.equals(name))
					return sb;
			return null;
		}

		public static String[] getNames() {
			String[] ret = new String[values().length];
			for(int i = 0; i < ret.length; i++)
				ret[i] = values()[i].name;
			return ret;
		}
	}

	private boolean visible = true;
	private float width = 2;
	private float length = 100;
	private Color color = Color.white;
	private float offset = 10;
	private Position pos = Position.VIEW_LOWER_LEFT;
	private boolean changedManually = false;

	private float[][] positions = new float[8][];

	private float pw, ph, pd;

	public Scalebar(float w, float h, float d, double pw, double ph, double pd) {
		positions[0] = new float[] {0,     h - 1, 0};
		positions[1] = new float[] {w - 1, h - 1, 0};
		positions[2] = new float[] {0,     0,     0};
		positions[3] = new float[] {w - 1, 0,     0};
		positions[4] = new float[] {0,     h - 1, d - 1};
		positions[5] = new float[] {w - 1, h - 1, d - 1};
		positions[6] = new float[] {0,     0,     d - 1};
		positions[7] = new float[] {w - 1, 0,     d - 1};

		this.pw = (float)pw;
		this.ph = (float)ph;
		this.pd = (float)pd;

		// length = calculateDefaultLength(w * this.pw);
	}

	private static double calculateDefaultLength(double outputPW) {
		// the rough length should be 60 pixels in the output image
		double about = 50 * outputPW;

		// bring it to the interval [1; 10]
		int shift = 0;
		while(about < 1) {
			about *= 10;
			shift--;
		}
		while(about > 10) {
			about /= 10;
			shift++;
		}

		double l = 0;
		if(about < 1.5)
			l = 1;
		else if(about < 3.5)
			l = 2;
		else if(about < 7.5)
			l = 5;
		else
			l = 10;

		l *= Math.pow(10, shift);

		return l;
	}

	public void setDefaultLength(double pwOut) {
		if(!changedManually) {
			this.length = (float)calculateDefaultLength(pwOut);
			System.out.println("*** changed default scalebar length to " + length + ", pwOut = " + pwOut);
		}
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean b) {
		visible = b;
	}

	public Position getPosition() {
		return pos;
	}

	public void setPosition(Position p) {
		pos = p;
	}

	public float getWidth() {
		return width;
	}

	public void setWidth(float w) {
		width = w;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color c) {
		color = c;
	}

	public float getLength() {
		return length;
	}

	public void setLength(float l) {
		changedManually = changedManually || l != length;
		length = l;
	}

	public float getOffset() {
		return offset;
	}

	public void setOffset(float l) {
		offset = l;
	}

	public void drawScalebar(ImageProcessor ip, float[] fwd, float[] inv, float pwOut) {
		if(!visible)
			return;

		Graphics2D g = ((BufferedImage)ip.createImage()).createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new java.awt.BasicStroke(width));
		g.setColor(color);

		if(pos.ordinal() < 8) { // object aligned
			float x0 = positions[pos.ordinal()][0] + pos.mul[0] * offset;
			float y0 = positions[pos.ordinal()][1] + pos.mul[1] * offset;
			float z0 = positions[pos.ordinal()][2] + pos.mul[2] * offset;

			float[] p0 = new float[3];
			float[] px = new float[3];
			float[] py = new float[3];
			float[] pz = new float[3];

			// the forward transformation applies to pixel coordinates, so
			// w here corresponds to w * pw in real-world, we need 100 in real-world:
			//            w ~ w * pw,
			// w / (w * pw) ~ 1
			//     100 / pw ~ lx
			float lx = length / pw;
			float ly = length / ph;
			float lz = length / pd;

			Transform.apply(fwd, x0, y0, z0, p0);
			Transform.apply(fwd, x0 - pos.mul[0] * lx, y0, z0, px);
			Transform.apply(fwd, x0, y0 - pos.mul[1] * ly, z0, py);
			Transform.apply(fwd, x0, y0, z0 - pos.mul[2] * lz, pz);

			g.drawLine(Math.round(p0[0]), Math.round(p0[1]), Math.round(px[0]), Math.round(px[1]));
			g.drawLine(Math.round(p0[0]), Math.round(p0[1]), Math.round(py[0]), Math.round(py[1]));
			g.drawLine(Math.round(p0[0]), Math.round(p0[1]), Math.round(pz[0]), Math.round(pz[1]));
		}
		else { // view aligned
			int w = ip.getWidth();
			int h = ip.getHeight();

			float lx = length / pwOut;

			float x = 0, y = 0;
			switch(pos) {
			case VIEW_LOWER_LEFT:  x = offset; y = h - offset; break;
			case VIEW_LOWER_RIGHT: x = w - lx - offset; y = h - offset; break;
			case VIEW_UPPER_LEFT:  x = offset; y = offset; break;
			case VIEW_UPPER_RIGHT: x = w - lx - offset; y = offset; break;
			}

			g.drawLine(Math.round(x), Math.round(y), Math.round(x + lx), Math.round(y));
		}
	}
}

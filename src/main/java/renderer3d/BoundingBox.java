package renderer3d;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import ij.gui.Line;
import ij.gui.Toolbar;
import ij.process.ImageProcessor;

public class BoundingBox {

	private static final int[][] QUADS = {
			{0, 3, 2, 1},
			{1, 2, 6, 5},
			{4, 5, 6, 7},
			{0, 4, 7, 3},
			{0, 1, 5, 4},
			{2, 3, 7, 6},
	};

	private static final float[][] normals = {
			{0, 0, -1},
			{1, 0, 0},
			{0, 0, 1},
			{-1, 0, 0},
			{0, -1, 0},
			{0, 1, 0},
	};

	private float[][] positions = new float[8][];

	private float pw, ph, pd;

	public BoundingBox(float w, float h, float d, double pw, double ph, double pd) {
		positions[0] = new float[] {0, 0, 0};
		positions[1] = new float[] {w, 0, 0};
		positions[2] = new float[] {w, h, 0};
		positions[3] = new float[] {0, h, 0};
		positions[4] = new float[] {0, 0, d};
		positions[5] = new float[] {w, 0, d};
		positions[6] = new float[] {w, h, d};
		positions[7] = new float[] {0, h, d};

		this.pw = (float)pw;
		this.ph = (float)ph;
		this.pd = (float)pd;
	}

	public boolean isQuadVisible(int quad, float dirx, float diry, float dirz) {
		// d1 = p3 - p0
		// d2 = p1 - p0
//		double d1x = positions[QUADS[quad][3]][0] - positions[QUADS[quad][0]][0];
//		double d1y = positions[QUADS[quad][3]][1] - positions[QUADS[quad][0]][1];
//		double d1z = positions[QUADS[quad][3]][2] - positions[QUADS[quad][0]][2];
//
//		double d2x = positions[QUADS[quad][1]][0] - positions[QUADS[quad][0]][0];
//		double d2y = positions[QUADS[quad][1]][1] - positions[QUADS[quad][0]][1];
//		double d2z = positions[QUADS[quad][1]][2] - positions[QUADS[quad][0]][2];
//
//		double cx = d2y * d1z - d1y * d2z;
//		double cy = d2x * d1z - d1x * d2z;
//		double cz = d2x * d1y - d1x * d2y;
//
//		double dot = cx * dirx + cy * diry + cz * dirz;

		double dot = normals[quad][0] * dirx + normals[quad][1] * diry + normals[quad][2] * dirz;

		return dot < 0;
	}

	public void drawScalebar(ImageProcessor ip, float[] fwd, float[] inv) {
		float x0 = positions[5][0] + 10;
		float y0 = positions[5][1] - 10;
		float z0 = positions[5][2] + 10;

		float[] p0 = new float[3];
		float[] px = new float[3];
		float[] py = new float[3];
		float[] pz = new float[3];

		// the forward transformation applies to pixel coordinates, so
		// w here corresponds to w * pw in real-world, we need 100 in real-world:
		//            w ~ w * pw,
		// w / (w * pw) ~ 1
		//     100 / pw ~ lx
		float lx = 100 / pw;
		float ly = 100 / ph;
		float lz = 100 / pd;

		Transform.apply(fwd, x0, y0, z0, p0);
		Transform.apply(fwd, x0 - lx, y0, z0, px);
		Transform.apply(fwd, x0, y0 + ly, z0, py);
		Transform.apply(fwd, x0, y0, z0 - lz, pz);

		Graphics2D g = ((BufferedImage)ip.createImage()).createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new java.awt.BasicStroke(Line.getWidth()));
		g.setColor(Toolbar.getForegroundColor());

		g.drawLine(Math.round(p0[0]), Math.round(p0[1]), Math.round(px[0]), Math.round(px[1]));
		g.drawLine(Math.round(p0[0]), Math.round(p0[1]), Math.round(py[0]), Math.round(py[1]));
		g.drawLine(Math.round(p0[0]), Math.round(p0[1]), Math.round(pz[0]), Math.round(pz[1]));
	}

	public void drawQuads(ImageProcessor ip, float[] fwd, float[] inv) {
		Graphics2D g = ((BufferedImage)ip.createImage()).createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new java.awt.BasicStroke(Line.getWidth()));
		g.setColor(Toolbar.getForegroundColor());

		float[] pos = new float[3];
		for(int q = 0; q < QUADS.length; q++) {
			System.out.println("dir = " + inv[2] + ", " + inv[6] + ", " + inv[10]);
			if(!isQuadVisible(q, inv[2], inv[6], inv[10]))
				continue;

			float x = positions[QUADS[q][0]][0];
			float y = positions[QUADS[q][0]][1];
			float z = positions[QUADS[q][0]][2];
			Transform.apply(fwd, x, y, z, pos);
			int x0 = Math.round(pos[0]);
			int y0 = Math.round(pos[1]);

			x = positions[QUADS[q][1]][0];
			y = positions[QUADS[q][1]][1];
			z = positions[QUADS[q][1]][2];
			Transform.apply(fwd, x, y, z, pos);
			int x1 = Math.round(pos[0]);
			int y1 = Math.round(pos[1]);

			x = positions[QUADS[q][2]][0];
			y = positions[QUADS[q][2]][1];
			z = positions[QUADS[q][2]][2];
			Transform.apply(fwd, x, y, z, pos);
			int x2 = Math.round(pos[0]);
			int y2 = Math.round(pos[1]);

			x = positions[QUADS[q][3]][0];
			y = positions[QUADS[q][3]][1];
			z = positions[QUADS[q][3]][2];
			Transform.apply(fwd, x, y, z, pos);
			int x3 = Math.round(pos[0]);
			int y3 = Math.round(pos[1]);

			g.drawLine(x0, y0, x1, y1);
			g.drawLine(x1, y1, x2, y2);
			g.drawLine(x2, y2, x3, y3);
			g.drawLine(x3, y3, x0, y0);
		}
	}
}

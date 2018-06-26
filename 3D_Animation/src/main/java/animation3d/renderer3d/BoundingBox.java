package animation3d.renderer3d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import animation3d.util.Transform;
import ij.process.ImageProcessor;

public class BoundingBox {

	private boolean boundingBoxVisible = true;
	private float boundingBoxWidth = 1;
	private Color boundingBoxColor = Color.GRAY;

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
		positions[1] = new float[] {w - 1, 0, 0};
		positions[2] = new float[] {w - 1, h - 1, 0};
		positions[3] = new float[] {0, h - 1, 0};
		positions[4] = new float[] {0, 0, d - 1};
		positions[5] = new float[] {w - 1, 0, d - 1};
		positions[6] = new float[] {w - 1, h - 1, d - 1};
		positions[7] = new float[] {0, h - 1, d - 1};

		this.pw = (float)pw;
		this.ph = (float)ph;
		this.pd = (float)pd;
	}

	public boolean isVisible() {
		return boundingBoxVisible;
	}

	public void setVisible(boolean b) {
		boundingBoxVisible = b;
	}

	public float getWidth() {
		return boundingBoxWidth;
	}

	public void setWidth(float w) {
		boundingBoxWidth = w;
	}

	public Color getColor() {
		return boundingBoxColor;
	}

	public void setColor(Color c) {
		boundingBoxColor = c;
	}

	private boolean isQuadVisible(int quad, float dirx, float diry, float dirz) {
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

	public void drawFrontClippingPlane(ImageProcessor ip, float[] fwd, float[] inv, float near) {
		if(!boundingBoxVisible)
			return;
		PlaneBoxIntersection.Point[] poly = new PlaneBoxIntersection.Point[6];

		// https://stackoverflow.com/questions/7685495/transforming-a-3d-plane-by-4x4-matrix
		// http://www.songho.ca/opengl/gl_normaltransform.html
		//
		// inversely transform the plane through (0, 0, near) with n = (0, 0, 1), i.e.
		// the plane z = near;
		//
		// vector4 O = (xyz * d, 1)
		// vector4 N = (xyz, 0)
		// O = M * O
		// N = transpose(invert(M)) * N
		// xyz = N.xyz
		// d = dot(O.xyz, N.xyz)


		float[] O = Transform.apply(inv, 0, 0, near, null);
		float[] N = Transform.apply(Transform.transpose3x3(fwd), 0f, 0f, 1, null);
		float d = O[0] * N[0] + O[1] * N[1] + O[2] * N[2];

		float nx = N[0];
		float ny = N[1];
		float nz = N[2];
		float[] ray = new float[] {nx, ny, nz};

		int nPoints = PlaneBoxIntersection.calculateIntersection(ray, -d, positions[0], positions[6], poly);
		if(nPoints < 3)
			return;

		Graphics2D g = ((BufferedImage)ip.createImage()).createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new java.awt.BasicStroke(2));
		g.setColor(Color.yellow);

		for(int i = 0; i < nPoints; i++) {
			Transform.apply(fwd, poly[i].c[0], poly[i].c[1], poly[i].c[2], poly[i].c);
		}

		for(int i = 0; i < nPoints - 1; i++)
			g.drawLine(
					Math.round(poly[i].c[0]), Math.round(poly[i].c[1]),
					Math.round(poly[i + 1].c[0]), Math.round(poly[i + 1].c[1]));

		g.drawLine(
				Math.round(poly[nPoints - 1].c[0]), Math.round(poly[nPoints - 1].c[1]),
				Math.round(poly[0].c[0]), Math.round(poly[0].c[1]));
	}

	public void drawBoundingBox(ImageProcessor ip, float[] fwd, float[] inv) {
		if(!boundingBoxVisible)
			return;
		Graphics2D g = ((BufferedImage)ip.createImage()).createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new java.awt.BasicStroke(boundingBoxWidth));
		g.setColor(boundingBoxColor);

		float[] pos = new float[3];
		for(int q = 0; q < QUADS.length; q++) {
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

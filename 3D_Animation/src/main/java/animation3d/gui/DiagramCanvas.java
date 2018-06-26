package animation3d.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class DiagramCanvas {

	private int marginLeft = 20;
	private int marginRight = 20;
	private int marginTop = 20;
	private int marginBottom = 20;

	private int frameWidth = 1;

	private double xMin, xMax;
	private double yMin, yMax;
	private double pw, ph;

	private double xMinBB, xMaxBB, yMinBB, yMaxBB;

	private int availableWidth;
	private int availableHeight;

	private BasicTextField xMinField = new BasicTextField();
	private BasicTextField xMaxField = new BasicTextField();
	private BasicTextField yMinField = new BasicTextField();
	private BasicTextField yMaxField = new BasicTextField();

	static DecimalFormat df = (DecimalFormat)NumberFormat.getNumberInstance(Locale.US);
	static {
		df.applyPattern("#,##0.##");
	}

	private boolean xMinSetManually = false;
	private boolean yMinSetManually = false;
	private boolean xMaxSetManually = false;
	private boolean yMaxSetManually = false;

	public DiagramCanvas() {
		xMinField.setJustification(BasicTextField.JUSTIFY_LEFT, BasicTextField.JUSTIFY_TOP);
		xMaxField.setJustification(BasicTextField.JUSTIFY_RIGHT, BasicTextField.JUSTIFY_TOP);
		xMinField.setPosition(canvasX(xMin), getBottomPixel());
		xMaxField.setPosition(canvasX(xMax), getBottomPixel());

		yMinField.setJustification(BasicTextField.JUSTIFY_RIGHT, BasicTextField.JUSTIFY_BOTTOM);
		yMaxField.setJustification(BasicTextField.JUSTIFY_RIGHT, BasicTextField.JUSTIFY_TOP);
		yMinField.setPosition(getLeftPixel(), canvasY(yMin));
		yMaxField.setPosition(getLeftPixel(), canvasY(yMax));

		BasicTextField.Listener l = new BasicTextField.Listener() {
			@Override
			public void textFieldChanged() {
				try {
					double xMin = df.parse(xMinField.getText()).doubleValue();
					double xMax = df.parse(xMaxField.getText()).doubleValue();
					double yMin = df.parse(yMinField.getText()).doubleValue();
					double yMax = df.parse(yMaxField.getText()).doubleValue();
					if(xMin != DiagramCanvas.this.xMin) xMinSetManually = true;
					if(yMin != DiagramCanvas.this.yMin) yMinSetManually = true;
					if(xMax != DiagramCanvas.this.xMax) xMaxSetManually = true;
					if(yMax != DiagramCanvas.this.yMax) yMaxSetManually = true;
					setDisplayRange(xMin, yMin, xMax, yMax, false);
				} catch(Exception e) {
					System.out.println(e.getMessage());
				}
			}
		};
		xMinField.addTextFieldListener(l);
		xMaxField.addTextFieldListener(l);
		yMinField.addTextFieldListener(l);
		yMaxField.addTextFieldListener(l);
	}

	public void addListenersTo(Component c) {
		c.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					boolean todo = false;
					if(e.getX() < getLeftPixel()) {
						yMinSetManually = yMaxSetManually = false;
						todo = true;
					}
					if(e.getY() > getBottomPixel()) {
						xMinSetManually = xMaxSetManually = false;
						todo = true;
					}
					if(todo) {
						e.consume();
						setBoundingBox(xMinBB, yMinBB, xMaxBB, yMaxBB);
					}
				}
			}
		});
		c.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.getX() < getLeftPixel() || e.getX() > getRightPixel())
					return;
				if(e.getY() < getTopPixel() || e.getY() > getBottomPixel())
					return;

				int mx = e.getX();
				double rx = realX(mx);

				double factor = 1;
				if(e.getUnitsToScroll() > 0)
					factor = 1.2;
				else if(e.getUnitsToScroll() < 0)
					factor = 5 / 6.0;

				double realDiffLower = rx - xMin;
				double realDiffUpper = xMax - rx;
				realDiffLower *= factor;
				realDiffUpper *= factor;
				setDisplayRange(rx - realDiffLower, yMin, rx + realDiffUpper, yMax);
				xMinSetManually = xMaxSetManually = true;
				e.getComponent().repaint();
			}
		});
		c.addMouseListener(xMinField);
		c.addMouseListener(xMaxField);
		c.addKeyListener(xMinField);
		c.addKeyListener(xMaxField);
		c.addMouseListener(yMinField);
		c.addMouseListener(yMaxField);
		c.addKeyListener(yMinField);
		c.addKeyListener(yMaxField);
	}

	public void setSizes(int w, int h) {
		availableWidth = w - marginLeft - marginRight - 2 * frameWidth;
		availableHeight = h - marginTop - marginBottom - 2 * frameWidth;

		pw = (xMax - xMin) / (availableWidth - 1);
		ph = (yMax - yMin) / (availableHeight - 1);

		xMinField.setPosition(canvasX(xMin), getBottomPixel());
		xMaxField.setPosition(canvasX(xMax), getBottomPixel());
		yMinField.setPosition(getLeftPixel(), canvasY(yMin));
		yMaxField.setPosition(getLeftPixel(), canvasY(yMax));
	}

	public void setDisplayRange(double xMin, double yMin, double xMax, double yMax) {
		this.setDisplayRange(xMin, yMin, xMax, yMax, true);
	}

	private void setDisplayRange(double xMin, double yMin, double xMax, double yMax, boolean updateTextFields) {
		System.out.println("setDisplayRange(" + xMin + ", " + yMin + ", " + xMax + ", " + yMax + ")");
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;

		if(updateTextFields) {
			xMinField.setText(df.format(xMin));
			xMaxField.setText(df.format(xMax));
			yMinField.setText(df.format(yMin));
			yMaxField.setText(df.format(yMax));
		}

		pw = (xMax - xMin) / (availableWidth - 1);
		ph = (yMax - yMin) / (availableHeight - 1);
	}

	/**
	 * Sets the display range if it wasn't set manually.
	 * @param xMin
	 * @param yMin
	 * @param xMax
	 * @param yMax
	 */
	public void setBoundingBox(double xMin, double yMin, double xMax, double yMax) {
		System.out.println("setBoundingBox(" + xMin + ", " + yMin + ", " + xMax + ", " + yMax + ")");
		if(xMax == xMin)
			xMax = xMin + 1;
		if(yMax == yMin)
			yMax = yMin + 1;

		xMinBB = xMin;
		yMinBB = yMin;
		xMaxBB = xMax;
		yMaxBB = yMax;
		System.out.println(xMin + ", " + yMin + ", " + xMax + ", " + yMax);
		if(!xMinSetManually) this.xMin = xMin;
		if(!xMaxSetManually) this.xMax = xMax;
		if(!yMinSetManually) this.yMin = yMin;
		if(!yMaxSetManually) this.yMax = yMax;
		System.out.println(this.xMin + ", " + this.yMin + ", " + this.xMax + ", " + this.yMax);

		xMinField.setText(df.format(this.xMin));
		xMaxField.setText(df.format(this.xMax));
		yMinField.setText(df.format(this.yMin));
		yMaxField.setText(df.format(this.yMax));

		pw = (this.xMax - this.xMin) / (availableWidth - 1);
		ph = (this.yMax - this.yMin) / (availableHeight - 1);
	}

//	public void increaseDisplayRange(double xMin, double yMin, double xMax, double yMax) {
//		System.out.println(xMin + ", " + yMin + ", " + xMax + ", " + yMax);
//		if(!xMinSetManually) this.xMin = Math.min(this.xMin, xMin);
//		if(!xMaxSetManually) this.xMax = Math.max(this.xMax, xMax);
//		if(!yMinSetManually) this.yMin = Math.min(this.yMin, yMin);
//		if(!yMaxSetManually) this.yMax = Math.max(this.yMax, yMax);
//		System.out.println(this.xMin + ", " + this.yMin + ", " + this.xMax + ", " + this.yMax);
//
//		xMinField.setText(df.format(this.xMin));
//		xMaxField.setText(df.format(this.xMax));
//		yMinField.setText(df.format(this.yMin));
//		yMaxField.setText(df.format(this.yMax));
//
//		pw = (this.xMax - this.xMin) / (availableWidth - 1);
//		ph = (this.yMax - this.yMin) / (availableHeight - 1);
//	}

	public void setMargins(int top, int left, int bottom, int right) {
		int w = availableWidth + marginLeft + marginRight + 2 * frameWidth;
		int h = availableHeight + marginTop + marginBottom + 2 * frameWidth;

		this.marginTop = top;
		this.marginLeft = left;
		this.marginBottom = bottom;
		this.marginRight = right;

		availableWidth = w - marginLeft - marginRight - 2 * frameWidth;
		availableHeight = h - marginTop - marginBottom - 2 * frameWidth;

		pw = (xMax - xMin) / (availableWidth - 1);
		ph = (yMax - yMin) / (availableHeight - 1);

		xMinField.setPosition(canvasX(xMin), getBottomPixel());
		xMaxField.setPosition(canvasX(xMax), getBottomPixel());
		yMinField.setPosition(getLeftPixel(), canvasY(yMin));
		yMaxField.setPosition(getLeftPixel(), canvasY(yMax));
	}

	public double canvasXDouble(double rx) {
		return marginLeft + frameWidth + (rx - xMin) / pw;
	}

	public int canvasX(double rx) {
		return (int)Math.round(canvasXDouble(rx));
	}

	public double canvasYDouble(double ry) {
		return marginTop + frameWidth + availableHeight - (ry - yMin) / ph;
	}

	public int canvasY(double ry) {
		return (int)Math.round(canvasYDouble(ry));
	}

	public double realX(double canvasx) {
		return xMin + (canvasx - marginLeft - frameWidth) * pw;
	}

	public double realY(double canvasy) {
		return yMin + (marginTop + frameWidth + availableHeight - canvasy) * ph;
	}

	public int getLeftPixel() {
		return marginLeft + frameWidth;
	}

	public int getRightPixel() {
		return marginLeft + frameWidth + availableWidth;
	}

	public int getAvailableWidth() {
		return availableWidth;
	}

	public int getTopPixel() {
		return marginTop + frameWidth;
	}

	public int getBottomPixel() {
		return marginTop + frameWidth + availableHeight;
	}

	public int getAvailableHeight() {
		return availableHeight;
	}

	public double getXMin() {
		return xMin;
	}

	public double getYMin() {
		return yMin;
	}

	public double getXMax() {
		return xMax;
	}

	public double getYMax() {
		return yMax;
	}

	public double pw() {
		return pw;
	}

	public double ph() {
		return ph;
	}

	public double clampRealX(double x) {
		return Math.max(xMin, Math.min(xMax, x));
	}

	public double clampRealY(double y) {
		return Math.max(yMin, Math.min(yMax, y));
	}

	public int clampCanvasX(int x) {
		return Math.max(getLeftPixel(), Math.min(getRightPixel(), x));
	}

	public int clampCanvasY(int y) {
		return Math.max(getTopPixel(), Math.min(getBottomPixel(), y));
	}

	public Rectangle getClippingRect() {
		return new Rectangle(marginLeft, marginTop, availableWidth + 1 * frameWidth, availableHeight + 1 * frameWidth);
	}

	public void drawFrame(Graphics g) {
		g.drawRect(marginLeft, marginTop, availableWidth + 1 * frameWidth, availableHeight + 1 * frameWidth);
		g.drawLine(marginLeft + availableWidth + frameWidth, marginTop, marginLeft + availableWidth + frameWidth, marginTop + availableHeight + frameWidth);
	}

	public void drawXMinMax(Graphics g) {
		xMinField.paint(g);
		xMaxField.paint(g);
	}

	public void drawYMinMax(Graphics g) {
		yMinField.paint(g);
		yMaxField.paint(g);
	}

	public static double calculateTickIncrement(double max) {
		double factor = 1;
		while(max > 10) {
			max /= 10;
			factor *= 10;
		}
		while(max < 1) {
			max *= 10;
			factor /= 10;
		}
		double inc = Math.floor(max / 2.0) * factor;
		if(max < 2) {
			max *= 10;
			factor /= 10;
			inc = ((int)max / 2) * factor;
		}
		return inc;
	}

	public static void main(String[] args) {
		for(int i = 0; i < 30; i++) {
			double v = Math.random() * 1000;
			System.out.println(v + ": " + df.format(v));
		}
	}


}

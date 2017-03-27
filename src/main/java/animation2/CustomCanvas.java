package animation2;

public abstract class CustomCanvas extends DoubleBuffer {

	public abstract double realx(double canvasx);

	public abstract int canvasx(double realx);

	public abstract int canvasy(double realy);

	public abstract double realy(double canvasy);

	public abstract double pw();

	public abstract double ph();

	public abstract double minX();
	public abstract double minY();
	public abstract double maxX();
	public abstract double maxY();
}

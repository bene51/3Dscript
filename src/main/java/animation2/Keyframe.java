package animation2;

public class Keyframe implements Comparable<Keyframe> {

	public static int UNSET = Integer.MIN_VALUE;

	private int frame;

	public RenderingSettings[] renderingSettings;
	public float near, far;

	public float scale;
	public float dx, dy, dz;
	public double angleX, angleY, angleZ;
	public int bbx, bby, bbz, bbw, bbh, bbd;

	public static void main(String[] args) throws Exception {
	}


	public Keyframe(int frame) {
		this.frame = frame;
	}

	public Keyframe(
			int frame,
			RenderingSettings[] renderingSettings,
			float near, float far,
			float scale,
			float dx, float dy, float dz,
			double angleX, double angleY, double angleZ,
			int bbx, int bby, int bbz, int bbw, int bbh, int bbd) {
		super();
		this.frame = frame;
		this.renderingSettings = renderingSettings;
		this.near = near;
		this.far = far;
		this.scale = scale;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.angleX = angleX;
		this.angleY = angleY;
		this.angleZ = angleZ;
		this.bbx = bbx;
		this.bby = bby;
		this.bbz = bbz;
		this.bbw = bbw;
		this.bbh = bbh;
		this.bbd = bbd;
	}

	public int getFrame() {
		return frame;
	}

	public void setFrame(int frame) {
		this.frame = frame;
	}

	@Override
	public int compareTo(Keyframe o) {
		if(frame < o.frame) return -1;
		if(frame > o.frame) return +1;
		return 0;
	}
}

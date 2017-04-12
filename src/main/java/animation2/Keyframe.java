package animation2;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Keyframe implements Comparable<Keyframe> {

	public static int UNSET = Integer.MIN_VALUE;

	private int frame;

	public RenderingSettings[] renderingSettings;
	public float near, far;

	public float scale;
	public float dx, dy, dz;
	public double angleX, angleY, angleZ;
	public int bbx, bby, bbz, bbw, bbh, bbd;

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

	public static void main(String[] args) throws Throwable {
		Keyframe kf = new Keyframe(2);
		kf.renderingSettings = new RenderingSettings[] {
				new RenderingSettings(0, 0, 0, 0, 0, 0),
				new RenderingSettings(0, 0, 0, 0, 0, 0),
		};

		Class<?> clazz = Keyframe.class;
		Field[] fields = clazz.getDeclaredFields();
		for(Field f : fields) {
			if(Modifier.isStatic(f.getModifiers())) {
				System.out.println("...Skipping static field " + f.getName());
			}
			else if(Modifier.isPrivate(f.getModifiers())) {
				System.out.println("...Skipping private field " + f.getName());
			}
			else if(f.getType().isArray()) {
				System.out.println("isArray");
				int l = Array.getLength(f.get(kf));
				for(int i = 0; i < l; i++) {
					Object o = Array.get(f.get(kf), i);
					Class<?> clazz2 = o.getClass();
					Field[] fields2 = clazz2.getDeclaredFields();
					for(Field f2 : fields2) {
						System.out.println(f2.getName() + ": " + f2.get(o));
					}
				}
			}
			else {
				System.out.println(f.getName() + ": " + f.get(kf) + " (" + f.getType() + ") isInteger? " + (f.getType() == Integer.TYPE));
			}
		}
		clazz.getField("dz").set(kf, 2);
		int i = Integer.MAX_VALUE;
		System.out.println((float)i);
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

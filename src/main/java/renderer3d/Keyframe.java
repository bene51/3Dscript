package renderer3d;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class Keyframe implements Comparable<Keyframe> {

	// order is the order of the declared fields
	public static final int TRANSX = 0;
	public static final int TRANSY = 1;
	public static final int TRANSZ = 2;

	public static final int ROTX   = 3;
	public static final int ROTY   = 4;
	public static final int ROTZ   = 5;

	public static final int SCALE  = 6;

	public static final int BOUNDINGBOX_XMIN  = 7;
	public static final int BOUNDINGBOX_YMIN  = 8;
	public static final int BOUNDINGBOX_ZMIN  = 9;
	public static final int BOUNDINGBOX_XMAX  = 10;
	public static final int BOUNDINGBOX_YMAX  = 11;
	public static final int BOUNDINGBOX_ZMAX  = 12;

	public static final int NEAR  = 13;
	public static final int FAR   = 14;

	public static final int COLOR_MIN   = 0;
	public static final int COLOR_MAX   = 1;
	public static final int COLOR_GAMMA = 2;
	public static final int ALPHA_MIN   = 3;
	public static final int ALPHA_MAX   = 4;
	public static final int ALPHA_GAMMA = 5;
	public static final int WEIGHT      = 6;


	public static int UNSET = Integer.MIN_VALUE;

	private int frame;

	@RenderingProperty(label="X Translation")
	public float dx;
	@RenderingProperty(label="Y Translation")
	public float dy;
	@RenderingProperty(label="Z Translation")
	public float dz;

	@RenderingProperty(label="X Rotation")
	public double angleX;
	@RenderingProperty(label="Y Rotation")
	public double angleY;
	@RenderingProperty(label="Z Rotation")
	public double angleZ;

	@RenderingProperty(label="Scale")
	public float scale;

	@RenderingProperty(label="Bounding Box X Min")
	public int bbx0;
	@RenderingProperty(label="Bounding Box Y Min")
	public int bby0;
	@RenderingProperty(label="Bounding Box Z Min")
	public int bbz0;
	@RenderingProperty(label="Bounding Box X Max")
	public int bbx1;
	@RenderingProperty(label="Bounding Box Y Max")
	public int bby1;
	@RenderingProperty(label="Bounding Box Z Max")
	public int bbz1;

	@RenderingProperty(label="Near")
	public float near;
	@RenderingProperty(label="Far")
	public float far;

	public RenderingSettings[] renderingSettings;

	private CombinedTransform fwdTransform;

	public Keyframe(int frame) {
		this.frame = frame;
	}

	public static int getNumberOfNonChannelProperties() {
		return 15;
	}

	public static int getNumberOfChannelProperties() {
		return 7;
	}

	public Keyframe(
			int frame,
			RenderingSettings[] renderingSettings,
			float near, float far,
			CombinedTransform fwdTransform,
			int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
		super();
		this.frame = frame;
		this.renderingSettings = renderingSettings;
		this.near = near;
		this.far = far;
		this.bbx0 = bbx0;
		this.bby0 = bby0;
		this.bbz0 = bbz0;
		this.bbx1 = bbx1;
		this.bby1 = bby1;
		this.bbz1 = bbz1;
		this.fwdTransform = fwdTransform;
		calculateFieldsFromTransformation();
	}

	void calculateFieldsFromTransformation() {
		float[] eulerAngles = fwdTransform.guessEulerAnglesDegree();
		float[] translation = fwdTransform.getTranslation();
		float scale = fwdTransform.getScale();
		this.scale = scale;
		this.dx = translation[0];
		this.dy = translation[1];
		this.dz = translation[2];
		this.angleX = eulerAngles[0];
		this.angleY = eulerAngles[1];
		this.angleZ = eulerAngles[2];
	}

	void calculateTransformationFromFields() {
		fwdTransform.setTransformation((float)angleX, (float)angleY, (float)angleZ, dx, dy, dz, scale);
	}

	public Keyframe(
			int frame,
			RenderingSettings[] renderingSettings,
			float near, float far,
			float scale,
			float dx, float dy, float dz,
			double angleX, double angleY, double angleZ,
			int bbx0, int bby0, int bbz0, int bbx1, int bby1, int bbz1) {
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
		this.bbx0 = bbx0;
		this.bby0 = bby0;
		this.bbz0 = bbz0;
		this.bbx1 = bbx1;
		this.bby1 = bby1;
		this.bbz1 = bbz1;
	}

	@Override
	public Keyframe clone() {
		Keyframe kf = createEmptyKeyframe(renderingSettings.length);
		kf.setFrom(this);
		return kf;
	}

	public void setFrom(Keyframe o) {
		this.frame = o.frame;
		if(this.renderingSettings.length != o.renderingSettings.length)
			renderingSettings = new RenderingSettings[o.renderingSettings.length];
		for(int i = 0; i < renderingSettings.length; i++)
			renderingSettings[i].set(o.renderingSettings[i]);
		this.near = o.near;
		this.far = o.far;
		this.scale = o.scale;
		this.dx = o.dx;
		this.dy = o.dy;
		this.dz = o.dz;
		this.angleX = o.angleX;
		this.angleY = o.angleY;
		this.angleZ = o.angleZ;
		this.bbx0 = o.bbx0;
		this.bby0 = o.bby0;
		this.bbz0 = o.bbz0;
		this.bbx1 = o.bbx1;
		this.bby1 = o.bby1;
		this.bbz1 = o.bbz1;
		if(o.fwdTransform != null) {
			this.fwdTransform = o.fwdTransform.clone();
		} else {
			this.fwdTransform = null;
		}
	}

	public void setFwdTransform(CombinedTransform fwd) {
		this.fwdTransform = fwd;
	}

	public CombinedTransform getFwdTransform() {
		return this.fwdTransform;
	}

	public static Keyframe createEmptyKeyframe() {
		return createEmptyKeyframe(1);
	}

	public static Keyframe createEmptyKeyframe(int channels) {
		RenderingSettings[] rs = new RenderingSettings[channels];
		for(int c = 0; c < channels; c++)
			rs[c] = new RenderingSettings(0, 1, 1, 0, 1, 1);

		Keyframe kf = new Keyframe(0,
				rs,
				0, 1,
				1,
				0, 0, 0,
				0, 0, 0,
				0, 0, 0, 1, 1, 1);
		return kf;
	}

	public static class KeyframeProperty {
		public final Field field;
		public final Object object;

		public KeyframeProperty(Field field, Object object) {
			this.field = field;
			this.object = object;
		}

		public String getLabel() {
			try {
				return field.getAnnotation(RenderingProperty.class).label();
			} catch(Throwable t) {
				throw new RuntimeException(t);
			}
		}

		public double getValue() {
			Class<?> c = field.getType();
			try {
				if(c == Integer.TYPE)
					return field.getInt(object);
				if(c == Float.TYPE)
					return field.getFloat(object);
				if(c == Double.TYPE)
					return field.getDouble(object);

				throw new RuntimeException("Unexpected type: " + c);
			} catch(Throwable t) {
				throw new RuntimeException(t);
			}
		}

		public void setValue(double v) {
			Class<?> c = field.getType();
			try {
				if(c == Integer.TYPE)
					field.setInt(object, (int)Math.round(v));
				else if(c == Float.TYPE)
					field.setFloat(object, (float)v);
				else if(c == Double.TYPE)
					field.setDouble(object, v);
				else
					throw new RuntimeException("Unexpected type: " + c);
			} catch(Throwable t) {
				throw new RuntimeException(t);
			}
		}

		@Override
		public String toString() {
			RenderingProperty a = field.getAnnotation(RenderingProperty.class);
			String ret = field.getName();
			try {
				ret += ": " + field.get(object) + " (" + field.getType() + ") annotation: " + a.label();
			} catch(Throwable t) {
				t.printStackTrace();
			}
			return ret;
		}
	}

	private void getKeyframePropertyRecursively(ArrayList<KeyframeProperty> result, Object object) throws Throwable {
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for(Field f : fields) {
			System.out.println(f.getName());
			if(Modifier.isStatic(f.getModifiers())) {
				System.out.println("...Skipping static field " + f.getName());
			}
			else if(Modifier.isPrivate(f.getModifiers())) {
				System.out.println("...Skipping private field " + f.getName());
			}
			else if(f.getType().isArray()) {
				System.out.println("isArray");
				int l = Array.getLength(f.get(object));
				for(int i = 0; i < l; i++) {
					Object o = Array.get(f.get(object), i);
					getKeyframePropertyRecursively(result, o);
				}
			}
			else if(!f.getType().isPrimitive()) {
				Object o = f.get(object);
				getKeyframePropertyRecursively(result, o);
			}
			else {
				System.out.println(f.getName() + ": " + f.get(object) + " (" + f.getType() + ") isInteger? " + (f.getType() == Integer.TYPE));
				RenderingProperty a = f.getAnnotation(RenderingProperty.class);
				if(a != null)
					result.add(new KeyframeProperty(f, object));
			}
		}
	}

	public KeyframeProperty[] getRenderingProperties() {
		ArrayList<KeyframeProperty> fieldsArray = new ArrayList<KeyframeProperty>();
		Object object = this;

		try {
			getKeyframePropertyRecursively(fieldsArray, object);
		} catch(Throwable t) {
			throw new RuntimeException(t);
		}

		KeyframeProperty[] ret = new KeyframeProperty[fieldsArray.size()];
		fieldsArray.toArray(ret);
		return ret;
	}

	public static void main(String[] args) throws Throwable {
		Keyframe kf = new Keyframe(2);
		kf.renderingSettings = new RenderingSettings[] {
				new RenderingSettings(0, 0, 0, 0, 0, 0),
				new RenderingSettings(0, 0, 0, 0, 0, 0),
		};

		KeyframeProperty[] properties1 = kf.getRenderingProperties();


		System.out.println("------");
		for(KeyframeProperty prop : properties1)
			System.out.println(prop);

//		Class<?> clazz = Keyframe.class;
//		Field[] fields = clazz.getDeclaredFields();
//		for(Field f : fields) {
//			if(Modifier.isStatic(f.getModifiers())) {
//				System.out.println("...Skipping static field " + f.getName());
//			}
//			else if(Modifier.isPrivate(f.getModifiers())) {
//				System.out.println("...Skipping private field " + f.getName());
//			}
//			else if(f.getType().isArray()) {
//				System.out.println("isArray");
//				int l = Array.getLength(f.get(kf));
//				for(int i = 0; i < l; i++) {
//					Object o = Array.get(f.get(kf), i);
//					Class<?> clazz2 = o.getClass();
//					Field[] fields2 = clazz2.getDeclaredFields();
//					for(Field f2 : fields2) {
//						System.out.println(f2.getName() + ": " + f2.get(o));
//					}
//				}
//			}
//			else {
//				System.out.println(f.getName() + ": " + f.get(kf) + " (" + f.getType() + ") isInteger? " + (f.getType() == Integer.TYPE));
//				RenderingProperty a = f.getAnnotation(RenderingProperty.class);
//				System.out.println("annotation: " + (a == null ? "null" : a.label()));
//			}
//		}
//		clazz.getField("dz").set(kf, 2);
//		int i = Integer.MAX_VALUE;
//		System.out.println((float)i);
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

package animation2;

import org.scijava.vecmath.AxisAngle4f;
import org.scijava.vecmath.Matrix4f;
import org.scijava.vecmath.Vector3f;

public class Rotation {

	static class MyQuat {

		float ux, uy, uz, a;

		MyQuat(float ux, float uy, float uz, float a) {
			this.ux = ux;
			this.uy = uy;
			this.uz = uz;
			this.a = a;
		}

		void mul(MyQuat o) {
			float A = a / 2 * ux + o.a / 2 * o.ux;
			float B = a / 2 * uy + o.a / 2 * o.uy;
			float C = a / 2 * uz + o.a / 2 * o.uz;

			float L = (float)Math.sqrt(A * A + B * B + C * C);

			a = 2 * L;
			ux = A / L;
			uy = B / L;
			uz = C / L;
		}
	}

	public static void main(String[] args) {
		Vector3f axis1 = new Vector3f(
				(float)Math.random(),
				(float)Math.random(),
				(float)Math.random());
		axis1.normalize();
		float angle1 = (float)(Math.random() * Math.PI * 2);
		AxisAngle4f aa1 = new AxisAngle4f(axis1, angle1);
		System.out.println(aa1);

		MyQuat quat1 = new MyQuat(axis1.x, axis1.y, axis1.z, angle1);

		Vector3f axis2 = new Vector3f(
				(float)Math.random(),
				(float)Math.random(),
				(float)Math.random());
		axis2.normalize();
		float angle2 = (float)(Math.random() * Math.PI);
		AxisAngle4f aa2 = new AxisAngle4f(axis2, angle2);
		System.out.println(aa2);

		MyQuat quat2 = new MyQuat(axis2.x, axis2.y, axis2.z, angle2);

		quat1.mul(quat2);
		System.out.println(quat1.a + ", " + quat1.ux + ", " + quat1.uy + ", " + quat2.uz);

		Matrix4f m1 = new Matrix4f();
		m1.set(aa1);
		System.out.println(m1);
		Matrix4f m2 = new Matrix4f();
		m2.set(aa2);
		m1.mul(m2);
		aa1.set(m1);

		System.out.println(aa1);
		System.out.println(aa1.x * aa1.x + aa1.y * aa1.y + aa1.z * aa1.z);

	}
}

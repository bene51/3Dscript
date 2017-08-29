package textanim;

import java.util.List;

import parser.NumberOrMacro;
import renderer3d.Keyframe;
import renderer3d.Transform;

public class RotationAnimation extends TransformationAnimation {

	private NumberOrMacro[] axis;
	private NumberOrMacro byDegree;
	private float[] center;

	public RotationAnimation(int fromFrame, int toFrame, NumberOrMacro[] axis, NumberOrMacro byDegree, float[] center) {
		super(fromFrame, toFrame);
		this.axis = axis;
		this.byDegree = byDegree;
		this.center = new float[3];
		System.arraycopy(center, 0, this.center, 0, center.length);
	}

	@Override
	public NumberOrMacro[] getNumberOrMacros() {
		return new NumberOrMacro[] {axis[0], axis[1], axis[2], byDegree};
	}

	@Override
	public void adjustKeyframe(Keyframe current, List<Keyframe> previous) {}


	@Override
	public void getTransformationAt(int frame, float[] matrix) {
		double angle = 0;
		if(byDegree.isMacro())
			angle = byDegree.evaluateMacro(frame, fromFrame, toFrame);
		else
			angle = interpolate(frame, 0, byDegree.getValue());

		float[] a = new float[3];
		double sum = 0;
		for(int i = 0; i < 3; i++) {
			NumberOrMacro ai = axis[i];
			a[i] = (float)(ai.isMacro() ? ai.evaluateMacro(frame, fromFrame, toFrame) : ai.getValue());
			sum += a[i] * a[i];
		}

		sum = Math.sqrt(sum);
		a[0] /= sum;
		a[1] /= sum;
		a[2] /= sum;

		float rad = (float)(Math.PI * angle / 180.0);
		float[] rot = Transform.fromAngleAxis(a, rad, null);
		rot = Transform.mul(rot, Transform.fromTranslation(-center[0], -center[1], -center[2], matrix));
		rot = Transform.mul(Transform.fromTranslation(center[0], center[1], center[2], null), rot);
		System.arraycopy(rot, 0, matrix, 0, 12);
	}
}

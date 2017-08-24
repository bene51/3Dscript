package animation2;

import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;
import renderer3d.Keyframe;
import renderer3d.Transform;
import textanim.Animation;
import textanim.TransformationAnimation;

public class Animator {

	private final List<Animation> animations;
	private final int nChannels;
	private final float[] fromCalib;
	private final float[] toTransform;

	public Animator(int nChannels) {
		animations = new ArrayList<Animation>();
		this.nChannels = nChannels;
	}

	public ImagePlus render(int from, int to) {

	}

	public void clearAnimations() {
		animations.clear();
	}

	public void addAnimation(Animation a) {
		animations.add(a);
	}

	public List<Keyframe> createKeyframes(int from, int to) {
		List<Keyframe> keyframes = new ArrayList<Keyframe>();
		for(int t = from; t <= to; t++) {
			Keyframe kf = Keyframe.createEmptyKeyframe(nChannels);
			float[] fwd = Transform.fromIdentity(null);
			for(Animation a : animations) {
				a.adjustKeyframe(kf, keyframes);

				if(a instanceof TransformationAnimation) {
					float[] x = new float[12];
					((TransformationAnimation)a).getTransformationAt(t, x);
					fwd = Transform.mul(x, fwd);
				}
			}
			fwd = Transform.mul(fwd, fromCalib);
			fwd = Transform.mul(toTransform, fwd);
			kf.setFwdTransform(fwd);

			keyframes.add(kf);
		}
		return keyframes;
	}
}

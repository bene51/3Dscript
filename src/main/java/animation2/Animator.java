package animation2;

import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import renderer3d.Keyframe;
import renderer3d.Renderer3D;
import renderer3d.Transform;
import textanim.Animation;
import textanim.ChangeAnimation;
import textanim.TransformationAnimation;

public class Animator {

	private final Renderer3D renderer;
	private final List<Animation> animations;

	public Animator(Renderer3D renderer) {
		this.renderer = renderer;
		animations = new ArrayList<Animation>();
	}

	public ImagePlus render(int from, int to) {
		List<Keyframe> frames = createKeyframes(from, to);
		ImageStack stack = null;
		for(Keyframe kf : frames) {
			ImageProcessor ip = renderer.render(kf);
			if(stack == null)
				stack = new ImageStack(ip.getWidth(), ip.getHeight());
			stack.addSlice(ip);
		}

		ImagePlus ret = new ImagePlus(renderer.getImage().getTitle() + ".avi", stack);
		frames.get(0).getFwdTransform().adjustOutputCalibration(ret.getCalibration());
		ret.getCalibration().setUnit(renderer.getImage().getCalibration().getUnit());
		return ret;
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
			Keyframe kf = renderer.getKeyframe().clone();
			kf.setFrame(t);
			float[] fwd = Transform.fromIdentity(null);
			for(Animation a : animations) {
				if(a instanceof ChangeAnimation)
					System.out.println("ChangeAnimation");
				a.adjustKeyframe(kf, keyframes);

				if(a instanceof TransformationAnimation) {
					float[] x = new float[12];
					((TransformationAnimation)a).getTransformationAt(t, x);
					fwd = Transform.mul(x, fwd);
				}
			}
			kf.getFwdTransform().setTransformation(fwd);

			keyframes.add(kf);
		}
		return keyframes;
	}
}

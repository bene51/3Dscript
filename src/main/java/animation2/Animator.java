package animation2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import renderer3d.Keyframe;
import renderer3d.Transform;
import textanim.Animation;
import textanim.Renderer3D;
import textanim.TransformationAnimation;

public class Animator {

	public interface Listener {
		public void animationFinished();
	}

	private final Renderer3D renderer;
	private final List<Animation> animations;
	private final ExecutorService exec = Executors.newSingleThreadExecutor();
	private boolean stopRendering = false;
	private boolean isExecuting = false;

	public Animator(Renderer3D renderer) {
		this.renderer = renderer;
		animations = new ArrayList<Animation>();
	}

	public void render(final int from, final int to, final Listener listener) throws InterruptedException, ExecutionException {
		exec.submit(new Runnable() {
			@Override
			public void run() {
				dorender(from, to, listener);
			}
		});
	}

	public void cancelRendering() {
		stopRendering = true;
	}

	public boolean isExecuting() {
		return isExecuting;
	}

	private ImagePlus dorender(int from, int to, Listener listener) {
		isExecuting = true;
		stopRendering = false;
		List<Keyframe> frames = createKeyframes(from, to);
		ImageStack stack = null;
		ImagePlus ret = null;
		for(Keyframe kf : frames) {
			if(stopRendering)
				break;
			ImageProcessor ip = renderer.render(kf);
			if(stack == null)
				stack = new ImageStack(ip.getWidth(), ip.getHeight());
			stack.addSlice(ip);

			if(stack.size() == 2 || (stack.size() == 1 && frames.size() == 1)) {
				ret = new ImagePlus(renderer.getImage().getTitle() + ".avi", stack);
				frames.get(0).getFwdTransform().adjustOutputCalibration(ret.getCalibration());
				ret.getCalibration().setUnit(renderer.getImage().getCalibration().getUnit());
				ret.show();
			}
			if(ret != null) {
				ret.setSlice(stack.size() - 1);
				ret.updateAndDraw();
			}
		}

		isExecuting = false;
		listener.animationFinished();
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
		Keyframe previous = renderer.getKeyframe();
		for(int t = from; t <= to; t++) {
			Keyframe kf = previous.clone();
			kf.setFrame(t);
			float[] fwd = Transform.fromIdentity(null);
			for(Animation a : animations) {
				a.adjustKeyframe(kf, keyframes);

				if(a instanceof TransformationAnimation) {
					float[] x = new float[12];
					((TransformationAnimation)a).getTransformationAt(t, x);
					fwd = Transform.mul(x, fwd);
				}
			}
			kf.getFwdTransform().setTransformation(fwd);
			keyframes.add(kf);
			previous = kf;
		}
		return keyframes;
	}
}

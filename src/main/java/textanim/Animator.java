package textanim;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import renderer3d.Transform;

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
				try {
					dorender(from, to, listener);
				} catch(Exception e) {
					e.printStackTrace();
				}
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
		List<Keyframe2> frames = createKeyframes(from, to);
		ImageStack stack = null;
		ImagePlus ret = null;
		for(Keyframe2 kf : frames) {
			if(stopRendering)
				break;
//if(kf.getFrame() < 52)
//	continue;
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
				ret.setSlice(stack.size());
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

	public List<Keyframe2> createKeyframes(int from, int to) {
		List<Keyframe2> keyframes = new ArrayList<Keyframe2>();
		Keyframe2 previous = renderer.getKeyframe();
		for(int t = from; t <= to; t++) {
			Keyframe2 kf = previous.clone();
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

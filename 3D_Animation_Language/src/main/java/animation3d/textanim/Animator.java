package animation3d.textanim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import animation3d.parser.Interpreter;
import animation3d.parser.NoSuchMacroException;
import animation3d.parser.ParsingResult;
import animation3d.parser.Preprocessor;
import animation3d.parser.Preprocessor.PreprocessingException;
import animation3d.util.Transform;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class Animator {

	public interface Listener {
		public void animationFinished(ImagePlus result);
	}

	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	private final IRenderer3D renderer;
	private final List<Animation> animations;
	private final ExecutorService exec = Executors.newSingleThreadExecutor();
	private boolean stopRendering = false;
	private boolean isExecuting = false;

	private double progress;

	public Animator(IRenderer3D renderer) {
		this.renderer = renderer;
		animations = new ArrayList<Animation>();
	}

	public void addAnimationListener(Listener l) {
		this.listeners.add(l);
	}

	public void removeAnimationListener(Listener l) {
		this.listeners.remove(l);
	}

	private Future<ImagePlus> submitted = null;

	private void render(final List<RenderingState> frames) throws InterruptedException, ExecutionException {
		submitted = exec.submit(new Callable<ImagePlus>() {
			@Override
			public ImagePlus call() {
				return dorender(frames);
			}
		});
	}

	public ImagePlus waitForRendering(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return submitted.get(timeout, unit);
	}

	public void render(String text) throws NoSuchMacroException, PreprocessingException, InterruptedException, ExecutionException {
		render(text, -1, -1);
	}

	public void render(String text, int f, int t) throws NoSuchMacroException, PreprocessingException, InterruptedException, ExecutionException {
		HashMap<String, String> macros = new HashMap<String, String>();
		ArrayList<String> lines = new ArrayList<String>();

		Preprocessor.preprocess(text, lines, macros);

		float[] rotcenter = renderer.getRotationCenter();
		clearAnimations();
		int from = Integer.MAX_VALUE;
		int to = 0;

		for(String line : lines) {
			ParsingResult pr = new ParsingResult();
			Interpreter.parse(renderer.getKeywordFactory(), line, rotcenter, pr);
			from = Math.min(from, pr.getFrom());
			to   = Math.max(to, pr.getTo());
			Animation ta = pr.getResult();
			if(ta != null) {
				ta.pickScripts(macros);
				addAnimation(ta);
			}
		}
		if(f >= 0)
			from = f;
		if(t >= 0) {
			if(t < f)
				t = f;
			to = t;
		}

		List<RenderingState> frames = createRenderingStates(from, to);
		render(frames);
	}

	public void render(String text, int[] frameIndices) throws NoSuchMacroException, PreprocessingException, InterruptedException, ExecutionException {
		HashMap<String, String> macros = new HashMap<String, String>();
		ArrayList<String> lines = new ArrayList<String>();

		Preprocessor.preprocess(text, lines, macros);

		float[] rotcenter = renderer.getRotationCenter();
		clearAnimations();

		for(String line : lines) {
			ParsingResult pr = new ParsingResult();
			Interpreter.parse(renderer.getKeywordFactory(), line, rotcenter, pr);
			Animation ta = pr.getResult();
			if(ta != null) {
				ta.pickScripts(macros);
				addAnimation(ta);
			}
		}

		List<RenderingState> frames = createRenderingStates(frameIndices);
		render(frames);
	}

	public double getProgress() {
		return progress;
	}

	public void cancelRendering() {
		stopRendering = true;
	}

	public boolean isExecuting() {
		return isExecuting;
	}

	private ImagePlus dorender(List<RenderingState> frames) {
		isExecuting = true;
		stopRendering = false;
		ImageStack stack = null;
		ImagePlus ret = null;
		progress = 0;
		for(int f = 0; f < frames.size(); f++) {
			progress = f / frames.size();
			RenderingState kf = frames.get(f);
			if(stopRendering)
				break;

			int fIdx = frames.indexOf(kf);
			boolean alreadyRendered = fIdx >= 0 && fIdx < f;
			ImageProcessor ip = alreadyRendered ? stack.getProcessor(fIdx + 1).duplicate() : renderer.render(kf);

			if(stack == null)
				stack = new ImageStack(ip.getWidth(), ip.getHeight());
			stack.addSlice(Integer.toString(kf.getFrame()), ip);

			if(stack.size() == 2 || (stack.size() == 1 && frames.size() == 1)) {
				ret = new ImagePlus(renderer.getTitle() + ".avi", stack);
				// frames.get(0).getFwdTransform().adjustOutputCalibration(ret.getCalibration());
				// ret.getCalibration().setUnit(renderer.getCalibrationUnit());
				ret.show();
			}
			if(ret != null) {
				ret.setSlice(stack.size());
				ret.updateAndDraw();
			}
		}
		progress = 1;
		isExecuting = false;
		fireAnimationFinished(ret);
		return ret;
	}

	public void clearAnimations() {
		animations.clear();
	}

	public void addAnimation(Animation a) {
		animations.add(a);
	}

	public List<RenderingState> createRenderingStates(int from, int to) {
		List<RenderingState> renderingStates = new ArrayList<RenderingState>();
		RenderingState previous = renderer.getRenderingState();
		for(int t = from; t <= to; t++) {
			RenderingState kf = previous.clone();
			kf.getFwdTransform().setTransformation(Transform.fromIdentity(null));
			kf.setFrame(t);
			for(Animation a : animations)
				a.adjustRenderingState(kf, renderingStates, renderer.getNChannels());
			renderingStates.add(kf);
			previous = kf;
		}
		return renderingStates;
	}

	public List<RenderingState> createRenderingStates(int[] frameIndices) {
		List<RenderingState> renderingStates = new ArrayList<RenderingState>();
		RenderingState previous = renderer.getRenderingState();
		for(int t : frameIndices) {
			RenderingState kf = previous.clone();
			kf.getFwdTransform().setTransformation(Transform.fromIdentity(null));
			kf.setFrame(t);
			for(Animation a : animations)
				a.adjustRenderingState(kf, renderingStates, renderer.getNChannels());
			renderingStates.add(kf);
			previous = kf;
		}
		return renderingStates;
	}

	private void fireAnimationFinished(ImagePlus ret) {
		for(Listener l : listeners)
			l.animationFinished(ret);
	}
}

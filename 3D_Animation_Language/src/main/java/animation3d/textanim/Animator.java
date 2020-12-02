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

import animation3d.parser.Autocompletion;
import animation3d.parser.Interpreter;
import animation3d.parser.NoSuchMacroException;
import animation3d.parser.ParsingException;
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
	private final List<RenderingState> renderingStates;

	private double progress;

	public Animator(IRenderer3D renderer) {
		this.renderer = renderer;
		animations = new ArrayList<Animation>();
		renderingStates = new ArrayList<RenderingState>();
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

	public void render(String text) throws NoSuchMacroException, PreprocessingException, ParsingException, InterruptedException, ExecutionException {
		render(text, -1, -1);
	}

	/**
	 * Do not modify this list
	 * @param frames
	 * @return
	 */
	public List<RenderingState> getRenderingStates() {
		return renderingStates;
	}

	public void setAnimationText(String text) throws NoSuchMacroException, PreprocessingException, ParsingException {
		HashMap<String, String> macros = new HashMap<String, String>();
		ArrayList<NumberedLine> lines = new ArrayList<NumberedLine>();

		Preprocessor.preprocess(text, lines, macros);

		float[] rotcenter = renderer.getRotationCenter();
		reset();
		int from = 0;
		int to = 0;

		for(NumberedLine line : lines) {
			System.out.println("setAnimationText: line: " + line.lineno + ": " + line.text);
			ParsingResult pr = new ParsingResult();
			try {
				Interpreter.parse(renderer.getKeywordFactory(), line.text, rotcenter, pr);
			} catch(ParsingException e) {
				Autocompletion ac = pr.getAutocompletion();
				String as = ac == null ? "" : ac.toString();
				if(as.length() != 0 && !as.equals("''")) // a meaningful autocompletion, replace existing exception
					e = new ParsingException(e.getPos(), "Expected " + ac);
				e.setLine(line.lineno);
				throw e;
			}
			to = Math.max(to, pr.getTo());
			Animation ta = pr.getResult();
			if(ta != null) {
				ta.pickScripts(macros);
				addAnimation(ta);
			}
		}
		createRenderingStates(from, to);
	}

	public void render(String text, int f, int t) throws NoSuchMacroException, PreprocessingException, ParsingException, InterruptedException, ExecutionException {
		setAnimationText(text);

		int from = 0;
		int to = renderingStates.get(renderingStates.size() - 1).frame;

		if(f >= 0)
			from = f;
		if(t >= 0) {
			if(t < f)
				t = f;
			to = t;
		}

		ArrayList<RenderingState> filtered = new ArrayList<RenderingState>();
		for(int i = from; i <= to; i++)
			filtered.add(renderingStates.get(Math.min(i, renderingStates.size() - 1)));

		render(filtered);
	}

	public void render(String text, int[] frameIndices) throws NoSuchMacroException, PreprocessingException, ParsingException, InterruptedException, ExecutionException {
		setAnimationText(text);
		List<RenderingState> filtered = new ArrayList<RenderingState>();
		for(int fIdx : frameIndices)
			filtered.add(renderingStates.get(Math.min(fIdx, renderingStates.size() - 1)));
		render(filtered);
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

		for(int i = 0; i < frames.size(); i++) {
			progress = (double) i / frames.size();
			RenderingState kf = frames.get(i);
			if(stopRendering)
				break;

			int fIdx = frames.indexOf(kf);
			boolean alreadyRendered = fIdx >= 0 && fIdx < i;
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

	private void reset() {
		animations.clear();
		renderingStates.clear();
		stopRendering = false;
		isExecuting = false;
		progress = 0;
	}

	public void addAnimation(Animation a) {
		animations.add(a);
	}

	private void createRenderingStates(int from, int to) {
		renderingStates.clear();
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
	}

	private void fireAnimationFinished(ImagePlus ret) {
		for(Listener l : listeners)
			l.animationFinished(ret);
	}
}

package animation2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Animate_ implements PlugIn {

	@Override
	public void run(String args) {
		try {
			// main(null);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private final int wOut, hOut;

	private final float[] toTransform;

	private CudaRaycaster projector = null;

	private final ImageSource imageSrc;

	private List<TransformationAnimation> anims = new ArrayList<TransformationAnimation>();
	private List<Processor> procs = new ArrayList<Processor>();

	private List<RenderCallback> listeners = new ArrayList<RenderCallback>();

	private RenderingSettings[] renderingSettings;

	public Animate_(
			ImageSource imageSrc,
			int w, int h, int d,
			double pw, double ph, double pd,
			int wOut, int hOut,
			RenderingSettings[] renderingSettings) {

		this.imageSrc = imageSrc;

		this.wOut = wOut;
		this.hOut = hOut;

		this.renderingSettings = renderingSettings;

		double pwOut = w * pw / wOut;
		double phOut = h * ph / hOut;
		double pdOut = pwOut;

		double x0Out = 0, y0Out = 0, z0Out = 0;
		if(pwOut < phOut) {
			pwOut = phOut;
			x0Out = -(wOut * pwOut - w * pw) / 2;
		} else if(phOut < pwOut) {
			phOut = pwOut;
			y0Out = -(hOut - h) * phOut / 2;
		}

		toTransform = Transform.fromCalibration(
				(float)pwOut, (float)phOut, (float)pdOut, (float)x0Out, (float)y0Out, (float)z0Out, null);
		Transform.invert(toTransform);

		float zStep = 1; // TODO
		projector = new CudaRaycaster(imageSrc.getInputImage(0), wOut, hOut, zStep); // TODO just taking the first timepoint for now
	}

//	public void setProjector(CompositeRaycaster projector) {
//		this.projector = projector;
//	}

	public void add(RenderCallback callback) {
		listeners.add(callback);
	}

	public void add(TransformationAnimation trans) {
		anims.add(trans);
	}

	public void add(Processor proc) {
		procs.add(proc);
	}

	public ImageProcessor render(int t, float near, float far) {
		float[] transform = Transform.fromIdentity(null);
		for(TransformationAnimation anim : anims) {
			float[] x = new float[12];
			anim.getTransformationAt(t, x);
			transform = Transform.mul(x, transform);
		}

		ImagePlus imp = imageSrc.getInputImage(t);
		if(t > 0 && imageSrc.hasChanged(t)) {
			throw new RuntimeException("Changing image not supported at the moment");
		}

		float[] fromCalib = Transform.fromCalibration(
				(float)imp.getCalibration().pixelWidth,
				(float)imp.getCalibration().pixelHeight,
				(float)imp.getCalibration().pixelDepth,
				0, 0, 0, null);
		transform = Transform.mul(transform, fromCalib);
		transform = Transform.mul(toTransform, transform);
		Transform.invert(transform);

		for(Processor proc : procs)
			proc.process(imp, t);

		ImageProcessor ret = projector.renderAndCompose(transform, renderingSettings, near, far).getProcessor();
		System.out.println(t + ": " + Arrays.toString(transform));

		for(RenderCallback callback : listeners)
			callback.frameRendered(t);
		return ret;
	}

	public ImagePlus render(int t0, int nFrames, float near, float far) {
		ImageStack stack = new ImageStack(wOut, hOut);
		ImagePlus imp = null;
		for(int t = t0; t < t0 + nFrames; t++) {
			if(IJ.escapePressed())
				break;
			stack.addSlice("", render(t, near, far));
			if(t == t0 + 1) {
				imp = new ImagePlus("animation", stack);
				imp.show();
			}
			if(t >= t0 + 1)
				imp.setSlice(t - t0 + 1);
			IJ.showProgress(t - t0 + 1, nFrames);
			IJ.showStatus((t - t0 + 1) + "/" + nFrames);
		}
		IJ.resetEscape();
		IJ.showProgress(1);
		IJ.showStatus("");
		return new ImagePlus("animation", stack);
	}
}

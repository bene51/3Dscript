package animation3d.ext;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.IKeywordFactory;
import animation3d.textanim.IRenderer3D;
import animation3d.textanim.RenderingState;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class ExtRenderer implements IRenderer3D{

	private ExtRenderingState rs;
	private ImagePlus input;
	private int tgtW;
	private int tgtH;

	// private final ThridPartyRenderer renderer;

	public ExtRenderer(ImagePlus input) {
		this.input = input;
		// renderer = new ThirdPartyRenderer();

		float[] pdIn = new float[] {
				(float)input.getCalibration().pixelWidth,
				(float)input.getCalibration().pixelHeight,
				(float)input.getCalibration().pixelDepth
		};

		float[] pdOut = new float[] {pdIn[0], pdIn[0], pdIn[0]};

		float[] rotcenter = new float[] {
				input.getWidth()   * pdIn[0] / 2,
				input.getHeight()  * pdIn[1] / 2,
				input.getNSlices() * pdIn[2] / 2};

		CombinedTransform transformation = new CombinedTransform(pdIn, pdOut, rotcenter);

		this.rs = new ExtRenderingState(0,
				transformation);
	}

	@Override
	public IKeywordFactory getKeywordFactory() {
		return ExtKeywordFactory.getInstance();
	}

	@Override
	public RenderingState getRenderingState() {
		return rs;
	}

	@Override
	public ImageProcessor render(RenderingState rs) {
		double brightness = rs.getNonChannelProperty(ExtRenderingState.BRIGHTNESS);

		// renderer.setBrightness(brightness);

		float[] matrix = rs.getFwdTransform().calculateInverseTransform();

		// renderer.setTransformation(matrix);

		ImageProcessor output = null;

		// output = renderer.render();

		return output;
	}

	@Override
	public float[] getRotationCenter() {
		return new float[] {
				input.getWidth()   * (float)input.getCalibration().pixelWidth  / 2f,
				input.getHeight()  * (float)input.getCalibration().pixelHeight / 2f,
				input.getNSlices() * (float)input.getCalibration().pixelDepth  / 2f
		};
	}

	@Override
	public String getTitle() {
		return input.getTitle();
	}

	@Override
	public int getNChannels() {
		return input.getNChannels();
	}

	@Override
	public void setTargetSize(int w, int h) {
		this.tgtW = w;
		this.tgtH = h;
	}

	@Override
	public int getTargetWidth() {
		return tgtW;
	}

	@Override
	public int getTargetHeight() {
		return tgtH;
	}
}

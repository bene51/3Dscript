package animation3d.ext;

import animation3d.textanim.IKeywordFactory;
import animation3d.textanim.IRenderer3D;
import animation3d.textanim.RenderingState;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class ExtRenderer implements IRenderer3D{

	private ExtRenderingState rs;
	private ImagePlus input;
	private ImageProcessor clown;

	// private final ThridPartyRenderer renderer;

	public ExtRenderer(ImagePlus input) {
		this.input = input;
		this.clown = ij.IJ.openImage("http://imagej.nih.gov/ij/images/clown.jpg").getProcessor().convertToByte(true);
		// renderer = new ThirdPartyRenderer();

		this.rs = new ExtRenderingState(0);
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
		this.rs.setFrom(rs);
		double brightness = rs.getNonChannelProperty(ExtRenderingState.BRIGHTNESS);

		// float[] matrix = rs.getFwdTransform().calculateInverseTransform();
		// renderer.setTransformation(matrix);

		ImageProcessor output = input.getProcessor().duplicate();
		output.setValue(brightness);

		int rw = (int)Math.round(50 * rs.getNonChannelProperty(ExtRenderingState.SCALE_X));
		int rh = (int)Math.round(50 * rs.getNonChannelProperty(ExtRenderingState.SCALE_Y));

		if(rw <= 0 || rh <= 0)
			return output;

		int rx = (input.getWidth()  - rw) / 2;
		int ry = (input.getHeight() - rh) / 2;

		ImageProcessor insert = clown.resize(rw, rh, true);
		output.insert(insert, rx, ry);
		// output.fillRect(rx, ry, rw, rh);

		// output = renderer.render();

		return output;
	}

	@Override
	public float[] getRotationCenter() {
		return new float[] {0, 0, 0};
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
	}

	@Override
	public int getTargetWidth() {
		return input.getWidth();
	}

	@Override
	public int getTargetHeight() {
		return input.getHeight();
	}
}

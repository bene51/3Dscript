package animation3d.ij3dviewer;

import java.awt.Color;
import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GraphicsConfigTemplate3D;
import org.scijava.java3d.ImageComponent;
import org.scijava.java3d.ImageComponent2D;
import org.scijava.java3d.Screen3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Color3f;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import renderer3d.Transform;
import textanim.CombinedTransform;
import textanim.IKeywordFactory;
import textanim.IRenderer3D;
import textanim.RenderingState;

public class IJ3DRenderer implements IRenderer3D {

	private IJ3DRenderingState rs;
	private ImagePlus input;
	private int tgtW;
	private int tgtH;
	private final Image3DUniverse viewer;
	private Content content;

	private TransformGroup animateTG;
	private Transform3D animateTF = new Transform3D();

	private Canvas3D offCanvas;

	public static void main(String[] args) {
		ImagePlus imp = ij.IJ.openImage("D:\\flybrain.green.tif");
		imp.show();
		IJ3DRenderer renderer = new IJ3DRenderer(imp);
		renderer.takeSnapshot().show();
	}

	public IJ3DRenderer(ImagePlus input) {
		this.input = input;
		viewer = new Image3DUniverse(512, 512);
		content = viewer.addVoltex(input);
		viewer.show();
		this.tgtW = viewer.getCanvas().getWidth();
		this.tgtH = viewer.getCanvas().getHeight();

		animateTG   = viewer.getAnimationTG();

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
		Color color = content.getColor() == null ? null : content.getColor().get();
		this.rs = new IJ3DRenderingState(0,
				content.getType(),
				color,
				content.getTransparency(),
				content.getThreshold(),
				transformation);

		defaultOrientation.setIdentity();
		defaultOrientation.rotX(Math.PI);
	}

	@Override
	public IKeywordFactory getKeywordFactory() {
		return IJ3DKeywordFactory.getInstance();
	}

	@Override
	public RenderingState getRenderingState() {
		return rs;
	}

	private Transform3D defaultOrientation = new Transform3D();

	@Override
	public ImageProcessor render(RenderingState kf) {
		int displayMode    =   (int)kf.getNonChannelProperty(IJ3DRenderingState.DISPLAY_MODE);
		int red            =   (int)kf.getNonChannelProperty(IJ3DRenderingState.COLOR_RED);
		int green          =   (int)kf.getNonChannelProperty(IJ3DRenderingState.COLOR_GREEN);
		int blue           =   (int)kf.getNonChannelProperty(IJ3DRenderingState.COLOR_BLUE);
		float transparency = (float)kf.getNonChannelProperty(IJ3DRenderingState.TRANSPARENCY);
		int threshold      =   (int)kf.getNonChannelProperty(IJ3DRenderingState.THRESHOLD);

		Color3f color = (red < 0 || green < 0 || blue < 0) ? null : new Color3f(red / 255f, green / 255f, blue / 255f);

		content.setThreshold(threshold);
		content.setTransparency(transparency);
		content.setColor(color);
		if(content.getType() != displayMode)
			content.displayAs(displayMode);

		CombinedTransform transform = kf.getFwdTransform();
		float[] translation = transform.getTranslation();
		float[] rotation    = transform.getRotation();
		float scale         = transform.getScale();

		if(Math.abs(scale - 1) > 1e-3)
			throw new RuntimeException("View zooming is now allowed with the 3D Viewer, please move in Z instead.");

		float[] fwd = rotation.clone();
		Transform.applyTranslation(translation[0], translation[1], translation[2], fwd);


		Transform.invert(fwd);
		float[] m = new float[16];
		System.arraycopy(fwd, 0, m, 0, 12);
		m[15] = 1;
		animateTF.set(m);
		animateTG.setTransform(animateTF);

		viewer.fireTransformationUpdated();

		return takeSnapshot().getProcessor();
	}

	public ImagePlus takeSnapshot() {
		final Canvas3D onCanvas = viewer.getCanvas();
		if (offCanvas == null) {
			final GraphicsConfigTemplate3D templ = new GraphicsConfigTemplate3D();
			templ.setDoubleBuffer(GraphicsConfigTemplate.UNNECESSARY);
			final GraphicsConfiguration gc =
				GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getBestConfiguration(templ);
			offCanvas = new Canvas3D(gc, true);
			System.out.println("construct canvas");
		}
		final Screen3D sOn = onCanvas.getScreen3D();
		final Screen3D sOff = offCanvas.getScreen3D();
		sOff.setSize(sOn.getSize());
		sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth());
		sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight());
		viewer.getViewer().getView().removeCanvas3D(onCanvas);
		viewer.getViewer().getView().addCanvas3D(offCanvas);

		BufferedImage bImage = new BufferedImage(tgtW, tgtH, BufferedImage.TYPE_INT_ARGB);
		final ImageComponent2D ic2d =
			new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);
		ic2d.setCapability(ImageComponent2D.ALLOW_IMAGE_READ);

		offCanvas.setOffScreenBuffer(ic2d);
		offCanvas.renderOffScreenBuffer();
		offCanvas.waitForOffScreenRendering();

		bImage = offCanvas.getOffScreenBuffer().getImage();
		offCanvas.setOffScreenBuffer(null);

		viewer.getViewer().getView().removeCanvas3D(offCanvas);
		viewer.getViewer().getView().addCanvas3D(onCanvas);
		return new ImagePlus("Snapshot", bImage);
	}

	@Override
	public ImagePlus getImage() {
		return input;
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

package animation3d.bdv;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.File;
import java.util.Arrays;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.IKeywordFactory;
import animation3d.textanim.IRenderer3D;
import animation3d.textanim.RenderingState;
import animation3d.util.Transform;
import bdv.BigDataViewer;
import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.ViewerState;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.LUT;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class BDVRenderer implements IRenderer3D {

//	private int tgtW;
//	private int tgtH;

	private BDVRenderingState rs;
	private BigDataViewer viewer;
	private ViewerOptions options;

	float[] rotationCenter;

	private static float[] calculateForwardTransform(CombinedTransform ct) {
		float scale = ct.getScale();
		float[] rotation = ct.getRotation();
		float[] translation = ct.getTranslation();
		float[] center = ct.getCenter();

		float[] scaleM = Transform.fromScale(scale, scale, scale, null);
		float[] centerM = Transform.fromTranslation(-center[0], -center[1], -center[2], null);

		float[] x = Transform.mul(scaleM, Transform.mul(rotation, centerM));
		Transform.applyTranslation(center[0], center[1], /* center[2] */0, x);
		Transform.applyTranslation(translation[0], translation[1], translation[2], x);

		return x;
	}

	private static double[] toDouble(float[] arr) {
		double[] ret = new double[arr.length];
		for(int i = 0; i < arr.length; i++)
			ret[i] = arr[i];
		return ret;
	}

	// public BDVRenderer(ImagePlus input) {
	public BDVRenderer(File xmlFile) throws SpimDataException {
		openBDV(xmlFile);
//		this.tgtW = 0; // TODO
//		this.tgtH = 0; // TODO

		float[] pdOut = new float[] {1, 1, 1};
		float[] pdIn  = new float[] {1, 1, 1};

		this.rotationCenter = calculateRotationCenter();
		System.out.println("rotcenter: " + Arrays.toString(rotationCenter));
		float[] rotcenter = rotationCenter.clone();

		CombinedTransform transformation = new CombinedTransform(pdIn, pdOut, rotcenter);
		DisplayMode mode = viewer.getViewer().getVisibilityAndGrouping().getDisplayMode();
		int tp = viewer.getViewer().getState().getCurrentTimepoint();
		Interpolation interpolation = viewer.getViewer().getState().getInterpolation();
		int currentSource = viewer.getViewer().getState().getCurrentSource();
		this.rs = new BDVRenderingState(0,
				mode,
				tp,
				interpolation,
				currentSource, // current source
				transformation);

	}

	@Override
	public IKeywordFactory getKeywordFactory() {
		return BDVKeywordFactory.getInstance();
	}

	@Override
	public RenderingState getRenderingState() {
		return rs;
	}

	@Override
	public ImageProcessor render(RenderingState kf) {
		BDVRenderingState bkf = (BDVRenderingState)kf;
		ViewerPanel panel = viewer.getViewer();
		ViewerState state = panel.getState();

		DisplayMode displaymode = bkf.getDisplayMode();
		Interpolation interpolation = bkf.getInterpolation();
		int timepoint = bkf.getTimepoint();
		int currentSource = bkf.getCurrentSource();



		panel.setDisplayMode(displaymode);
		if(state.getInterpolation() != interpolation)
			panel.toggleInterpolation();
		panel.setTimepoint(timepoint);
		panel.getVisibilityAndGrouping().setCurrentSource(currentSource);

		CombinedTransform transform = kf.getFwdTransform();

		float[] tmp = calculateForwardTransform(transform);


		int w = panel.getWidth();
		int h = panel.getHeight();

		double rw = 2 * rotationCenter[0];
		double rh = 2 * rotationCenter[1];
		double scaleX = w / rw;
		double scaleY = h / rh;
		double scale = Math.min(scaleX, scaleY);
		rw = rw * scale;
		rh = rh * scale;
		double tx = (w - rw) / 2;
		double ty = (h - rh) / 2;

		AffineTransform3D affine = new AffineTransform3D();

		affine.set(toDouble(tmp));

		affine.scale(scale);
		affine.translate(tx, ty, 0);

		panel.setCurrentViewerTransform(affine);

		return takeSnapshot().getProcessor();
	}

	public ImagePlus takeSnapshot() {
		ViewerFrame win = viewer.getViewerFrame();
		win.toFront();
		IJ.wait(500);
		Rectangle bounds = win.getBounds();
		Rectangle r = bounds;
		ImagePlus imp = null;
		Image img = null;
		try {
			Robot robot = new Robot();
			img = robot.createScreenCapture(r);
		} catch(Exception e) { }
		if (img != null) {
			imp = new ImagePlus("", img);
		}
		return imp;
	}

	private float[] calculateRotationCenter() {
		ViewerPanel panel = viewer.getViewer();
		final RealPoint gPos = new RealPoint(3);

		panel.displayToGlobalCoordinates(panel.getWidth() / 2, panel.getHeight() / 2, gPos);

		float[] ret = new float[3];
		gPos.localize(ret);

		System.out.println("rotc is " + ret[0] + ", " + ret[1] + ", " + ret[2]);
		return ret;
	}

	@Override
	public float[] getRotationCenter() {
		return rotationCenter;
	}

	@Override
	public String getTitle() {
		return "";
	}

	@Override
	public int getNChannels() {
		ViewerPanel panel = viewer.getViewer();
		ViewerState state = panel.getState();
		return state.numSources();
	}

	@Override
	public void setTargetSize(int w, int h) {
//		this.tgtW = w;
//		this.tgtH = h;
		// TODO
	}

	@Override
	public int getTargetWidth() {
		return viewer.getViewer().getWidth();
	}

	@Override
	public int getTargetHeight() {
		return viewer.getViewer().getHeight();
	}

	public void openBDV(File xmlFile) throws SpimDataException {
		options = ViewerOptions.options();
		viewer = BigDataViewer.open(xmlFile.getAbsolutePath(), xmlFile.getName(), new ProgressWriterIJ(), options);
	}

//	public void openBDV(ImagePlus imp) {
//		if (ij.Prefs.setIJMenuBar)
//			System.setProperty("apple.laf.useScreenMenuBar", "true");
//
//		// make sure there is one
//		if (imp == null) {
//			IJ.showMessage("Please open an image first.");
//			return;
//		}
//
//		// check the image type
//		switch (imp.getType()) {
//		case ImagePlus.GRAY8:
//		case ImagePlus.GRAY16:
//		case ImagePlus.GRAY32:
//		case ImagePlus.COLOR_RGB:
//			break;
//		default:
//			IJ.showMessage("Only 8, 16, 32-bit images and RGB images are supported currently!");
//			return;
//		}
//
//		// check the image dimensionality
//		if (imp.getNDimensions() < 3) {
//			IJ.showMessage("Image must be at least 3-dimensional!");
//			return;
//		}
//
//		// get calibration and image size
//		final double pw = imp.getCalibration().pixelWidth;
//		final double ph = imp.getCalibration().pixelHeight;
//		final double pd = imp.getCalibration().pixelDepth;
//		String punit = imp.getCalibration().getUnit();
//		if (punit == null || punit.isEmpty())
//			punit = "px";
//		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions(punit, pw, ph, pd);
//		final int w = imp.getWidth();
//		final int h = imp.getHeight();
//		final int d = imp.getNSlices();
//		final FinalDimensions size = new FinalDimensions(new int[] { w, h, d });
//
//		// propose reasonable mipmap settings
//		// final ExportMipmapInfo autoMipmapSettings =
//		// ProposeMipmaps.proposeMipmaps( new BasicViewSetup( 0, "", size,
//		// voxelSize ) );
//
//		// imp.getDisplayRangeMin();
//		// imp.getDisplayRangeMax();
//
//		// create ImgLoader wrapping the image
//		final BasicImgLoader imgLoader;
//		if (imp.getStack().isVirtual()) {
//			switch (imp.getType()) {
//			case ImagePlus.GRAY8:
//				imgLoader = VirtualStackImageLoader.createUnsignedByteInstance(imp);
//				break;
//			case ImagePlus.GRAY16:
//				imgLoader = VirtualStackImageLoader.createUnsignedShortInstance(imp);
//				break;
//			case ImagePlus.GRAY32:
//				imgLoader = VirtualStackImageLoader.createFloatInstance(imp);
//				break;
//			case ImagePlus.COLOR_RGB:
//			default:
//				imgLoader = VirtualStackImageLoader.createARGBInstance(imp);
//				break;
//			}
//		} else {
//			switch (imp.getType()) {
//			case ImagePlus.GRAY8:
//				imgLoader = ImageStackImageLoader.createUnsignedByteInstance(imp);
//				break;
//			case ImagePlus.GRAY16:
//				imgLoader = ImageStackImageLoader.createUnsignedShortInstance(imp);
//				break;
//			case ImagePlus.GRAY32:
//				imgLoader = ImageStackImageLoader.createFloatInstance(imp);
//				break;
//			case ImagePlus.COLOR_RGB:
//			default:
//				imgLoader = ImageStackImageLoader.createARGBInstance(imp);
//				break;
//			}
//		}
//
//		final int numTimepoints = imp.getNFrames();
//		final int numSetups = imp.getNChannels();
//
//		// create setups from channels
//		final HashMap<Integer, BasicViewSetup> setups = new HashMap<>(numSetups);
//		for (int s = 0; s < numSetups; ++s) {
//			final BasicViewSetup setup = new BasicViewSetup(s, String.format("channel %d", s + 1), size, voxelSize);
//			setup.setAttribute(new Channel(s + 1));
//			setups.put(s, setup);
//		}
//
//		// create timepoints
//		final ArrayList<TimePoint> timepoints = new ArrayList<>(numTimepoints);
//		for (int t = 0; t < numTimepoints; ++t)
//			timepoints.add(new TimePoint(t));
//		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal(new TimePoints(timepoints), setups,
//				imgLoader, null);
//
//		// create ViewRegistrations from the images calibration
//		final AffineTransform3D sourceTransform = new AffineTransform3D();
//		sourceTransform.set(pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0);
//		final ArrayList<ViewRegistration> registrations = new ArrayList<>();
//		for (int t = 0; t < numTimepoints; ++t)
//			for (int s = 0; s < numSetups; ++s)
//				registrations.add(new ViewRegistration(t, s, sourceTransform));
//
//		final File basePath = new File(".");
//		final SpimDataMinimal spimData = new SpimDataMinimal(basePath, seq, new ViewRegistrations(registrations));
//		WrapBasicImgLoader.wrapImgLoaderIfNecessary(spimData);
//
//		options = ViewerOptions.options();
//		viewer = BigDataViewer.open(spimData, "BigDataViewer", new ProgressWriterIJ(),
//				options);
//		final SetupAssignments sa = viewer.getSetupAssignments();
//		final VisibilityAndGrouping vg = viewer.getViewer().getVisibilityAndGrouping();
//		if (imp.isComposite())
//			transferChannelSettings((CompositeImage) imp, sa, vg);
//		else
//			transferImpSettings(imp, sa);
//	}

	protected void transferChannelSettings(final CompositeImage ci, final SetupAssignments setupAssignments,
			final VisibilityAndGrouping visibility) {
		final int nChannels = ci.getNChannels();
		final int mode = ci.getCompositeMode();
		final boolean transferColor = mode == IJ.COMPOSITE || mode == IJ.COLOR;
		for (int c = 0; c < nChannels; ++c) {
			final LUT lut = ci.getChannelLut(c + 1);
			final ConverterSetup setup = setupAssignments.getConverterSetups().get(c);
			if (transferColor)
				setup.setColor(new ARGBType(lut.getRGB(255)));
			setup.setDisplayRange(lut.min, lut.max);
		}
		if (mode == IJ.COMPOSITE) {
			final boolean[] activeChannels = ci.getActiveChannels();
			visibility.setDisplayMode(DisplayMode.FUSED);
			for (int i = 0; i < activeChannels.length; ++i)
				visibility.setSourceActive(i, activeChannels[i]);
		} else
			visibility.setDisplayMode(DisplayMode.SINGLE);
		visibility.setCurrentSource(ci.getChannel() - 1);
	}

	protected void transferImpSettings(final ImagePlus imp, final SetupAssignments setupAssignments) {
		final ConverterSetup setup = setupAssignments.getConverterSetups().get(0);
		setup.setDisplayRange(imp.getDisplayRangeMin(), imp.getDisplayRangeMax());
	}
}

package animation3d.bdv;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.IKeywordFactory;
import animation3d.textanim.IRenderer3D;
import animation3d.textanim.RenderingState;
import animation3d.util.Transform;
import bdv.BigDataViewer;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.cache.CacheControl;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.Affine3DHelpers;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.LUT;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Cursor;
import net.imglib2.RealPoint;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.util.LinAlgHelpers;

public class BDVRenderer implements IRenderer3D {
	
	private BDVRenderingState rs;
	private int tgtW;
	private int tgtH;
	private final ViewerPanel viewer;
	private final SetupAssignments setupAssignments;

	final float[] rotationCenter;
	
	private float[] calculateRotationCenter() {
		final RealPoint gPos = new RealPoint(3);
		viewer.displayToGlobalCoordinates(tgtW/2, tgtH/2, gPos);

		float[] ret = new float[3];
		gPos.localize(ret);
		
		return ret;
	}

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

	public BDVRenderer(final BigDataViewer bdv) throws SpimDataException {
		viewer = bdv.getViewer();
		
		tgtW = viewer.getDisplay().getWidth();
		tgtH = viewer.getDisplay().getHeight();

		setupAssignments = bdv.getSetupAssignments();
			
		float[] pdOut = new float[] {1, 1, 1};
		float[] pdIn  = new float[] {1, 1, 1};

		rotationCenter = calculateRotationCenter();
		System.out.println("rotc: " + Arrays.toString(rotationCenter));

		CombinedTransform transformation = new CombinedTransform(pdIn, pdOut, rotationCenter);
		DisplayMode mode = viewer.getVisibilityAndGrouping().getDisplayMode();
		ViewerState state = viewer.getState();
		int tp = state.getCurrentTimepoint();
		Interpolation interpolation = state.getInterpolation();
		int currentSource = state.getCurrentSource();
		int nSources = state.numSources();
		double[] min = new double[nSources];
		double[] max = new double[nSources];
		Color[] colors = new Color[nSources];

		for(int s = 0; s < nSources; s++) {
			final ConverterSetup setup = setupAssignments.getConverterSetups().get(s);
			colors[s] = new Color(setup.getColor().get());
			min[s] = setup.getDisplayRangeMin();
			max[s] = setup.getDisplayRangeMax();
		}

		this.rs = 
				new BDVRenderingState(0,
				mode,
				tp,
				interpolation,
				currentSource, // current source
				transformation,
				colors,
				min,
				max);

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
	public ImageProcessor render(RenderingState rs) {
		final ViewerState renderState = viewer.getState();
		
		final CombinedTransform transform = rs.getFwdTransform();
		float[] tmp = transform.calculateForwardTransform();
		final AffineTransform3D fwd = new AffineTransform3D();
		fwd.set(toDouble(tmp));
		
		final AffineTransform3D c = new AffineTransform3D();
		renderState.getViewerTransform(c);
		
		final double[] qTarget = new double[ 4 ];
		Affine3DHelpers.extractRotation( fwd, qTarget );
		viewer.setTransformAnimator(
				new RotationAnimator(c, tgtW/2, tgtH/2, qTarget, 0));
		
		/*
		CombinedTransform transform = rs.getFwdTransform();
		float[] translation = transform.getTranslation();
		float[] rotation    = transform.getRotation();
		float scale         = transform.getScale();
		
		final AffineTransform3D c = new AffineTransform3D();
		renderState.getViewerTransform(c);
		
		double rcw = rotationCenter[0];
		double rch = rotationCenter[1];
		//NOTE extract?
		viewer.setTransformAnimator(
				new RotationAnimator(c, rcw, rch, toDouble(rotation), 0));
		*/
			
		/*
		double rw = 2 * rotationCenter[0];
		double rh = 2 * rotationCenter[1];
		double scaleX = tgtW / rw;
		double scaleY = tgtH / rh;
		double scale = Math.min(scaleX, scaleY);
		affine.scale(scale);
		
		rw *= scale;
		rh *= scale;
		double tx = (tgtW - rw) / 2;
		double ty = (tgtH - rh) / 2;
		affine.translate(tx, ty, 0);
		*/
		//viewer.setCurrentViewerTransform(affine);
		
		/*
		BDVRenderingState bkf = (BDVRenderingState)kf;
					
		for(int s = 0; s < state.numSources(); s++) {
			final ConverterSetup setup = setupAssignments.getConverterSetups().get(s);
			setup.setColor(new ARGBType(bkf.getChannelColor(s).getRGB()));
			setup.setDisplayRange(bkf.getChannelMin(s), bkf.getChannelMax(s));
		}

		DisplayMode displaymode = bkf.getDisplayMode();
		Interpolation interpolation = bkf.getInterpolation();
		int timepoint = bkf.getTimepoint();
		int currentSource = bkf.getCurrentSource();
		
		viewer.setDisplayMode(displaymode);
		if(state.getInterpolation() != interpolation)
			viewer.toggleInterpolation();
		viewer.setTimepoint(timepoint);
		viewer.getVisibilityAndGrouping().setCurrentSource(currentSource);

		CombinedTransform transform = kf.getFwdTransform();

		float[] tmp = calculateForwardTransform(transform);

		double rw = 2 * rotationCenter[0];
		double rh = 2 * rotationCenter[1];
		double scaleX = tgtW / rw;
		double scaleY = tgtH / rh;
		double scale = Math.min(scaleX, scaleY);
		rw = rw * scale;
		rh = rh * scale;
		double tx = (tgtW - rw) / 2;
		double ty = (tgtH - rh) / 2;

		AffineTransform3D affine = new AffineTransform3D();

		affine.set(toDouble(tmp));

		affine.scale(scale);
		affine.translate(tx, ty, 0);

		viewer.setCurrentViewerTransform(affine);
		*/

		return takeSnapshot().getProcessor();
	}

	public ImagePlus takeSnapshot() {
		System.out.println("takeSnapshot()");
		final ViewerState renderState = viewer.getState();
		
		final AffineTransform3D tGV = new AffineTransform3D();
		renderState.getViewerTransform( tGV );
		/*
		tGV.set( tGV.get( 0, 3 ) - canvasW / 2, 0, 3 );
		tGV.set( tGV.get( 1, 3 ) - canvasH / 2, 1, 3 );
		tGV.scale( ( double ) width / canvasW );
		tGV.set( tGV.get( 0, 3 ) + width / 2, 0, 3 );
		tGV.set( tGV.get( 1, 3 ) + height / 2, 1, 3 );
		*/
		
		final AffineTransform3D affine = new AffineTransform3D();
		
		// get voxel width transformed to current viewer coordinates
		final AffineTransform3D tSV = new AffineTransform3D();
		renderState.getSources().get( 0 ).getSpimSource().getSourceTransform( 0, 0, tSV );
		final double[] sO = new double[] { 0, 0, 0 };
		final double[] sX = new double[] { 1, 0, 0 };
		final double[] vO = new double[ 3 ];
		final double[] vX = new double[ 3 ];
		tSV.apply( sO, vO );
		tSV.apply( sX, vX );
		LinAlgHelpers.subtract( vO, vX, vO );
		final double dd = LinAlgHelpers.length( vO );
		
		// MIP renderer
		class MyTarget implements RenderTarget {
			final ARGBScreenImage accumulated;
			
			public MyTarget() {
				accumulated = new ARGBScreenImage(tgtW, tgtH);
			}
			
			public void clear() {
				for (final ARGBType acc : accumulated) {
					acc.setZero();
				}
			}
			
			@Override
			public BufferedImage setBufferedImage(final BufferedImage bufferedImage) {
				final Img< ARGBType > argbs = ArrayImgs.argbs( ( ( DataBufferInt ) bufferedImage.getData().getDataBuffer() ).getData(), tgtW, tgtH );
				final Cursor< ARGBType > c = argbs.cursor();
				for ( final ARGBType acc : accumulated )
				{
					final int current = acc.get();
					final int in = c.next().get();
					acc.set( ARGBType.rgba(
							Math.max( ARGBType.red( in ), ARGBType.red( current ) ),
							Math.max( ARGBType.green( in ), ARGBType.green( current ) ),
							Math.max( ARGBType.blue( in ), ARGBType.blue( current ) ),
							Math.max( ARGBType.alpha( in ), ARGBType.alpha( current ) )	) );
				}
				return null;
			}
			
			@Override
			public final int getWidth() {
				return tgtW;
			}

			@Override
			public int getHeight() {
				return tgtH;
			}
		}
		
		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, 
				new PainterThread( null ), 
				new double[] { 1 }, 
				0, 
				false, 
				16, //1, // n_rendering_thread 
				null, 
				false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), 
				//new CacheControl.Dummy() 
				new VolatileGlobalCellCache(1, 8) // cache (n_mip_level, n_thread)
			);
		
		//FIXME
		final int numStep = 500;
		final int stepSize = 1;
		
		target.clear();
		for (int step = 0; step < numStep; ++step) {
			affine.set(
					1, 0, 0, 0,
					0, 1, 0, 0,
					0, 0, 1, -dd * stepSize * step );
			affine.concatenate( tGV );
			renderState.setViewerTransform( affine );
			renderer.requestRepaint();
			renderer.paint( renderState );
		}
		
		final BufferedImage bImage = target.accumulated.image();
		return new ImagePlus("Snapshot", bImage);
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
		ViewerState state = viewer.getState();
		return state.numSources();
	}

	@Override
	public void setTargetSize(int w, int h) {
		tgtW = w;
		tgtH = h;
	}

	@Override
	public int getTargetWidth() {
		return tgtW;
	}

	@Override
	public int getTargetHeight() {
		return tgtH;
	}

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

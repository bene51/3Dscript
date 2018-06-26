package animation3d.ij3dviewer;

import java.awt.Color;

import animation3d.textanim.CombinedTransform;
import animation3d.textanim.RenderingState;

public class IJ3DRenderingState extends RenderingState {

	public static final int DISPLAY_MODE = 0;
	public static final int COLOR_RED    = 1;
	public static final int COLOR_GREEN  = 2;
	public static final int COLOR_BLUE   = 3;
	public static final int TRANSPARENCY = 4;
	public static final int THRESHOLD    = 5;

	public IJ3DRenderingState(int frame, CombinedTransform fwdTransform) {
		super(frame, fwdTransform);
		nonChannelProperties = new double[6];
		channelProperties = null;
	}

	public IJ3DRenderingState(
			int frame,
			int displayMode,
			Color color,
			double transparency,
			int threshold,
			CombinedTransform fwdTransform) {
		this(frame, fwdTransform);

		int red   = color != null ? color.getRed()   : -1;
		int green = color != null ? color.getGreen() : -1;
		int blue  = color != null ? color.getBlue()  : -1;

		nonChannelProperties[DISPLAY_MODE] = displayMode;
		nonChannelProperties[COLOR_RED]    = red;
		nonChannelProperties[COLOR_GREEN]  = green;
		nonChannelProperties[COLOR_BLUE]   = blue;
		nonChannelProperties[TRANSPARENCY] = transparency;
		nonChannelProperties[THRESHOLD]    = threshold;
	}

	@Override
	public IJ3DRenderingState clone() {
		IJ3DRenderingState kf = new IJ3DRenderingState(0, null);
		kf.setFrom(this);
		return kf;
	}
}

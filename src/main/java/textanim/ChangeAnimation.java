package textanim;

import java.util.List;

import parser.NumberOrMacro;
import renderer3d.Keyframe;
import renderer3d.Keyframe.KeyframeProperty;

public class ChangeAnimation extends Animation {

	private int[] timelineIndices;
	private NumberOrMacro[] vTos;

	public ChangeAnimation(int fromFrame, int toFrame, int timelineIdx, NumberOrMacro vTo) {
		super(fromFrame, toFrame);
		this.timelineIndices = new int[] { timelineIdx };
		this.vTos = new NumberOrMacro[] { vTo };
	}

	public ChangeAnimation(int fromFrame, int toFrame, int[] timelineIdx, NumberOrMacro[] vTo) {
		super(fromFrame, toFrame);
		this.timelineIndices = timelineIdx;
		this.vTos = vTo;
	}

	@Override
	protected NumberOrMacro[] getNumberOrMacros() {
		return vTos;
	}

	/**
	 * t = current.getFrame
	 *
	 * If t >  to,               return previous value
	 * If t <  from,             return previous value
	 * If t == from,
	 *            if t == to,    return tgt value
	 *            else           interpolate
	 * If t >  from and t <= to: interpolate
	 *
	 *
	 * interpolate: return linear interpolation of (fromFrame, vFrom, toFrame, vTo),
	 *              where vFrom is the value at fromFrame.
	 */
	@Override
	public void adjustKeyframe(Keyframe current, List<Keyframe> previous) {
		int t = current.getFrame();
		if(t < fromFrame || t > toFrame)
			return;
		for(int i = 0; i < timelineIndices.length; i++) {
			int timelineIdx = timelineIndices[i];
			NumberOrMacro vTo = vTos[i];

			KeyframeProperty kfpCurr = current.getRenderingProperties()[timelineIdx];

			// if it's a macro, just set the value to the macro evaluation
			if(vTo.isMacro()) {
				kfpCurr.setValue(vTo.evaluateMacro(t, fromFrame, toFrame));
				return;
			}

			double valFrom = -1;
			double valTo = vTo.getValue();
			// otherwise, let's see if there exists a value at fromFrame; if not
			// just use the same value as the target value, unless it's t = fromFrame
			if(t == fromFrame)
				valFrom = kfpCurr.getValue();
			else {
				Keyframe kfFrom = previous.get(fromFrame);
				KeyframeProperty kfpFrom = kfFrom.getRenderingProperties()[timelineIdx];
				valFrom = kfpFrom.getValue();
			}

			// gives precedence to valTo
			kfpCurr.setValue(super.interpolate(current.getFrame(), valFrom, valTo));
		}
	}
}

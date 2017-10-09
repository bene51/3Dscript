package textanim;

import java.util.List;

import parser.NumberOrMacro;

public class ChangeAnimation extends Animation {

	private int[] timelineIndices;
	private NumberOrMacro[] vTos;
	private int channel;

	/**
	 * If channel < 0, it's a non-channel property
	 */
	public ChangeAnimation(int fromFrame, int toFrame, int channel, int timelineIdx, NumberOrMacro vTo) {
		super(fromFrame, toFrame);
		this.channel = channel;
		this.timelineIndices = new int[] { timelineIdx };
		this.vTos = new NumberOrMacro[] { vTo };
	}

	public ChangeAnimation(int fromFrame, int toFrame, int channel, int[] timelineIdx, NumberOrMacro[] vTo) {
		super(fromFrame, toFrame);
		this.channel = channel;
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
	public void adjustRenderingState(RenderingState current, List<RenderingState> previous) {
		int t = current.getFrame();
		if(t < fromFrame || t > toFrame)
			return;
		for(int i = 0; i < timelineIndices.length; i++) {
			int timelineIdx = timelineIndices[i];
			NumberOrMacro vTo = vTos[i];

			// if it's a macro, just set the value to the macro evaluation
			if(vTo.isMacro()) {
				setRenderingStateProperty(current, timelineIdx, channel, vTo.evaluateMacro(t, fromFrame, toFrame));
				return;
			}

			double valFrom = -1;
			double valTo = vTo.getValue();
			// otherwise, let's see if there exists a value at fromFrame; if not
			// just use the same value as the target value, unless it's t = fromFrame
			if(t == fromFrame)
				valFrom = getRenderingStateProperty(current, timelineIdx, channel);
			else {
				RenderingState kfFrom = previous.get(fromFrame);
				valFrom = getRenderingStateProperty(kfFrom, timelineIdx, channel);
			}

			// gives precedence to valTo
			setRenderingStateProperty(current, timelineIdx, channel, super.interpolate(current.getFrame(), valFrom, valTo));
		}
	}

	private double getRenderingStateProperty(RenderingState rs, int property, int channel) {
		if(channel < 0)
			return rs.getNonchannelProperty(property);
		return rs.getChannelProperty(channel, property);
	}

	private void setRenderingStateProperty(RenderingState rs, int property, int channel, double v) {
		if(channel < 0)
			rs.setNonchannelProperty(property, v);
		else
			rs.setChannelProperty(channel, property, v);
	}
}

package textanim;

public class RenderingState implements Comparable<RenderingState> {


	public static int UNSET = Integer.MIN_VALUE;

	protected int frame;

	protected CombinedTransform fwdTransform;

	protected double[] nonChannelProperties;
	protected double[][] channelProperties;

	public RenderingState(int frame) {
		this.frame = frame;
	}

	public RenderingState(
			int frame,
			CombinedTransform fwdTransform) {
		super();
		this.frame = frame;
		this.fwdTransform = fwdTransform;

		nonChannelProperties = null;
		channelProperties = null;
	}

	public double getChannelProperty(int channel, int channelProperty) {
		return channelProperties[channel][channelProperty];
	}

	public double getNonchannelProperty(int nonchannelProperty) {
		return nonChannelProperties[nonchannelProperty];
	}

	public void setChannelProperty(int channel, int channelProperty, double v) {
		channelProperties[channel][channelProperty] = v;
	}

	public void setNonchannelProperty(int nonchannelProperty, double v) {
		nonChannelProperties[nonchannelProperty] = v;
	}

	@Override
	public RenderingState clone() {
		RenderingState kf = new RenderingState(0, null);
		kf.setFrom(this);
		return kf;
	}

	public void setFrom(RenderingState o) {
		this.frame = o.frame;
		if(o.fwdTransform != null) {
			this.fwdTransform = o.fwdTransform.clone();
		} else {
			this.fwdTransform = null;
		}
		if(o.channelProperties == null)
			this.channelProperties = null;
		else {
			if(this.channelProperties == null || this.channelProperties.length != o.channelProperties.length)
				this.channelProperties = new double[o.channelProperties.length][o.channelProperties[0].length];
			for(int c = 0; c < this.channelProperties.length; c++)
				System.arraycopy(o.channelProperties[c], 0, this.channelProperties[c], 0, this.channelProperties[0].length);
		}
		if(o.nonChannelProperties == null)
			this.nonChannelProperties = null;
		else {
			System.arraycopy(o.nonChannelProperties, 0, this.nonChannelProperties, 0, this.nonChannelProperties.length);
		}

	}

	public void setFwdTransform(CombinedTransform fwd) {
		this.fwdTransform = fwd;
	}

	public CombinedTransform getFwdTransform() {
		return this.fwdTransform;
	}

	public int getFrame() {
		return frame;
	}

	public void setFrame(int frame) {
		this.frame = frame;
	}

	@Override
	public int compareTo(RenderingState o) {
		if(frame < o.frame) return -1;
		if(frame > o.frame) return +1;
		return 0;
	}
}

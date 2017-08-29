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

	@Override
	public void adjustKeyframe(Keyframe current, List<Keyframe> previous) {
		for(int i = 0; i < timelineIndices.length; i++) {
			int timelineIdx = timelineIndices[i];
			NumberOrMacro vTo = vTos[i];

			KeyframeProperty kfpCurr = current.getRenderingProperties()[timelineIdx];

			// if it's a macro, just set the value to the macro evaluation
			if(vTo.isMacro()) {
				kfpCurr.setValue(vTo.evaluateMacro(current.getFrame(), fromFrame, toFrame));
				return;
			}

			double valFrom = -1;
			double valTo = vTo.getValue();
			// otherwise, let's see if there exists a value at fromFrame; if not
			// just use the same value as the target value
			Keyframe kfFrom = null;
			if(previous == null || previous.size() <= fromFrame || (kfFrom = previous.get(fromFrame)) == null) {
				valFrom = valTo;
			}
			else {
				KeyframeProperty kfpFrom = kfFrom.getRenderingProperties()[timelineIdx];
				valFrom = kfpFrom.getValue();
			}

			kfpCurr.setValue(super.interpolate(current.getFrame(), valFrom, valTo));
		}
	}
}

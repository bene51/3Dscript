package textanim;

import java.util.List;
import java.util.Map;

import parser.NoSuchMacroException;
import parser.NumberOrMacro;
import renderer3d.Keyframe;
import renderer3d.Keyframe.KeyframeProperty;

public class ChangeAnimation extends Animation {

	private int timelineIdx;
	private NumberOrMacro vTo;

	public ChangeAnimation(int fromFrame, int toFrame, int timelineIdx, NumberOrMacro vTo) {
		super(fromFrame, toFrame);
		this.timelineIdx = timelineIdx;
		this.vTo = vTo;
	}

	@Override
	public void pickScripts(Map<String, String> scripts) throws NoSuchMacroException {
		pickScripts(scripts, vTo);
	}

	@Override
	public void adjustKeyframe(Keyframe current, List<Keyframe> previous) {
		KeyframeProperty kfpCurr = current.getRenderingProperties()[timelineIdx];

		// if it's a macro, just set the value to the macro evaluation
		if(vTo.isMacro()) {
			kfpCurr.setValue(vTo.evaluateMacro(current.getFrame()));
			return;
		}

		double valFrom = -1;
		double valTo = vTo.getValue();
		// otherwise, let's see if there exists a value at fromFrame; if not
		// just use the same value as the target value
		Keyframe kfFrom = previous.get(fromFrame);
		if(kfFrom == null) {
			valFrom = valTo;
		}
		else {
			KeyframeProperty kfpFrom = kfFrom.getRenderingProperties()[timelineIdx];
			valFrom = kfpFrom.getValue();
		}

		kfpCurr.setValue(super.interpolate(current.getFrame(), valFrom, valTo));
	}
}

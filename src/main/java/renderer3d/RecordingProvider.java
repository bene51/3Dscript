package renderer3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import textanim.IRecordingProvider;
import textanim.Keyframe2;

public class RecordingProvider implements IRecordingProvider {

	private static RecordingProvider instance;

	private final List<RecordingItem> recordingItems = new ArrayList<RecordingItem>();

	private RecordingProvider() {
		recordingItems.add(new RecordingItem() {
			@Override
			public String getCommand() {
				return "Record contrast";
			}

			@Override
			public String getRecording(Keyframe2 keyframe) {
				ExtendedKeyframe kf = (ExtendedKeyframe)keyframe;
				StringBuffer text = new StringBuffer("At frame X:\n");
				for(int c = 0; c < kf.getNChannels(); c++) {
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(ExtendedKeywordFactory.ChannelKeyword.COLOR.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedKeyframe.COLOR_MIN)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedKeyframe.COLOR_MAX)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedKeyframe.COLOR_GAMMA))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(ExtendedKeywordFactory.ChannelKeyword.ALPHA.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedKeyframe.ALPHA_MIN)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedKeyframe.ALPHA_MAX)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedKeyframe.ALPHA_GAMMA))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(ExtendedKeywordFactory.ChannelKeyword.WEIGHT.getKeyword())
						.append(" to ")
						.append(kf.getChannelProperty(c, ExtendedKeyframe.WEIGHT))
						.append("\n");
				}
				return text.toString();
			}
		});

		recordingItems.add(new RecordingItem() {
			@Override
			public String getCommand() {
				return "Record cropping";
			}

			@Override
			public String getRecording(Keyframe2 keyframe) {
				ExtendedKeyframe kf = (ExtendedKeyframe)keyframe;
				StringBuffer text = new StringBuffer("At frame X:\n");
				text.append("- change ")
					.append(ExtendedKeywordFactory.NonChannelKeyword.BOUNDING_BOX_X.getKeyword())
					.append(" to (")
					.append(kf.getNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_XMIN))
					.append(", ")
					.append(kf.getNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_XMAX))
					.append(")\n");
				text.append("- change ")
				.append(ExtendedKeywordFactory.NonChannelKeyword.BOUNDING_BOX_Y.getKeyword())
					.append(" to (")
					.append(kf.getNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_YMIN))
					.append(", ")
					.append(kf.getNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_YMAX))
					.append(")\n");
				text.append("- change ")
				.append(ExtendedKeywordFactory.NonChannelKeyword.BOUNDING_BOX_Z.getKeyword())
					.append(" to (")
					.append(kf.getNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_ZMIN))
					.append(", ")
					.append(kf.getNonchannelProperty(ExtendedKeyframe.BOUNDINGBOX_ZMAX))
					.append(")\n");
				return text.toString();
			}
		});
	}

	public static RecordingProvider getInstance() {
		if(instance == null)
			instance = new RecordingProvider();
		return instance;
	}

	@Override
	public Iterator<RecordingItem> iterator() {
		return recordingItems.iterator();
	}

	@Override
	public RecordingItem get(int i) {
		return recordingItems.get(i);
	}
}

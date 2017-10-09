package renderer3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import textanim.IRecordingProvider;
import textanim.RenderingState;

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
			public String getRecording(RenderingState rs) {
				ExtendedRenderingState kf = (ExtendedRenderingState)rs;
				StringBuffer text = new StringBuffer("At frame X:\n");
				for(int c = 0; c < kf.getNChannels(); c++) {
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(ExtendedKeywordFactory.ChannelKeyword.COLOR.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.COLOR_MIN)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.COLOR_MAX)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.COLOR_GAMMA))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(ExtendedKeywordFactory.ChannelKeyword.ALPHA.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.ALPHA_MIN)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.ALPHA_MAX)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.ALPHA_GAMMA))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(ExtendedKeywordFactory.ChannelKeyword.WEIGHT.getKeyword())
						.append(" to ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.WEIGHT))
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
			public String getRecording(RenderingState rs) {
				ExtendedRenderingState kf = (ExtendedRenderingState)rs;
				StringBuffer text = new StringBuffer("At frame X:\n");
				text.append("- change ")
					.append(ExtendedKeywordFactory.NonChannelKeyword.BOUNDING_BOX_X.getKeyword())
					.append(" to (")
					.append(kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMIN))
					.append(", ")
					.append(kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_XMAX))
					.append(")\n");
				text.append("- change ")
				.append(ExtendedKeywordFactory.NonChannelKeyword.BOUNDING_BOX_Y.getKeyword())
					.append(" to (")
					.append(kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMIN))
					.append(", ")
					.append(kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_YMAX))
					.append(")\n");
				text.append("- change ")
				.append(ExtendedKeywordFactory.NonChannelKeyword.BOUNDING_BOX_Z.getKeyword())
					.append(" to (")
					.append(kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMIN))
					.append(", ")
					.append(kf.getNonchannelProperty(ExtendedRenderingState.BOUNDINGBOX_ZMAX))
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

package animation3d.renderer3d;

import animation3d.textanim.Default3DRecordingProvider;
import animation3d.textanim.RenderingState;

public class RecordingProvider extends Default3DRecordingProvider {

	public static Default3DRecordingProvider getInstance() {
		if(instance == null)
			instance = new RecordingProvider();
		return instance;
	}

	private RecordingProvider() {
		add(new RecordingItem() {
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
						.append(KeywordFactory.ChannelKeyword.INTENSITY.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.INTENSITY_MIN)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.INTENSITY_MAX)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.INTENSITY_GAMMA))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(KeywordFactory.ChannelKeyword.ALPHA.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.ALPHA_MIN)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.ALPHA_MAX)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.ALPHA_GAMMA))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(KeywordFactory.ChannelKeyword.COLOR.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.CHANNEL_COLOR_RED)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.CHANNEL_COLOR_GREEN)).append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.CHANNEL_COLOR_BLUE))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(KeywordFactory.ChannelKeyword.WEIGHT.getKeyword())
						.append(" to ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.WEIGHT))
						.append("\n");
				}
				return text.toString();
			}
		});

		add(new RecordingItem() {
			@Override
			public String getCommand() {
				return "Record cropping";
			}

			@Override
			public String getRecording(RenderingState rs) {
				ExtendedRenderingState kf = (ExtendedRenderingState)rs;
				StringBuffer text = new StringBuffer("At frame X:\n");
				for(int c = 0; c < kf.getNChannels(); c++) {
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(KeywordFactory.ChannelKeyword.BOUNDING_BOX_X.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.BOUNDINGBOX_XMIN))
						.append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.BOUNDINGBOX_XMAX))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(KeywordFactory.ChannelKeyword.BOUNDING_BOX_Y.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.BOUNDINGBOX_YMIN))
						.append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.BOUNDINGBOX_YMAX))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(KeywordFactory.ChannelKeyword.BOUNDING_BOX_Z.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.BOUNDINGBOX_ZMIN))
						.append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.BOUNDINGBOX_ZMAX))
						.append(")\n");
					text.append("- change channel ")
						.append(c + 1)
						.append(" ")
						.append(KeywordFactory.ChannelKeyword.FRONT_BACK_CLIPPING.getKeyword())
						.append(" to (")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.NEAR))
						.append(", ")
						.append(kf.getChannelProperty(c, ExtendedRenderingState.FAR))
						.append(")\n");
				}
				return text.toString();
			}
		});
	}
}

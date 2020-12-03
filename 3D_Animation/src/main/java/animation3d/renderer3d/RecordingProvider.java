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
				return "At frame X:\n" + getRecordingForContrast(rs);
			}
		});

		add(new RecordingItem() {
			@Override
			public String getCommand() {
				return "Record cropping";
			}

			@Override
			public String getRecording(RenderingState rs) {
				return "At frame X:\n" + getRecordingForCropping(rs);
			}
		});
	}

	public String getRecordingForEverything(ExtendedRenderingState rs) {
		StringBuffer text = new StringBuffer();
		text.append(super.getRecordingForTransformation(rs))
			.append(getRecordingForContrast(rs))
			.append(getRecordingForCropping(rs))
			.append(getRecordingForBoundingBox(rs))
			.append(getRecordingForScalebar(rs))
			.append(getRecordingForTimelapse(rs));
		return text.toString();
	}

	public String getRecordingForTimelapse(RenderingState rs) {
		ExtendedRenderingState kf = (ExtendedRenderingState)rs;
		StringBuffer text = new StringBuffer();
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.TIMEPOINT.getKeyword())
			.append(" to ")
			.append((int)kf.getNonChannelProperty(ExtendedRenderingState.BOUNDINGBOX_WIDTH))
			.append("\n");
		return text.toString();
	}

	public String getRecordingForBoundingBox(RenderingState rs) {
		ExtendedRenderingState kf = (ExtendedRenderingState)rs;
		StringBuffer text = new StringBuffer();
		String vis = kf.getNonChannelProperty(ExtendedRenderingState.SHOW_BOUNDINGBOX) == 0 ? "off" : "on";
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.BOUNDINGBOX_VISIBILITY.getKeyword())
			.append(" to ")
			.append(vis)
			.append("\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.BOUNDINGBOX_COLOR.getKeyword())
			.append(" to (")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.BOUNDINGBOX_RED  )).append(", ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.BOUNDINGBOX_GREEN)).append(", ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.BOUNDINGBOX_BLUE ))
			.append(")\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.BOUNDINGBOX_WIDTH.getKeyword())
			.append(" to ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.BOUNDINGBOX_WIDTH))
			.append("\n");
		return text.toString();
	}

	public String getRecordingForScalebar(RenderingState rs) {
		ExtendedRenderingState kf = (ExtendedRenderingState)rs;
		StringBuffer text = new StringBuffer();
		String vis = kf.getNonChannelProperty(ExtendedRenderingState.SHOW_SCALEBAR) == 0 ? "off" : "on";
		int pos = (int)kf.getNonChannelProperty(ExtendedRenderingState.SCALEBAR_POSITION);
		String posString = Scalebar.Position.getNames()[pos];
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.SCALEBAR_VISIBILITY.getKeyword())
			.append(" to ")
			.append(vis)
			.append("\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.SCALEBAR_LENGTH.getKeyword())
			.append(" to ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.SCALEBAR_LENGTH))
			.append("\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.SCALEBAR_COLOR.getKeyword())
			.append(" to (")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.SCALEBAR_RED  )).append(", ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.SCALEBAR_GREEN)).append(", ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.SCALEBAR_BLUE ))
			.append(")\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.SCALEBAR_WIDTH.getKeyword())
			.append(" to ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.SCALEBAR_WIDTH))
			.append("\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.SCALEBAR_POSITION.getKeyword())
			.append(" to ")
			.append(posString)
			.append("\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.SCALEBAR_OFFSET.getKeyword())
			.append(" to ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.SCALEBAR_OFFSET))
			.append("\n");
		return text.toString();
	}

	public String getRecordingForCropping(RenderingState rs) {
		ExtendedRenderingState kf = (ExtendedRenderingState)rs;
		StringBuffer text = new StringBuffer();
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

	public String getRecordingForContrast(RenderingState rs) {
		ExtendedRenderingState kf = (ExtendedRenderingState)rs;
		StringBuffer text = new StringBuffer();
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
				.append(KeywordFactory.ChannelKeyword.LIGHT.getKeyword())
				.append(" to (")
				.append(kf.getChannelProperty(c, ExtendedRenderingState.LIGHT_K_OBJECT)).append(", ")
				.append(kf.getChannelProperty(c, ExtendedRenderingState.LIGHT_K_DIFFUSE)).append(", ")
				.append(kf.getChannelProperty(c, ExtendedRenderingState.LIGHT_K_SPECULAR)).append(", ")
				.append(kf.getChannelProperty(c, ExtendedRenderingState.LIGHT_SHININESS))
				.append(")\n");
			String uselight = kf.getChannelProperty(c, ExtendedRenderingState.USE_LIGHT) == 0 ? "off" : "on";
			text.append("- change channel ")
				.append(c + 1)
				.append(" ")
				.append(KeywordFactory.ChannelKeyword.USE_LIGHT.getKeyword())
				.append(" to ")
				.append(uselight)
				.append("\n");
			text.append("- change channel ")
				.append(c + 1)
				.append(" ")
				.append(KeywordFactory.ChannelKeyword.WEIGHT.getKeyword())
				.append(" to ")
				.append(kf.getChannelProperty(c, ExtendedRenderingState.WEIGHT))
				.append("\n");
		}
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.RENDERING_ALGORITHM.getKeyword())
			.append(" to ")
			.append(kf.getRenderingAlgorithm().toString().toLowerCase().replace('_', ' '))
			.append("\n");
		text.append("- change ")
			.append(KeywordFactory.NonChannelKeyword.BG_COLOR.getKeyword())
			.append(" to (")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.BG_COLOR_RED)).append(", ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.BG_COLOR_GREEN)).append(", ")
			.append(kf.getNonChannelProperty(ExtendedRenderingState.BG_COLOR_BLUE))
			.append(")\n");
		return text.toString();
	}
}

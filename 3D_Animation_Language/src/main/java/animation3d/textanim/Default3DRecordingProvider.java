package animation3d.textanim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import animation3d.parser.Keyword.GeneralKeyword;
import animation3d.util.Transform;

public class Default3DRecordingProvider implements IRecordingProvider {

	protected static Default3DRecordingProvider instance;

	private final List<RecordingItem> recordingItems = new ArrayList<RecordingItem>();

	protected Default3DRecordingProvider() {
		add(new RecordingItem() {
			@Override
			public String getCommand() {
				return "Record transformation";
			}

			@Override
			public String getRecording(RenderingState rs) {
				CombinedTransform t = rs.fwdTransform;
				float[] rotcenter = t.getCenter();

				float[] m = rs.getFwdTransform().calculateForwardTransformWithoutCalibration();

				// M = T * C^{-1} * S * R * C
				// T * S * R = C * M * C^{-1}
				Transform.applyTranslation(-rotcenter[0], -rotcenter[1], -rotcenter[2], m);
				Transform.applyTranslation(m, rotcenter[0], rotcenter[1], rotcenter[2]);

				// extract scale
				float scale = (float)Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);

				// extract translation
				float dx = m[3];
				float dy = m[7];
				float dz = m[11];

				m[3] = m[7] = m[11] = 0;
				for(int i = 0; i < 12; i++)
					m[i] *= 1f / scale;

				// extract rotation
				float[] euler = new float[3];
				Transform.guessEulerAngles(m, euler);

				StringBuffer text = new StringBuffer("At frame X:\n");
				// rotate around x-axis (vertically)
				text.append("- reset transformation\n");
				text.append("- ")
					.append(GeneralKeyword.ROTATE.getKeyword()).append(" ")
					.append(CustomDecimalFormat.format(euler[0] * 180 / (float)Math.PI, 1)).append(" ")
					.append(GeneralKeyword.DEGREES.getKeyword()).append(" ")
					.append("vertically\n");

				// rotate around z-axis
				text.append("- ")
					.append(GeneralKeyword.ROTATE.getKeyword()).append(" ")
					.append(CustomDecimalFormat.format(euler[2] * 180 / (float)Math.PI, 1)).append(" ")
					.append(GeneralKeyword.DEGREES.getKeyword()).append(" ")
					.append(GeneralKeyword.AROUND.getKeyword()).append(" ")
					.append("(")
					.append(0).append(", ")
					.append(0).append(", ")
					.append(1)
					.append(")\n");

				// rotate around y-axis
				text.append("- ")
					.append(GeneralKeyword.ROTATE.getKeyword()).append(" ")
					.append(CustomDecimalFormat.format(euler[1] * 180 / (float)Math.PI, 1)).append(" ")
					.append(GeneralKeyword.DEGREES.getKeyword()).append(" ")
					.append("horizontally\n");


				text.append("- ")
					.append(GeneralKeyword.ZOOM.getKeyword()).append(" ")
					.append(CustomDecimalFormat.format(scale, 1))
					.append("\n");
				text.append("- ")
					.append(GeneralKeyword.TRANSLATE.getKeyword()).append(" ")
					.append(GeneralKeyword.BY.getKeyword()).append(" ")
					.append("(")
					.append(CustomDecimalFormat.format(dx, 1)).append(", ")
					.append(CustomDecimalFormat.format(dy, 1)).append(", ")
					.append(CustomDecimalFormat.format(dz, 1))
					.append(")\n");

				return text.toString();
			}
		});
	}

	protected void add(RecordingItem ri) {
		recordingItems.add(ri);
	}

	public static Default3DRecordingProvider getInstance() {
		if(instance == null)
			instance = new Default3DRecordingProvider();
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

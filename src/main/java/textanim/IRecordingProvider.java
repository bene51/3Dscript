package textanim;

import textanim.IRecordingProvider.RecordingItem;

public interface IRecordingProvider extends Iterable<RecordingItem> {

	public interface RecordingItem {

		public String getCommand();

		public String getRecording(Keyframe2 keyframe);
	}

	public RecordingItem get(int i);
}

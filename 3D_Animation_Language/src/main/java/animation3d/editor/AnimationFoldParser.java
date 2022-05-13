package animation3d.editor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldType;

import java.util.ArrayList;
import java.util.List;

public class AnimationFoldParser implements FoldParser {
	@Override
	public List<Fold> getFolds(RSyntaxTextArea textArea) {
		List<Fold> folds = new ArrayList<>();

		Fold currentFold = null;
		int lineCount = textArea.getLineCount();
		int endOfLastNonEmptyLine = 0;

		try {
			for (int line = 0; line < lineCount; line++) {
				int t0 = textArea.getLineStartOffset(line);
				int t1 = textArea.getLineEndOffset(line);
				int len = t1 - t0;
				String lineString = textArea.getText(t0, len).trim();
				if(!lineString.isEmpty())
					endOfLastNonEmptyLine = t1 - 1;
				if(!lineString.startsWith("-")) {
					if(currentFold != null) {
						currentFold.setEndOffset(endOfLastNonEmptyLine);
						currentFold = null;
					}
				}
				if(lineString.endsWith(":")) {
					currentFold = new Fold(FoldType.CODE, textArea, t0);
					folds.add(currentFold);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return folds;
	}
}

package editor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProviderBase;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.autocomplete.TemplateCompletion;

import parser.Autocompletion;
import parser.Autocompletion.ChoiceAutocompletion;
import parser.Autocompletion.StringAutocompletion;
import parser.Interpreter;
import parser.ParsingResult;

public class AnimationCompletionProvider extends CompletionProviderBase {


	@Override
	public boolean isAutoActivateOkay(JTextComponent comp) {
		return true;
	}

	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {

		System.out.println("getAlreadyEnteredText");
		Document doc = comp.getDocument();
		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);

		int start = elem.getStartOffset();
		int len = dot - start;

		String input = "";
		try {
			input = doc.getText(start, len);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return input;
		}


		ParsingResult result = null;
		try {
			result = Interpreter.parse(input, dot - start, new float[] {100, 100, 100});
		} catch(Exception e) {
			e.printStackTrace();
		}

		Autocompletion autocompletion = result.getAutocompletion();
		int atype = autocompletion.type;

		if(atype == Autocompletion.AUTOCOMPLETION_LIST) {
			ChoiceAutocompletion ca = (ChoiceAutocompletion)autocompletion;
			String[] options = ca.getOptions();
			if(options == null || options.length == 0)
				return "";

			String alreadyEntered = input.substring(ca.getInsertionPosition(), dot - start);
			System.out.println("alreadyEntered: " + alreadyEntered);
			return alreadyEntered;
		}
		if(atype == Autocompletion.AUTOCOMPLETION_STRING) {
			StringAutocompletion ca = (StringAutocompletion)autocompletion;
			String option = ca.getString();
			if(option == null || option.length() == 0)
				return "";

			String alreadyEntered = input.substring(ca.getInsertionPosition(), dot - start);
			System.out.println("alreadyEntered: " + alreadyEntered);
			return alreadyEntered;
		}
		System.out.println("alreadyEntered: EMPTY");
		return "";
	}

	@Override
	public List<Completion> getCompletionsAt(JTextComponent arg0, Point arg1) {
        throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent arg0) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {
		System.out.println("getCompletionsImpl");
		List<Completion> completions = new ArrayList<Completion>();

		System.out.println("getAlreadyEnteredText");
		Document doc = comp.getDocument();
		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);

		int start = elem.getStartOffset();
		int len = dot - start;

		String input = "";
		try {
			input = doc.getText(start, len);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return completions;
		}


		ParsingResult result = null;
		try {
			result = Interpreter.parse(input, dot - start, new float[] {100, 100, 100});
		} catch(Exception e) {
			e.printStackTrace();
		}

		Autocompletion autocompletion = result.getAutocompletion();
		int atype = autocompletion.type;

		if(atype == Autocompletion.AUTOCOMPLETION_LIST) {
			ChoiceAutocompletion ca = (ChoiceAutocompletion)autocompletion;
			for(String option : ca.getOptions()) {
				if(option.matches(".*?\\(.*?,.*?,.*?\\).*")) {
					String t = option.replaceAll("\\(.*?,.*?,.*?\\)", "(\\${x}, \\${y}, \\${z}).\\${cursor}");
					completions.add(new TemplateCompletion(this, "input", option, t));
				}
				else
					completions.add(new BasicCompletion(this, option + " "));
			}
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_STRING) {
			StringAutocompletion sa = (StringAutocompletion)autocompletion;
			String s = sa.getString();
			if(s != null && s.length() != 0)
				completions.add(new BasicCompletion(this, sa.getString() + " "));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_INTEGER) {
			completions.add(new TemplateCompletion(this, "input", "0", "${x}${cursor}"));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_REAL) {
			completions.add(new TemplateCompletion(this, "input", "0", "${x}${cursor}"));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_TRIPLE) {
			completions.add(new TemplateCompletion(this, "input", "(0, 1, 0)", "(${x}, ${y}, ${z})${cursor}"));
		}
		System.out.println("return completions: " + Arrays.toString(completions.toArray()));

		return completions;
	}
}

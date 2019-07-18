package animation3d.editor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProviderBase;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.autocomplete.TemplateCompletion;

import animation3d.parser.Autocompletion;
import animation3d.parser.Autocompletion.ChoiceAutocompletion;
import animation3d.parser.Autocompletion.IntegerAutocompletion;
import animation3d.parser.Autocompletion.QuadrupleAutocompletion;
import animation3d.parser.Autocompletion.RealAutocompletion;
import animation3d.parser.Autocompletion.StringAutocompletion;
import animation3d.parser.Autocompletion.TripleAutocompletion;
import animation3d.parser.Autocompletion.TupleAutocompletion;
import animation3d.parser.Interpreter;
import animation3d.parser.ParsingResult;
import animation3d.parser.Preprocessor;
import animation3d.textanim.IKeywordFactory;

public class AnimationCompletionProvider extends CompletionProviderBase {

	private final IKeywordFactory kwFactory;

	public AnimationCompletionProvider(IKeywordFactory kwFactory) {
		this.kwFactory = kwFactory;
	}

	@Override
	public boolean isAutoActivateOkay(JTextComponent comp) {
		return true;
	}

	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {

		System.out.println("getAlreadyEnteredText");
//		Document doc = comp.getDocument();
		int dot = comp.getCaretPosition();
//		Element root = doc.getDefaultRootElement();
//		int index = root.getElementIndex(dot);
//		Element elem = root.getElement(index);

//		int start = elem.getStartOffset();
//		int len = dot - start;

		String input = "";
//		try {
//			input = doc.getText(start, len);
//		} catch (BadLocationException e) {
//			e.printStackTrace();
//			return input;
//		}


		ParsingResult result = new ParsingResult();
		try {
			input = Preprocessor.getLineForCursor(comp.getText(), dot - 1);
			System.out.println("Current line = " + input);
//			Interpreter.parse(input, dot - start, new float[] {100, 100, 100}, result);
			Interpreter.parse(kwFactory, input, input.length(), new float[] {100, 100, 100}, result);
		} catch(Exception e) {
			// e.printStackTrace();
		}

		Autocompletion autocompletion = result.getAutocompletion();
		if(autocompletion == null)
			return "";

		int atype = autocompletion.type;

		if(atype == Autocompletion.AUTOCOMPLETION_LIST) {
			ChoiceAutocompletion ca = (ChoiceAutocompletion)autocompletion;
			String[] options = ca.getOptions();
			if(options == null || options.length == 0)
				return "";

//			String alreadyEntered = input.substring(ca.getInsertionPosition(), dot - start);
			String alreadyEntered = input.substring(ca.getInsertionPosition(), input.length());
			System.out.println("alreadyEntered: " + alreadyEntered);
			return alreadyEntered;
		}
		if(atype == Autocompletion.AUTOCOMPLETION_STRING) {
			StringAutocompletion ca = (StringAutocompletion)autocompletion;
			String option = ca.getString();
			if(option == null || option.length() == 0)
				return "";

//			String alreadyEntered = input.substring(ca.getInsertionPosition(), dot - start);
			String alreadyEntered = input.substring(ca.getInsertionPosition(), input.length());
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
//		Document doc = comp.getDocument();
		int dot = comp.getCaretPosition();
//		Element root = doc.getDefaultRootElement();
//		int index = root.getElementIndex(dot);
//		Element elem = root.getElement(index);

//		int start = elem.getStartOffset();
//		int len = dot - start;

		String input = "";
//		try {
//			input = doc.getText(start, len);
//		} catch (BadLocationException e) {
//			e.printStackTrace();
//			return completions;
//		}

		ParsingResult result = new ParsingResult();
		try {
			input = Preprocessor.getLineForCursor(comp.getText(), dot - 1);
			System.out.println("Current line = " + input);
//			Interpreter.parse(input, dot - start, new float[] {100, 100, 100}, result);
			Interpreter.parse(kwFactory, input, input.length(), new float[] {100, 100, 100}, result);
		} catch(Exception e) {
//			e.printStackTrace();
		}

		Autocompletion autocompletion = result.getAutocompletion();
		if(autocompletion == null)
			return completions;

		int atype = autocompletion.type;

		if(atype == Autocompletion.AUTOCOMPLETION_LIST) {
			ChoiceAutocompletion ca = (ChoiceAutocompletion)autocompletion;
			int relevance = ca.getOptions().length;
			for(String option : ca.getOptions()) {
				if(option.matches(".*?\\(.*?,.*?,.*?\\).*")) {
					String t = option.replaceAll("\\(.*?,.*?,.*?\\)", "(\\${x}, \\${y}, \\${z}).\\${cursor}");
					TemplateCompletion tc = new TemplateCompletion(this, "input", option, t);
					tc.setRelevance(relevance--);
					completions.add(tc);
				}
				else {
					BasicCompletion bc = new BasicCompletion(this, option + " ");
					bc.setRelevance(relevance--);
					completions.add(bc);
				}
			}
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_STRING) {
			StringAutocompletion sa = (StringAutocompletion)autocompletion;
			String s = sa.getString();
			if(s != null && s.length() != 0)
				completions.add(new BasicCompletion(this, sa.getString() + " "));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_INTEGER) {
			IntegerAutocompletion ia = (IntegerAutocompletion)autocompletion;
			String desc = ia.getDescription();
			completions.add(new TemplateCompletion(this, "input", "0", "${" + desc + "}${cursor}"));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_REAL) {
			RealAutocompletion ia = (RealAutocompletion)autocompletion;
			String desc = ia.getDescription();
			completions.add(new TemplateCompletion(this, "input", "0", "${" + desc + "}${cursor}"));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_QUADRUPLE) {
			QuadrupleAutocompletion ia = (QuadrupleAutocompletion)autocompletion;
			String desc0 = ia.getDescription(0);
			String desc1 = ia.getDescription(1);
			String desc2 = ia.getDescription(2);
			String desc3 = ia.getDescription(3);
			completions.add(new TemplateCompletion(this, "input", "(0, 1, 0, 0)", "(${" + desc0 + "}, ${" + desc1 + "}, ${" + desc2 + "}, ${" + desc3 + "})${cursor}"));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_TRIPLE) {
			TripleAutocompletion ia = (TripleAutocompletion)autocompletion;
			String desc0 = ia.getDescription(0);
			String desc1 = ia.getDescription(1);
			String desc2 = ia.getDescription(2);
			completions.add(new TemplateCompletion(this, "input", "(0, 1, 0)", "(${" + desc0 + "}, ${" + desc1 + "}, ${" + desc2 + "})${cursor}"));
		}
		else if(atype == Autocompletion.AUTOCOMPLETION_TUPLE) {
			TupleAutocompletion ia = (TupleAutocompletion)autocompletion;
			String desc0 = ia.getDescription(0);
			String desc1 = ia.getDescription(1);
			completions.add(new TemplateCompletion(this, "input", "(0, 1)", "(${" + desc0 + "}, ${" + desc1 + "})${cursor}"));
		}
		System.out.println("return completions: " + Arrays.toString(completions.toArray()));

		return completions;
	}
}

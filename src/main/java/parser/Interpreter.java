package parser;

import parser.Autocompletion.ChoiceAutocompletion;
import parser.Autocompletion.IntegerAutocompletion;
import parser.Autocompletion.RealAutocompletion;
import parser.Autocompletion.StringAutocompletion;
import parser.Autocompletion.TripleAutocompletion;
import parser.Autocompletion.TupleAutocompletion;
import parser.Keyword2.GeneralKeyword;
import parser.Keyword2.Transition;
import textanim.Animation;
import textanim.ChangeAnimation;
import textanim.IKeywordFactory;
import textanim.RotationAnimation;
import textanim.ScaleAnimation;
import textanim.TranslationAnimation;

public class Interpreter {

	private final IKeywordFactory kwFactory;
	private final Lexer lexer;
	private float[] center;

	private Interpreter(IKeywordFactory kwFactory, String text, float[] center) {
		this(kwFactory, new Lexer(text), center);
	}

	private Interpreter(IKeywordFactory kwFactory, Lexer lexer, float[] center) {
		this.kwFactory = kwFactory;
		this.lexer = lexer;
		this.center = center;
	}

	private void skipSpace() {
		while(lexer.getNextToken(TokenType.SPACE, true) != null)
			;
	}

	Token letter(boolean optional) {
		return lexer.getNextToken(TokenType.LETTER, optional);
	}

	Token underscore(boolean optional) {
		return lexer.getNextToken(TokenType.UNDERSCORE, optional);
	}

	Token sign(boolean optional) {
		skipSpace();
		return lexer.getNextToken(TokenType.SIGN, optional);
	}

	Token digit(boolean optional) {
		return lexer.getNextToken(TokenType.DIGIT, optional);
	}

	Token dot(boolean optional) {
		return lexer.getNextToken(TokenType.DOT, optional);
	}

	Token lparen(boolean optional) {
		skipSpace();
		return lexer.getNextToken(TokenType.LPAREN, optional);
	}

	Token rparen(boolean optional) {
		skipSpace();
		return lexer.getNextToken(TokenType.RPAREN, optional);
	}

	Token comma(boolean optional) {
		skipSpace();
		return lexer.getNextToken(TokenType.COMMA, optional);
	}

	Token keyword(Keyword2 kw, boolean optional) {
		skipSpace();
		return lexer.getNextToken(kw, optional);
	}

	Token space(ParsingResult result, boolean optional) {
		if(!optional)
			result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), ""));
		return lexer.getNextToken(TokenType.SPACE, optional);
	}

	/**
	 * integer :: S?D+
	 */
	int integer() {
		StringBuffer buffer = new StringBuffer();
		Token token;
		if((token = sign(true)) != null)
			buffer.append(token.text);

		buffer.append(digit(false).text);
		while((token = digit(true)) != null)
			buffer.append(token.text);
		return Integer.parseInt(buffer.toString());
	}

	/**
	 * real :: S?D+(.D*)?
	 *    D :: (0|1|2|3|4|5|6|7|8|9)
	 *    S :: (+|-)
	 */
	double real() {
		StringBuffer buffer = new StringBuffer();
		Token token;
		if((token = sign(true)) != null)
			buffer.append(token.text);

		buffer.append(digit(false).text);
		while((token = digit(true)) != null)
			buffer.append(token.text);

		if((token = dot(true)) != null) {
			buffer.append(token.text);

			// buffer.append(digit(false).text);
			while((token = digit(true)) != null)
				buffer.append(token.text);
		}
		return Double.parseDouble(buffer.toString());
	}

	/**
	 * mor :: (macro | real)
	 * @return
	 */
	NumberOrMacro mor() {
		String functionName = macro();
		if(functionName != null)
			return new NumberOrMacro(functionName);
		double v = real();
		return new NumberOrMacro(v);
	}

	/**
	 * macro :: (L|_)(L|_|D)*
	 *    L :: (a-z|A-Z)
	 *    D :: (0|1|2|3|4|5|6|7|8|9)
	 */
	String macro() {
		StringBuffer buffer = new StringBuffer();
		Token token;
		if((token = letter(true)) != null)
			buffer.append(token.text);
		else if((token = underscore(true)) != null)
			buffer.append(token.text);
		else
			return null;

		while(true) {
			if((token = letter(true)) != null)
				buffer.append(token.text);
			else if((token = underscore(true)) != null)
				buffer.append(token.text);
			else if((token = digit(true)) != null)
				buffer.append(token.text);
			else
				break;
		}
		return buffer.toString();
	}

	/**
	 * tuple :: (real ,real)
	 */
	NumberOrMacro[] tuple(ParsingResult result) {
		lparen(false);

		skipSpace();
		NumberOrMacro a = mor();
		skipSpace();
		comma(false);

		skipSpace();
		NumberOrMacro b = mor();
		skipSpace();

		rparen(false);

		return new NumberOrMacro[] {a, b};
	}

	/**
	 * triple :: (real ,real, real)
	 */
	NumberOrMacro[] triple(ParsingResult result) {
		lparen(false);

		skipSpace();
		NumberOrMacro a = mor();
		skipSpace();
		comma(false);

		skipSpace();
		NumberOrMacro b = mor();
		skipSpace();
		comma(false);

		skipSpace();
		NumberOrMacro c = mor();
		skipSpace();
		rparen(false);

		return new NumberOrMacro[] {a, b, c};
	}

	/**
	 * rotation :: rotate by real degrees (horizontally | vertically | around triple)
	 */
	RotationAnimation rotation(int from, int to, ParsingResult result, int cursorpos) {
		if(keyword(GeneralKeyword.ROTATE, true) == null)
			return null;

		space(result, false);

		result.setAutocompletion(new RealAutocompletion("<degrees>"));

		NumberOrMacro degrees = mor();

		space(result, false);

		result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), GeneralKeyword.DEGREES.getKeyword()));
		keyword(GeneralKeyword.DEGREES, false);

		space(result, false);

		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos,
						GeneralKeyword.HORIZONTALLY,
						GeneralKeyword.VERTICALLY,
						GeneralKeyword.AROUND)));

		NumberOrMacro[] axis = null;
		if(keyword(GeneralKeyword.HORIZONTALLY, true) != null) {
			axis = new NumberOrMacro[] {
					new NumberOrMacro(0),
					new NumberOrMacro(1),
					new NumberOrMacro(0)};
		}
		else if(keyword(GeneralKeyword.VERTICALLY, true) != null)
			axis = new NumberOrMacro[] {
					new NumberOrMacro(1),
					new NumberOrMacro(0),
					new NumberOrMacro(0)};
		else {
			keyword(GeneralKeyword.AROUND, false);

			space(result, false);

			result.setAutocompletion(new TripleAutocompletion("<vx>", "<vy>", "<vz>"));
			axis = triple(result);
		}

		RotationAnimation ra = new RotationAnimation(from, to, axis, degrees, center);
		return ra;
	}

	/**
	 * translation :: translate (horizontally by X | vertically by Y | by TRIPLE)
	 */
	TranslationAnimation translation(int from, int to, ParsingResult result, int cursorpos) {
		if(keyword(GeneralKeyword.TRANSLATE, true) == null)
			return null;

		space(result, false);

		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos,
						GeneralKeyword.HORIZONTALLY.getKeyword(),
						GeneralKeyword.VERTICALLY.getKeyword(),
						GeneralKeyword.BY.getKeyword() + " (X, Y, Z)")));

		NumberOrMacro[] dx = new NumberOrMacro[3];

		if(keyword(GeneralKeyword.HORIZONTALLY, true) != null) {
			space(result, false);

			result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), GeneralKeyword.BY.getKeyword()));
			keyword(GeneralKeyword.BY, false);

			space(result, false);

			result.setAutocompletion(new RealAutocompletion("<dx>"));
			dx[0] = mor();
			dx[1] = new NumberOrMacro(0);
			dx[2] = new NumberOrMacro(0);
		}
		else if(keyword(GeneralKeyword.VERTICALLY, true) != null) {
			space(result, false);

			result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), GeneralKeyword.BY.getKeyword()));
			keyword(GeneralKeyword.BY, false);

			space(result, false);

			result.setAutocompletion(new RealAutocompletion("<dy>"));
			dx[0] = new NumberOrMacro(0);
			dx[1] = mor();
			dx[2] = new NumberOrMacro(0);
		}
		else {
			keyword(GeneralKeyword.BY, false);

			space(result, false);
			// result.setAutocompletion(new TripleAutocompletion());
			dx = triple(result);
		}

		return new TranslationAnimation(from, to, dx[0], dx[1], dx[2]);
	}

	/**
	 * zoom :: zoom by a factor of real
	 */
	ScaleAnimation zoom(int from, int to, ParsingResult result) {
		if(keyword(GeneralKeyword.ZOOM, true) == null)
			return null;
		space(result, false);
		result.setAutocompletion(new RealAutocompletion("<zoom>"));
		NumberOrMacro factor = mor();
		return new ScaleAnimation(from, to, factor, center);
	}

	/**
	 * channelproperty :: (color min | color max | color gamma | alpha min | alpha max | alpha gamma | weight | color | alpha)
	 */
	Keyword2 channelproperty(ParsingResult result, int cursorpos) {
		Keyword2[] channelKeywords = kwFactory.getChannelKeywords();
		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos, kwFactory.getChannelKeywords())));

		for(Keyword2 cp : channelKeywords) {
			if(keyword(cp, true) != null) {
				return cp;
			}
		}
		throw new RuntimeException("Expected channel property");
	}

	/**
	 * nonchannelproperty :: (bounding box min x | bounding box max x | bounding box min y | bounding box max y |
	 *                        bounding box min z | bounding box max z | front clipping | back clipping |
	 *                        bounding box x | bounding box y | bounding box z)
	 */
	Keyword2 nonchannelproperty(ParsingResult result, int cursorpos) {
		Keyword2[] nonChannelKeywords = kwFactory.getNonChannelKeywords();
		for(Keyword2 cp : nonChannelKeywords) {
			if(keyword(cp, true) != null) {
				return cp;
			}
		}
		throw new RuntimeException("Expected non-channel property");
	}

	/**
	 *             change :: change (channel X channelproperty | renderingproperty) to (real | macro)
	 *    channelproperty :: (color min | color max | color gamma | alpha min | alpha max | alpha gamma | weight)
	 * nonchannelproperty :: (bounding box min x | bounding box max x | bounding box min y | bounding box max y |
	 *                        bounding box min z | bounding box max z | front clipping | back clipping)
	 */
	ChangeAnimation change(int from, int to, ParsingResult result, int cursorpos) {
		if(keyword(GeneralKeyword.CHANGE, true) == null)
			return null;

		space(result, false);

		Keyword2[] nonChannelKeywords = kwFactory.getNonChannelKeywords();
		Keyword2[] choice = new Keyword2[nonChannelKeywords.length + 1];
		int i = 0;
		choice[i++] = GeneralKeyword.CHANNEL;
		for(Keyword2 rp : nonChannelKeywords)
			choice[i++] = rp;
		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos, choice)));

		int[] timelineIdcs = null;
		String[] autocompletionDescriptions = null;
		int channel = -1;
		if(keyword(GeneralKeyword.CHANNEL, true) != null) {
			space(result, false);
			result.setAutocompletion(new IntegerAutocompletion("<channel>"));
			channel = integer() - 1;
			space(result, false);
			Keyword2 cp = channelproperty(result, cursorpos);
			timelineIdcs = cp.getRenderingStateProperties();
			autocompletionDescriptions = cp.getAutocompletionDescriptions();
		}
		else {
			Keyword2 cp = nonchannelproperty(result, cursorpos);
			timelineIdcs = cp.getRenderingStateProperties();
			autocompletionDescriptions = cp.getAutocompletionDescriptions();
		}

		space(result, false);
		result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), GeneralKeyword.TO.getKeyword()));
		keyword(GeneralKeyword.TO, false);

		space(result, false);

		NumberOrMacro[] tgts = null;
		switch(timelineIdcs.length) {
		case 1:
			result.setAutocompletion(new RealAutocompletion(autocompletionDescriptions[0]));
			tgts = new NumberOrMacro[] { mor() };
			break;
		case 2:
			result.setAutocompletion(new TupleAutocompletion(
					autocompletionDescriptions[0],
					autocompletionDescriptions[1]));
			tgts = tuple(result);
			break;
		case 3:
			result.setAutocompletion(new TripleAutocompletion(
					autocompletionDescriptions[0],
					autocompletionDescriptions[1],
					autocompletionDescriptions[2]));
			tgts = triple(result);
			break;
		}

		ChangeAnimation ca = new ChangeAnimation(from, to, channel, timelineIdcs, tgts);
		result.setResult(ca);

		return ca;
	}

	float[] transition() {
		if(keyword(Transition.NONE, true) != null)
			return Transition.NONE.getTransition();
		if(keyword(Transition.EASE_IN_OUT, true) != null)
			return Transition.EASE_IN_OUT.getTransition();
		if(keyword(Transition.EASE_IN, true) != null)
			return Transition.EASE_IN.getTransition();
		if(keyword(Transition.EASE_OUT, true) != null)
			return Transition.EASE_OUT.getTransition();
		if(keyword(Transition.EASE, true) != null)
			return Transition.EASE.getTransition();
		return Transition.LINEAR.getTransition();
	}

	/**
	 * action :: (rotation | translation | zoom | change) (transition)?
	 * transition :: (linear | ease | ease-in | ease-out | ease-in-out)
	 *
	 * https://www.w3.org/TR/css3-transitions/#transition-timing-function
	 */
	void action(int from, int to, ParsingResult result, int cursorpos) {
		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos, GeneralKeyword.ROTATE, GeneralKeyword.TRANSLATE, GeneralKeyword.ZOOM, GeneralKeyword.CHANGE)));
		Animation ta = null;

		ta = rotation(from, to, result, cursorpos);
		if(ta == null)
			ta = translation(from, to, result, cursorpos);
		if(ta == null)
			ta = zoom(from, to, result);
		if(ta == null)
			ta = change(from, to, result, cursorpos);

		if(ta == null)
			throw new RuntimeException("Expected rotation, translation, zoom or change for action");
		else
			result.setResult(ta);

		skipSpace();

		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(), lexer.getAutocompletionList(cursorpos, Transition.values())));
		float[] transition = transition();

		if(ta != null)
			ta.setBezierControls(transition[0], transition[1], transition[2], transition[3]);
	}

	/**
	 * line :: From frame integer to frame integer action
	 */
	public void line(ParsingResult result, int cursorpos) {
		skipSpace();

		result.setAutocompletion(
				new ChoiceAutocompletion(lexer.getIndex(),
						lexer.getAutocompletionList(cursorpos,
								GeneralKeyword.FROM_FRAME,
								GeneralKeyword.AT_FRAME)));

		int from = -1, to = -1;

		if(keyword(GeneralKeyword.AT_FRAME, true) != null) {
			space(result, false);
			result.setAutocompletion(new IntegerAutocompletion("<frame>"));
			from = to = integer();
		} else {
			keyword(GeneralKeyword.FROM_FRAME, false);

			space(result, false);
			result.setAutocompletion(new IntegerAutocompletion("<frame>"));
			from = integer();

			space(result, false);
			result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), GeneralKeyword.TO_FRAME.getKeyword()));
			lexer.getNextToken(GeneralKeyword.TO_FRAME, false);

			space(result, false);
			result.setAutocompletion(new IntegerAutocompletion("<frame>"));
			to = integer();
		}

		space(result, false);

		result.setFromTo(from, to);
		action(from, to, result, cursorpos);

	}

	public static void parse(IKeywordFactory kwFactory, String line, int length, float[] center, ParsingResult result) {
		Interpreter i = new Interpreter(kwFactory, line, center);
		i.line(result, length);
	}

	public static void parse(IKeywordFactory kwFactory, String line, float[] center, ParsingResult result) {
		parse(kwFactory, line, line.length(), center, result);
	}

	public static void main(String...args) {
		String input = "From frame 0 to frame 10 rotate by 30 degrees around (1, 0, 0)";

		ParsingResult res = new ParsingResult();
		Interpreter.parse(null, input, new float[3], res);
		System.out.println(res);
	}
}

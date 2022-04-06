package animation3d.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import animation3d.parser.Autocompletion.ChoiceAutocompletion;
import animation3d.parser.Autocompletion.IntegerAutocompletion;
import animation3d.parser.Autocompletion.QuadrupleAutocompletion;
import animation3d.parser.Autocompletion.RealAutocompletion;
import animation3d.parser.Autocompletion.StringAutocompletion;
import animation3d.parser.Autocompletion.TripleAutocompletion;
import animation3d.parser.Autocompletion.TupleAutocompletion;
import animation3d.parser.Keyword.GeneralKeyword;
import animation3d.parser.Keyword.Transition;
import animation3d.textanim.Animation;
import animation3d.textanim.ChangeAnimation;
import animation3d.textanim.IKeywordFactory;
import animation3d.textanim.ResetTransformAnimation;
import animation3d.textanim.RotationAnimation;
import animation3d.textanim.ScaleAnimation;
import animation3d.textanim.TranslationAnimation;

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

	private void skipSpace() throws ParsingException {
		while(lexer.getNextToken(TokenType.SPACE, true) != null)
			;
	}

	Token letter(boolean optional) throws ParsingException {
		return lexer.getNextToken(TokenType.LETTER, optional);
	}

	Token underscore(boolean optional) throws ParsingException {
		return lexer.getNextToken(TokenType.UNDERSCORE, optional);
	}

	Token sign(boolean optional) throws ParsingException {
		skipSpace();
		return lexer.getNextToken(TokenType.SIGN, optional);
	}

	Token exp(boolean optional) throws ParsingException {
		return lexer.getNextToken(TokenType.EXP, optional);
	}

	Token digit(boolean optional) throws ParsingException {
		return lexer.getNextToken(TokenType.DIGIT, optional);
	}

	Token dot(boolean optional) throws ParsingException {
		return lexer.getNextToken(TokenType.DOT, optional);
	}

	Token lparen(boolean optional) throws ParsingException {
		skipSpace();
		return lexer.getNextToken(TokenType.LPAREN, optional);
	}

	Token rparen(boolean optional) throws ParsingException {
		skipSpace();
		return lexer.getNextToken(TokenType.RPAREN, optional);
	}

	Token comma(boolean optional) throws ParsingException {
		skipSpace();
		return lexer.getNextToken(TokenType.COMMA, optional);
	}

	Token keyword(Keyword kw, boolean optional) throws ParsingException {
		skipSpace();
		return lexer.getNextToken(kw, optional);
	}

	Token space(ParsingResult result, boolean optional) throws ParsingException {
		if(!optional)
			result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), ""));
		return lexer.getNextToken(TokenType.SPACE, optional);
	}

	/**
	 * integer :: S?D+
	 */
	int integer() throws ParsingException {
		try {
			StringBuffer buffer = new StringBuffer();
			Token token;
			if((token = sign(true)) != null)
				buffer.append(token.text);

			buffer.append(digit(false).text);
			while((token = digit(true)) != null)
				buffer.append(token.text);
			return Integer.parseInt(buffer.toString());
		} catch(ParsingException e) {
			throw new ParsingException(e.getPos(), "Expected an integer number");
		}
	}

	/**
	 * real :: S?D+(.D+)?(ES?D+)?
	 *    D :: (0|1|2|3|4|5|6|7|8|9)
	 *    S :: (+|-)
	 *    E :: (e|E)
	 */
	double real() throws ParsingException {
		try {
			StringBuffer buffer = new StringBuffer();
			Token token;

			// [-+]?
			if((token = sign(true)) != null)
				buffer.append(token.text);

			// [0-9]+
			buffer.append(digit(false).text);
			while((token = digit(true)) != null)
				buffer.append(token.text);

			if((token = dot(true)) != null) {
				// [.]?
				buffer.append(token.text);
				// [0-9]+
				buffer.append(digit(false).text);
				while((token = digit(true)) != null)
					buffer.append(token.text);
			}

			if((token = exp(true)) != null) {
				// [eE]
				buffer.append(token.text);
				// [-+]?
				if((token = sign(true)) != null)
					buffer.append(token.text);
				// [0-9]+
				buffer.append(digit(false).text);
				while((token = digit(true)) != null)
					buffer.append(token.text);

			}

			return Double.parseDouble(buffer.toString());
		} catch(ParsingException e) {
			throw new ParsingException(e.getPos(), "Expected a real number");
		}
	}

	/**
	 * mor :: (macro | real)
	 * @return
	 */
	NumberOrMacro mor() throws ParsingException {
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
	String macro() throws ParsingException {
		try {
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
		} catch(ParsingException e) {
			throw new ParsingException(e.getPos(), "Expected a macro name");
		}
	}

	/**
	 * tuple :: (real ,real)
	 */
	NumberOrMacro[] tuple(ParsingResult result) throws ParsingException {
		try {
			lparen(false);
		} catch(ParsingException e) {
			throw new ParsingException(e.getPos(), "Expected a tuple '(<number>, <number>)'");
		}
		result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), ""));

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
	NumberOrMacro[] triple(ParsingResult result) throws ParsingException {
		try {
			lparen(false);
		} catch(ParsingException e) {
			throw new ParsingException(e.getPos(), "Expected a 3-tuple '(<number>, <number>, <number>)'");
		}
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
	 * quadruple :: (real ,real, real, real)
	 */
	NumberOrMacro[] quadruple(ParsingResult result) throws ParsingException {
		try {
			lparen(false);
		} catch(ParsingException e) {
			throw new ParsingException(e.getPos(), "Expected a 4-tuple '(<number>, <number>, <number>, <number>)'");
		}
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
		comma(false);

		skipSpace();
		NumberOrMacro d = mor();
		skipSpace();
		rparen(false);

		return new NumberOrMacro[] {a, b, c, d};
	}

	/**
	 * rotation :: rotate by real degrees (horizontally | vertically | around triple)
	 */
	RotationAnimation rotation(int from, int to, ParsingResult result, int cursorpos) throws ParsingException {
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

	ResetTransformAnimation resetTransform(int from, int to, ParsingResult result, int cursorpos) throws ParsingException {
		if(keyword(GeneralKeyword.RESET_TRANSFORM, true) == null)
			return null;

		return new ResetTransformAnimation(from, to, center);
	}

	/**
	 * translation :: translate (horizontally by X | vertically by Y | by TRIPLE)
	 */
	TranslationAnimation translation(int from, int to, ParsingResult result, int cursorpos) throws ParsingException {
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
	ScaleAnimation zoom(int from, int to, ParsingResult result) throws ParsingException {
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
	Keyword channelproperty(ParsingResult result, int cursorpos) throws ParsingException {
		Keyword[] channelKeywords = kwFactory.getChannelKeywords();
		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos, kwFactory.getChannelKeywords())));

		for(Keyword cp : channelKeywords) {
			if(keyword(cp, true) != null) {
				return cp;
			}
		}
		throw new ParsingException(cursorpos, "Expected channel property");
	}

	/**
	 * nonchannelproperty :: (bounding box min x | bounding box max x | bounding box min y | bounding box max y |
	 *                        bounding box min z | bounding box max z | front clipping | back clipping |
	 *                        bounding box x | bounding box y | bounding box z)
	 */
	Keyword nonchannelproperty(ParsingResult result, int cursorpos) throws ParsingException {
		Keyword[] nonChannelKeywords = kwFactory.getNonChannelKeywords();
		for(Keyword cp : nonChannelKeywords) {
			if(keyword(cp, true) != null) {
				return cp;
			}
		}
		throw new ParsingException(cursorpos, "Expected non-channel property");
	}

	/**
	 *             change :: change (channel X channelproperty | renderingproperty) to (real | macro)
	 *    channelproperty :: (color min | color max | color gamma | alpha min | alpha max | alpha gamma | weight)
	 * nonchannelproperty :: (bounding box min x | bounding box max x | bounding box min y | bounding box max y |
	 *                        bounding box min z | bounding box max z | front clipping | back clipping)
	 */
	ChangeAnimation change(int from, int to, ParsingResult result, int cursorpos) throws ParsingException {
		if(keyword(GeneralKeyword.CHANGE, true) == null)
			return null;

		space(result, false);

		Keyword[] nonChannelKeywords = kwFactory.getNonChannelKeywords();
		Keyword[] choice = new Keyword[nonChannelKeywords.length + 2];
		int i = 0;
		choice[i++] = GeneralKeyword.CHANNEL;
		choice[i++] = GeneralKeyword.ALL_CHANNELS;
		for(Keyword rp : nonChannelKeywords)
			choice[i++] = rp;
		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos, choice)));

		int[] timelineIdcs = null;
		String[] autocompletionDescriptions = null;
		Map<String, double[]> replacements = null;
		int channel = -1;
		if(keyword(GeneralKeyword.CHANNEL, true) != null) {
			space(result, false);
			result.setAutocompletion(new IntegerAutocompletion("<channel>"));
			channel = integer() - 1;
			space(result, false);
			Keyword cp = channelproperty(result, cursorpos);
			timelineIdcs = cp.getRenderingStateProperties();
			autocompletionDescriptions = cp.getAutocompletionDescriptions();
			replacements = cp.getReplacementMap();
		} else if(keyword(GeneralKeyword.ALL_CHANNELS, true) != null) {
			channel = ChangeAnimation.ALL_CHANNELS;
			space(result, false);
			Keyword cp = channelproperty(result, cursorpos);
			timelineIdcs = cp.getRenderingStateProperties();
			autocompletionDescriptions = cp.getAutocompletionDescriptions();
			replacements = cp.getReplacementMap();
		}
		else {
			Keyword cp = nonchannelproperty(result, cursorpos);
			timelineIdcs = cp.getRenderingStateProperties();
			autocompletionDescriptions = cp.getAutocompletionDescriptions();
			replacements = cp.getReplacementMap();
		}

		if(replacements == null)
			replacements = new HashMap<String, double[]>();

		space(result, false);
		result.setAutocompletion(new StringAutocompletion(lexer.getIndex(), GeneralKeyword.TO.getKeyword()));
		keyword(GeneralKeyword.TO, false);

		space(result, false);

		ArrayList<String> compl = new ArrayList<String>(replacements.keySet());

		NumberOrMacro[] tgts = null;

		switch(timelineIdcs.length) {
		case 1:
			if(compl.isEmpty())
				result.setAutocompletion(new RealAutocompletion(autocompletionDescriptions[0]));
			else {
				compl.add(0, autocompletionDescriptions[0]);
				result.setAutocompletion(new ChoiceAutocompletion(lexer.getIndex(), compl.toArray(new String[] {})));
			}
			if(!compl.isEmpty()) { // replacments available
				Token token = lexer.getNextToken(replacements.keySet(), true);
				if(token == null) // not one of the replacement strings
					tgts = new NumberOrMacro[] { mor() };
				else {
					double[] vals = replacements.get(token.text);
					tgts = new NumberOrMacro[] { new NumberOrMacro(vals[0]) };
				}
			} else {
				tgts = new NumberOrMacro[] { mor() };
			}
			break;
		case 2:
			if(compl.isEmpty())
				result.setAutocompletion(new TupleAutocompletion(
						autocompletionDescriptions[0],
						autocompletionDescriptions[1]));
			else {
				compl.add(0, "(" + autocompletionDescriptions[0] + ", " + autocompletionDescriptions[1] + ")");
				result.setAutocompletion(new ChoiceAutocompletion(lexer.getIndex(), compl.toArray(new String[] {})));
			}
			if(!compl.isEmpty()) { // replacments available
				Token token = lexer.getNextToken(replacements.keySet(), true);
				if(token == null) // not one of the replacement strings
					tgts = tuple(result);
				else {
					double[] vals = replacements.get(token.text);
					tgts = new NumberOrMacro[] { new NumberOrMacro(vals[0]), new NumberOrMacro(vals[1]) };
				}
			} else {
				tgts = tuple(result);
			}
			break;
		case 3:
			if(compl.isEmpty())
				result.setAutocompletion(new TripleAutocompletion(
						autocompletionDescriptions[0],
						autocompletionDescriptions[1],
						autocompletionDescriptions[2]));
			else {
				compl.add(0, "(" + autocompletionDescriptions[0] + ", " + autocompletionDescriptions[1] + ", " + autocompletionDescriptions[2] + ")");
				result.setAutocompletion(new ChoiceAutocompletion(lexer.getIndex(), compl.toArray(new String[] {})));
			}
			if(!compl.isEmpty()) { // replacments available
				Token token = lexer.getNextToken(replacements.keySet(), true);
				if(token == null) // not one of the replacement strings
					tgts = triple(result);
				else {
					double[] vals = replacements.get(token.text);
					tgts = new NumberOrMacro[] { new NumberOrMacro(vals[0]), new NumberOrMacro(vals[1]), new NumberOrMacro(vals[2]) };
				}
			} else {
				tgts = triple(result);
			}
			break;
		case 4:
			if(compl.isEmpty())
				result.setAutocompletion(new QuadrupleAutocompletion(
						autocompletionDescriptions[0],
						autocompletionDescriptions[1],
						autocompletionDescriptions[2],
						autocompletionDescriptions[3]));
			else {
				compl.add(0, "(" + autocompletionDescriptions[0] + ", " + autocompletionDescriptions[1] + ", " + autocompletionDescriptions[2] + ", " + autocompletionDescriptions[3] + ")");
				result.setAutocompletion(new ChoiceAutocompletion(lexer.getIndex(), compl.toArray(new String[] {})));
			}
			if(!compl.isEmpty()) { // replacments available
				Token token = lexer.getNextToken(replacements.keySet(), true);
				if(token == null) // not one of the replacement strings
					tgts = quadruple(result);
				else {
					double[] vals = replacements.get(token.text);
					tgts = new NumberOrMacro[] { new NumberOrMacro(vals[0]), new NumberOrMacro(vals[1]), new NumberOrMacro(vals[2]), new NumberOrMacro(vals[3]) };
				}
			} else {
				tgts = quadruple(result);
			}
			break;
		}

		ChangeAnimation ca = new ChangeAnimation(from, to, channel, timelineIdcs, tgts);
		result.setResult(ca);

		return ca;
	}

	float[] transition() throws ParsingException {
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
	void action(int from, int to, ParsingResult result, int cursorpos) throws ParsingException {
		result.setAutocompletion(new ChoiceAutocompletion(
				lexer.getIndex(),
				lexer.getAutocompletionList(cursorpos, GeneralKeyword.ROTATE, GeneralKeyword.TRANSLATE, GeneralKeyword.ZOOM, GeneralKeyword.RESET_TRANSFORM, GeneralKeyword.CHANGE)));
		Animation ta = null;

		ta = rotation(from, to, result, cursorpos);
		if(ta == null)
			ta = translation(from, to, result, cursorpos);
		if(ta == null)
			ta = zoom(from, to, result);
		if(ta == null)
			ta = resetTransform(from, to, result, cursorpos);
		if(ta == null)
			ta = change(from, to, result, cursorpos);

		if(ta == null)
			throw new ParsingException(cursorpos, "Expected rotation, translation, zoom, reset transform or change for action");
		else
			result.setResult(ta);

		Token space = space(result, true);

		float[] transition = Transition.LINEAR.getTransition();
		if(space != null) {
			result.setAutocompletion(new ChoiceAutocompletion(
					lexer.getIndex(), lexer.getAutocompletionList(cursorpos, Transition.values())));
			transition = transition();
		}

		if(ta != null)
			ta.setBezierControls(transition[0], transition[1], transition[2], transition[3]);
	}

	/**
	 * line :: From frame integer to frame integer action
	 */
	public void line(ParsingResult result, int cursorpos) throws ParsingException {
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

	public static void parse(IKeywordFactory kwFactory, String line, int length, float[] center, ParsingResult result) throws ParsingException {
		Interpreter i = new Interpreter(kwFactory, line, center);
		i.line(result, length);
	}

	public static void parse(IKeywordFactory kwFactory, String line, float[] center, ParsingResult result) throws ParsingException {
		parse(kwFactory, line, line.length(), center, result);
	}

	public static void main(String...args) throws ParsingException {
		String input = "From frame 0 to frame 10 rotate by 30 degrees around (1, 0, 0)";

		ParsingResult res = new ParsingResult();
		Interpreter.parse(null, input, new float[3], res);
		System.out.println(res);
	}
}

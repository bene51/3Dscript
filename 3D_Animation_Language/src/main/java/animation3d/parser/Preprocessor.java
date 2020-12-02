package animation3d.parser;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import animation3d.textanim.NumberedLine;

public class Preprocessor {

	public static class PreprocessingException extends Exception {

		private static final long serialVersionUID = -1757691060685086864L;

		public PreprocessingException(String msg) {
			super(msg);
		}

		public PreprocessingException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}

	public static String getMacroSkeletonForFunction(String function) {
		return
				"script\n" +
				"function " + function + "(t) {\n" +
				"	return 0;\n" +
				"}\n";
	}

	public static Set<String> getMacroFunctions(String ttext) {
		StringBuffer text = new StringBuffer(ttext);
		MacroExtractor me = new MacroExtractor(text);
		try {
			me.parse();
		} catch(Exception e) {
			e.printStackTrace();
		}

		return me.scripts.keySet();
	}

	public static String getLineForCursor(String ttext, int pos) {
		StringBuffer text = new StringBuffer(ttext);
		MacroExtractor me = new MacroExtractor(text);
		try {
			me.parse();
		} catch(Exception e) {
			e.printStackTrace();
		}

		for(int m = 0; m < me.scriptStart.size(); m++) {
			int start = me.scriptStart.get(m);
			// macro starts beyond cursor pos: we are definitely not in the macro
			if(start > pos)
				break;

			// maybe the parser failed because the macro isn't finished yet;
			// then me.functionEnd might be smaller
			// treat this as inside a script
			if(m >= me.functionEnd.size() || pos <= me.functionEnd.get(m))
				return null;
		}

		// if we are here, cursorpos is not inside a macro, return the actual line

		if(pos >= text.length())
			pos = text.length() - 1;

		int p = 0;
		// find out the start pos of the current line
		int lineStart = 0;
		String lastLineWithoutDash = "";

		for(p = 0; p <= pos; p++) {
			if(ttext.regionMatches(p, "\r\n", 0, 2)) {
				String line = ttext.substring(lineStart, p).trim();
				if(line.endsWith(":"))
					lastLineWithoutDash = line.substring(0, line.length() - 1);
				lineStart = p + 2;
				p++;
			}
			else if(ttext.regionMatches(p, "\r", 0, 1)) {
				String line = ttext.substring(lineStart, p).trim();
				if(line.endsWith(":"))
					lastLineWithoutDash = line.substring(0, line.length() - 1);
				lineStart = p + 1;
			}
			else if(ttext.regionMatches(p, "\n", 0, 1)) {
				String line = ttext.substring(lineStart, p).trim();
				if(line.endsWith(":"))
					lastLineWithoutDash = line.substring(0, line.length() - 1);
				lineStart = p + 1;
			}
		}

		// find the end pos of the current line
//		int lineEnd = p;
//		for(p = 0; p <= pos; p++) {
//			if(ttext.regionMatches(p, "\\r\\n", 0, 2)) {
//				lineEnd = p - 1;
//				break;
//			}
//			else if(ttext.regionMatches(p, "\\r", 0, 1)) {
//				lineEnd = p - 1;
//				break;
//			}
//			else if(ttext.regionMatches(p, "\\n", 0, 1)) {
//				lineEnd = p - 1;
//				break;
//			}
//		}
//		return text.substring(lineStart, lineEnd + 1);
		String line = trimLeading(text.substring(lineStart, pos + 1));
		if(line.startsWith("//"))
			return null;

		if(line.trim().endsWith(":"))
			return null;

		if(line.startsWith("-")) {
			line = trimLeading(line.substring(1));
			line = lastLineWithoutDash + " " + line;
		}
		return line;

	}

	private static String trimLeading(String text) {
        int len = text.length();
        int st = 0;

        while ((st < len) && (text.charAt(st) <= ' ')) {
            st++;
        }
        return (st > 0) ? text.substring(st, len) : text;
    }

	public static void preprocess(String ttext, ArrayList<NumberedLine> lines, HashMap<String, String> scripts) throws PreprocessingException {
		lines.clear();
		scripts.clear();
		StringBuffer text = new StringBuffer(ttext);
		collectMacros(text, true, scripts);
		String rem = text.toString();

		StringReader sr = new StringReader(rem);
		BufferedReader buf = new BufferedReader(sr);
		String lastLineWithoutDash = "";
		String line;
		int lineno = -1;
		try {
			while((line = buf.readLine()) != null) {
				lineno++;
				// line = line.trim();
				if(line.trim().isEmpty())
					continue;
				if(line.trim().startsWith("//"))
					continue;
				if(line.trim().endsWith(":"))
					lastLineWithoutDash = line.trim().substring(0, line.trim().length() - 1);
				else if(line.startsWith("-")) {
					line = trimLeading(line.substring(1));
					line = lastLineWithoutDash + " " + line;
					lines.add(new NumberedLine(lineno, line));
				}
				else
					lines.add(new NumberedLine(lineno, line));
			}
		} catch(Exception ex) {
			throw new PreprocessingException("Error reading animations", ex);
		} finally {
		}
	}

	private static void collectMacros(StringBuffer textbuffer, boolean del, HashMap<String, String> ret) throws PreprocessingException {
		MacroExtractor me = new MacroExtractor(textbuffer);
		me.parse();
		if(del)
			me.deleteScripts();
		ret.clear();
		ret.putAll(me.scripts);
	}

	private static class MacroExtractor {

		private final StringBuffer textbuffer;
		List<Integer> scriptStart;
		List<Integer> functionStart;
		List<Integer> functionEnd;
		private HashMap<String, String> scripts;

		MacroExtractor(StringBuffer textbuffer) {
			this.textbuffer = textbuffer;
		}

		void parse() throws PreprocessingException {
			String text = textbuffer.toString();
			scriptStart   = new ArrayList<Integer>();
			functionStart = new ArrayList<Integer>();
			functionEnd   = new ArrayList<Integer>();
			scripts = new HashMap<String, String>();
			int nOpen = 0;
			int index = 0;
			while(index < text.length()) {
				while(index < text.length() && !text.regionMatches(true, index, "script", 0, "script".length())) {
					index++;
				}
				if(index == text.length())
					return;

				scriptStart.add(index);
				index++;

				while(index < text.length() && !text.regionMatches(true, index, "function", 0, "function".length())) {
					index++;
				}
				if(index == text.length())
					throw new PreprocessingException("Could not find keyword 'function' for script");

				int start = index;
				functionStart.add(start);

				index += "function".length();
				StringBuffer fun = new StringBuffer();
				while(index < text.length() && text.charAt(index) != '(')
					fun.append(text.charAt(index++));
				if(index == text.length())
					throw new PreprocessingException("Expected '(' after function name: " + fun);

				index++;

				while(index < text.length() && text.charAt(index) != '{')
					index++;

				if(index == text.length())
					throw new PreprocessingException("Could not find opening { for function " + fun);

				nOpen = 1;
				index++;

				while(index < text.length()) {
					char ch = text.charAt(index);
					if(ch == '{')
						nOpen++;
					if(ch == '}')
						nOpen--;

					if(nOpen == 0)
						break;
					index++;
				}

				if(index == text.length())
					throw new PreprocessingException("Could not find closing } for function " + fun);

				int stop = index;

				functionEnd.add(stop + 1);

				scripts.put(fun.toString().trim(), text.substring(start, stop + 1));

				index++;
			}
		}

		void deleteScripts() {
			for(int i = scriptStart.size() - 1; i >= 0; i--)
				textbuffer.delete(scriptStart.get(i), functionEnd.get(i));
		}
	}
}

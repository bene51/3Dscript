package parser;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	public static void preprocess(String ttext, ArrayList<String> lines, HashMap<String, String> scripts) throws PreprocessingException {
		lines.clear();
		scripts.clear();
		StringBuffer text = new StringBuffer(ttext);
		collectMacros(text, true, scripts);
		String rem = text.toString();

		StringReader sr = new StringReader(rem);
		BufferedReader buf = new BufferedReader(sr);
		String line;
		try {
			while((line = buf.readLine()) != null) {
				if(!line.trim().isEmpty())
					lines.add(line);
			}
		} catch(Exception ex) {
			throw new PreprocessingException("Error reading animations", ex);
		} finally {
		}
	}

	private static void collectMacros(StringBuffer textbuffer, boolean del, HashMap<String, String> ret) throws PreprocessingException {
		String text = textbuffer.toString();
		List<Integer> offsets = new ArrayList<Integer>();
		List<Integer> to = new ArrayList<Integer>();
		int nOpen = 0;
		int index = 0;
		while(index < text.length()) {
			while(index < text.length() && !text.regionMatches(true, index, "script", 0, "script".length())) {
				index++;
			}
			if(index == text.length())
				return;

			offsets.add(index);
			index++;

			while(index < text.length() && !text.regionMatches(true, index, "function", 0, "function".length())) {
				index++;
			}
			if(index == text.length())
				throw new PreprocessingException("Could not find keyword 'function' for script");

			int start = index;

			index += "function".length();
			StringBuffer fun = new StringBuffer();
			while(index < text.length() && text.charAt(index) != '(')
				fun.append(text.charAt(index++));
			if(index == text.length())
				throw new PreprocessingException("Could not detect function name");

			index++;

			while(index < text.length() && text.charAt(index) != '{')
				index++;

			if(index == text.length())
				throw new PreprocessingException("Could not find opening { for script");

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
				throw new PreprocessingException("Could not find closing } for script");

			int stop = index;

			to.add(stop + 1);

			ret.put(fun.toString().trim(), text.substring(start, stop + 1));

			index++;
		}

		for(int i = offsets.size() - 1; i >= 0; i--)
			textbuffer.delete(offsets.get(i), to.get(i));
	}
}

package editor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.IconGroup;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.RecordableTextAction;

import ij.Prefs;
import parser.Interpreter;
import parser.ParsingResult;
import parser.Preprocessor;
import textanim.Animation;
import textanim.IKeywordFactory;


public class EditorPane extends RSyntaxTextArea implements DocumentListener {

	private String fallBackBaseName;
	private File curFile;
	private long fileLastModified;
	private Gutter gutter;
	private IconGroup iconGroup;
	private int modifyCount;

	private boolean undoInProgress;
	private boolean redoInProgress;

	private final IKeywordFactory kwFactory;

	/**
	 * Constructor.
	 */
	public EditorPane(final IKeywordFactory kwFactory) {
		this.kwFactory = kwFactory;
		setLineWrap(false);
		setTabSize(8);

		getActionMap()
			.put(DefaultEditorKit.nextWordAction, wordMovement(+1, false));
		getActionMap().put(DefaultEditorKit.selectionNextWordAction,
			wordMovement(+1, true));
		getActionMap().put(DefaultEditorKit.previousWordAction,
			wordMovement(-1, false));
		getActionMap().put(DefaultEditorKit.selectionPreviousWordAction,
			wordMovement(-1, true));
		ToolTipManager.sharedInstance().registerComponent(this);
		getDocument().addDocumentListener(this);

		CompletionProvider provider = new AnimationCompletionProvider(kwFactory);
		final AutoCompletion ac = new AutoCompletion(provider);
		ac.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK, false));
		ac.setAutoCompleteEnabled(true);
		ac.setAutoActivationDelay(5);
		ac.setAutoActivationEnabled(true);
		ac.setAutoCompleteSingleChoices(true);
		ac.setParameterAssistanceEnabled(true);

		ac.install(this);

		this.getDocument().addDocumentListener(new DocumentListener() {

			private boolean isNewline(char c) {
				return c == '\r' || c == '\n';
			}

			private String getLineForPosition(String text, int pos) {
				try {
					int line = getLineOfOffset(pos);
					int lineStart = getLineStartOffset(line);
					int lineEnd = getLineEndOffset(line);
					return text.substring(lineStart, lineEnd);
				} catch (BadLocationException e) {
					return null;
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				Document d = e.getDocument();
				String ttext = null;
				try {
					ttext = d.getText(0, d.getLength());
				} catch(BadLocationException ex) {
					return;
				}

				final int dot = getCaretPosition();
				if(dot <= 0)
					return;

				char lastCharacter = ttext.charAt(dot);
				System.out.println("EditorPane: insertUpdate(): lastCharacter = " + lastCharacter + "(" + Integer.toHexString(lastCharacter) + ")");
				if(!isNewline(lastCharacter))
					return;

				String prevLine = getLineForPosition(ttext, dot).trim();
				System.out.println("EditorPane: insertUpdate(): prevLine = " + prevLine);
				if(prevLine.endsWith(":") || prevLine.startsWith("-")) {
					final String text = new StringBuffer(ttext).insert(dot + 1, "- ").toString();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							setText(text);
							setCaretPosition(dot + 3);
						}
					});
				}

				String line = Preprocessor.getLineForCursor(ttext, dot - 1);
				System.out.println("EditorPane: insertUpdate(): line = " + line);
				ParsingResult result = new ParsingResult();
				try {
					Interpreter.parse(kwFactory, line, new float[] {}, result);
				} catch(Exception ex) {
					return;
				}
				Animation a = result.getResult();
				Set<String> availableFunctions = Preprocessor.getMacroFunctions(ttext);
				if(a == null || a.getUsedMacroFunctions().isEmpty())
					return;

				final StringBuffer toAppend = new StringBuffer();

				for(String funName : a.getUsedMacroFunctions())
					if(!availableFunctions.contains(funName))
						toAppend.append(Preprocessor.getMacroSkeletonForFunction(funName));

				int nNewLines = availableFunctions.size() == 0 ? 3 : 2;

				final String text = ttext;
				if(toAppend.length() > 0) {
					for(int i = 0; i < nNewLines; i++)
						if(!isNewline(text.charAt(text.length() - i - 1)))
							toAppend.insert(0, '\n');

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							setText(text + toAppend.toString());
							setCaretPosition(dot + 1);
						}
					});
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {}

			@Override
			public void changedUpdate(DocumentEvent e) {}
		});
	}

	@Override
	public void setTabSize(final int width) {
		if (getTabSize() != width) super.setTabSize(width);
	}

	/**
	 * Add this {@link EditorPane} with scrollbars to a container.
	 *
	 * @param container the container to add this editor pane to.
	 */
	public void embedWithScrollbars(final Container container) {
		container.add(wrappedInScrollbars());
	}

	/**
	 * @return this EditorPane wrapped in a {@link RTextScrollPane}.
	 */
	public RTextScrollPane wrappedInScrollbars() {
		final RTextScrollPane sp = new RTextScrollPane(this);
		sp.setPreferredSize(new Dimension(600, 350));
		sp.setIconRowHeaderEnabled(true);

		gutter = sp.getGutter();
//		iconGroup = new IconGroup("bullets", "images/", null, "png", null);
//		gutter.setBookmarkIcon(iconGroup.getIcon("var"));

		URL url = ClassLoader.getSystemClassLoader().getResource("eye.png");
		ImageIcon icon = new ImageIcon(url);
		gutter.setBookmarkIcon(icon);
		gutter.setBookmarkingEnabled(true);

		return sp;
	}

	/**
	 * TODO
	 *
	 * @param direction
	 * @param select
	 * @return
	 */
	RecordableTextAction wordMovement(final int direction, final boolean select) {
		final String id = "WORD_MOVEMENT_" + select + direction;
		return new RecordableTextAction(id) {

			@Override
			public void actionPerformedImpl(final ActionEvent e,
				final RTextArea textArea)
			{
				int pos = textArea.getCaretPosition();
				final int end = direction < 0 ? 0 : textArea.getDocument().getLength();
				while (pos != end && !isWordChar(textArea, pos))
					pos += direction;
				while (pos != end && isWordChar(textArea, pos))
					pos += direction;
				if (select) textArea.moveCaretPosition(pos);
				else textArea.setCaretPosition(pos);
			}

			@Override
			public String getMacroID() {
				return id;
			}

			boolean isWordChar(final RTextArea textArea, final int pos) {
				try {
					final char c =
						textArea.getText(pos + (direction < 0 ? -1 : 0), 1).charAt(0);
					return c > 0x7f || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
						(c >= '0' && c <= '9') || c == '_';
				}
				catch (final BadLocationException e) {
					return false;
				}
			}
		};
	}

	@Override
	public void undoLastAction() {
		undoInProgress = true;
		super.undoLastAction();
		undoInProgress = false;
	}

	@Override
	public void redoLastAction() {
		redoInProgress = true;
		super.redoLastAction();
		redoInProgress = false;
	}

	/**
	 * @return <code>true</code> if the file in this {@link EditorPane} was
	 *         changes since it was last saved.
	 */
	public boolean fileChanged() {
		return modifyCount != 0;
	}

	@Override
	public void insertUpdate(final DocumentEvent e) {
		modified();
	}

	@Override
	public void removeUpdate(final DocumentEvent e) {
		modified();
	}

	// triggered only by syntax highlighting
	@Override
	public void changedUpdate(final DocumentEvent e) {}

	/**
	 * Set the title according to whether the file was modified or not.
	 */
	protected void modified() {
		if (undoInProgress) {
			modifyCount--;
		}
		else if (redoInProgress || modifyCount >= 0) {
			modifyCount++;
		}
		else {
			// not possible to get back to clean state
			modifyCount = Integer.MIN_VALUE;
		}
	}

	/**
	 * @return <code>true</code> if the file in this {@link EditorPane} is an
	 *         unsaved new file which has not been edited yet.
	 */
	public boolean isNew() {
		return !fileChanged() && curFile == null && fallBackBaseName == null &&
			getDocument().getLength() == 0;
	}

	/**
	 * @return true if the file in this {@link EditorPane} was changed ouside of
	 *         this {@link EditorPane} since it was openend.
	 */
	public boolean wasChangedOutside() {
		return curFile != null && curFile.exists() &&
			curFile.lastModified() != fileLastModified;
	}

	/**
	 * Write the contents of this {@link EditorPane} to given file.
	 *
	 * @param file File to write the contents of this editor to.
	 * @throws IOException
	 */
	public void write(final File file) throws IOException {
		final File dir = file.getParentFile();
		if (dir != null && !dir.exists()) {
			// create needed parent directories
			if (!dir.mkdirs()) {
				throw new IOException("Cannot create directory: " + dir);
			}
		}
		final BufferedWriter outFile =
			new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
				"UTF-8"));
		outFile.write(getText());
		outFile.close();
		modifyCount = 0;
		fileLastModified = file.lastModified();
	}

	/**
	 * Load editor contents from given file.
	 *
	 * @param file file to load.
	 * @throws IOException
	 */
	public void open(final File file) throws IOException {
		final File oldFile = curFile;
		curFile = null;
		if (file == null) setText("");
		else {
			int line = 0;
			try {
				if (file.getCanonicalPath().equals(oldFile.getCanonicalPath())) line =
					getCaretLineNumber();
			}
			catch (final Exception e) { /* ignore */}
			if (!file.exists()) {
				modifyCount = Integer.MIN_VALUE;
				setFileName(file);
				return;
			}
			final StringBuffer string = new StringBuffer();
			final BufferedReader reader =
				new BufferedReader(new InputStreamReader(new FileInputStream(file),
					"UTF-8"));
			final char[] buffer = new char[16384];
			for (;;) {
				final int count = reader.read(buffer);
				if (count < 0) break;
				string.append(buffer, 0, count);
			}
			reader.close();
			setText(string.toString());
			curFile = file;
			if (line > getLineCount()) line = getLineCount() - 1;
			try {
				setCaretPosition(getLineStartOffset(line));
			}
			catch (final BadLocationException e) { /* ignore */}
		}
		discardAllEdits();
		modifyCount = 0;
		fileLastModified = file == null || !file.exists() ? 0 : file.lastModified();
	}

	/**
	 * Set the name to use for new files. The file extension for the current
	 * script language is added automatically.
	 *
	 * @param baseName the fallback base name.
	 */
	public void setFileName(final String baseName) {
		fallBackBaseName = baseName;
	}

	/**
	 * TODO
	 *
	 * @param file
	 */
	public void setFileName(final File file) {
		curFile = file;

		if (file != null) {
			fallBackBaseName = null;
		}
		fileLastModified = file == null || !file.exists() ? 0 : file.lastModified();
	}

	/**
	 * @return name of the currently open file.
	 */
	protected String getFileName() {
		if (curFile != null) return curFile.getName();
		String extension = "";
		return (fallBackBaseName == null ? "New_" : fallBackBaseName) + extension;
	}


	/**
	 * Get file currently open in this {@link EditorPane}.
	 *
	 * @return the file.
	 */
	public File getFile() {
		return curFile;
	}

	/**
	 * @return font size of this editor.
	 */
	public float getFontSize() {
		return getFont().getSize2D();
	}

	/**
	 * Set the font size for this editor.
	 *
	 * @param size the new font size.
	 */
	public void setFontSize(final float size) {
		increaseFontSize(size / getFontSize());
	}

	/**
	 * Increase font size of this editor by a given factor.
	 *
	 * @param factor Factor to increase font size.
	 */
	public void increaseFontSize(final float factor) {
		if (factor == 1) return;
		final SyntaxScheme scheme = getSyntaxScheme();
		for (int i = 0; i < scheme.getStyleCount(); i++) {
			final Style style = scheme.getStyle(i);
			if (style == null || style.font == null) continue;
			final float size = Math.max(5, style.font.getSize2D() * factor);
			style.font = style.font.deriveFont(size);
		}
		final Font font = getFont();
		final float size = Math.max(5, font.getSize2D() * factor);
		setFont(font.deriveFont(size));
		setSyntaxScheme(scheme);
		Component parent = getParent();
		if (parent instanceof JViewport) {
			parent = parent.getParent();
			if (parent instanceof JScrollPane) {
				parent.repaint();
			}
		}
		parent.repaint();
	}

	/**
	 * @return the underlying {@link RSyntaxDocument}.
	 */
	protected RSyntaxDocument getRSyntaxDocument() {
		return (RSyntaxDocument) getDocument();
	}

	/**
	 * Add/remove bookmark for line containing the cursor/caret.
	 */
	public void toggleBookmark() {
		toggleBookmark(getCaretLineNumber());
	}

	/**
	 * Add/remove bookmark for a specific line.
	 *
	 * @param line line to toggle the bookmark on.
	 */
	public void toggleBookmark(final int line) {
		if (gutter != null) {
			try {
				gutter.toggleBookmark(line);
			}
			catch (final BadLocationException e) {
				/* ignore */
				System.out.println("Cannot toggle bookmark at this location.");
			}
		}
	}

	@Override
	public void convertTabsToSpaces() {
		beginAtomicEdit();
		try {
			super.convertTabsToSpaces();
		}
		catch (final Throwable t) {
			t.printStackTrace();
		}
		finally {
			endAtomicEdit();
		}
	}

	@Override
	public void convertSpacesToTabs() {
		beginAtomicEdit();
		try {
			super.convertSpacesToTabs();
		}
		catch (final Throwable t) {
			t.printStackTrace();
		}
		finally {
			endAtomicEdit();
		}
	}

	// --- Preferences ---
	public static final String FONT_SIZE_PREFS = "animation.editor.FontSize";
	public static final String LINE_WRAP_PREFS = "animation.editor.WrapLines";
	public static final String TAB_SIZE_PREFS = "animation.editor.TabSize";
	public static final String TABS_EMULATED_PREFS = "animation.editor.TabsEmulated";

	public static final int DEFAULT_TAB_SIZE = 4;

	/**
	 * Loads the preferences for the Tab and apply them.
	 */
	public void loadPreferences() {
		resetTabSize();
		setFontSize((float)(Prefs.getDouble(FONT_SIZE_PREFS, getFontSize())));
		setLineWrap(Prefs.getBoolean(LINE_WRAP_PREFS, getLineWrap()));
		setTabsEmulated(Prefs.getBoolean(TABS_EMULATED_PREFS,
			getTabsEmulated()));
	}

	/**
	 * Retrieves and saves the preferences to the persistent store
	 */
	public void savePreferences() {
		Prefs.set(TAB_SIZE_PREFS, getTabSize());
		Prefs.set(FONT_SIZE_PREFS, getFontSize());
		Prefs.set(LINE_WRAP_PREFS, getLineWrap());
		Prefs.set(TABS_EMULATED_PREFS, getTabsEmulated());
		Prefs.savePreferences();
	}

	/**
	 * Reset tab size to current preferences.
	 */
	public void resetTabSize() {
		setTabSize(Prefs.getInt(TAB_SIZE_PREFS, DEFAULT_TAB_SIZE));
	}

}

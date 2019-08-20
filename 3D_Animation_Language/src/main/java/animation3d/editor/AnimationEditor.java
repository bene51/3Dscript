package animation3d.editor;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import animation3d.parser.Keyword.GeneralKeyword;
import animation3d.textanim.Animator;
import animation3d.textanim.CustomDecimalFormat;
import animation3d.textanim.IRecordingProvider;
import animation3d.textanim.IRecordingProvider.RecordingItem;
import animation3d.textanim.IRenderer3D;
import animation3d.textanim.RenderingState;
import animation3d.util.Transform;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;


@SuppressWarnings("serial")
public class AnimationEditor extends JFrame implements ActionListener, ChangeListener, DocumentListener, Animator.Listener
{
//	public static void main(String[] args) {
//		new AnimationEditor(null).setVisible(true);
//	}

	public static final String WINDOW_HEIGHT = "animation.editor.height";
	public static final String WINDOW_WIDTH = "animation.editor.width";
	public static final int DEFAULT_WINDOW_WIDTH = 800;
	public static final int DEFAULT_WINDOW_HEIGHT = 600;

	private JTabbedPane tabbed;
	private JMenuItem newFile, open, save, saveas,
			close, undo, redo, cut, copy, paste, find, replace, selectAll, kill,
			gotoLine,
			findNext, findPrevious, clearScreen,
			nextTab, previousTab, runSelection, run,
			decreaseFontSize, increaseFontSize, chooseFontSize,
			savePreferences,
//			recordContrast,
			recordTransformation,
//			recordCropping,
			recordTransitionStart, recordTransitionEnd;
	private List<JMenuItem> customRecording = new ArrayList<JMenuItem>();

	private JMenu tabsMenu, fontSizeMenu, tabSizeMenu, runMenu, recordMenu;

	private int tabsMenuTabsStart;
	private Set<JMenuItem> tabsMenuItems;
	private FindAndReplaceDialog findDialog;
	private JCheckBoxMenuItem wrapLines, tabsEmulated;
	private JTextArea errorScreen = new JTextArea();

	private final IRenderer3D renderer;
	private final Animator animator;

	private final IRecordingProvider recordingProvider;

	public AnimationEditor(IRenderer3D renderer, IRecordingProvider recordingProvider) {
		this(renderer, new Animator(renderer), recordingProvider);
	}

	public AnimationEditor(IRenderer3D renderer, Animator animator, IRecordingProvider recordingProvider) {
		super("Script Editor");
		this.renderer = renderer;
		this.animator = animator;
		this.recordingProvider = recordingProvider;
		animator.addAnimationListener(this);

		loadPreferences();

		// Initialize menu
		final int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		final int shift = ActionEvent.SHIFT_MASK;
		final JMenuBar mbar = new JMenuBar();
		setJMenuBar(mbar);

		final JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);
		newFile = addToMenu(file, "New", KeyEvent.VK_N, ctrl);
		newFile.setMnemonic(KeyEvent.VK_N);
		open = addToMenu(file, "Open...", KeyEvent.VK_O, ctrl);
		open.setMnemonic(KeyEvent.VK_O);
		save = addToMenu(file, "Save", KeyEvent.VK_S, ctrl);
		save.setMnemonic(KeyEvent.VK_S);
		saveas = addToMenu(file, "Save as...", 0, 0);
		saveas.setMnemonic(KeyEvent.VK_A);
		file.addSeparator();
		file.addSeparator();
		close = addToMenu(file, "Close", KeyEvent.VK_W, ctrl);

		mbar.add(file);

		final JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);
		undo = addToMenu(edit, "Undo", KeyEvent.VK_Z, ctrl);
		redo = addToMenu(edit, "Redo", KeyEvent.VK_Y, ctrl);
		edit.addSeparator();
		selectAll = addToMenu(edit, "Select All", KeyEvent.VK_A, ctrl);
		cut = addToMenu(edit, "Cut", KeyEvent.VK_X, ctrl);
		copy = addToMenu(edit, "Copy", KeyEvent.VK_C, ctrl);
		paste = addToMenu(edit, "Paste", KeyEvent.VK_V, ctrl);
		edit.addSeparator();
		find = addToMenu(edit, "Find...", KeyEvent.VK_F, ctrl);
		find.setMnemonic(KeyEvent.VK_F);
		findNext = addToMenu(edit, "Find Next", KeyEvent.VK_F3, 0);
		findNext.setMnemonic(KeyEvent.VK_N);
		findPrevious = addToMenu(edit, "Find Previous", KeyEvent.VK_F3, shift);
		findPrevious.setMnemonic(KeyEvent.VK_P);
		replace = addToMenu(edit, "Find and Replace...", KeyEvent.VK_H, ctrl);
		gotoLine = addToMenu(edit, "Goto line...", KeyEvent.VK_G, ctrl);
		gotoLine.setMnemonic(KeyEvent.VK_G);
		edit.addSeparator();

		// Font adjustments
		decreaseFontSize =
			addToMenu(edit, "Decrease font size", KeyEvent.VK_MINUS, ctrl);
		decreaseFontSize.setMnemonic(KeyEvent.VK_D);
		increaseFontSize =
			addToMenu(edit, "Increase font size", KeyEvent.VK_PLUS, ctrl);
		increaseFontSize.setMnemonic(KeyEvent.VK_C);

		fontSizeMenu = new JMenu("Font sizes");
		fontSizeMenu.setMnemonic(KeyEvent.VK_Z);
		final boolean[] fontSizeShortcutUsed = new boolean[10];
		final ButtonGroup buttonGroup = new ButtonGroup();
		for (final int size : new int[] { 8, 10, 12, 16, 20, 28, 42 }) {
			final JRadioButtonMenuItem item =
				new JRadioButtonMenuItem("" + size + " pt");
			item.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent event) {
					getEditorPane().setFontSize(size);
					updateTabAndFontSize(false);
				}
			});
			for (final char c : ("" + size).toCharArray()) {
				final int digit = c - '0';
				if (!fontSizeShortcutUsed[digit]) {
					item.setMnemonic(KeyEvent.VK_0 + digit);
					fontSizeShortcutUsed[digit] = true;
					break;
				}
			}
			buttonGroup.add(item);
			fontSizeMenu.add(item);
		}
		chooseFontSize = new JRadioButtonMenuItem("Other...", false);
		chooseFontSize.setMnemonic(KeyEvent.VK_O);
		chooseFontSize.addActionListener(this);
		buttonGroup.add(chooseFontSize);
		fontSizeMenu.add(chooseFontSize);
		edit.add(fontSizeMenu);

		// Add tab size adjusting menu
		tabSizeMenu = new JMenu("Tab sizes");
		tabSizeMenu.setMnemonic(KeyEvent.VK_T);
		final ButtonGroup bg = new ButtonGroup();
		for (final int size : new int[] { 2, 4, 8 }) {
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem("" + size);
			item.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent event) {
					getEditorPane().setTabSize(size);
					updateTabAndFontSize(false);
				}
			});
			item.setMnemonic(KeyEvent.VK_0 + (size % 10));
			bg.add(item);
			tabSizeMenu.add(item);
		}
		edit.add(tabSizeMenu);

		wrapLines = new JCheckBoxMenuItem("Wrap lines");
		wrapLines.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				getEditorPane().setLineWrap(wrapLines.getState());
			}
		});
		edit.add(wrapLines);

		// Add Tab inserts as spaces
		tabsEmulated = new JCheckBoxMenuItem("Tab key inserts spaces");
		tabsEmulated.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				getEditorPane().setTabsEmulated(tabsEmulated.getState());
			}
		});
		edit.add(tabsEmulated);

		savePreferences = addToMenu(edit, "Save Preferences", 0, 0);

		edit.addSeparator();

		clearScreen = addToMenu(edit, "Clear output panel", 0, 0);
		clearScreen.setMnemonic(KeyEvent.VK_L);


		edit.addSeparator();
		mbar.add(edit);


		recordMenu = new JMenu("Record");
		if(recordingProvider != null) {
			for(RecordingItem item : recordingProvider)
				customRecording.add(addToMenu(recordMenu, item.getCommand(), 0, 0));
			recordMenu.addSeparator();
		}
		recordTransitionStart = addToMenu(recordMenu, "Start recording transition", 0, 0);
		recordTransitionEnd = addToMenu(recordMenu, "Stop recording transition", 0, 0);
		mbar.add(recordMenu);


		runMenu = new JMenu("Run");
		runMenu.setMnemonic(KeyEvent.VK_R);

		run = addToMenu(runMenu, "Run", KeyEvent.VK_R, ctrl);

		runSelection =
			addToMenu(runMenu, "Run selected code", KeyEvent.VK_R, ctrl | shift);
		runSelection.setMnemonic(KeyEvent.VK_S);


		runMenu.addSeparator();

		kill = addToMenu(runMenu, "Cancel animation", 0, 0);
		kill.setMnemonic(KeyEvent.VK_K);
		kill.setEnabled(false);

		mbar.add(runMenu);

		tabsMenu = new JMenu("Tabs");
		tabsMenu.setMnemonic(KeyEvent.VK_A);
		nextTab = addToMenu(tabsMenu, "Next Tab", KeyEvent.VK_PAGE_DOWN, ctrl);
		nextTab.setMnemonic(KeyEvent.VK_N);
		previousTab =
			addToMenu(tabsMenu, "Previous Tab", KeyEvent.VK_PAGE_UP, ctrl);
		previousTab.setMnemonic(KeyEvent.VK_P);
		tabsMenu.addSeparator();
		tabsMenuTabsStart = tabsMenu.getItemCount();
		tabsMenuItems = new HashSet<JMenuItem>();
		mbar.add(tabsMenu);

		// Add the editor and output area
		tabbed = new JTabbedPane();
		tabbed.addChangeListener(this);
		open(null); // make sure the editor pane is added

		tabbed.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		getContentPane().setLayout(
			new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(tabbed);

		// for Eclipse and MS Visual Studio lovers
		addAccelerator(nextTab, KeyEvent.VK_PAGE_DOWN, ctrl, true);
		addAccelerator(previousTab, KeyEvent.VK_PAGE_UP, ctrl, true);

		addAccelerator(increaseFontSize, KeyEvent.VK_EQUALS, ctrl | shift, true);

		// make sure that the window is not closed by accident
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				if (!confirmClose()) return;
				dispose();
			}
		});

		addWindowFocusListener(new WindowAdapter() {

			@Override
			public void windowGainedFocus(final WindowEvent e) {
				checkForOutsideChanges();
			}
		});

		final Font font = new Font("Courier", Font.PLAIN, 12);
		errorScreen.setFont(font);
		errorScreen.setEditable(false);
		errorScreen.setLineWrap(true);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		try {
			if (SwingUtilities.isEventDispatchThread()) {
				pack();
			}
			else {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						pack();
					}
				});
			}
		}
		catch (final Exception ie) {
			/* ignore */
		}
		findDialog = new FindAndReplaceDialog(this);

		// Save the size of the window in the preferences
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				saveWindowSizeToPrefs();
			}
		});

		setLocationRelativeTo(null); // center on screen

		open(null);

		final EditorPane editorPane = getEditorPane();
		editorPane.requestFocus();
	}

	public JTextArea getErrorScreen() { return errorScreen; }

	public void setErrorScreen(final JTextArea errorScreen) {
		this.errorScreen = errorScreen;
	}

	/**
	 * Check whether the file was edited outside of this {@link EditorPane} and
	 * ask the user whether to reload.
	 */
	public void checkForOutsideChanges() {
		final EditorPane editorPane = getEditorPane();
		if (editorPane.wasChangedOutside()) {
			reload("The file " + editorPane.getFile().getName() +
				" was changed outside of the editor");
		}

	}

	/**
	 * Loads the preferences for the JFrame from file
	 */
	public void loadPreferences() {
		final Dimension dim = getSize();

		// If a dimension is 0 then use the default dimension size
		if (0 == dim.width) {
			dim.width = DEFAULT_WINDOW_WIDTH;
		}
		if (0 == dim.height) {
			dim.height = DEFAULT_WINDOW_HEIGHT;
		}

		setPreferredSize(new Dimension(
				Prefs.getInt(WINDOW_WIDTH, dim.width),
				Prefs.getInt(WINDOW_HEIGHT, dim.height)));
	}

	/**
	 * Saves the window size to preferences.
	 * <p>
	 * Separated from savePreferences because we always want to save the window
	 * size when it's resized, however, we don't want to automatically save the
	 * font, tab size, etc. without the user pressing "Save Preferences"
	 * </p>
	 */
	public void saveWindowSizeToPrefs() {
		final Dimension dim = getSize();
		Prefs.set(WINDOW_HEIGHT, dim.height);
		Prefs.set(WINDOW_WIDTH, dim.width);
		try {
			Prefs.savePreferences();
		} catch(Exception e) {}
	}

	final public RSyntaxTextArea getTextArea() {
		return getEditorPane();
	}

	/**
	 * Get the currently selected tab.
	 *
	 * @return The currently selected tab. Never null.
	 */
	public TextEditorTab getTab() {
		int index = tabbed.getSelectedIndex();
		if (index < 0) {
			// should not happen, but safety first.
			if (tabbed.getTabCount() == 0) {
				// should not happen either, but, again, safety first.
				createNewDocument();
			}

			// Ensure the new document is returned - otherwise we would pass
			// the negative index to the getComponentAt call below.
			tabbed.setSelectedIndex(0);
			index = 0;
		}
		return (TextEditorTab) tabbed.getComponentAt(index);
	}

	/**
	 * Get tab at provided index.
	 *
	 * @param index the index of the tab.
	 * @return the {@link TextEditorTab} at given index or <code>null</code>.
	 */
	public TextEditorTab getTab(final int index) {
		return (TextEditorTab) tabbed.getComponentAt(index);
	}

	/**
	 * Return the {@link EditorPane} of the currently selected
	 * {@link TextEditorTab}.
	 *
	 * @return the current {@link EditorPane}. Never <code>null</code>.
	 */
	public EditorPane getEditorPane() {
		return getTab().editorPane;
	}

	public JMenuItem addToMenu(final JMenu menu, final String menuEntry,
		final int key, final int modifiers)
	{
		final JMenuItem item = new JMenuItem(menuEntry);
		menu.add(item);
		if (key != 0) item.setAccelerator(KeyStroke.getKeyStroke(key, modifiers));
		item.addActionListener(this);
		return item;
	}

	protected static class AcceleratorTriplet {

		JMenuItem component;
		int key, modifiers;
	}

	protected List<AcceleratorTriplet> defaultAccelerators =
		new ArrayList<AcceleratorTriplet>();

	public void addAccelerator(final JMenuItem component, final int key,
		final int modifiers)
	{
		addAccelerator(component, key, modifiers, false);
	}

	public void addAccelerator(final JMenuItem component, final int key,
		final int modifiers, final boolean record)
	{
		if (record) {
			final AcceleratorTriplet triplet = new AcceleratorTriplet();
			triplet.component = component;
			triplet.key = key;
			triplet.modifiers = modifiers;
			defaultAccelerators.add(triplet);
		}

		final RSyntaxTextArea textArea = getTextArea();
		if (textArea != null) addAccelerator(textArea, component, key, modifiers);
	}

	public void addAccelerator(final RSyntaxTextArea textArea,
		final JMenuItem component, final int key, final int modifiers)
	{
		textArea.getInputMap().put(KeyStroke.getKeyStroke(key, modifiers),
			component);
		textArea.getActionMap().put(component, new AbstractAction() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (!component.isEnabled()) return;
				final ActionEvent event = new ActionEvent(component, 0, "Accelerator");
				AnimationEditor.this.actionPerformed(event);
			}
		});
	}

	public void addDefaultAccelerators(final RSyntaxTextArea textArea) {
		for (final AcceleratorTriplet triplet : defaultAccelerators)
			addAccelerator(textArea, triplet.component, triplet.key,
				triplet.modifiers);
	}

	public void createNewDocument() {
		open(null);
	}

	public void createNewDocument(final String title, final String text) {
		open(null);
		final EditorPane editorPane = getEditorPane();
		editorPane.setText(text);
		setEditorPaneFileName(title);
	}

	public boolean fileChanged() {
		return getEditorPane().fileChanged();
	}

	public boolean handleUnsavedChanges() {
		if (!fileChanged()) return true;

		switch (JOptionPane.showConfirmDialog(this, "Do you want to save changes?")) {
			case JOptionPane.NO_OPTION:
				return true;
			case JOptionPane.YES_OPTION:
				if (save()) return true;
		}

		return false;
	}

	@Override
	public void actionPerformed(final ActionEvent ae) {
		final Object source = ae.getSource();
		int i = 0;
		for(JMenuItem ri : customRecording) {
			if(source == ri) {
				handleCustomRecording(i);
				return;
			}
			i++;
		}
		if (source == newFile) createNewDocument();
		else if (source == open) {
			final EditorPane editorPane = getEditorPane();
			final File defaultDir =
				editorPane.getFile() != null ? editorPane.getFile().getParentFile() : null;
			final File file = openWithDialog(defaultDir);
			if (file != null) new Thread() {

				@Override
				public void run() {
					open(file);
				}
			}.start();
			return;
		}
		else if (source == save) save();
		else if (source == saveas) saveAs();
		else if (source == run) runText(false);
		else if (source == recordTransformation) recordTransformation();
		else if (source == recordTransitionStart) recordTransitionStart();
		else if (source == recordTransitionEnd) recordTransitionEnd();
		else if (source == runSelection) runText(true);
		else if (source == kill) cancelAnimation();
		else if (source == close) if (tabbed.getTabCount() < 2) processWindowEvent(new WindowEvent(
			this, WindowEvent.WINDOW_CLOSING));
		else {
			if (!handleUnsavedChanges()) return;
			int index = tabbed.getSelectedIndex();
			removeTab(index);
			if (index > 0) index--;
			switchTo(index);
		}
		else if (source == cut) getTextArea().cut();
		else if (source == copy) getTextArea().copy();
		else if (source == paste) getTextArea().paste();
		else if (source == undo) getTextArea().undoLastAction();
		else if (source == redo) getTextArea().redoLastAction();
		else if (source == find) findOrReplace(false);
		else if (source == findNext) findDialog.searchOrReplace(false);
		else if (source == findPrevious) findDialog.searchOrReplace(false, false);
		else if (source == replace) findOrReplace(true);
		else if (source == gotoLine) gotoLine();
		else if (source == selectAll) {
			getTextArea().setCaretPosition(0);
			getTextArea().moveCaretPosition(getTextArea().getDocument().getLength());
		}
		else if (source == clearScreen) {
			getTab().getScreen().setText("");
		}
		else if (source == savePreferences) {
			getEditorPane().savePreferences();
		}
		else if (source == increaseFontSize || source == decreaseFontSize) {
			getEditorPane().increaseFontSize(
				(float) (source == increaseFontSize ? 1.2 : 1 / 1.2));
			updateTabAndFontSize(false);
		}
		else if (source == nextTab) switchTabRelative(1);
		else if (source == previousTab) switchTabRelative(-1);
		else if (handleTabsMenu(source)) return;
	}

	protected boolean handleTabsMenu(final Object source) {
		if (!(source instanceof JMenuItem)) return false;
		final JMenuItem item = (JMenuItem) source;
		if (!tabsMenuItems.contains(item)) return false;
		for (int i = tabsMenuTabsStart; i < tabsMenu.getItemCount(); i++)
			if (tabsMenu.getItem(i) == item) {
				switchTo(i - tabsMenuTabsStart);
				return true;
			}
		return false;
	}

	@Override
	public void stateChanged(final ChangeEvent e) {
		final int index = tabbed.getSelectedIndex();
		if (index < 0) {
			setTitle("");
			return;
		}
		final EditorPane editorPane = getEditorPane(index);
		editorPane.requestFocus();
		checkForOutsideChanges();

		setTitle();
	}

	public EditorPane getEditorPane(final int index) {
		return getTab(index).editorPane;
	}

	public void findOrReplace(final boolean doReplace) {
		findDialog.setLocationRelativeTo(this);

		// override search pattern only if
		// there is sth. selected
		final String selection = getTextArea().getSelectedText();
		if (selection != null) findDialog.setSearchPattern(selection);

		findDialog.show(doReplace);
	}

	public void gotoLine() {
		final String line =
			JOptionPane.showInputDialog(this, "Line:", "Goto line...",
				JOptionPane.QUESTION_MESSAGE);
		if (line == null) return;
		try {
			gotoLine(Integer.parseInt(line));
		}
		catch (final BadLocationException e) {
			error("Line number out of range: " + line);
		}
		catch (final NumberFormatException e) {
			error("Invalid line number: " + line);
		}
	}

	public void gotoLine(final int line) throws BadLocationException {
		getTextArea().setCaretPosition(getTextArea().getLineStartOffset(line - 1));
	}

	public boolean reload() {
		return reload("Reload the file?");
	}

	public boolean reload(final String message) {
		final EditorPane editorPane = getEditorPane();

		final File file = editorPane.getFile();
		if (file == null || !file.exists()) return true;

		final boolean modified = editorPane.fileChanged();
		final String[] options = { "Reload", "Do not reload" };
		if (modified) options[0] = "Reload (discarding changes)";
		switch (JOptionPane.showOptionDialog(this, message, "Reload",
			JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options,
			options[0])) {
			case 0:
				try {
					editorPane.open(file);
					return true;
				}
				catch (final IOException e) {
					error("Could not reload " + file.getPath());
				}

				break;
		}
		return false;
	}

	private RenderingState rsRecordStart = null;

	public void recordTransitionStart() {
		rsRecordStart = renderer.getRenderingState().clone();
	}

	public void recordTransitionEnd() {
		GenericDialog gd = new GenericDialog("");
		gd.addNumericField("Transition_start", 0, 0);
		gd.addNumericField("Transition_end", 20, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int tStart = (int)gd.getNextNumber();
		int tEnd   = (int)gd.getNextNumber();

		float[] rotcenter = renderer.getRotationCenter();

		RenderingState rsRecordEnd = renderer.getRenderingState().clone();

		float[] t0 = rsRecordStart.getFwdTransform().calculateForwardTransformWithoutCalibration();
		float[] t1 = rsRecordEnd.getFwdTransform().calculateForwardTransformWithoutCalibration();

		System.out.println("t0 = \n" + Transform.toString(t0));

		// T0 * M = T1  =>  M = T0^{-1} * T1
		Transform.invert(t0);
		System.out.println("t0^(-1) = \n" + Transform.toString(t0));
		System.out.println("t1      = \n" + Transform.toString(t1));
//		float[] m = Transform.mul(t0, t1);
		float[] m = Transform.mul(t1, t0);
		System.out.println("m = \n" + Transform.toString(m));

		// M = T * C^{-1} * S * R * C
		// T * S * R = C * M * C^{-1}
		Transform.applyTranslation(-rotcenter[0], -rotcenter[1], -rotcenter[2], m);
		Transform.applyTranslation(m, rotcenter[0], rotcenter[1], rotcenter[2]);

		System.out.println("m(1) = \n" + Transform.toString(m));

		// extract scale
		float scale = (float)Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
		System.out.println("scale = " + scale);

		// extract translation
		float dx = m[3];
		float dy = m[7];
		float dz = m[11];
		System.out.println("translation = [" + dx + ", " + dy + ", " + dz + "]");

		m[3] = m[7] = m[11] = 0;
		for(int i = 0; i < 12; i++)
			m[i] *= 1f / scale;
		System.out.println("m(2) = \n" + Transform.toString(m));

		// extract rotation
		float[] euler = new float[3];
		Transform.guessEulerAngles(m, euler);

		StringBuffer text = new StringBuffer(recordTransformation(rsRecordStart, Integer.toString(tStart)));

		text.append("From frame " + tStart + " to frame " + tEnd + ":\n");
		// rotate around x-axis (vertically)
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

		final TextEditorTab tab = getTab();
		StringBuffer originalText = new StringBuffer(tab.editorPane.getText());
		int lineOfCursor = tab.editorPane.getCaretLineNumber();
		int offset = tab.editorPane.getText().length();
		try {
			offset = tab.editorPane.getLineStartOffset(lineOfCursor + 1);
		} catch(Exception e) {}
		originalText.insert(offset, text.toString());

		tab.editorPane.getAutoCompletion().setAutoActivationEnabled(false);
		tab.editorPane.setText(originalText.toString());
		tab.editorPane.getAutoCompletion().setAutoActivationEnabled(true);

		int xStart = text.indexOf("X") + offset;
		tab.editorPane.setSelectionStart(xStart);
		tab.editorPane.setSelectionEnd(xStart + 1);

		rsRecordStart = null;
		rsRecordEnd = null;
	}

	public void handleCustomRecording(int i) {
		RecordingItem ri = recordingProvider.get(i);
		String text = ri.getRecording(renderer.getRenderingState());
		addRecording(text);
	}

	public void addRecording(String s) {
		final TextEditorTab tab = getTab();

		StringBuffer originalText = new StringBuffer(tab.editorPane.getText());
		int lineOfCursor = tab.editorPane.getCaretLineNumber();
		int offset = tab.editorPane.getText().length();
		try {
			offset = tab.editorPane.getLineStartOffset(lineOfCursor + 1);
		} catch(Exception e) {}
		originalText.insert(offset, s);

		tab.editorPane.getAutoCompletion().setAutoActivationEnabled(false);
		tab.editorPane.setText(originalText.toString());
		tab.editorPane.getAutoCompletion().setAutoActivationEnabled(true);


		int xStart = s.indexOf("X") + offset;
		tab.editorPane.setSelectionStart(xStart);
		tab.editorPane.setSelectionEnd(xStart + 1);
	}

	public void recordTransformation() {
		String text = recordTransformation(renderer.getRenderingState().clone(), "X");
		addRecording(text);
	}

	public String recordTransformation(RenderingState rs, String tStart) {
		float[] rotcenter = renderer.getRotationCenter();

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

		StringBuffer text = new StringBuffer("At frame " + tStart + ":\n");
		text.append("- reset transformation\n");
		// rotate around x-axis (vertically)
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

	// TODO respect selection
	public void runText(boolean selection) {
		final TextEditorTab tab = getTab();
		tab.showOutput();
		tab.prepare();

		String text = tab.editorPane.getText();

		try {
			animator.render(text);
		} catch(Exception ex) {
			handleException(ex);
			tab.restore();
			throw new RuntimeException("Error reading animations", ex);
		}
	}

	// Animator.Listener interface
	@Override
	public void animationFinished(ImagePlus result) {
		getTab().restore();
	}

	public void cancelAnimation() {
		animator.cancelRendering();
	}

	public boolean isExecuting() {
		return animator.isExecuting();
	}

	public static boolean isBinary(final File file) {
		if (file == null) return false;
		// heuristic: read the first up to 8000 bytes, and say that it is binary if
		// it contains a NUL
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			int left = 8000;
			final byte[] buffer = new byte[left];
			while (left > 0) {
				final int count = in.read(buffer, 0, left);
				if (count < 0) break;
				for (int i = 0; i < count; i++)
					if (buffer[i] == 0) {
						in.close();
						return true;
					}
				left -= count;
			}
			return false;
		}
		catch (final IOException e) {
			return false;
		}
		finally {
			if(in != null) {
				try {
					in.close();
				} catch(Exception e) {}
			}
		}
	}

	/**
	 * Open a new tab with some content; the languageExtension is like ".java",
	 * ".py", etc.
	 */
	public TextEditorTab newTab(final String content) {
		final TextEditorTab tab = open(null);
		if (null != content) {
			tab.editorPane.setText(content);
		}

		return tab;
	}

	public TextEditorTab open(final File file) {
		if (isBinary(file)) {
			throw new RuntimeException("Cannot open binary file");
		}

		try {
			TextEditorTab tab = (tabbed.getTabCount() == 0) ? null : getTab();
			final boolean wasNew = tab != null && tab.editorPane.isNew();
			if (!wasNew) {
				tab = new TextEditorTab(this, renderer.getKeywordFactory());
				tab.editorPane.loadPreferences();
				tab.editorPane.getDocument().addDocumentListener(this);
				addDefaultAccelerators(tab.editorPane);
			}
			synchronized (tab.editorPane) { // tab is never null at this location.
				tab.editorPane.open(file);
				if (wasNew) {
					final int index = tabbed.getSelectedIndex() + tabsMenuTabsStart;
					tabsMenu.getItem(index).setText(tab.editorPane.getFileName());
				}
				else {
					tabbed.addTab("", tab);
					switchTo(tabbed.getTabCount() - 1);
					tabsMenuItems.add(addToMenu(tabsMenu, tab.editorPane.getFileName(),
						0, 0));
				}
				setEditorPaneFileName(tab.editorPane.getFile());
				try {
					updateTabAndFontSize(true);
				}
				catch (final NullPointerException e) {
					/* ignore */
				}
			}

			return tab;
		}
		catch (final FileNotFoundException e) {
			e.printStackTrace();
			error("The file '" + file + "' was not found.");
		}
		catch (final Exception e) {
			e.printStackTrace();
			error("There was an error while opening '" + file + "': " + e);
		}
		return null;
	}

	public boolean saveAs() {
		final EditorPane editorPane = getEditorPane();
		File file = editorPane.getFile();
		if (file == null) {
			file = new File("", editorPane.getFileName());
		}
		ij.io.SaveDialog sd = new ij.io.SaveDialog("Save as...", file.getName(), ".animation.txt");
		String dir = sd.getDirectory();
		String name = sd.getFileName();
		if(dir == null || name == null)
			return false;
		final File fileToSave = new File(dir, name);
		return saveAs(fileToSave.getAbsolutePath(), true);
	}

	public void saveAs(final String path) {
		saveAs(path, true);
	}

	public boolean saveAs(final String path, final boolean askBeforeReplacing) {
		final File file = new File(path);
		if (file.exists() &&
			askBeforeReplacing &&
			JOptionPane.showConfirmDialog(this, "Do you want to replace " + path +
				"?", "Replace " + path + "?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return false;
		if (!write(file)) return false;
		setEditorPaneFileName(file);
		setTitle();
		return true;
	}

	public boolean save() {
		final File file = getEditorPane().getFile();
		if (file == null) {
			return saveAs();
		}
		if (!write(file)) {
			return false;
		}

		setTitle();

		return true;
	}

	public boolean write(final File file) {
		try {
			getEditorPane().write(file);
			return true;
		}
		catch (final IOException e) {
			error("Could not save " + file.getName());
			e.printStackTrace();
			return false;
		}
	}

	static byte[] readFile(final String fileName) throws IOException {
		final File file = new File(fileName);
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			final byte[] buffer = new byte[(int) file.length()];
			in.read(buffer);
			return buffer;
		}
		finally {
			if(in != null)
				in.close();
		}
	}

	static void deleteRecursively(final File directory) {
		for (final File file : directory.listFiles())
			if (file.isDirectory()) deleteRecursively(file);
			else file.delete();
		directory.delete();
	}

	public void updateTabAndFontSize(final boolean setByLanguage) {
		final EditorPane pane = getEditorPane();

		final int tabSize = pane.getTabSize();
		boolean defaultSize = false;
		for (int i = 0; i < tabSizeMenu.getItemCount(); i++) {
			final JMenuItem item = tabSizeMenu.getItem(i);
			if (tabSize == Integer.parseInt(item.getText())) {
				item.setSelected(true);
				defaultSize = true;
			}
		}
		final int fontSize = (int) pane.getFontSize();
		defaultSize = false;
		for (int i = 0; i < fontSizeMenu.getItemCount(); i++) {
			final JMenuItem item = fontSizeMenu.getItem(i);
			if (item == chooseFontSize) {
				item.setSelected(!defaultSize);
				item.setText("Other" + (defaultSize ? "" : " (" + fontSize + ")") +
					"...");
				continue;
			}
			String label = item.getText();
			if (label.endsWith(" pt")) label = label.substring(0, label.length() - 3);
			if (fontSize == Integer.parseInt(label)) {
				item.setSelected(true);
				defaultSize = true;
			}
		}
		wrapLines.setState(pane.getLineWrap());
		tabsEmulated.setState(pane.getTabsEmulated());
	}

	public void setEditorPaneFileName(final String baseName) {
		getEditorPane().setFileName(baseName);
	}

	public void setEditorPaneFileName(final File file) {
		final EditorPane editorPane = getEditorPane();
		editorPane.setFileName(file);
	}

	void setTitle() {
		final EditorPane editorPane = getEditorPane();

		final boolean fileChanged = editorPane.fileChanged();
		final String fileName = editorPane.getFileName();
		final String title = (fileChanged ? "*" : "") + fileName;
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				setTitle(title); // to the main window
				// Update all tabs: could have changed
				for (int i = 0; i < tabbed.getTabCount(); i++)
					tabbed.setTitleAt(i, ((TextEditorTab) tabbed.getComponentAt(i))
						.getTitle());
			}
		});
	}

	@Override
	public synchronized void setTitle(final String title) {
		super.setTitle(title);
		final int index = tabsMenuTabsStart + tabbed.getSelectedIndex();
		if (index < tabsMenu.getItemCount()) {
			final JMenuItem item = tabsMenu.getItem(index);
			if (item != null) item.setText(title);
		}
	}


	public String getSelectedTextOrAsk(final String label) {
		String selection = getTextArea().getSelectedText();
		if (selection == null || selection.indexOf('\n') >= 0) {
			selection =
				JOptionPane.showInputDialog(this, label + ":", label + "...",
					JOptionPane.QUESTION_MESSAGE);
			if (selection == null) return null;
		}
		return selection;
	}

	public String getSelectedClassNameOrAsk() {
		String className = getSelectedTextOrAsk("Class name");
		if (className != null) className = className.trim();
		return className;
	}

	public void switchTo(final String path, final int lineNumber)
		throws IOException
	{
		switchTo(new File(path).getCanonicalFile(), lineNumber);
	}

	public void switchTo(final File file, final int lineNumber) {
		if (!editorPaneContainsFile(getEditorPane(), file)) switchTo(file);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					gotoLine(lineNumber);
				}
				catch (final BadLocationException e) {
					// ignore
				}
			}
		});
	}

	public void switchTo(final File file) {
		for (int i = 0; i < tabbed.getTabCount(); i++)
			if (editorPaneContainsFile(getEditorPane(i), file)) {
				switchTo(i);
				return;
			}
		open(file);
	}

	public void switchTo(final int index) {
		if (index == tabbed.getSelectedIndex()) return;
		tabbed.setSelectedIndex(index);
	}

	private void switchTabRelative(final int delta) {
		final int count = tabbed.getTabCount();
		int index = ((tabbed.getSelectedIndex() + delta) % count);
		if (index < 0) {
			index += count;
		}

		switchTo(index);
	}

	private void removeTab(final int index) {
		final int menuItemIndex = index + tabsMenuTabsStart;

		tabbed.remove(index);
		tabsMenuItems.remove(tabsMenu.getItem(menuItemIndex));
		tabsMenu.remove(menuItemIndex);
	}

	boolean editorPaneContainsFile(final EditorPane editorPane, final File file) {
		try {
			return file != null && editorPane != null &&
				editorPane.getFile() != null &&
				file.getCanonicalFile().equals(editorPane.getFile().getCanonicalFile());
		}
		catch (final IOException e) {
			return false;
		}
	}

	public File getFile() {
		return getEditorPane().getFile();
	}

	public File getFileForBasename(final String baseName) {
		File file = getFile();
		if (file != null && file.getName().equals(baseName)) return file;
		for (int i = 0; i < tabbed.getTabCount(); i++) {
			file = getEditorPane(i).getFile();
			if (file != null && file.getName().equals(baseName)) return file;
		}
		return null;
	}

	private File openWithDialog(final File defaultDir) {
		String defaultdir = defaultDir == null ? "" : defaultDir.getAbsolutePath();
		OpenDialog od = new OpenDialog("Open", defaultdir, "");
		String path = od.getPath();
		return path == null ? null : new File(path);
	}

	/**
	 * Write a message to the output screen
	 *
	 * @param message The text to write
	 */
	public void write(String message) {
		final TextEditorTab tab = getTab();
		if (!message.endsWith("\n")) message += "\n";
		tab.screen.insert(message, tab.screen.getDocument().getLength());
	}

	public void writeError(String message) {
		getTab().showErrors();
		if (!message.endsWith("\n")) message += "\n";
		errorScreen.insert(message, errorScreen.getDocument().getLength());
	}

	private void error(final String message) {
		JOptionPane.showMessageDialog(this, message);
	}

	public void handleException(final Throwable e) {
		handleException(e, errorScreen);
		getTab().showErrors();
	}

	public static void
		handleException(final Throwable e, final JTextArea textArea)
	{
		final CharArrayWriter writer = new CharArrayWriter();
		PrintWriter out = null;
		try {
			out = new PrintWriter(writer);
			e.printStackTrace(out);
			for (Throwable cause = e.getCause(); cause != null; cause =
					cause.getCause())
			{
				out.write("Caused by: ");
				cause.printStackTrace(out);
			}
		}
		finally {
			if(out != null)
				out.close();
		}
		textArea.append(writer.toString());
	}

	public boolean confirmClose() {
		while (tabbed.getTabCount() > 0) {
			if (!handleUnsavedChanges()) return false;
			final int index = tabbed.getSelectedIndex();
			removeTab(index);
		}
		return true;
	}

	@Override
	public void insertUpdate(final DocumentEvent e) {
		setTitle();
		checkForOutsideChanges();
	}

	@Override
	public void removeUpdate(final DocumentEvent e) {
		setTitle();
		checkForOutsideChanges();
	}

	@Override
	public void changedUpdate(final DocumentEvent e) {
		setTitle();
	}

}

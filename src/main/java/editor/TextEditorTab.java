package editor;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

public class TextEditorTab extends JSplitPane {

	protected final EditorPane editorPane;
	protected final JTextArea screen = new JTextArea();
	protected final JScrollPane scroll;
	protected boolean showingErrors;
	private final JButton runit, killit, toggleErrors;

	private final AnimationEditor textEditor;

	public TextEditorTab(final AnimationEditor textEditor) {
		super(JSplitPane.VERTICAL_SPLIT);
		super.setResizeWeight(350.0 / 430.0);

		this.textEditor = textEditor;
		editorPane = new EditorPane();

		screen.setEditable(false);
		screen.setLineWrap(true);
		screen.setFont(new Font("Courier", Font.PLAIN, 12));

		final JPanel bottom = new JPanel();
		bottom.setLayout(new GridBagLayout());
		final GridBagConstraints bc = new GridBagConstraints();

		bc.gridx = 0;
		bc.gridy = 0;
		bc.weightx = 0;
		bc.weighty = 0;
		bc.anchor = GridBagConstraints.NORTHWEST;
		bc.fill = GridBagConstraints.NONE;
		runit = new JButton("Run");
		runit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent ae) {
				textEditor.runText(false);
			}
		});
		bottom.add(runit, bc);

		bc.gridx = 1;
		killit = new JButton("Kill");
		killit.setEnabled(false);
		killit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent ae) {
				textEditor.cancelAnimation();
			}
		});
		bottom.add(killit, bc);

		bc.gridx = 2;
		bc.fill = GridBagConstraints.HORIZONTAL;
		bc.weightx = 1;
		bottom.add(new JPanel(), bc);

		bc.gridx = 3;
		bc.fill = GridBagConstraints.NONE;
		bc.weightx = 0;
		bc.anchor = GridBagConstraints.NORTHEAST;
		toggleErrors = new JButton("Show Errors");
		toggleErrors.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				toggleErrors();
			}
		});
		bottom.add(toggleErrors, bc);

		bc.gridx = 4;
		bc.fill = GridBagConstraints.NONE;
		bc.weightx = 0;
		bc.anchor = GridBagConstraints.NORTHEAST;
		final JButton clear = new JButton("Clear");
		clear.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent ae) {
				getScreen().setText("");
			}
		});
		bottom.add(clear, bc);

		bc.gridx = 0;
		bc.gridy = 1;
		bc.anchor = GridBagConstraints.NORTHWEST;
		bc.fill = GridBagConstraints.BOTH;
		bc.weightx = 1;
		bc.weighty = 1;
		bc.gridwidth = 5;
		screen.setEditable(false);
		screen.setLineWrap(true);
		final Font font = new Font("Courier", Font.PLAIN, 12);
		screen.setFont(font);
		scroll = new JScrollPane(screen);
		scroll.setPreferredSize(new Dimension(600, 80));
		bottom.add(scroll, bc);

		super.setTopComponent(editorPane.wrappedInScrollbars());
		super.setBottomComponent(bottom);
	}

	/** Invoke in the context of the event dispatch thread. */
	public void prepare() {
		editorPane.setEditable(false);
		runit.setEnabled(false);
		killit.setEnabled(true);
	}

	public void restore() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				editorPane.setEditable(true);
				runit.setEnabled(true);
				killit.setEnabled(false);
			}
		});
	}

	public void toggleErrors() {
		showingErrors = !showingErrors;
		if (showingErrors) {
			toggleErrors.setText("Show Output");
			scroll.setViewportView(textEditor.getErrorScreen());
		}
		else {
			toggleErrors.setText("Show Errors");
			scroll.setViewportView(screen);
		}
	}

	public void showErrors() {
		if (!showingErrors) toggleErrors();
		else if (scroll.getViewport().getView() == null) {
			scroll.setViewportView(textEditor.getErrorScreen());
		}
	}

	public void showOutput() {
		if (showingErrors) toggleErrors();
	}

	public JTextArea getScreen() {
		return showingErrors ? textEditor.getErrorScreen() : screen;
	}

	private boolean isExecuting() {
		return textEditor.isExecuting();
	}

	public final String getTitle() {
		return (editorPane.fileChanged() ? "*" : "") + editorPane.getFileName() +
			(isExecuting() ? " (Running)" : "");
	}

	public JTextComponent getEditorPane() {
		return editorPane;
	}
}

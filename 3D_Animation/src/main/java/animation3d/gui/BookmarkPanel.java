package animation3d.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;

import animation3d.parser.Interpreter;
import animation3d.parser.NoSuchMacroException;
import animation3d.parser.ParsingException;
import animation3d.parser.ParsingResult;
import animation3d.parser.Preprocessor;
import animation3d.parser.Preprocessor.PreprocessingException;
import animation3d.renderer3d.ExtendedRenderingState;
import animation3d.renderer3d.RecordingProvider;
import animation3d.renderer3d.Renderer3D;
import animation3d.textanim.Animation;
import animation3d.textanim.NumberedLine;
import animation3d.textanim.RenderingState;
import animation3d.util.Transform;
import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;

@SuppressWarnings("serial")
public class BookmarkPanel extends JPanel {

	private final Renderer3D renderer;

	private final ArrayList<Bookmark> bookmarks = new ArrayList<Bookmark>();

	private final BookmarkTableModel dm;

	public interface Listener {
		public void gotoBookmark(Bookmark bookmark);
	}

	private ArrayList<Listener> listeners =	new ArrayList<Listener>();

	public void addBookmarkPanelListener(Listener l) {
        listeners.add(l);
    }

	private void fireGotoBookmark(Bookmark bookmark) {
		for(Listener l : listeners)
			l.gotoBookmark(bookmark);
	}

	public BookmarkPanel(Renderer3D renderer) {
		super(new BorderLayout());
		this.renderer = renderer;

		bookmarks.add(new Bookmark("Home", renderer.getRenderingState().clone()));

		dm = new BookmarkTableModel(bookmarks);
		JTable table = new JTable(dm) {
		    @Override // Always selectAll()
		    public boolean editCellAt(int row, int column, EventObject e) {
		        boolean result = super.editCellAt(row, column, e);
		        final Component editor = getEditorComponent();
		        if(editor == null)
		        	return result;

		        if(!(editor instanceof JPanel))
		        	return result;

		        JPanel panel = (JPanel) editor;

		        if(panel.getComponentCount() < 2)
		        	return result;

		        Component comp = panel.getComponent(1);
		        if(!(comp instanceof JTextComponent))
		        	return result;

		            SwingUtilities.invokeLater(() -> {
		            	comp.requestFocusInWindow();
		                ((JTextComponent) comp).selectAll();
		            });
		        return result;
		    }
		};
		ImageIcon star = new ImageIcon(BookmarkPanel.class.getResource("/star.png"));
		ImageIcon home = new ImageIcon(BookmarkPanel.class.getResource("/home.png"));

		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(
                    JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				((JLabel)c).setBorder(null);
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			    panel.setBorder(null);
			    panel.setBackground(c.getBackground());
				panel.add(new JLabel(row == 0 ? home : star));
				panel.add(c);
				int h = panel.getPreferredSize().height;
				if (h > 0 && table.getRowHeight() != h + 2) {
		            table.setRowHeight(h + 2);
		        }
			    return panel;
			}
		});
		table.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()) {
			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
					int column) {
				Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
				JTextComponent jtc = (JTextComponent) c;
				jtc.setBorder(null);
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			    panel.setBorder(null);
			    panel.setBackground(c.getBackground());
				panel.add(new JLabel(row == 0 ? home : star));
				panel.add(c);
			    return panel;
			}
		});
		table.setShowGrid(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		table.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent evt) {
//				if (evt.getClickCount() == 2) {
//					Point pnt = evt.getPoint();
//					int row = table.rowAtPoint(pnt);
//					if(row >= 0 && row < bookmarks.size()) {
//						Bookmark bookmark = bookmarks.get(row);
//						fireGotoBookmark(bookmark);
//					}
//				}
//			}
//		});

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int row = table.getSelectedRow();
				if(row >= 0 && row < bookmarks.size()) {
					Bookmark bookmark = bookmarks.get(row);
					fireGotoBookmark(bookmark);
				}
			}
		});

		JScrollPane scrollTable = new JScrollPane(table);
		scrollTable.setColumnHeader(null);
		scrollTable.setPreferredSize(new Dimension(40, 160));
		add(scrollTable, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton add = new JButton();
		add.setIcon(new ImageIcon(BookmarkPanel.class.getResource("/add.png")));
		add.setPreferredSize(new java.awt.Dimension(24, 24));
		add.setSize(24, 24);
		add.setMinimumSize(new java.awt.Dimension(24, 24));
		buttons.add(add);

		JButton remove = new JButton();
		remove.setIcon(new ImageIcon(BookmarkPanel.class.getResource("/delete.png")));
		remove.setPreferredSize(new java.awt.Dimension(24, 24));
		remove.setSize(24, 24);
		remove.setMinimumSize(new java.awt.Dimension(24, 24));
		buttons.add(remove);

		JButton save = new JButton();
		save.setIcon(new ImageIcon(BookmarkPanel.class.getResource("/save.png")));
		save.setPreferredSize(new java.awt.Dimension(24, 24));
		save.setSize(24, 24);
		save.setMinimumSize(new java.awt.Dimension(24, 24));
		buttons.add(save);

		JButton open = new JButton();
		open.setIcon(new ImageIcon(BookmarkPanel.class.getResource("/folder.png")));
		open.setPreferredSize(new java.awt.Dimension(24, 24));
		open.setSize(24, 24);
		open.setMinimumSize(new java.awt.Dimension(24, 24));
		buttons.add(open);

		// JButton add = new JButton("add");
		add.addActionListener(e -> {
			int index = bookmarks.size();
			String name = "Bookmark " + dm.getRowCount();
			bookmarks.add(new Bookmark(name, renderer.getRenderingState().clone()));
			dm.fireTableDataChanged();
			table.changeSelection(index, 0, false, false);
			table.editCellAt(index, 0);
		});
		remove.addActionListener(e -> {
			int idx = table.getSelectedRow();
			if (idx > 0) {
				bookmarks.remove(idx);
				dm.fireTableDataChanged();
				if (idx < dm.getRowCount())
					table.setRowSelectionInterval(idx, idx);
			}
		});
		save.addActionListener(e -> {
			saveBookmarks();
		});
		open.addActionListener(e -> {
			loadBookmarks();
		});
		// buttons.add(remove);
		add(buttons, BorderLayout.SOUTH);
	}

	void loadBookmarks() {
		JFileChooser fc = new JFileChooser(OpenDialog.getLastDirectory());
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".bookmarks.txt");
			}

			@Override
			public String getDescription() {
				return "Bookmark File";
			}
		});
		int s = fc.showOpenDialog(null);
		if(s != JFileChooser.APPROVE_OPTION)
			return;

		String path = fc.getSelectedFile().getAbsolutePath();
		loadBookmarks(path);
	}

	public void loadBookmarks(String path) {
		try {
			StringBuffer buf = new StringBuffer();;
			ArrayList<String> names = new ArrayList<String>();
			List<String> lines = Files.readAllLines(Paths.get(path));
			int fidx = 0;
			for(String line : lines) {
				line = line.trim();
				if(line.startsWith("At bookmark ")) {
					String name = line.substring(11);
					if(name.endsWith(":"))
						name = name.substring(0, name.length() - 1);
					names.add(name);
					buf.append("At frame " + fidx + ":\n");
					fidx++;
				}
				else {
					buf.append(line).append("\n");
				}
			}
			List<RenderingState> renderingStates = computeAnimations(buf.toString(), 0, fidx - 1);
			bookmarks.clear();
			for(int i = 0; i < renderingStates.size(); i++) {
				bookmarks.add(new Bookmark(
						names.get(i),
						(ExtendedRenderingState)renderingStates.get(i)));
			}
			dm.fireTableDataChanged();
		} catch (IOException e) {
			IJ.handleException(e);
			return;
		} catch (NoSuchMacroException e) {
			IJ.handleException(e);
			return;
		} catch (PreprocessingException e) {
			IJ.handleException(e);
			return;
		} catch (ParsingException e) {
			IJ.handleException(e);
			return;
		}

	}

	public List<RenderingState> computeAnimations(String text, int from, int to) throws NoSuchMacroException, PreprocessingException, ParsingException {
		HashMap<String, String> macros = new HashMap<String, String>();
		ArrayList<NumberedLine> lines = new ArrayList<NumberedLine>();

		Preprocessor.preprocess(text, lines, macros);

		ImagePlus imp = renderer.getImage();
		float[] rotcenter = new float[] {
				(float)imp.getCalibration().pixelWidth  * imp.getWidth()   / 2,
				(float)imp.getCalibration().pixelHeight * imp.getHeight()  / 2,
				(float)imp.getCalibration().pixelDepth  * imp.getNSlices() / 2
		};
		List<Animation> animations = new ArrayList<Animation>();
		for(NumberedLine line : lines) {
			ParsingResult pr = new ParsingResult();
			try {
				Interpreter.parse(renderer.getKeywordFactory(), line.text, rotcenter, pr);
			} catch(ParsingException pa) {
				pa.setLine(line.lineno);
				throw pa;
			}
			Animation ta = pr.getResult();
			if(ta != null) {
				ta.pickScripts(macros);
				animations.add(ta);
			}
		}

		List<RenderingState> renderingStates = new ArrayList<RenderingState>();
		RenderingState previous = renderer.getRenderingState();
		for(int t = from; t <= to; t++) {
			RenderingState kf = previous.clone();
			kf.getFwdTransform().setTransformation(Transform.fromIdentity(null));
			kf.setFrame(t);
			for(Animation a : animations)
				a.adjustRenderingState(kf, renderingStates, renderer.getImage().getNChannels());
			renderingStates.add(kf);
			previous = kf;
		}
		return renderingStates;
	}

	void saveBookmarks() {
		JFileChooser fc = new JFileChooser(OpenDialog.getLastDirectory());
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".bookmarks.txt");
			}

			@Override
			public String getDescription() {
				return "Bookmark File";
			}
		});
		int s = fc.showSaveDialog(null);
		if(s == JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getAbsolutePath();
			saveBookmarks(path);
		}
	}

	public void saveBookmarks(String path) {
		try {
			if(!path.toLowerCase().endsWith(".bookmarks.txt"))
				path += ".bookmarks.txt";
			StringBuffer buf = new StringBuffer();
			for(int bIndex = 0; bIndex < bookmarks.size(); bIndex++) {
				Bookmark b = bookmarks.get(bIndex);
				RecordingProvider recordings = (RecordingProvider) RecordingProvider.getInstance();
				buf.append("At bookmark " + b.getName() + ":\n")
					.append(recordings.getRecordingForEverything(b.getRenderingState()));
			}
			Files.write(Paths.get(path), buf.toString().getBytes());
		} catch(IOException e) {
			IJ.handleException(e);
		}
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new BookmarkPanel(null));
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.pack();
		frame.setVisible(true);
	}

	static class BookmarkTableModel extends AbstractTableModel {

		private ArrayList<Bookmark> bookmarks;

		public BookmarkTableModel(ArrayList<Bookmark> bookmarks) {
			this.bookmarks = bookmarks;
		}

	    @Override
		public int getColumnCount() {
	        return 1;
	    }

	    @Override
		public int getRowCount() {
	        return bookmarks.size();
	    }

	    @Override
		public String getColumnName(int col) {
	        return "";
	    }

	    @Override
		public Object getValueAt(int row, int col) {
	        return bookmarks.get(row);
	    }

	    @Override
		public Class<?> getColumnClass(int c) {
	        return Bookmark.class;
	    }

	    @Override
		public boolean isCellEditable(int row, int col) {
	    	return row > 0;
	    }

	    @Override
		public void setValueAt(Object value, int row, int col) {
	        bookmarks.get(row).setName(value.toString());
	        fireTableCellUpdated(row, col);
	    }
	}
}

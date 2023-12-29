package edu.kuczapski.turtlecut;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import edu.kuczapski.turtlecut.scripting.AntlrTokenMaker;
import edu.kuczapski.turtlecut.scripting.Cutter;
import edu.kuczapski.turtlecut.scripting.RenderingThread;
import edu.kuczapski.turtlecut.scripting.TurtleParser;

public class MainWindow extends JFrame {

    private static final double DEFAULT_CUTTING_SPEED = 20;
	private static final String YOUR_APPLICATION_NAME = "Turtle Cut";
    private static final int CANVAS_BORDER = 10;
    
	private RSyntaxTextArea textEditor;
    private JPanel canvas;
    private final PersistedDataObject<File> currentFile = new PersistedDataObject<>(YOUR_APPLICATION_NAME, File::getAbsolutePath, File::new); // To store the currently loaded file
    private final PersistedDataObject<String> lastDirectory = new PersistedDataObject<>("last-folder", e->e, e->e) ; // To store the last selected directory
    private final PersistedDataObject<String> lastEditedProgram = new PersistedDataObject<>("last-edited-program", e->e, e->e) ; // To store the last selected directory
    
    private BufferedImage currentCutImage = null; 
    
    private Cutter cutter = new Cutter(400,400, 0.25);
    {
    	cutter.setDrawingListener(this::onNewCanvasImage);
    }

    private RenderingThread renderingThread = new RenderingThread();
    
    public MainWindow() {
        super(YOUR_APPLICATION_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");

        // Using system icons for New, Open, Save
        Action newAction = new AbstractAction("New", UIManager.getIcon("FileView.fileIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFile();
            }
        };

        Action openAction = new AbstractAction("Open", UIManager.getIcon("FileView.directoryIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        };

        Action saveAction = new AbstractAction("Save", UIManager.getIcon("FileView.floppyDriveIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentFile.get() != null) {
                    saveToFile(currentFile.get());
                } else {
                    saveFileAs();
                }
            }
        };

        Action saveAsAction = new AbstractAction("Save As", UIManager.getIcon("FileView.floppyDriveIcon")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFileAs();
            }
        };
        
        Action playAction = new AbstractAction("Play", new ImageIcon("turtle-small.png")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                play();
            }
        };
        
		Action exportGCodeAction = new AbstractAction("Export GCode", new ImageIcon("turtle-small.png")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				exportGCode();
			}
		};

        JMenuItem newItem = new JMenuItem(newAction);
        JMenuItem openItem = new JMenuItem(openAction);
        JMenuItem saveItem = new JMenuItem(saveAction);
        JMenuItem saveAsItem = new JMenuItem(saveAsAction);
        JMenuItem exitItem = new JMenuItem("Exit");

        newItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFile();
            }
        });

        saveAsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFileAs();
            }
        });

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit menu (you can customize this based on your application's features)
        JMenu editMenu = new JMenu("Edit");
        // Add additional edit menu items as needed

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");

        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAboutDialog();
            }
        });

        helpMenu.add(aboutItem);

        // Add menus to the menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        // Create a toolbar
        JToolBar toolBar = new JToolBar();
        JButton newButton = new JButton(newAction);
        JButton openButton = new JButton(openAction);
        JButton saveButton = new JButton(saveAction);
        JButton saveAsButton = new JButton(saveAsAction);
        JButton playButton = new JButton(playAction); 
        JButton exoprtGCodeButton = new JButton(exportGCodeAction);
        

        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(saveAsButton);
        toolBar.add(playButton);
        toolBar.add(exoprtGCodeButton);

        
        
        // Create the text editor on the left with syntax highlighting
        
        TokenMakerFactory.setDefaultInstance(AntlrTokenMaker.FACTORY);
        
        textEditor = new RSyntaxTextArea(20, 60);
       
        textEditor.setSyntaxEditingStyle("Turtle Cut");    
        
        RTextScrollPane textScrollPane = new RTextScrollPane(textEditor);

        // Create the canvas on the right
        canvas = new JPanel() {
        	@Override
        	public void paint(Graphics g) {
        		super.paint(g);
        		doCanvasPaint(g);
        	}
        };
        canvas.setBackground(Color.darkGray);
        

        // Create a split pane to divide the frame
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, canvas);
        splitPane.setDividerLocation(0.5); // Set the initial divider location to 50%

        // Add the split pane to the frame
        add(splitPane, BorderLayout.CENTER);

        // Add the toolbar to the frame
        add(toolBar, BorderLayout.PAGE_START);

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        // Apply a theme for better visual appearance (optional)
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            theme.apply(textEditor);
        } catch (IOException e) {
            e.printStackTrace();
        }
       
        
        Font font = textEditor.getFont();
        font = new Font(font.getName(), font.getStyle(), 16);
        
        textEditor.setFont(font);
        
        textEditor.getSyntaxScheme().getStyle(Token.ERROR_CHAR).underline = true;
        textEditor.getSyntaxScheme().getStyle(Token.ERROR_CHAR).foreground = Color.red;
        textEditor.getSyntaxScheme().getStyle(Token.ERROR_CHAR).background = null;
        
        textEditor.getSyntaxScheme().getStyle(Token.IDENTIFIER).foreground = Color.CYAN;
        textEditor.getSyntaxScheme().getStyle(Token.FUNCTION).foreground = Color.ORANGE;
        
        textEditor.getSyntaxScheme().getStyle(Token.LITERAL_NUMBER_FLOAT).foreground = new Color(0, 200, 200);
        textEditor.getSyntaxScheme().getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = new Color(0, 200, 200);
        
        CompletionProvider provider = createCompletionProvider();
        AutoCompletion ac = new AutoCompletion(provider);
        ac.install(textEditor);
        
        
        cutter.execute("", 0, 0);
        
        textEditor.getDocument().addDocumentListener(new DocumentListener() {

    	   @Override
    	   public void removeUpdate(DocumentEvent e) {}

    	   @Override
    	   public void insertUpdate(DocumentEvent e) {}

    	   @Override
    	   public void changedUpdate(DocumentEvent e) {
    		String program = textEditor.getText();
    		lastEditedProgram.set(program);
			renderingThread.requestJob(cutter, program , 0.0, textEditor.getCaretLineNumber()+1);
    	   }
       });
       textEditor.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				renderingThread.requestJob(cutter, textEditor.getText() , 0.0, textEditor.getCaretLineNumber()+1);
			}
	    });
        
		if (lastEditedProgram.get() != null) {
			textEditor.setText(lastEditedProgram.get());
		}
    
		if(currentFile.get() != null) {
			 setTitle(YOUR_APPLICATION_NAME + " - " + currentFile.get().getName()); // Update window title
		}
    }

	protected void play() {
		renderingThread.requestJob(cutter, textEditor.getText() , DEFAULT_CUTTING_SPEED , textEditor.getCaretLineNumber()+1);
	}

	protected void exportGCode() {
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("GCode Files (.gcode)", "gcode");
		fileChooser.setFileFilter(filter);

		// Set the initial directory if it exists
		if (lastDirectory.get() != null) {
			fileChooser.setCurrentDirectory(new File(lastDirectory.get()));
		}
		
	
		
		//if current file is not null use the name of the current file but with .gcode extension
		if (currentFile.get() != null) {
			fileChooser.setSelectedFile(new File(currentFile.get().getAbsolutePath().replace(".cut", ".gcode")));
		}
		
		int returnValue = fileChooser.showSaveDialog(this);

		if (returnValue == JFileChooser.APPROVE_OPTION) {
			currentFile.set( fileChooser.getSelectedFile());

			// Append ".cut" extension if not already present
			if (!currentFile.get().getName().toLowerCase().endsWith(".gcode")) {
				currentFile.set( new File(currentFile.get().getAbsolutePath() + ".gcode"));
			}

			lastDirectory.set(currentFile.get().getParent()); // Store the last selected directory
			setTitle(YOUR_APPLICATION_NAME + " - " + currentFile.get().getName()); // Update window title

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile.get()))) {
				writer.write(cutter.generateGCode(textEditor.getText()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void newFile() {
        // Add your logic for creating a new file (clearing content, resetting variables, etc.)
        textEditor.setText("");
        currentFile.set(null);
        setTitle(YOUR_APPLICATION_NAME);
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Cut Files (.cut)", "cut");
        fileChooser.setFileFilter(filter);

        // Set the initial directory if it exists
        if (lastDirectory.get() != null) {
            fileChooser.setCurrentDirectory(new File(lastDirectory.get()));
        }

        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFile.set( fileChooser.getSelectedFile());
            lastDirectory.set(currentFile.get().getParent()); // Store the last selected directory
            setTitle(YOUR_APPLICATION_NAME + " - " + currentFile.get().getName()); // Update window title

            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile.get()))) {
                textEditor.read(reader, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveToFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            textEditor.write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Cut Files (.cut)", "cut");
        fileChooser.setFileFilter(filter);

        // Set the initial directory if it exists
        if (lastDirectory.get() != null) {
            fileChooser.setCurrentDirectory(new File(lastDirectory.get()));
        }

        int returnValue = fileChooser.showSaveDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFile.set( fileChooser.getSelectedFile());

            // Append ".cut" extension if not already present
            if (!currentFile.get().getName().toLowerCase().endsWith(".cut")) {
                currentFile.set(new File(currentFile.get().getAbsolutePath() + ".cut"));
            }

            lastDirectory.set(currentFile.get().getParent()); // Store the last selected directory
            setTitle(YOUR_APPLICATION_NAME + " - " + currentFile.get().getName()); // Update window title

            saveToFile(currentFile.get());
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this, YOUR_APPLICATION_NAME+"\nVersion 1.0\nCopyright © 2023 Kuczapski Artur",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainWindow();
        });
    }
    
    private CompletionProvider createCompletionProvider() {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        
    	for(String tokenName:TurtleParser.tokenNames) {
    		if(tokenName.startsWith("'")) {
    			 provider.addCompletion(new BasicCompletion(provider, tokenName.substring(1, tokenName.length()-1)));
    		}
    	}
    
        return provider;

     }

	private void onNewCanvasImage(BufferedImage image) {
	 	
		BufferedImage clone = Cutter.deepCopy(image);
		
		SwingUtilities.invokeLater(() -> {
			this.currentCutImage = clone;
			canvas.repaint();
		});
	}
	
    protected void doCanvasPaint(Graphics g) {
    	BufferedImage img = currentCutImage;
    	
    	if(img==null) return;
		
    	Rectangle bounds =  canvas.getBounds();//t g.getClipBounds();
		 
		
		 int maxWidth = (bounds.width - CANVAS_BORDER*2);
		 int maxHeight = (bounds.height - CANVAS_BORDER*2);
		 
		 int drawWidth = img.getWidth();
		 int drawHeight = img.getHeight();
		 
		 double widthScale = 1;
		 double heightScale = 1;
		 
		 while(drawWidth*widthScale*2 < maxWidth) {
			 widthScale *= 2;
		 }
		 
		 while(drawHeight*heightScale*2 < maxHeight) {
			 heightScale *= 2;
		 }
		 
		 
		 if(maxWidth<drawWidth) {
			 widthScale = (double)maxWidth/drawWidth;
		 }
		 
		 if(maxHeight<drawHeight) {
			 heightScale = (double)maxHeight/drawHeight;
		 }
		 
		 double scale = Math.min(widthScale, heightScale);
		 
		 drawWidth = (int) (drawWidth * scale);
		 drawHeight = (int) (drawHeight * scale);
				 		 
		g.drawImage( img.getScaledInstance(drawWidth, drawHeight, java.awt.Image.SCALE_SMOOTH), bounds.width/2 -  drawWidth/2, bounds.height/2 - drawHeight/2,  null);
				 
	}
    
    
}


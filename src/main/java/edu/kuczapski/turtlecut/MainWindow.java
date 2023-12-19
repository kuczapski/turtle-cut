package edu.kuczapski.turtlecut;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import edu.kuczapski.turtlecut.scripting.AntlrTokenMaker;

public class MainWindow extends JFrame {

    private RSyntaxTextArea textEditor;
    private JPanel canvas;
    private File currentFile; // To store the currently loaded file
    private String lastDirectory; // To store the last selected directory

    public MainWindow() {
        super("Your Application Name");
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
                if (currentFile != null) {
                    saveToFile(currentFile);
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
        JButton sampleButton = new JButton(new ImageIcon("sample.png")); // You can replace this with an appropriate image

        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFile();
            }
        });

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentFile != null) {
                    saveToFile(currentFile);
                } else {
                    saveFileAs();
                }
            }
        });

        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFileAs();
            }
        });

        sampleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Your custom action for the sample button
            }
        });

        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(saveAsButton);
        toolBar.add(sampleButton);

        
        
        // Create the text editor on the left with syntax highlighting
        
        TokenMakerFactory.setDefaultInstance(AntlrTokenMaker.FACTORY);
        
        textEditor = new RSyntaxTextArea(20, 60);
        
        //textEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        //textEditor.setCodeFoldingEnabled(true);
        
        textEditor.setSyntaxEditingStyle("Turtle Cut");
        

        
        
        
        RTextScrollPane textScrollPane = new RTextScrollPane(textEditor);

        // Create the canvas on the right
        canvas = new JPanel();
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
        
        textEditor.getSyntaxScheme().getStyle(Token.ERROR_CHAR).underline = true;
        textEditor.getSyntaxScheme().getStyle(Token.ERROR_CHAR).foreground = Color.red;
        
        textEditor.getSyntaxScheme().getStyle(Token.IDENTIFIER).foreground = Color.CYAN;
        textEditor.getSyntaxScheme().getStyle(Token.FUNCTION).foreground = Color.ORANGE;
        
        textEditor.getSyntaxScheme().getStyle(Token.LITERAL_NUMBER_FLOAT).foreground = new Color(0, 200, 200);
        textEditor.getSyntaxScheme().getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = new Color(0, 200, 200);
        
        
    }

    private void newFile() {
        // Add your logic for creating a new file (clearing content, resetting variables, etc.)
        textEditor.setText("");
        currentFile = null;
        setTitle("Your Application Name");
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Cut Files (.cut)", "cut");
        fileChooser.setFileFilter(filter);

        // Set the initial directory if it exists
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(new File(lastDirectory));
        }

        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            lastDirectory = currentFile.getParent(); // Store the last selected directory
            setTitle("Your Application Name - " + currentFile.getName()); // Update window title

            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
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
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(new File(lastDirectory));
        }

        int returnValue = fileChooser.showSaveDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();

            // Append ".cut" extension if not already present
            if (!currentFile.getName().toLowerCase().endsWith(".cut")) {
                currentFile = new File(currentFile.getAbsolutePath() + ".cut");
            }

            lastDirectory = currentFile.getParent(); // Store the last selected directory
            setTitle("Your Application Name - " + currentFile.getName()); // Update window title

            saveToFile(currentFile);
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this, "Your Application Name\nVersion 1.0\nCopyright © 2023 Your Company",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainWindow();
        });
    }
}


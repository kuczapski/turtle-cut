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
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

public class MainWindow extends JFrame {

    private RSyntaxTextArea textEditor;
    private JPanel canvas;
    private File currentFile; // To store the currently loaded file

    public MainWindow() {
        super("Your Application Name");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        JMenuItem exitItem = new JMenuItem("Exit");

        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentFile != null) {
                    saveToFile(currentFile);
                } else {
                    saveFileAs();
                }
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
        JButton sampleButton = new JButton("Sample Button");
        toolBar.add(sampleButton);

        // Create the text editor on the left with syntax highlighting
        textEditor = new RSyntaxTextArea(20, 60);
        textEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textEditor.setCodeFoldingEnabled(true);

        RTextScrollPane textScrollPane = new RTextScrollPane(textEditor);

        // Create the canvas on the right
        canvas = new JPanel();
        canvas.setBackground(Color.WHITE);

        // Create a split pane to divide the frame
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, canvas);
        splitPane.setDividerLocation(200); // Adjust the initial divider position

        // Add the split pane to the frame
        add(splitPane, BorderLayout.CENTER);

        // Add the toolbar to the frame
        add(toolBar, BorderLayout.PAGE_START);

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        // Apply a theme for better visual appearance (optional)
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml"));
            theme.apply(textEditor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Cut Files (.cut)", "cut");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
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

        int returnValue = fileChooser.showSaveDialog(this);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();

            // Append ".cut" extension if not already present
            if (!currentFile.getName().toLowerCase().endsWith(".cut")) {
                currentFile = new File(currentFile.getAbsolutePath() + ".cut");
            }

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

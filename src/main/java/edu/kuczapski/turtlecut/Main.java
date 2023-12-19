package edu.kuczapski.turtlecut;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

public class Main {
    public static void main(String[] args) {
    	FlatLaf.registerCustomDefaultsSource( "com.amorph.sense.peopletracker.unit" );
		
		FlatDarkLaf flat = new FlatDarculaLaf();
		FlatLaf.setup(flat);
		
        SwingUtilities.invokeLater(() -> {
            new MainWindow();
        });
    }
}

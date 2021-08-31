package spcsim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/*
 * @author K. A. F.
 * 
 * This class has a library of static utility functions for gui management
 * Created: 10-31-20
 * Dependencies: None
 */

public class GUIHandler {
    
    //private constructor
    private GUIHandler() { }
    
    
    //dialog utility functions
    //displays error message then terminates the program
    public static void fatalErrorMessage( String message ) {
    	JOptionPane.showMessageDialog( null, 
               "Error: " + message + "\nProgram will close now.", "Fatal Error", 
               JOptionPane.ERROR_MESSAGE );
    	System.exit(-1);
    }
    
    //displays error message
    public static void errorMessage( String message ) {
    	JOptionPane.showMessageDialog( null, "Error: " + message, "Error", 
                JOptionPane.ERROR_MESSAGE );
    }
    
    //displays a confirmation message and returns the result as a boolean
    public static boolean confirmSelection( String message ) {
        int i = JOptionPane.showConfirmDialog( null, message );
        return i == JOptionPane.OK_OPTION;
    }
	
    //displays a regular informative message
    public static void regularMessage( String message, String title ) {
        JOptionPane.showMessageDialog( 
                null, message, title, JOptionPane.INFORMATION_MESSAGE );
    }
	
    //displays a message with text field, and returns user input
    public static String inquiryMessage( String message, String field ) {
    	return JOptionPane.showInputDialog( message, field );
    }
    
    //displays a confirmation message and returns the result as an int
    public static int confirmIntSelection( String message ) {
    	return JOptionPane.showConfirmDialog( null, message );
    }
    
    //prompts user to select a file and returns the file path
    public static String selectFile( String extension, String description, String action, boolean includeDir ) {
        JFileChooser fc = new JFileChooser( "." );
        fc.setDialogTitle( action );
        fc.setApproveButtonText( action );
        if( includeDir ) {
            fc.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
        } else {
            fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
        }
		
        fc.setFileFilter( new FileFilter() {
            @Override
            public boolean accept( File file ) {
                if( file.isDirectory() || extension.equals( "*" ) ) {
                    return true;
                } else {
                    String name = file.getName();
                    int i = name.lastIndexOf( '.' );
                    if ( i == -1 ) return false;
                    return name.substring( i + 1 ).toLowerCase().equals( extension );
                }
            }
            
            @Override
            public String getDescription() {
                return description + ": (*." + extension + ")";
            }
	} );
            
    int returnVal = fc.showOpenDialog( null );
	if ( returnVal == JFileChooser.APPROVE_OPTION ) {
            return fc.getSelectedFile().getAbsolutePath();
	} else {
            return null;
        }
    }
    
    //prompts user to select a colors
    public static Color selectColor( String title ) {
        return JColorChooser.showDialog( null, title, Color.WHITE );
    }
    
    //gets screen size width
    public static double getScreenWidth() {
        return Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    }
    
    //gets screen size height
    public static double getScreenHeight() {
        return Toolkit.getDefaultToolkit().getScreenSize().getHeight();
    }
    
    
    public static void forEachChild( Container container, Consumer<Component> consumer ) {
        for( int i = 0; i < container.getComponentCount(); i++ ) {
            consumer.accept( container.getComponent( i ) );
        }
    }
    
    public static void forEachTree( Container container, Consumer<Component> consumer ) {
        for( int i = 0; i < container.getComponentCount(); i++ ) {
            Component c = container.getComponent( i );
            consumer.accept( c );
            if( c instanceof Container ) {
                forEachTree( (Container)c, consumer );
            }
        }
    }
}

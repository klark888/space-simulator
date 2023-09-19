package spcsim.base;

/* Author: Kent Fukuda
 * Description: component that edits the particles in a simulation
 * Created: 8-29-21
 * Status: environment class, finished
 * Dependencies: SimObject, Units, Logger
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Button;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Label;
import java.awt.LayoutManager;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;

public final class EditPane extends Container {
    
    //file type of this simulation
    public static final String SPCOBJ_EXTENSION = ".spcobj";
    //menu types
    public static final int FILE_TYPE = 0, EDIT_TYPE = 1, VIEW_TYPE = 2, CONTROL_TYPE = 3, SIMULATION_TYPE = 4, ENGINE_TYPE = 5, ASSET_TYPE = 6, ABOUT_TYPE = 7;
    
    //constants
    private static final int THICKNESS = 20;//thickness of each edit pane entry
    private static final Font FONT = new Font( "Arial", Font.BOLD, 12 );//default font for editpane
    //reference
    private static SoftReference<HashMap<Units,Function<String,String>>> SELECT_DIALOGS = new SoftReference<>( null );
    
    //private fields
    private final MenuBar menuBar;//menubar for the menus added
    private final Menu[] menus;//menu types
    private final FileDialog fileBrowser;//file browser to select files
    private final HashMap<Units,String> defaultUnits;//stores default units of the simulation
    private final ArrayList<Runnable> updaters;//updaters for each entry
    private final ArrayList<IntUnaryOperator> layouters;//layout managers for each entry
    private ExpOperon<FileOutputStream> saveAction;
    private int minHeight;//preferred height of editpane
    
    
    //constructor
    public EditPane() {
        menuBar = new MenuBar();
        menus = new Menu[]{ new Menu( "File" ), new Menu( "Edit" ), new Menu( "View" ), new Menu( "Controls" ), 
                new Menu( "Simulation" ), new Menu( "Engine" ), new Menu( "Assets" ), new Menu( "About" ) };
        fileBrowser = new FileDialog( (Frame)null, "File Selector" );
        defaultUnits = new HashMap<>();
        updaters = new ArrayList<>();
        layouters = new ArrayList<>();
        saveAction = in -> {};
        minHeight = 0;
        super.setFont( FONT );
        super.setBackground( Color.DARK_GRAY );
        super.setForeground( Color.CYAN );
        super.setFocusable( true );
        super.enableEvents( MouseEvent.MOUSE_EVENT_MASK );
        for( var menu : menus )
            menuBar.add( menu );
    }
    
    
    //clear editpane
    public synchronized void clearEditPane() {
        for( var m : menus )
            m.removeAll();
        updaters.clear();
        layouters.clear();
        defaultUnits.clear();
        saveAction = in -> {};
        minHeight = 0;
        super.removeAll();
    }
    
    //sets the default unit for a category of units
    public void setDefaultUnit( Units units, String unit ) {
        defaultUnits.put( units, unit );
    }
    
    //returns the default unit for a category of units
    public String getDefaultUnit( Units units ) {
        return defaultUnits.get( units );
    }
    
    //sets the save action when exit confirmation occurs
    public void setSaveAction( ExpOperon<FileOutputStream> action ) {
        if( action == null )
            action = in -> {};
        saveAction = action;
    }
    
    //returns the save action when exit confirmation occurs
    public ExpOperon<FileOutputStream> getSaveAction() {
        return saveAction;
    }
    
    //updates the pane
    public void updatePane() {
        updaters.forEach( u -> u.run() );
    }
    
    //returns the menubar
    public MenuBar getMenuBar() {
        return menuBar;
    }
    
    //confirms save action
    public void confirmSaveAction( Runnable action ) {
        switch( JOptionPane.showConfirmDialog( null, "Save simulation before exiting?" ) ) {
            case JOptionPane.OK_OPTION :
                if( saveFileAction( SPCOBJ_EXTENSION, saveAction ) )
                    break;
            case JOptionPane.NO_OPTION :
                action.run();
            default :
        }
    }
    
    //entry adding methods
    //adds a field for modifying object strings
    public void addStringField( String label, Component repaint, Consumer<String> setter, Supplier<String> getter ) throws IllegalStateException {
        int height = THICKNESS * 2;
        Label labelComp = new Label( label );
        TextField field = createField( repaint, setter, getter );
        layouters.add( i -> {
            int w = super.getWidth();
            labelComp.setBounds( 0, i, w, THICKNESS );
            field.setBounds( 0, i + THICKNESS, w, THICKNESS );
            return height;
        } );
        add( labelComp );
        add( field );
        minHeight += height;
    }
    
    //adds a field for modifying object double values
    public void addValueField( String label, Component repaint, DoubleConsumer setter, DoubleSupplier getter ) throws IllegalStateException {
        addStringField( label, repaint, str -> setter.accept( Double.parseDouble( str ) ), () -> Double.toString( getter.getAsDouble() ) );
        /*int height = THICKNESS * 2;
        Label labelComp = new Label( label );
        TextField field = createField( repaint, str -> setter.accept( Double.parseDouble( str ) ), () -> Double.toString( getter.getAsDouble() ) );
        layouters.add( i -> {
            int w = super.getWidth();
            labelComp.setBounds( 0, i, w, THICKNESS );
            field.setBounds( 0, i + THICKNESS, w, THICKNESS );
            return height;
        } );
        add( labelComp );
        add( field );
        minHeight += height;*/
    }
    
    //adds a field for modifying single unit doubles
    public void addUnitField( String label, Component repaint, DoubleConsumer setter, DoubleSupplier getter, Units unitType, String unit ) throws IllegalStateException {
        int height = THICKNESS * 2;
        Label labelComp = new Label( label );
        Button button = selectButton( updaters.size(), unitType );
        button.setLabel( unit );
        TextField field = createField( repaint, str -> 
                setter.accept( unitType.convert( Double.parseDouble( str ), button.getLabel(), defaultUnits.get( unitType ) ) ),
                () -> Double.toString( unitType.convert( getter.getAsDouble(), defaultUnits.get( unitType ), button.getLabel() ) ) );
        layouters.add( i -> {
            int bw = widthOf( button ), w = super.getWidth();
            labelComp.setBounds( 0, i, w, THICKNESS );
            field.setBounds( 0, i += THICKNESS, w -= bw, THICKNESS );
            button.setBounds( w, i, bw, THICKNESS );
            return height;
        } );
        add( labelComp );
        add( field );
        add( button );
        minHeight += height;
    }
    
    //adds a field for modifying double unit doubles
    public void addDoubleUnitField( String label, Component repaint, DoubleConsumer setter, DoubleSupplier getter, 
            Units u1, Units u2, String n1, String n2 ) throws IllegalStateException {
        int height = THICKNESS * 2;
        Label labelComp = new Label( label );
        int index = updaters.size();
        Button button1 = selectButton( index, u1 );
        Button button2 = selectButton( index, u2 );
        button1.setLabel( n1 );
        button2.setLabel( n2 );
        TextField field = createField( repaint, ( str ) ->  setter.accept( u2.convert( u1.convert( Double.parseDouble( str ), button1.getLabel(), 
                defaultUnits.get( u1 ) ), defaultUnits.get( u2 ), button2.getLabel() ) ), () -> Double.toString( u1.convert( u2.convert(
                getter.getAsDouble(), button2.getLabel(), defaultUnits.get( u2 ) ), defaultUnits.get( u1 ), button1.getLabel() ) ) );
        layouters.add( i -> {
            int bw1 = widthOf( button1 ), bw2 = widthOf( button2 ), w = super.getWidth();
            labelComp.setBounds( 0, i, w, THICKNESS );
            field.setBounds( 0, i += THICKNESS, w -= bw1 + bw2, THICKNESS );
            button1.setBounds( w, i, bw1, THICKNESS );
            button2.setBounds( w + bw1, i, bw2, THICKNESS );
            return height;
        } );
        add( labelComp );
        add( field );
        add( button1 );
        add( button2 );
        minHeight += height;
    }
    
    //adds a button for modifying colors
    public void addColorButton( String label, Component repaint, Consumer<Color> setter, Supplier<Color> getter ) throws IllegalStateException {
        Button button = new Button( label );
        button.addActionListener( a -> {
            Color color = JColorChooser.showDialog( null, "Choose Color", getter.get() );
            if( color != null ) {
                setter.accept( color );
                button.setBackground( color );
                repaint.repaint();
            }
        } );
        updaters.add( () -> button.setBackground( getter.get() ) );
        layouters.add( i -> {
            button.setBounds( 0, i, super.getWidth(), THICKNESS );
            return THICKNESS;
        } );
        add( button );
        minHeight += THICKNESS;
    }
    
    //add menu item
    public MenuItem addMenuItem( int menuType, String label, ActionListener listener ) {
        var item = new MenuItem( label );
        item.addActionListener( listener );
        menus[menuType].add( item );
        return item;
    }
    
    //add menu item with keybind
    public MenuItem addMenuItem( int menuType, String label, int key, boolean shift, ActionListener listener ) {
        var item = addMenuItem( menuType, label, listener );
        item.setShortcut( new MenuShortcut( key, shift ) );
        return item;
    }
    
    //adds a menu item that is toggleable
    public CheckboxMenuItem addToggleMenuItem( int menuType, String label, BooleanSupplier get, Consumer<Boolean> set ) {
        var item = new CheckboxMenuItem( label, get.getAsBoolean() );
        item.addItemListener( i -> {
            set.accept( item.getState() );
        } );
        menus[menuType].add( item );
        return item;
    }
    
    //adds a menu item that is toggleable and has a keybind
    public CheckboxMenuItem addToggleMenuItem( int menuType, String label, int key, boolean shift, BooleanSupplier get, Consumer<Boolean> set ) {
        var item = addToggleMenuItem( menuType, label,  get, set );
        item.setShortcut( new MenuShortcut( key, shift ) );
        return item;
    }
    
    //adds a menu item that asks for an input value
    public MenuItem addValueMenuItem( int menuType, String label, String message, DoubleSupplier get, DoubleConsumer set ) {
        return addMenuItem( menuType, label, a -> {
            String response = JOptionPane.showInputDialog( message, Double.toString( get.getAsDouble() ) );
            try {
                if( response != null )
                    set.accept( Double.parseDouble( response ) );
            } catch( IllegalArgumentException e ) {
                JOptionPane.showMessageDialog( null, "Could not parse invalid number", "Error", JOptionPane.ERROR_MESSAGE );
            }
        } );
    }
    
    //adds a menu item that asks for an input value with units
    public MenuItem addUnitValueMenuItem( int menuType, String label, String unit, DoubleSupplier get, DoubleConsumer set ) {
        return addValueMenuItem( menuType, label, label + " (" + unit + "):", get, set );
    }
    
    //adds a menu item that asks for confirmation to save simulation before exiting
    public MenuItem addConfirmSaveMenuItem( int menuType, String label, Runnable action ) {
        return addMenuItem( menuType, label, a -> confirmSaveAction( action ) );
    }
    
    //adds a menu item that asks for confirmation to save simulation before exiting with a keybind
    public MenuItem addConfirmSaveMenuItem( int menuType, String label, int key, boolean shift, Runnable action ) {
        var item = addConfirmSaveMenuItem( menuType, label, action );
        item.setShortcut( new MenuShortcut( key, shift ) );
        return item;
    }
    
    //adds a menu item that asks for confirmation to save before opening a file
    public MenuItem addConfirmFileOpenMenuItem( int menuType, String label, String extension, ExpOperon<FileInputStream> action ) {
        return addConfirmSaveMenuItem( menuType, label, () -> openFileAction( extension, action ) );
    }
    
    //adds a menu item that asks for confirmation to save before opening a file with a keybind
    public MenuItem addConfirmFileOpenMenuItem( int menuType, String label, int key, boolean shift,  String extension, ExpOperon<FileInputStream> action ) {
        var item = addFileOpenMenuItem( menuType, label, extension, action );
        item.setShortcut( new MenuShortcut( key, shift ) );
        return item;
    }
    
    //adds a menu item that opens a file
    public MenuItem addFileOpenMenuItem( int menuType, String label, String extension, ExpOperon<FileInputStream> action ) {
        return addMenuItem( menuType, label, a -> openFileAction( extension, action ) );
    }
    
    //adds a menu item that opens a file with a keybind
    public MenuItem addFileOpenMenuItem( int menuType, String label, int key, boolean shift, String extension, ExpOperon<FileInputStream> action ) {
        var item = addFileOpenMenuItem( menuType, label, extension, action );
        item.setShortcut( new MenuShortcut( key, shift ) );
        return item;
    }
    
    //adds a menu item that saves a file
    public MenuItem addFileSaveMenuItem( int menuType, String label, String extension, ExpOperon<FileOutputStream> action ) {
        return addMenuItem( menuType, label, a -> saveFileAction( extension, action ) );
    }
    
    //adds a menu item that saves a file with a keybind
    public MenuItem addFileSaveMenuItem( int menuType, String label, int key, boolean shift, String extension, ExpOperon<FileOutputStream> action ) {
        var item = addFileSaveMenuItem( menuType, label, extension, action );
        item.setShortcut( new MenuShortcut( key, shift ) );
        return item;
    }
    
    
    //overridden methods
    @Override
    public void setFont( Font font ) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setLayout( LayoutManager manager ) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension( 0, minHeight );
    }
    
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    @Override
    public void doLayout() {
        int height = 0;
        for( IntUnaryOperator layouter : layouters ) {
            height += layouter.applyAsInt( height );
        }
    }
    
    @Override
    public void paint( Graphics g ) {
        g.setColor( super.getBackground() );
        g.fillRect( 0, 0, super.getWidth(), super.getHeight() );
        super.paint( g );
    }
    
    @Override
    protected void addImpl( Component comp, Object constraints, int index ) {
        comp.setForeground( super.getForeground() );
        comp.setBackground( super.getBackground() );
        super.addImpl( comp, constraints, index );
    }
    
    @Override
    protected void processMouseEvent( MouseEvent m ) {
        if( m.getID() == MouseEvent.MOUSE_CLICKED ) {
            super.requestFocus();
        }
        super.processMouseEvent( m );
    }
    
    
    //private utility methods
    //creates a field entry for the editpane
    private TextField createField( Component repaint, Consumer<String> setter, Supplier<String> getter ) {
        TextField field = new TextField() {
            {
                super.enableEvents( ActionEvent.ACTION_EVENT_MASK|FocusEvent.FOCUS_EVENT_MASK );
            }
            
            @Override
            protected void processActionEvent( ActionEvent a ) {
                setter.accept( super.getText() );
                repaint.repaint();
                super.processActionEvent( a );
            }
            
            @Override
            protected void processFocusEvent( FocusEvent f ) {
                if( super.isFocusOwner() )
                    super.selectAll();
                else
                    super.setText( getter.get() );
                super.processFocusEvent( f );
            }
        };
        updaters.add( () -> {
            if( field.isFocusOwner() )
                field.setText( getter.get() );
            else
                field.setText( getter.get() );
        } );
        return field;
    }
    
    //creates a button for units for the editpane
    private Button selectButton( int updateIndex, Units units ) {
        Button button = new Button( defaultUnits.getOrDefault( units, "???" ) );
        button.addActionListener( a -> {
            button.setLabel( getUnitSelector( units ).apply( button.getLabel() ) );
            updaters.get( updateIndex ).run();
            doLayout();
        } );
        return button;
    }
    
    //width needed for a button
    private int widthOf( Button button ) {
        Graphics g = super.getGraphics();
        if( g != null )
            return g.getFontMetrics( button.getFont() ).getStringBounds( button.getLabel(), g ).getBounds().width + 10;
        else
            return THICKNESS * 3;
    }
    
    //opens a file using the file dialog
    private void openFileAction( String extension, ExpOperon<FileInputStream> action ) {
        fileBrowser.setMode( FileDialog.LOAD );
        fileBrowser.setMultipleMode( true );
        fileBrowser.setFilenameFilter( ( file, name ) -> file.isDirectory() || name.endsWith( extension ) );
        fileBrowser.setVisible( true );
        for( var file : fileBrowser.getFiles() ) {
            var path = file.getAbsolutePath();
            Logger.logThreadMessage( "Opening file: " + path );
            try( var in = new FileInputStream( path ) ) {
                action.apply( in );
                in.close();
                JOptionPane.showMessageDialog( null, "Successfully loaded " + path, "Open File", JOptionPane.INFORMATION_MESSAGE );
            } catch( Exception e ) {
                Logger.logThrowable( e, "Failed to open file: " + path );
                JOptionPane.showMessageDialog( null, "Could not load file " + path + ":\n" + e, "Error", JOptionPane.ERROR_MESSAGE );
            }
        }
    }
    
    //saves a file using the file dialog
    private boolean saveFileAction( String extension, ExpOperon<FileOutputStream> action ) {
        fileBrowser.setMode( FileDialog.SAVE );
        fileBrowser.setMultipleMode( false );
        fileBrowser.setFilenameFilter( ( file, name ) -> file.isDirectory() || name.endsWith( extension ) );
        fileBrowser.setVisible( true );
        var file = fileBrowser.getFile();
        if( file != null ) {
            var path = new File( file ).getAbsolutePath();
            path = path.endsWith( extension ) ? path : path + extension;
            Logger.logThreadMessage( "Saving file: " + path );
            try( var out = new FileOutputStream( path ) ) {
                action.apply( out );
                JOptionPane.showMessageDialog( null, "Successfully saved " + path, "Save File", JOptionPane.INFORMATION_MESSAGE );
                return false;
            } catch( Exception e ) {
                Logger.logThrowable( e, "Failed to save file: " + path );
                JOptionPane.showMessageDialog( null, "Could not save file " + path + ":\n" + e, "Error", JOptionPane.ERROR_MESSAGE );
            }
        }
        return true;
    }
    
    
    //static utility method for creating selection for list of sim objects
    public static <T extends SimObject> Function<T[],T> getSimObjectSelector() {
        var reference = new SimObject[1];
        var array = new SimObject[1][];
        var list = new java.awt.List();
        var search = new TextField();
        var dialog = getListSelector( "Select Object", list, search );
        search.addActionListener( a -> {
            var txt = search.getText();
            for( var obj : array[0] )
                if( txt.equals( obj.name() ) ) {
                    reference[0] = obj;
                    dialog.setVisible( false );
                } 
        } );
        list.addActionListener( a -> {
            reference[0] = array[0][ list.getSelectedIndex() ];
            dialog.setVisible( false );
        } );
        return arr -> {
            synchronized( dialog ) {
                array[0] = arr;
                reference[0] = null;
                list.removeAll();
                for( var item : arr )
                    list.add( item.name() );
                dialog.setVisible( true );
                return (T)reference[0];
            }
        };
    }
    
    //static utility method for creating a unit selection dialog
    public static synchronized Function<String,String> getUnitSelector( Units units ) {
        var map = SELECT_DIALOGS.get();
        if( map == null ) {
            map = new HashMap<>();
            SELECT_DIALOGS = new SoftReference<>( map );
        }
        var select = map.get( units );
        if( select == null ) {
            var reference = new String[1];
            var list = new java.awt.List();
            var search = new TextField();
            var dialog = getListSelector( "Select Measurement Units", list, search );
            search.addActionListener( a -> {
                var txt = search.getText();
                if( units.valid( txt ) ) {
                    reference[0] = txt;
                    dialog.setVisible( false );
                }
            } );
            list.addActionListener( a -> {
                reference[0] = list.getSelectedItem();
                dialog.setVisible( false );
            } );
            select = original -> {
                synchronized( dialog ) {
                    reference[0] = original;
                    dialog.setVisible( true );
                    return reference[0];
                }
            };
            units.forEach( s -> list.add( s ) );
            map.put( units, select );
        }
        return select;
    }
    
    //private method for creating a list selection dialog
    private static Dialog getListSelector( String text, java.awt.List list, TextField search ) {
        var screen = Toolkit.getDefaultToolkit().getScreenSize();
        var dialog = new Dialog( (Window)null, text ) {
            {
                super.enableEvents( WindowEvent.WINDOW_EVENT_MASK );
            }
            
            @Override
            public void doLayout() {
                Insets insets = super.getInsets();
                int w = super.getWidth() - insets.left - insets.right;
                search.setBounds( insets.left, insets.top, w, THICKNESS );
                list.setBounds( insets.left, insets.top + THICKNESS, w, super.getHeight() - insets.top - insets.bottom - THICKNESS );
            }
            
            @Override
            protected void processWindowEvent( WindowEvent w ) {
                if( w.getID() == WindowEvent.WINDOW_CLOSING )
                    super.setVisible( false );
                super.processWindowEvent( w );
            }
        };
        search.addFocusListener( new FocusListener() {
            @Override
            public void focusGained( FocusEvent f ) {
                search.setText( "" );
            }
            @Override
            public void focusLost( FocusEvent f ) {
                search.setText( "Search..." );
            }
        } );
        search.setText( "Search..." );
        list.setMultipleMode( false );
        dialog.add( list );
        dialog.add( search );
        dialog.setModal( true );
        dialog.setBounds( screen.width / 2 - 100, screen.height / 2 - 100, 200, 200 );
        dialog.setResizable( false );
        return dialog;
    }
    
    @FunctionalInterface
    public static interface ExpOperon<Type> {
        //method to implement by function
        public void apply( Type type ) throws Exception;
    }
}
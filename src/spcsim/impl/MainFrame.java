package spcsim.impl;

/* Author: Kent Fukuda
 * Description: class to display various components of the simulation environment
 * Created: 8-27-21
 * Status: environment class, finished
 * Dependencies: EditPane, Logger, SimObject, Environment
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import spcsim.base.EditPane;
import spcsim.base.Logger;
import spcsim.base.SimObject;

public final class MainFrame extends JFrame implements SimObject {

    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "APPCTN" );
    
    //file path to default config file
    private final String filePath;
    //component variables
    private final Container container;//container housing environment
    private final ScrollPane scrollPane;//scroll pane housing edit pane
    private final EditPane editPane;//edit pane of simulation
    //simulation environment storage
    private final HashMap<Class<? extends Environment<?>>,EnvCache> envCache;//cache of previously used environments
    private transient Environment environment;//current simulation environment
    //frame setting variables
    private String currentEnvironment;//indicates the current environment the frame is using
    private boolean paneVisible;//indicates if the edit pane panel is visible
    private int paneSize, frameX, frameY, frameWidth, frameHeight;//dimensions of various aspects of the frame
    
    //constructor
    public MainFrame() {
        String path = System.getProperty( "user.home" );
        filePath = ( path == null ? "" : ( path.endsWith( "/" ) || path.endsWith( "\\" ) ? path : 
                path + File.separatorChar ) ) + "config.spcsim.acad.kaf.cfg";
        Logger.logMessage( "Configuration file path set to: " + filePath );
        container = new Container();
        scrollPane = new ScrollPane();
        editPane = new EditPane();
        envCache = new HashMap<>();
        currentEnvironment = "null";
        paneVisible = true;
        paneSize = 300;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frameX = screen.width /= 10;
        frameY = screen.height /= 10;
        frameWidth = screen.width * 7;
        frameHeight = screen.height * 7;
        scrollPane.add( editPane );
        container.setLayout( new CardLayout() );
        try( var stream = MainFrame.class.getClassLoader().getResourceAsStream( "spcsim/icon.png" ) ) {
            super.setIconImage( ImageIO.read( stream ) );
        } catch( IOException|IllegalArgumentException|NullPointerException e ) { }
        super.setTitle( "Space Simulation v" + Environment.VERSION_NAME  );
        super.setBounds( frameX, frameY, frameWidth, frameHeight );
        super.setMenuBar( editPane.getMenuBar() );
        super.enableEvents( WindowEvent.WINDOW_EVENT_MASK );
        super.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        super.setBackground( Color.DARK_GRAY );
        super.add( container );
        super.add( scrollPane );
        super.setLayout( null );
        Logger.logThreadMessage( "MainFrame @ " + super.hashCode() + " created" );
    }
    
    @Override
    public void doLayout() {
        super.doLayout();
        int w = super.rootPane.getWidth();
        int h = super.rootPane.getHeight();
        if( paneVisible ) {
            w -= paneSize;
            scrollPane.setBounds( w, 0, paneSize, h );
        }
        container.setBounds( 0, 0, w, h );
    }
    
    
    //returns the visibility of the edit pane
    public boolean getEditPaneVisible() {
        return paneVisible;
    }
    
    //sets the visibility of the edit pane
    public void setEditPaneVisible( boolean visible ) {
        paneVisible = visible;
        scrollPane.setVisible( visible );
        super.revalidate();
    }
    
    //sets the environment 
    public void setSimulation( Class<?> envClass ) {
        var cls = (Class<? extends Environment<?>>)envClass;
        var cache = envCache.get( cls );
        Environment<?> env;
        if( cache == null ) {
            try {
                env = SimObject.newInstance( cls );
                cache = new EnvCache();
                cache.envInfo = env.formatString();
                cache.reference = new SoftReference<>( env );
                envCache.put( cls, cache );
            } catch( IllegalStateException e ) {
                Logger.logThrowable( e );
                return;
            }
        } else {
            env = cache.reference.get();
            if( env == null ) {
                try {
                    env = (Environment<?>)SimObject.valueOf( cache.envInfo );
                } catch( IllegalStateException|IllegalArgumentException e ) {
                    Logger.logThrowable( e );
                    try {
                        env = SimObject.newInstance( cls );
                    } catch( IllegalStateException e1 ) {
                        Logger.logThrowable( e1 );
                    }
                }
                cache.reference = new SoftReference<>( env );
            }
        }
        if( environment != null ) {
            envCache.get( (Class<Environment<?>>)environment.getClass() ).envInfo = environment.formatString();
            environment.stop();
            container.removeAll();
            if( environment.acceptedType().equals( env.acceptedType() ) ) {
                var add = Arrays.asList( environment.getParticleList() );
                env.queueOperation( list -> {
                    list.clear();
                    ( (List<SimObject>)list ).addAll( add );
                } );
            }
        }
        editPane.clearEditPane();
        env.generateGUI( editPane, this );
        editPane.revalidate();
        environment = env;
        container.add( env );
        super.revalidate();
        Logger.logThreadMessage( "Environment set to: " + environment );
    }
    
    //returns current simulation class
    public Class<?> getSimulation() {
        return environment == null ? null : environment.getClass();
    }
    
    //reads the config file to the simulation data
    public void readConfig( FileInputStream input ) throws NullPointerException, NoSuchElementException, IllegalStateException, IllegalArgumentException {
        Logger.logMessage( "Reading configuration file" );
        Scanner scanner = new Scanner( input );
        String config = scanner.nextLine();
        while( scanner.hasNextLine() ) {
            var str = scanner.nextLine();
            Class<? extends Environment<?>> cls;
            try {
                cls = (Class<? extends Environment<?>>)SimObject.classType( str.substring( 0, str.indexOf( '[' ) ) );
            } catch( IndexOutOfBoundsException|IllegalStateException e ) {
                e.printStackTrace();
                continue;
            }
            var cache = envCache.get( cls );
            if( cache == null ) {
                cache = new EnvCache();
                cache.reference = new SoftReference<>( null );
                envCache.put( cls, cache );
            }
            cache.envInfo = str;
            var env = cache.reference.get();
            if( env != null )
                try {
                    env.parseString( str );
                } catch( IllegalStateException|IllegalArgumentException e ) { }
        }
        parseString( config );
    }
    
    //writes the config file with simulation data
    public void writeConfig( FileOutputStream output ) throws NullPointerException, IllegalArgumentException, IllegalStateException {
        Logger.logMessage( "Writing configuration file" );
        PrintStream stream = new PrintStream( output );
        stream.println( formatString() );
        if( environment != null )
            envCache.get( (Class<Environment<?>>)environment.getClass() ).envInfo = environment.formatString();
        envCache.forEach( ( cls, cache ) -> stream.println( cache.envInfo ) );
    }
    
    //reads the default config file from home file path
    public boolean readConfig() {
        try( var stream = new FileInputStream( filePath ) ) {
            readConfig( stream );
            return true;
        } catch( IOException|SecurityException|NullPointerException|IllegalArgumentException|IllegalStateException|NoSuchElementException e ) { 
            Logger.logThrowable( e, "Unable to read default configuration file" );
            return false;
        }
    }
    
    //writes the default config file from home file path
    public boolean writeConfig() {
        try( var stream = new FileOutputStream( filePath ) ) {
            writeConfig( stream );
            stream.close();
            return true;
        } catch( IOException|SecurityException|NullPointerException|IllegalArgumentException|IllegalStateException e ) { 
            Logger.logThrowable( e, "Unable to write default configuration file" );
            return false;
        }
    }
    
    
    @Override
    protected void processWindowEvent( WindowEvent w ) {
        super.processWindowEvent( w );
        if( w.getID() == WindowEvent.WINDOW_CLOSING ) {
            editPane.confirmSaveAction( () -> {
                dispose();
                writeConfig();
            } );
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        Logger.logMessage( "Shutting down Space Simulation Program" );
    }
    
    @Override
    public String formatString() throws IllegalStateException {
        currentEnvironment = environment == null ? "null" : environment.typeName();
        frameX = super.getX();
        frameY = super.getY();
        frameWidth = super.getWidth();
        frameHeight = super.getHeight();
        paneVisible = scrollPane.isVisible();
        return SimObject.super.formatString();
    }
    
    @Override
    public void parseString( String stringForm ) throws IllegalStateException, IllegalArgumentException {
        SimObject.super.parseString( stringForm );
        setSimulation( SimObject.classType( currentEnvironment ) );
        super.setBounds( frameX, frameY, frameWidth, frameHeight );
        scrollPane.setVisible( paneVisible );
    }
    
    
    private static class EnvCache {
        private String envInfo;
        private SoftReference<Environment<?>> reference;
        
        private EnvCache() {
            envInfo = "";
            reference = new SoftReference<>( null );
        }
    }
}
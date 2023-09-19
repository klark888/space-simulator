package spcsim.impl;

/* Author: Kent Fukuda
 * Description: Class that stores the simulation environment including the list of SpaceObjects and simulates them
 * Created: 7-13-21
 * Status: environment class, finished
 * Dependencies: SimObject, EditPane, Units, MainFrame
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import spcsim.base.EditPane;
import spcsim.base.Logger;
import spcsim.base.SimObject;
import spcsim.base.Units;

public abstract class Environment<Type extends SimObject> extends Component implements SimObject, Runnable {
    
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), null );
    //version name
    public static final String VERSION_NAME = "2.0.0";
    //simulator variables
    protected final ArrayList<Type> particles;//lists of spaceobjects in the simulation
    private final List<Consumer<List<Type>>> operationQueue;//queue of external operations queued to the spaceObjects
    private final Class<Type> acceptedType;//accepted type of particle
    private final String[] assetNames;
    private transient Thread mainThread;//thread simulator runs on
    //environment variables
    protected double timeStep;//indicates the time passed per tick of simulation
    protected double timePassed;//indicates days passed in simulation
    private long tickLength;//minumum length of each tick
    private long refreshLength;//minimum length of each simulation update
    protected transient volatile boolean simActive;//if simulation is active
    
    
    //constructor
    protected Environment( Class<Type> accept, String... assets ) {
        particles = new ArrayList<>();
        operationQueue = Collections.synchronizedList( new ArrayList<>() );
        acceptedType = accept;
        assetNames = assets;
        mainThread = null;
        timeStep = 1;
        timePassed = 0;
        tickLength = 8;
        refreshLength = 16;
        simActive = false;
        Logger.logCreation( "Environment " + toString() + " for class type " + accept + " created" );
    }
    
    
    //returns accepted type
    public final Class<Type> acceptedType() {
        return acceptedType;
    }
    
    //starts simulation thread
    public final synchronized void start() {
        mainThread = new Thread( this, "Simulator-" + SimObject.super.typeName() + "-Main" );
        mainThread.setDaemon( true );
        mainThread.setPriority( Thread.MAX_PRIORITY );
        mainThread.start();
        try {
            while( !simActive )
                Thread.sleep( 16 );
        } catch( InterruptedException e ) { }
    }
    
    //stops simulation thread
    public final synchronized void stop() {
        simActive = false;
        try {
            while( mainThread != null )
                Thread.sleep( 16 );
        } catch( InterruptedException e ) { }
        clearQueue();
    }
    
    //queues an operation to spaceObject list
    public final synchronized void queueOperation( Consumer<List<Type>> operation ) {
        if( mainThread == null ) {
            operation.accept( particles );
            super.repaint();
        } else
            operationQueue.add( operation );
    }
    
    //returns copy of particle list
    public final Type[] getParticleList() {
        return particles.toArray( (Type[])Array.newInstance( acceptedType, particles.size() ) );
    }
    
    //generates menu items for this environment
    protected void generateGUI( EditPane editPane, MainFrame frame ) {
        Logger.logMessage( "Generating GUI for " + toString() );
        editPane.setSaveAction( out -> SimObject.write( getParticleList(), out ) );
        editPane.addConfirmSaveMenuItem( EditPane.FILE_TYPE, "New", KeyEvent.VK_N, false, () -> queueOperation( list -> list.clear() ) );
        editPane.addFileOpenMenuItem( EditPane.FILE_TYPE, "Open", KeyEvent.VK_O, false, EditPane.SPCOBJ_EXTENSION, in -> {
            var add = Arrays.asList( (Type[])SimObject.read( in ) );
            queueOperation( list -> {
                list.clear();
                list.addAll( add );
            } );
        } );
        editPane.addFileOpenMenuItem( EditPane.FILE_TYPE, "Add", KeyEvent.VK_O, true, EditPane.SPCOBJ_EXTENSION, in -> {
            var add = (List<Type>)Arrays.asList( SimObject.read( in ) );
            setPosToCamera( add );
            queueOperation( list -> list.addAll( add ) );
        } );
        editPane.addFileSaveMenuItem( EditPane.FILE_TYPE, "Save", KeyEvent.VK_S, false, EditPane.SPCOBJ_EXTENSION, editPane.getSaveAction() );
        editPane.addConfirmFileOpenMenuItem( EditPane.FILE_TYPE, "Import TXT", ".txt", in -> {
            var scanner = new Scanner( in );
            ArrayList<Type> add = new ArrayList<>();
            while( scanner.hasNextLine() ) {
                var obj = SimObject.valueOf( scanner.nextLine() );
                add.add( (Type)obj );
            }
            queueOperation( list -> {
                list.clear();
                list.addAll( add );
            } );
        } );
        editPane.addFileSaveMenuItem( EditPane.FILE_TYPE, "Export TXT", ".txt", out -> {
            var stream = new PrintStream( out );
            for( var obj : getParticleList() )
                stream.println( obj.formatString() );
        } );
        editPane.addFileOpenMenuItem( EditPane.FILE_TYPE, "Import Config", ".cfg", in -> frame.readConfig( in ) );
        editPane.addFileSaveMenuItem( EditPane.FILE_TYPE, "Export Config", ".cfg", out -> {
            frame.writeConfig( out );
            super.repaint();
        } );
        editPane.addConfirmSaveMenuItem( EditPane.FILE_TYPE, "Exit", () -> {
            frame.dispose();
            frame.writeConfig();
        } );
        editPane.addValueMenuItem( EditPane.VIEW_TYPE, "FPS", "Frames Per Second", () -> 1000.0 / refreshLength, val -> refreshLength = (int)( 1000.0 / val ) );
        editPane.addMenuItem( EditPane.VIEW_TYPE, "Reset Simulation Counter", a -> {
            timePassed = 0;
            super.repaint();
        } );
        editPane.addToggleMenuItem( EditPane.VIEW_TYPE, "Edit Pane Visible", frame::getEditPaneVisible, frame::setEditPaneVisible );
        editPane.addMenuItem( EditPane.CONTROL_TYPE, "Start", KeyEvent.VK_Q, false, a -> start() );
        editPane.addMenuItem( EditPane.CONTROL_TYPE, "Stop", KeyEvent.VK_W, false, a -> stop() );
        String timeUnit = editPane.getDefaultUnit( Units.TIME );
        timeUnit = timeUnit == null ? "days" : timeUnit;
        editPane.addUnitValueMenuItem( EditPane.CONTROL_TYPE, "Simulation Speed", timeUnit, () -> timeStep * 1000 / tickLength, val -> {
            if( Double.isInfinite( val ) )
                tickLength = -1;
            else
                timeStep = val * tickLength / 1000;
        } );
        editPane.addUnitValueMenuItem( EditPane.CONTROL_TYPE, "Time Step", timeUnit, () -> timeStep, val -> timeStep = val );
        editPane.addValueMenuItem( EditPane.CONTROL_TYPE, "TPS", "Ticks Per Second", () -> 1000.0 / tickLength, val -> tickLength = (int)( 1000.0 / val ) );
        editPane.addMenuItem( EditPane.CONTROL_TYPE, "Max Tick Speed", a -> tickLength = -1 );
        iterate( editPane, frame, Environment.class );
        for( var cls : SimObject.subClassesOf( (Class<? extends Environment>)getClass().getSuperclass() ) )
            editPane.addMenuItem( EditPane.ENGINE_TYPE, cls.getSimpleName(), a -> frame.setSimulation( cls ) );
        for( var asset : assetNames )
            editPane.addMenuItem( EditPane.ASSET_TYPE, asset, a -> {
                try( var stream = Environment.class.getClassLoader().getResourceAsStream( "assets/" + asset + ".spcobj" ) ) {
                    var add = (List<Type>)Arrays.asList( SimObject.read( stream ) );
                    setPosToCamera( add );
                    stream.close();
                    queueOperation( list -> list.addAll( add ) );
                } catch( SecurityException|NullPointerException|IOException|IllegalStateException e ) { 
                    JOptionPane.showMessageDialog( null, "Failed to open selected asset", "Error", JOptionPane.ERROR_MESSAGE );
                    Logger.logThrowable( e );
                }
            } );
        editPane.addMenuItem( EditPane.ABOUT_TYPE, "Information", a -> JOptionPane.showMessageDialog( null, 
                "Program: Space Simulation Program\n" + 
                "Author:  Kent Fukuda\n" + 
                "Version: " + VERSION_NAME + "\n" +
                "Created: 8-31-2021\n" +
                "License: GNU Affero General Public License",
                "Information", JOptionPane.INFORMATION_MESSAGE ) );
        editPane.addMenuItem( EditPane.ABOUT_TYPE, "GitHub", a -> {
            String url = "https://github.com/klark888/space-simulator";
            try {
                Desktop.getDesktop().browse( new URI( url ) );
            } catch( UnsupportedOperationException|IOException|URISyntaxException e ) {
                JOptionPane.showMessageDialog( null, "Could not open browser. Please use this link:\n" + url, "Error", JOptionPane.ERROR_MESSAGE );
            }
        } );
        editPane.addMenuItem( EditPane.ABOUT_TYPE, "Help", a -> JOptionPane.showMessageDialog(  null, 
                "File - Open and save the simulation environment, program configuration, and add script classes.\n" +
                "Edit - Select or create a new object in the simulation or change the current selected object.\n" + 
                "Simulation - Start and stop simulation or change the speed to the simulation.\n" + 
                "View - Zoom in, out, change measurement units, or set FPS.\n" + 
                "Assets - Select an asset to import into the simulation.\n" +
                "About - Information about the program.\n" +
                "Editor Pane - Edit the current selected object.\n" + 
                "Simulation View - Drag mouse to move camera. Click on a object to select it.\n" +
                "Edit Object - Drag arrow around to change velocity. Drag camera to change position.", 
                "Help", JOptionPane.INFORMATION_MESSAGE ) );
        editPane.addMenuItem( EditPane.ABOUT_TYPE, "Changelog", a -> JOptionPane.showMessageDialog( null, 
                "Version " + VERSION_NAME + ":\n" +
                "Merged particle simulation.\n" + 
                "Refactored code.", 
                "Changelog", JOptionPane.INFORMATION_MESSAGE ) );
    }
    
    //sets the position of objects to the camera
    protected void setPosToCamera( List<Type> objects ) { }
    
    //abstract methods to implement
    //simulate
    protected abstract void simulate();
    
    //paint
    @Override
    public abstract void paint( Graphics g );
    
    //overridden runnable method
    @Override
    public final void run() {
        //check if thread is main sim thread
        if( mainThread != Thread.currentThread() )
            throw new IllegalStateException();
        simActive = true;
        Logger.logThreadMessage( "Simulation started for " + toString() );
        long refreshTime = 0, simTime = 0, currentTime;
        while( simActive ) {
            currentTime = System.currentTimeMillis();
            //repaints simulation and consumes queued operations
            if( currentTime > refreshTime + refreshLength ) {
                clearQueue();
                super.revalidate();
                super.repaint();
                refreshTime = currentTime;
            }
            //simulates a single tick of the simulation
            if( currentTime > simTime + tickLength ) {
                simulate();
                timePassed += timeStep;
                simTime = currentTime;
            }
        }
        mainThread = null;
        Logger.logThreadMessage( "Simulation stopped for " + toString() );
    }
    
    //overridden to string method
    @Override
    public final String toString() {
        return getClass().getName() + '@' + System.identityHashCode( this );
    }
    
    //private utility method executes and clears the operation queue
    private boolean clearQueue() {
        if( !operationQueue.isEmpty() ) {
            try {
                synchronized( particles ) {
                    do {
                        operationQueue.remove( 0 ).accept( particles );
                    } while( !operationQueue.isEmpty() );
                }
            } catch( Throwable t ) {
                t.printStackTrace( System.err );
            }
            super.repaint();
            return true;
        }
        return false;
    }
    
    //private method for iterating over environment class to add to menu
    private void iterate( EditPane editPane, MainFrame frame, Class<? extends Environment> parent ) {
        for( var cls : SimObject.subClassesOf( parent ) ) {
            if( SimObject.typeName( cls ) == null )
                iterate( editPane, frame, cls );
            else
                editPane.addConfirmSaveMenuItem( EditPane.SIMULATION_TYPE, cls.getSimpleName(), () -> {
                    if( !cls.equals( getClass().getSuperclass() ) )
                        frame.setSimulation( SimObject.subClassesOf( cls )[0] );
                } );
        }
    }
}
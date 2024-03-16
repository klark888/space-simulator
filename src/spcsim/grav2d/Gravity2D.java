package spcsim.grav2d;

/* Author: Kent Fukuda
 * Description: Parent class of the Gravity2D simulations implementations
 * Created: 9-1-22
 * Status: environment class, finished
 * Dependencies: SimObject, Env2D, SpaceObject2D, EditPane, Logger, Units, MainFrame
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.function.Function;
import javax.swing.JOptionPane;
import spcsim.impl.MainFrame;
import spcsim.base.EditPane;
import spcsim.base.Logger;
import spcsim.base.SimObject;
import spcsim.base.Units;
import spcsim.impl.Env2D;

public abstract class Gravity2D extends Env2D<SpaceObject2D> {
    
    //serialversionuid
    public static final long serialVersionUID;
    //default settings
    private static final String DEFAULT_MASS = "earth masses", DEFAULT_LENGTH = "earth radii", DEFAULT_TIME = "days", DEFAULT_DEGREE = "";
    //object used for when selectioned object is null
    private static final SpaceObject2D NULL_SELECT = new SpaceObject2D();
    
    //environment variables for rendering settings
    private boolean showNames, showEnvStatus;
    private String lengthUnit, timeUnit;
    private transient boolean isVelocDragMode, addMoonMode;
    private transient SpaceObject2D selected;//selected object to edit
    
    
    //static initializzer
    static {
        serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "GRZDAB" );
        SimObject.ensureLoaded( SpaceObject2D.class );
    }
    
    //constructor
    protected Gravity2D() {
        super( SpaceObject2D.class, "Default System", "Proto System", "Solar System", "Spiral System" );
        super.timeStep = 1.0 / 24;
        timeUnit = "days";
        lengthUnit = "AU";
        showNames = true;
        isVelocDragMode = false;
        addMoonMode = false;
        showEnvStatus = true;
        selected = new SpaceObject2D( "Default Planet", 0xFFC97C2E, 5.5171459763102915, 3.74510897085609, 0, 0, 0, 0 );
        particles.add( selected );
    }
    
    
    //overridden methods
    //events processing for when mouse is clicked or released
    @Override
    protected final void processMouseEvent( MouseEvent e ) {
        switch( e.getID() ) {
            case MouseEvent.MOUSE_CLICKED :
                if( addMoonMode ) {
                    try {
                        double moonMass = Double.parseDouble( JOptionPane.showInputDialog( "Mass of moon:", Double.toString( selected.mass * 0.1 ) ) );
                        super.queueOperation( list -> {
                            var moon = new SpaceObject2D();
                            moon.name = "Moon of " + ( selected.name == null ? "Unnamed" : selected.name );
                            moon.color( selected.color() );
                            moon.mass = moonMass;
                            moon.radius = Math.pow( moonMass / selected.mass, 0.33333333 ) * selected.radius;
                            moon.xPos = super.translateX( e.getX() );
                            moon.yPos = super.translateY( e.getY() );
                            SpaceObject2D.orbit( selected, moon, 0 );
                            list.add( moon );
                        } );
                    } catch( IllegalArgumentException|NullPointerException e1 ) { }
                    addMoonMode = false;
                } else {
                    super.queueOperation( list -> {
                        //searched for object at the location and selects it
                        for( var obj : list ) {
                            int xDiff = e.getX() - super.translateX( obj.xPos );
                            int yDiff = e.getY() - super.translateY( obj.yPos );
                            if( Math.sqrt( xDiff * xDiff + yDiff * yDiff ) < Math.max( obj.radius * scale, 5 ) ) {
                                selected = obj;
                                return;
                            }
                        }
                        //deselects current object
                        selected = null;
                    } );
                }
                e.getComponent().requestFocus();
                super.repaint();
                break;
            case MouseEvent.MOUSE_RELEASED : lastDrag = null;
            default :
        }
        super.processMouseEvent( e );
    }
    
    //events processing for when the mouse is moved or dragged
    @Override
    protected final void processMouseMotionEvent( MouseEvent e ) {
        if( e.getID() == MouseEvent.MOUSE_DRAGGED ) {
            if( lastDrag != null ) {
                double dragX = -( e.getX() - lastDrag.getX() ) / scale;
                double dragY = ( e.getY() - lastDrag.getY() ) / scale;
                if( selected != null ) {
                    if( isVelocDragMode ) {
                        //drags the velocity vector of selectedObject
                        double mult = Math.sqrt( scale );
                        selected.xVeloc -= dragX * mult;
                        selected.yVeloc -= dragY * mult;
                    } else {
                        //drags the position of selectedObject
                        selected.xPos += dragX;
                        selected.yPos += dragY;
                    }
                } else {
                    //drags the position of camera
                    posX += dragX;
                    posY += dragY;
                }
            } else if( selected != null ) {
                //determines if the current mouse dragging movement is dragging the velocity vector arrow or position
                double mult = Math.sqrt( scale );
                int xDiff = (int)( selected.xVeloc * mult) + super.getWidth() / 2 - e.getX();
                int yDiff = -(int)( selected.yVeloc * mult) + super.getHeight() / 2 - e.getY();
                isVelocDragMode = Math.sqrt( xDiff * xDiff + yDiff * yDiff) < 20;
            }
            lastDrag = e;
        }
        lastPos = e;
        super.repaint();
        super.processMouseMotionEvent( e );
    }
    
    //adds gui specific to the gravity2d simulation
    @Override
    protected void generateGUI( EditPane editPane, MainFrame application ) {
        Function<SpaceObject2D[],SpaceObject2D> objSelect = EditPane.getSimObjectSelector();
        editPane.setDefaultUnit( Units.MASS, DEFAULT_MASS );
        editPane.setDefaultUnit( Units.LENGTH, DEFAULT_LENGTH );
        editPane.setDefaultUnit( Units.TIME, DEFAULT_TIME );
        editPane.setDefaultUnit( Units.DEGREE, DEFAULT_DEGREE );
        super.generateGUI( editPane, application );
        editPane.addStringField( "Name", this, v -> selected().name = v, () -> selected().name );
        editPane.addColorButton( "Color", this, v -> selected().color( v ), () -> selected().color() );
        editPane.addUnitField( "Mass",this,  v -> selected().mass = v, () -> selected().mass, Units.MASS, "earth masses" );
        editPane.addUnitField( "Radius", this, v -> selected().radius = v, () -> selected().radius, Units.LENGTH, "km" );
        editPane.addUnitField( "X - Position", this, v -> selected().xPos = v, () -> selected().xPos, Units.LENGTH, "AU" );
        editPane.addUnitField( "Y - Position", this, v -> selected().yPos = v, () -> selected().yPos, Units.LENGTH, "AU" );
        editPane.addDoubleUnitField( "Speed", this, v -> {
            double deg = Math.atan2( selected().yVeloc, selected().xVeloc );
            selected().xVeloc = v * Math.cos( deg );
            selected().yVeloc = v * Math.sin( deg );
        }, () -> Math.sqrt( selected().xVeloc * selected().xVeloc + selected().yVeloc * selected().yVeloc ), Units.LENGTH, Units.TIME, "km", "s" );
        editPane.addUnitField( "Direction", this, v -> {
            double mult = Math.sqrt( selected().xVeloc * selected().xVeloc + selected().yVeloc * selected().yVeloc );
            selected().xVeloc = mult * Math.cos( v );
            selected().yVeloc = mult * Math.sin( v );
        }, () -> Math.atan2( selected().yVeloc, selected().xVeloc ), Units.DEGREE, "degrees" );
        editPane.addDoubleUnitField( "X - Velocity", this, v -> selected().xVeloc = v, () -> selected().xVeloc, Units.LENGTH, Units.TIME, "km", "s" );
        editPane.addDoubleUnitField( "Y - Velocity", this, v -> selected().yVeloc = v, () -> selected().yVeloc, Units.LENGTH, Units.TIME, "km", "s" );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Select", a -> {
            selected = objSelect.apply( getParticleList() );
            super.repaint();
        } );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "New Object", KeyEvent.VK_N, true, a -> {
            var obj = new SpaceObject2D();
            setPosToCamera( Arrays.asList( obj ) );
            selected = obj;
            queueOperation( list -> list.add( obj ) ); 
        } );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Copy", KeyEvent.VK_C, true, a -> {
            if( selected != null )
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents( new StringSelection( selected.formatString() ), null );
        } );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Paste", KeyEvent.VK_V, true, a -> {
            try {
                var obj = (SpaceObject2D)SimObject.valueOf( (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData( DataFlavor.stringFlavor ) );
                setPosToCamera( Arrays.asList( obj ) );
                selected = obj;
                queueOperation( list -> list.add( obj ) );
                super.repaint();
            } catch ( IllegalArgumentException|IllegalStateException|IOException|UnsupportedFlavorException|ClassCastException e ) {
                Logger.logThrowable( e, "Error in copying from clipboard" );
            }
        } );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Cut", KeyEvent.VK_X, true, a -> queueOperation( list -> list.remove( selected ) ) );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Add Moon", a -> {
            addMoonMode = selected() != null;
            super.repaint();
        } );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Random Planet", a -> {
            SpaceObject2D rand = new SpaceObject2D();
            rand.name = "-";
            for( int i = 0; i < 3; i++ ) {
                rand.name = (char)( (int)( Math.random() * 26 ) + 'A' ) + rand.name;
                rand.name += (int)( Math.random() * 10 );
            }
            rand.mass = 0.3 / ( Math.random() + 0.00009 ) - 0.25;
            rand.radius = rand.mass < 5.288 ? Math.pow( rand.mass, 0.27 ) : Math.pow( 0.0046 * rand.mass + 0.3832, -2.5 ) + 11;
            rand.radius *= 0.9 + Math.random() * 0.2;
            rand.xPos = posX;
            rand.yPos = posY;
            selected = rand;
            super.queueOperation( list -> list.add( rand ) );
        } );
        editPane.addToggleMenuItem( EditPane.VIEW_TYPE, "Toggle Name Labels", KeyEvent.VK_W, true, () -> showNames, val -> {
            showNames = val;
            super.repaint();
        } );
        editPane.addToggleMenuItem( EditPane.VIEW_TYPE, "Toggle Stats Boxes", KeyEvent.VK_Q, true, () -> showEnvStatus, val -> {
            showEnvStatus = val;
            super.repaint();
        } );
        editPane.addMenuItem( EditPane.VIEW_TYPE, "Time Units", a -> {
            timeUnit = EditPane.getUnitSelector( Units.TIME ).apply( timeUnit );
            super.repaint();
        } );
        editPane.addMenuItem( EditPane.VIEW_TYPE, "Length Units", a -> {
            lengthUnit = EditPane.getUnitSelector( Units.LENGTH ).apply( lengthUnit );
            super.repaint();
        } );
        super.addPropertyChangeListener( "repaint", p -> editPane.updatePane() );
    }
    
    //overridden methods rendering spaceobjects on the screen
    @Override
    public final void paint( Graphics g ) {
        //paints spaceobject objects
        int sw = super.getWidth(), sh = super.getHeight();
        g.setColor( Color.BLACK );
        g.fillRect( 0, 0, sw, sh );
        int w = sw / 2, h = sh / 2;
        
        if( selected != null && particles.contains( selected )  ) {
            //renders velocity vector
            posX = selected.xPos;
            posY = selected.yPos;
            double mult = Math.sqrt( scale );
            int velX = (int) ( selected.xVeloc * mult ) + w;
            int velY = -(int) ( selected.yVeloc * mult ) + h;
            g.setColor( Color.YELLOW );
            g.drawLine( w, h, velX, velY );
            g.drawOval(velX - 10, velY - 10, 20, 20 );
            //renders moon orbit
            if( addMoonMode ) {
                MouseEvent pos = lastPos;
                int mx = w - pos.getX();
                int my = h - pos.getY();
                int dist = (int) Math.sqrt(mx * mx + my * my);
                g.setColor( Color.LIGHT_GRAY );
                g.drawOval(-dist + w, -dist + h, dist * 2, dist * 2);
            }
            super.firePropertyChange( "repaint", 0, 1 );
        }
        
        //paints spaceobjects
        super.paint( g );
        //paints spaceobject names
        synchronized( particles ) {
            g.setColor( Color.WHITE );
            if( showNames )
                particles.forEach( obj -> {
                    String name;
                    if( obj != null && ( name = obj.name ) != null ) {
                        g.drawString( name, translateX( obj.xPos ), translateY( obj.yPos ) );
                    }
                } );
        }
        
        //paints environment status;
        if( showEnvStatus )
            g.drawString( "Coordinates: ( " + format( Units.LENGTH, posX + ( lastPos.getX() - w ) / scale, DEFAULT_LENGTH, lengthUnit ) + ", " + 
                    format( Units.LENGTH, posY - ( lastPos.getY() - h ) / scale, DEFAULT_LENGTH, lengthUnit ) + ") --- Simulation Time: " + 
                    format( Units.TIME, timePassed, DEFAULT_TIME, timeUnit ) + " --- Zoom Magnitude: " + (int)( scale * 100 ) + "%", 0, 10 );
    }
    
    
    //private methods
    //formats a double value to unit
    private String format( Units converter, double value, String defaultUnit, String unit ) {
        return Integer.toString( (int)converter.convert( value, defaultUnit, unit ) ) + ' ' + unit;
    }
    
    //get null selection to prevent NullPointerExceptions when selecting an object
    private SpaceObject2D selected() {
        return selected == null ? NULL_SELECT : selected;
    }
}
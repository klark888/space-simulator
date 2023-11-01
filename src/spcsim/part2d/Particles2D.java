package spcsim.part2d;

/* Author: Kent Fukuda
 * Description: Parent class of the Particles2D simulations implementations
 * Created: 9-16-23
 * Status: environment class, finished
 * Dependencies: SimObject, Env2D, AxiomObject2D, EditPane, MainFrame
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import spcsim.base.EditPane;
import spcsim.base.SimObject;
import spcsim.impl.Env2D;
import spcsim.impl.MainFrame;

public abstract class Particles2D extends Env2D<AxiomObject2D> {
    
    //clickmode constants
    private static final int PLACE = 0, REMOVE = 1, LOCK = 2;
    //serialversionuid
    public static final long serialVersionUID;
    //model object to be placed
    private transient final AxiomObject2D modelObject;
    private transient AxiomObject2D locked;
    private int count, clickMode;
    private double clickRadius, temperature, angularVeloc;
    
    //static initializzer
    static {
        serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "PTZDAB" );
        SimObject.ensureLoaded( AxiomObject2D.class );
    }
    
    //constructor
    protected Particles2D() {
        super( AxiomObject2D.class, "Ring Formation", "Black Hole", "Direct Collision", "Penetration Collision", "Hit and Run Collision", 
                "Cosmological Sponge", "Moon Creating Collision", "Mantle Differentiation", "Angular Momentum", "Accretion Disk", "Protoplanetary Disk" );
        modelObject = new AxiomObject2D( 0x7FFFFFFF, 1, 3, 0.5, 0.1, 0, 0, 0, 0 );
        locked = null;
        count = 10;
        clickMode = PLACE;
        clickRadius = 10;
        temperature = 0;
        angularVeloc = 0;
    }
    
    //overridden methods
    //events processing for when mouse is clicked or released
    @Override
    protected final void processMouseEvent( MouseEvent e ) {
        switch( e.getID() ) {
            case MouseEvent.MOUSE_CLICKED :
                double mouseX = super.translateX( e.getX() ), mouseY = super.translateY( e.getY() );
                switch( e.getButton() ) {
                    case MouseEvent.BUTTON1 :
                        switch( clickMode ) {
                            case PLACE :
                                super.queueOperation( list -> {
                                    for( int i = 0; i < count; i++ ) {
                                        AxiomObject2D obj = (AxiomObject2D)modelObject.clone();
                                        double dist = Math.sqrt( Math.random() ) * clickRadius, angle = Math.random() * Math.PI * 2;
                                        double x = Math.cos( angle ), y = Math.sin( angle );
                                        obj.xPos = mouseX + x * dist;
                                        obj.yPos = mouseY + y * dist;
                                        obj.xVeloc = y * angularVeloc * dist + ( Math.random() - 0.5 ) * temperature;
                                        obj.yVeloc = -x * angularVeloc * dist + ( Math.random() - 0.5 ) * temperature;
                                        list.add( obj );
                                    }
                                } );
                                break;
                            case REMOVE :
                                super.queueOperation( list -> {
                                    for( int i = 0 ; i < list.size(); i++ ) {
                                        AxiomObject2D obj = list.get( i );
                                        double xDiff = obj.xPos - mouseX;
                                        double yDiff = obj.yPos - mouseY;
                                        if( xDiff * xDiff + yDiff * yDiff < clickRadius * clickRadius ) {
                                            list.remove( i-- );
                                        }
                                    }
                                } );
                                break;
                            case LOCK :
                                super.queueOperation( list -> {
                                    //searched for object at the location and selects it
                                    for( var obj : list ) {
                                        double xDiff = obj.xPos - mouseX;
                                        double yDiff = obj.yPos - mouseY;
                                        if( Math.sqrt( xDiff * xDiff + yDiff * yDiff ) < Math.max( obj.radius * scale, 5 ) ) {
                                            locked = obj;
                                            return;
                                        }
                                    }
                                    //deselects current object
                                    locked = null;
                            } );
                            default :
                        }
                        break;
                    case MouseEvent.BUTTON3 :
                        super.queueOperation( list -> {
                            for( int i = 0; i < list.size(); i++ ) {
                                var obj = list.get( i );
                                double xDiff = obj.xPos - mouseX, yDiff = obj.yPos - mouseY;
                                if( xDiff * xDiff + yDiff * yDiff < clickRadius * clickRadius )
                                    list.remove( i-- );
                            }
                        } );
                        break;
                    default :
                }
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
                //drags the position of camera
                posX += dragX;
                posY += dragY;
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
        super.generateGUI( editPane, application );
        editPane.addColorButton( "Color", this, v -> modelObject.color( v ), () -> modelObject.color() );
        editPane.addValueField( "Particle Mass", this,  v -> modelObject.mass = v, () -> modelObject.mass );
        editPane.addValueField( "Particle Radius", this, v -> modelObject.radius = v, () -> modelObject.radius );
        editPane.addValueField( "Spring Force",this,  v -> modelObject.invSpring = 1 / v, () -> 1 / modelObject.invSpring );
        editPane.addValueField( "Drag Force", this, v -> modelObject.drag = v, () -> modelObject.drag );
        editPane.addValueField( "Click Radius", this, v -> clickRadius = v, () -> clickRadius );
        editPane.addValueField( "Particle Count", this, v -> count = (int)v, () -> count );
        editPane.addValueField( "Angular Velocity", this, v -> angularVeloc = v, () -> angularVeloc );
        editPane.addValueField( "Temperature", this, v -> temperature = v, () -> temperature );
        editPane.addValueField( "X - Velocity", this, v -> modelObject.xVeloc = v, () -> modelObject.xVeloc );
        editPane.addValueField( "Y - Velocity", this, v -> modelObject.yVeloc = v, () -> modelObject.yVeloc );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Place Mode", a -> clickMode = PLACE );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Remove Mode", a -> clickMode = REMOVE );
        editPane.addMenuItem( EditPane.EDIT_TYPE, "Lock Mode", a -> clickMode = LOCK );
        super.addPropertyChangeListener( "repaint", p -> editPane.updatePane() );
        editPane.updatePane();
    }
    
    //overridden methods rendering spaceobjects on the screen
    @Override
    public final void paint( Graphics g ) {
        g.setColor( Color.BLACK );
        g.fillRect( 0, 0, super.getWidth(), super.getHeight() );
        if( locked != null && particles.contains( locked )  ) {
            posX = locked.xPos;
            posY = locked.yPos;
        }
        super.paint( g );
        switch( clickMode ) {
            case PLACE : g.setColor( Color.WHITE ); break;
            case REMOVE : g.setColor( Color.RED ); break;
            case LOCK : g.setColor( Color.GREEN ); break;
            default :
        }
        int size = (int)( scale * 2 * clickRadius );
        g.drawOval( lastPos.getX() - size / 2, lastPos.getY() - size / 2, size, size );
    }
    
    //overridden for detecting editPane update events
    @Override
    public void parseString( String stringForm ) {
        super.parseString( stringForm );
        super.firePropertyChange( "repaint", 0, 1 );
    }
}

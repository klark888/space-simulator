package spcsim.impl;

/* Author: Kent Fukuda
 * Description: Particle entity used for 2D particle environment simulations
 * Created: 9-16-23
 * Status: environment class, finished
 * Dependencies: Objecti2D, EditPane, SimObject
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.invoke.MethodHandles;
import java.util.List;
import spcsim.base.EditPane;
import spcsim.base.SimObject;

public abstract class Env2D<Type extends Object2D> extends Environment<Type> {
    
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), null );
    //environment variables for rendering settings
    protected double scale, posX, posY;
    protected transient MouseEvent lastPos, lastDrag;
    
    protected Env2D( Class<Type> accept, String... assets ) {
        super( accept, assets );
        scale = 1;
        posX = 0;
        posY = 0;
        lastPos = new MouseEvent( this, 0, 0, 0, 0, 0, 0, false );
        lastDrag = null;
        super.enableEvents( MouseEvent.MOUSE_EVENT_MASK|MouseEvent.MOUSE_MOTION_EVENT_MASK|MouseWheelEvent.MOUSE_WHEEL_EVENT_MASK );
    }
    
    
    //utility methods for implementations
    //method for translating simulation x coordinates into screen x coordinates
    protected final int translateX( double x ) {
        return (int)( ( x - posX ) * scale ) + super.getWidth() / 2;
    }
    
    //method for translating simulation y coordinates into screen y coordinates
    protected final int translateY( double y ) {
        return (int)( ( posY - y ) * scale ) + super.getHeight() / 2;
    }
    
    //method for translating screen x coordinates into simulation x coordinates
    protected final double translateX( int x ) {
        return posX + ( x - super.getWidth() / 2 ) / scale;
    }
    
    //method for translating screen y coordinates into simulation y coordinates
    protected final double translateY( int y ) {
        return posY - ( y - super.getHeight() / 2 ) / scale;
    }
    
    //overridden methods
    //paints spaceobject objects
    @Override
    public void paint( Graphics g ) {
        synchronized( particles ) {
            //paints spaceobjects
            particles.forEach( obj -> {
                if( obj != null ) {
                    g.setColor( obj.color() );
                    int s = Math.max( (int)( obj.radius * 2 * scale ), 2 );
                    g.fillOval( translateX( obj.xPos - obj.radius ), translateY( obj.yPos + obj.radius ), s, s );
                }
            } );
        }
    }
    
    //sets position of a list of objects to the pov of the camera
    @Override
    protected void setPosToCamera( List<Type> objects ) {
        if( !objects.isEmpty() ) {
            var first = objects.get( 0 );
            double xDiff = first.xPos - posX, yDiff = first.yPos - posY;
            objects.forEach( object -> {
                object.xPos -= xDiff;
                object.yPos -= yDiff;
            } );
        }
    }
    
    //events processing for mouse wheel zoom in and outs
    @Override
    protected final void processMouseWheelEvent( MouseWheelEvent e ) {
        double mult = Math.pow( 1.1, -e.getPreciseWheelRotation() * e.getScrollAmount() );
        double x = ( e.getX() - super.getWidth() / 2 ) / scale;
        double y = ( super.getHeight() / 2 - e.getY() ) / scale;
        posX += -x + x * mult;
        posY += -y + y * mult;
        scale *= mult;
        super.repaint();
        super.processMouseWheelEvent( e );
    }
    
    //adds gui specific to the gravity2d simulation
    @Override
    protected void generateGUI( EditPane editPane, MainFrame application ) {
        super.generateGUI( editPane, application );
        editPane.addMenuItem( EditPane.VIEW_TYPE, "Zoom In", KeyEvent.VK_EQUALS, false, a -> {
            scale *= 1.25;
            super.repaint();
        } );
        editPane.addMenuItem( EditPane.VIEW_TYPE, "Zoom Out", KeyEvent.VK_MINUS, false, a -> {
            scale *= 0.8;
            super.repaint();
        } );
        editPane.addMenuItem( EditPane.VIEW_TYPE, "Default Zoom", KeyEvent.VK_0, false, a -> {
            scale = 1;
            super.repaint();
        } );
    }
    
    //utlities for implementations
    protected final void simulateStable( double ratioThresh, boolean removeContacts ) {
        int size = particles.size();
        double localTime = timeStep;
        while( localTime > 0 ) {
            double maxStepSq = localTime * localTime;
            for( int i = 0; i < size; i++ ) {
                Object2D p1 = particles.get( i );
                for( int j = i + 1; j < size; j++ ) {
                    Object2D p2 = particles.get( j );
                    double distSq = p1.interact( p2 );
                    if( removeContacts && distSq < 0 ) {
                        synchronized( particles ) {
                            particles.remove( j-- );
                        }
                        size--;
                    } else {
                        double xvDiff = p2.xVeloc - p1.xVeloc;
                        double yvDiff = p2.yVeloc - p1.yVeloc;
                        double stepSq = ratioThresh * distSq / ( xvDiff * xvDiff + yvDiff * yvDiff );
                        if( maxStepSq > stepSq ) {
                            maxStepSq = stepSq;
                        }
                    }
                }
            }
            double maxStep = Math.sqrt( maxStepSq );
            for( int i = 0; i < size; i++ ) {
                Object2D p = particles.get( i );
                p.update( maxStep );
            }
            localTime -= maxStep;
        }
    }
}

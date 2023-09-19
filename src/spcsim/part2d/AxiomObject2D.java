package spcsim.part2d;

/* Author: Kent Fukuda
 * Description: AxiomObjects entities that are simulated by the environment
 * Created: 9-16-23
 * Status: entity class, finished
 * Dependencies: Object2D
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import spcsim.base.SimObject;
import spcsim.impl.Object2D;

public class AxiomObject2D extends Object2D<AxiomObject2D> {
    
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "AXIOZD" );
    double invSpring, drag;
    
    //default constructor
    public AxiomObject2D() {
        this( 0xFF7F7F7F, 1, 1, 1, 1, 0, 0, 0, 0 );
    }
    
    //constructor for inputting full parameters
    public AxiomObject2D( int color, double mass, double radius, double spring, double drag, double xPos, double yPos, double xVeloc, double yVeloc ) {
        super( color, mass, radius, xPos, yPos, xVeloc, yVeloc );
        this.invSpring = 1 / spring;
        this.drag = drag;
    }
    
    //overridden methods from simulation object
    @Override
    public void write( DataOutput out ) throws IOException {
        out.writeDouble( invSpring );
        out.writeDouble( drag );
        super.write( out );
    }
    
    @Override
    public void read( DataInput in ) throws IOException {
        invSpring = in.readDouble();
        drag = in.readDouble();
        super.read( in );
    }
    
    //calculates interaction between two particles and returns the distance squared
    @Override
    public double interact( AxiomObject2D p ) {
        double xDiff = xPos - p.xPos;
        double yDiff = yPos - p.yPos;
        double distSq = xDiff * xDiff + yDiff * yDiff;
        double dist = Math.sqrt( distSq );
        double totRad = radius + p.radius;
        double force;
        //test for contact between particles
        if( dist <= totRad ) {
            double vxDiff = p.xVeloc - xVeloc + xDiff;
            double vyDiff = p.yVeloc - yVeloc + yDiff;
        	//linear restoring spring force, drag, decreasing gravity
            force = ( ( totRad / dist - 1 ) / ( invSpring + p.invSpring ) + 
                    drag * p.drag * ( Math.sqrt( vxDiff * vxDiff + vyDiff * vyDiff ) - dist ) ) 
                    / ( mass * p.mass ) - 1 / ( totRad * totRad * totRad );
        } else {
            //normal gravity calculations
            force = -1 / ( distSq * dist );
        }
        double forceX = force * xDiff;
        double forceY = force * yDiff;
        xAccel += forceX * p.mass;
        yAccel += forceY * p.mass;
        p.xAccel -= forceX * mass;
        p.yAccel -= forceY * mass;
        return dist;
    }
}
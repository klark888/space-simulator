package spcsim.grav2d;

/* Author: Kent Fukuda
 * Description: SpaceObjects entities that are simulated by the environment
 * Created: 7-13-21
 * Status: entity class, finished
 * Dependencies: Object2D, Units
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import spcsim.base.SimObject;
import spcsim.base.Units;
import spcsim.impl.Object2D;

public final class SpaceObject2D extends Object2D<SpaceObject2D> {
    
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "SPCOZD" );
    //gravity constant
    private static final double OPT_GRAVITY_PULL = Units.GRAVITY_CONSTANT * Units.EARTH_MASS / 
            Units.EARTH_RADIUS / Units.EARTH_RADIUS / Units.EARTH_RADIUS * Units.DAY_LENGTH * Units.DAY_LENGTH;
    
    //fields for a spaceobject
    String name;
    
    //default constructor
    public SpaceObject2D() {
        this( null, 0xFF7F7F7F, 1, 1, 0, 0, 0, 0 );
    }
    
    //constructor for inputting full parameters
    public SpaceObject2D( String name, int color, double mass, double radius, double xPos, double yPos, double xVeloc, double yVeloc ) {
        super( color, mass, radius, xPos, yPos, xVeloc, yVeloc );
        this.name = name;
    }
    
    
    //overridden methods from simulation object
    @Override
    public String name() {
        return name == null ? "Unnamed" : name;
    }
    
    @Override
    public void write( DataOutput out ) throws IOException {
        out.writeUTF( name == null ? "null" : name );
        super.write( out );
    }
    
    @Override
    public void read( DataInput in ) throws IOException {
        name = in.readUTF();
        super.read( in );
    }
    
    //calculates interaction between two particles and returns the distance squared
    @Override
    public double interact( SpaceObject2D p ) {
        double xDiff = xPos - p.xPos;
        double yDiff = yPos - p.yPos;
        double distSq = xDiff * xDiff + yDiff * yDiff;
        double dist = Math.sqrt( distSq );
        //test for contact between particles
        if( dist <= radius + p.radius ) {
            //collision simulation
            double newMass = mass + p.mass;
            double oldXMom = xVeloc * mass;
            double oldYMom = yVeloc * mass;
            double newXMom = p.xVeloc * p.mass + oldXMom;
            double newYMom = p.yVeloc * p.mass + oldYMom;
            if( p.mass > mass ) {
                name = p.name;
                super.color( p.color() );
            }
            xVeloc = newXMom / newMass;
            yVeloc = newYMom / newMass;
            xPos = ( xPos * mass + p.xPos * p.mass ) / newMass;
            yPos = ( yPos * mass + p.yPos * p.mass ) / newMass;
            radius = Math.pow( radius * radius * radius + p.radius * p.radius * p.radius, 0.33333333333333333333333 );
            mass = newMass;
            return -1;
        } else {
            //normal gravity calculations
            double force = OPT_GRAVITY_PULL / ( distSq * dist );
            double forceX = force * xDiff;
            double forceY = force * yDiff;
            xAccel += forceX * p.mass;
            yAccel += forceY * p.mass;
            p.xAccel -= forceX * mass;
            p.yAccel -= forceY * mass;
        }
        return distSq;
    }
    
    //makes obj1 and obj2 orbit each other with a certain eccentricity
    public static void orbit( SpaceObject2D obj1, SpaceObject2D obj2, double eccentricity ) {
        if( 0 <= eccentricity && eccentricity < 1 ) {
            eccentricity = Math.sqrt( 1 - eccentricity );
            double totalMass = obj1.mass + obj2.mass;
            double xDiff = obj1.xPos - obj2.xPos;
            double yDiff = obj1.yPos - obj2.yPos;
            double xVeloc = obj1.xVeloc;
            double yVeloc = obj1.yVeloc;
            double distance = Math.sqrt( xDiff * xDiff + yDiff * yDiff) ;
            double moment = Math.sqrt( totalMass / distance * -OPT_GRAVITY_PULL ) / totalMass * eccentricity;
            double veloc = moment * obj2.mass;
            xDiff /= distance;
            yDiff /= distance;
            obj1.xVeloc = xVeloc - yDiff * veloc;
            obj1.yVeloc = yVeloc + xDiff * veloc;
            veloc = moment * obj1.mass;
            obj2.xVeloc = xVeloc + yDiff * veloc;
            obj2.yVeloc = yVeloc - xDiff * veloc;
        } else {
            throw new IllegalArgumentException("Eccentricity must be between 0 and 1.0");
        }
    }
}
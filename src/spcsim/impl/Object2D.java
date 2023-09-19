package spcsim.impl;

/* Author: Kent Fukuda
 * Description: Particle entity used for 2D particle environment simulations
 * Created: 9-16-23
 * Status: entity class, finished
 * Dependencies: SimObject
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Color;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import spcsim.base.SimObject;

public abstract class Object2D<Type extends Object2D> implements SimObject, Cloneable {
    
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), null );
    //fields for a spaceobject
    private int color;
    private transient Color cacheColor;
    public double mass, radius, xPos, yPos, xVeloc, yVeloc;
    protected transient double xAccel, yAccel;
    
    //default constructor
    public Object2D() {
        this( 0xFF7F7F7F, 1, 1, 0, 0, 0, 0 );
    }
    
    //constructor for inputting full parameters
    public Object2D( int color, double mass, double radius, double xPos, double yPos, double xVeloc, double yVeloc ) {
        this.color = color;
        cacheColor = new Color( color, true );
        this.mass = mass;
        this.radius = radius;
        this.xPos = xPos;
        this.yPos = yPos;
        this.xVeloc = xVeloc;
        this.yVeloc = yVeloc;
    }
    
    //method to interact with another object
    public abstract double interact( Type p );
    
    //overridden methods fro simulation object
    @Override
    public synchronized void parseString( String stringForm ) throws IllegalStateException {
        SimObject.super.parseString( stringForm );
        cacheColor = new Color( color, true );
        xAccel = yAccel = 0;
    }
    
    @Override
    public void write( DataOutput out ) throws IOException {
        out.writeInt( color );
        out.writeDouble( mass );
        out.writeDouble( radius );
        out.writeDouble( xPos );
        out.writeDouble( yPos );
        out.writeDouble( xVeloc );
        out.writeDouble( yVeloc );
    }
    
    @Override
    public void read( DataInput in ) throws IOException {
        color = in.readInt();
        cacheColor = new Color( color, true );
        mass = in.readDouble();
        radius = in.readDouble();
        xPos = in.readDouble();
        yPos = in.readDouble();
        xVeloc = in.readDouble();
        yVeloc = in.readDouble();
    }
    
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch( CloneNotSupportedException e ) {
            return null;
        }
    }
    
    //accessors for color
    public final Color color() {
        return cacheColor;
    }
    
    public final void color( Color c ) {
        cacheColor = c;
        color = c.getRGB();
    }
    
    //simulations the movement of the particle without resetting delta t variables
    public final void update( double timeStep ) {
        xPos += ( xVeloc += xAccel * timeStep ) * timeStep;
        yPos += ( yVeloc += yAccel * timeStep ) * timeStep;
        xAccel = yAccel = 0;
    }
}

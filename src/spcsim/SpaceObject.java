package spcsim;

/* Author: Kent Fukuda
 * Description: SpaceObjects entities that are simulated by the environment
 * Created: 7-13-21
 * Status: entity class, wip
 * Dependencies: none
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Color;
import java.awt.Graphics;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


public final class SpaceObject implements Externalizable, Cloneable {

	private static final long serialVersionUID = 0xB16B00BAE66DADD7L;//nice uid
	
	//measurement constants (1 unit in the simulation is a earth mass, earth radius, or 1 day)
	public static final String SHORTHAND = "spcobj";
	public static final double EARTH_MASS = 5.9742e24;//kilograms
	public static final double EARTH_RADIUS = 6.371e6;//meters
	public static final double DAY_LENGTH = 86400;//seconds
	public static final double RED_WAVELENGTH = 690e-9;//micrometers
	public static final double GREEN_WAVELENGTH = 530e-9;//micrometers
	public static final double BLUE_WAVELENGTH = 470e-9;//micrometers
	
	//cosmological constants used in the simulation
	public static final double PLANCK_CONSTANT = 6.62607015e-34;// J/Hz=kg*m^2/s // 1 / EARTH_MASS / EARTH_RADIUS / EARTH_RADIUS * DAY_LENGTH
	public static final double LIGHT_SPEED = 299792458;// m/s // DAY_LENGTH / EARTH_RADIUS
	public static final double BOLTZMANN_CONSTANT = 1.380649e-23;// J/K=kg*(m/s)^2/K // 1 / EARTH_MASS / EARTH_RADIUS * DAY_LENGTH / EARTH_RADIUS * DAY_LENGTH 
	public static final double GRAVITY_CONSTANT = 6.67430e-11;// m^3/kg/s^2 // EARTH_MASS * DAY_LENGTH * DAY_LENGTH / EARTH_RADIUS / EARTH_RADIUS / EARTH_RADIUS
	public static final double STEFAN_BOLTZMANN = 2 * Math.pow( Math.PI, 5 ) * Math.pow( BOLTZMANN_CONSTANT, 4 ) / ( 15 * Math.pow( PLANCK_CONSTANT, 3 ) * Math.pow( LIGHT_SPEED, 2 ) );
	public static final double JOULE_KCAL = 0.000239;// kcal/J=K*(m/s)^2 // DAY_LENGTH / EARTH_RADIUS
	
	//optimized and private constants
	private static final double OPT_BLACK_BODY_1 = 2300 * 2 * Math.PI * PLANCK_CONSTANT * LIGHT_SPEED;
	private static final double OPT_BLACK_BODY_2 = PLANCK_CONSTANT * LIGHT_SPEED / BOLTZMANN_CONSTANT;
	private static final double OPT_TOTAL_RADIATION = STEFAN_BOLTZMANN * JOULE_KCAL / EARTH_MASS * EARTH_RADIUS * EARTH_RADIUS * DAY_LENGTH;
	private static final double OPT_GRAVITY_PULL = GRAVITY_CONSTANT * EARTH_MASS / EARTH_RADIUS / EARTH_RADIUS / EARTH_RADIUS * DAY_LENGTH * DAY_LENGTH;
	private static final double OPT_KE_TEMP = EARTH_RADIUS * EARTH_RADIUS * JOULE_KCAL / 1000 / DAY_LENGTH / DAY_LENGTH;
	
	
	//fields for a spaceobject
	private String name;
	private double mass;//EARTHI_MASS
	private double radius;//EARTH_RADIUS
	private double temperature;//K
	private double currentStep;//DAY_LENGTH
	private double xPosition;//EARTH_RADIUS
	private double yPosition;//EARTH_RADIUS
	private double xVelocity;//EARTH_RADIUS/DAY_LENGTH/timeStep
	private double yVelocity;//EARTH_RADIUS/DAY_LENGTH/timeStep
	

	//constructors
	public SpaceObject() {
		this( null, 0, 0, 0, 0, 0, 0, 0 );
	}
	
	public SpaceObject( String stringData ) {
		try {
			stringData = stringData.substring( stringData.indexOf( '[' ) + 1, stringData.lastIndexOf( ']' ) );
			String[] fields = stringData.split( "," );
			for( String field : fields ) {
				int equals = field.indexOf( '=' );
				String val = field.substring( equals + 1 );
				switch( field.substring( 0, equals ) ) {
					case "name" :
						name = val.replace( "\\c", "," ).replace( "\\n", "\n" ).replace( "\\r", "\r" ).replace( "\\\\", "\\" ); break;
					case "mass" :
						mass = Double.parseDouble( val ); break;
					case "radius" :
						radius = Double.parseDouble( val ); break;
					case "temperature" :
						temperature = Double.parseDouble( val ); break;
					case "xPosition" :
						xPosition = Double.parseDouble( val ); break;
					case "yPosition" :
						yPosition = Double.parseDouble( val ); break;
					case "xVelocity" :
						xVelocity = Double.parseDouble( val ); break;
					case "yVelocity" :
						yVelocity = Double.parseDouble( val ); break;
					default :
						throw new IllegalArgumentException( "Unrecognizable string" );
				}
			}
		} catch( IndexOutOfBoundsException|NumberFormatException e ) {
			throw new IllegalArgumentException( "Unrecognizable string", e );
		}
		currentStep = 1;
	}
	
	public SpaceObject( double mass, double radius, double temperature ) {
		this( null, mass, radius, temperature, 0, 0, 0, 0 );
	}
	
	public SpaceObject( String name, double mass, double radius, double temperature ) {
		this( name, mass, radius, temperature, 0, 0, 0, 0 );
	}
	
	public SpaceObject( String name, double mass, double radius, double temperature, double xPosition, double yPosition, double xVelocity, double yVelocity ) {
		this.name = name;
		this.mass = mass;
		this.radius = radius;
		this.temperature = temperature;
		this.currentStep = 1;
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.xVelocity = xVelocity;
		this.yVelocity = yVelocity;
	}
	
	
	//method called when rendering
	void render( Graphics graphics, EnvInfo env ) {
		//gets black body radiation (assumes complete black body)
		double red = blackBodyRadiation( RED_WAVELENGTH ); 
		double green = blackBodyRadiation( GREEN_WAVELENGTH );
		double blue = blackBodyRadiation( BLUE_WAVELENGTH );
		double max = Math.max( Math.max( 255, red ), Math.max( green, blue ) );
		graphics.setColor( new Color( (int)( red * 255 / max ), (int)( green * 255 / max ), (int)( blue * 255 / max ) ) );
		//translates simulation object into graphics
		int s = Math.max( (int)( radius * 2 * env.zoom ), 2 );
		graphics.fillOval( env.translateX( xPosition - radius ),
				env.translateY( yPosition + radius ), s, s );
	}
	
	//method called when simulating
	synchronized void simulate( List<SpaceObject> spaceObjects, EnvInfo env ) {
		updateTimeStep( env.timeStep );
		//simulate interactions with other bodies
		for( int k = env.numID + 1; k < spaceObjects.size(); k++ ) {
			//initialize variables
			SpaceObject pull = spaceObjects.get( k );
			synchronized( pull ) {
				pull.updateTimeStep( currentStep );
				double xDiff = xPosition - pull.xPosition;
				double yDiff = yPosition - pull.yPosition;
				double dist = Math.sqrt( xDiff * xDiff + yDiff * yDiff );
				boolean collides = true;
				
				//collision area detection
				if( dist > radius + pull.radius ) {
					//gravitation attraction simulation
					double force = OPT_GRAVITY_PULL * currentStep * currentStep / dist / dist / dist;
					double accel = force * mass;
					pull.xVelocity += accel * xDiff;
					pull.yVelocity += accel * yDiff;
					accel = force * pull.mass;
					xVelocity -= accel * xDiff;
					yVelocity -= accel * yDiff;
					collides = false;
					
					//crossing path collision detection
					if( Math.abs( xDiff ) < Math.abs( xVelocity - pull.xVelocity ) &&
							Math.abs( yDiff ) < Math.abs( yVelocity - pull.yVelocity ) ) {
							double ycx = yVelocity * pull.xVelocity;
							double cyx = pull.yVelocity * xVelocity;
							double ycxcyx = ycx - cyx;
							double x = ( ycx * xPosition - cyx * pull.xPosition - pull.xVelocity * xVelocity * yDiff ) / ycxcyx;
							double y = ( ycx * pull.yPosition - cyx * yPosition - yVelocity * pull.yVelocity * -xDiff ) / ycxcyx;
							collides = x - xPosition < xVelocity && y - yPosition < yVelocity &&
									x - pull.xPosition < pull.xVelocity && y - pull.yPosition < pull.yVelocity;
					}
				}
				
				//collision simulation
				if( collides ) {
					double newMass = mass + pull.mass;
					double oldXMom = xVelocity * mass;
					double oldYMom = yVelocity * mass;
					double newXMom = ( pull.xVelocity * pull.mass + oldXMom );
					double newYMom = ( pull.yVelocity * pull.mass + oldYMom );
					double oldKE = oldXMom * xVelocity + oldYMom * yVelocity;
					
					name = pull.mass > mass ? pull.name : name;
					xVelocity = newXMom / newMass;
					yVelocity = newYMom / newMass;
					xPosition = ( xPosition * mass + pull.xPosition * pull.mass ) / newMass;
					yPosition = ( yPosition * mass + pull.yPosition * pull.mass ) / newMass;
					temperature = ( ( temperature * mass + pull.temperature * pull.mass ) + 
							OPT_KE_TEMP / currentStep / currentStep * Math.abs( oldKE - newXMom * xVelocity - newYMom * yVelocity ) ) / newMass;
					radius = Math.sqrt( radius * radius + pull.radius * pull.radius );
					mass = newMass;
					spaceObjects.remove( k-- );
				}
			}
		}
		
		//none-interactive simulation
		xPosition += xVelocity;
		yPosition += yVelocity;
		temperature -= OPT_TOTAL_RADIATION * temperature * temperature * temperature * temperature / mass * currentStep * radius * radius;
		temperature = temperature < 0 ? 0 : temperature;
	}
	
	
	//mutator methods
	public synchronized void setName( String name ) {
		this.name = name;
	}
	
	public synchronized void setMass( double mass ) {
		this.mass = mass;
	}
	
	public synchronized void setRadius( double radius ) {
		this.radius = radius;
	}
	
	public synchronized void setTemperature( double temperature ) {
		this.temperature = temperature;
	}
	
	public synchronized void setXPosition( double xPosition ) {
		this.xPosition = xPosition;
	}
	
	public synchronized void setYPosition( double yPosition ) {
		this.yPosition = yPosition;
	}
	
	public synchronized void setXVelocity( double xVelocity ) {
		this.xVelocity = xVelocity * currentStep;
	}
	
	public synchronized void setYVelocity( double yVelocity ) {
		this.yVelocity = yVelocity * currentStep;
	}
	
	
	//accessor methods
	public String getName() {
		return name;
	}
	
	public double getMass() {
		return mass;
	}
	
	public double getRadius() {
		return radius;
	}
	
	public double getTemperature() {
		return temperature;
	}
	
	public double getXPosition() {
		return xPosition;
	}
	
	public double getYPosition() {
		return yPosition;
	}
	
	public double getXVelocity() {
		return xVelocity / currentStep;
	}
	
	public double getYVelocity() {
		return yVelocity / currentStep;
	}
	
	
	//overridden methods
	@Override
	public void writeExternal( ObjectOutput out ) throws IOException {
		out.writeUTF( name == null ? "" : name );
		out.writeDouble( mass );
		out.writeDouble( radius );
		out.writeDouble( temperature );
		out.writeDouble( xPosition );
		out.writeDouble( yPosition );
		out.writeDouble( xVelocity / currentStep );
		out.writeDouble( yVelocity / currentStep );
	}

	@Override
	public synchronized void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
		String utf = in.readUTF();
		name = utf.equals( "" ) ? null : utf;
		mass = in.readDouble();
		radius = in.readDouble();
		temperature = in.readDouble();
		currentStep = 1;
		xPosition = in.readDouble();
		yPosition = in.readDouble();
		xVelocity = in.readDouble();
		yVelocity = in.readDouble();
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch( CloneNotSupportedException e ) {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return super.toString() + "[name=" + ( name == null ? "null" : name.replace( "\\", "\\\\" ).replace( ",", "\\c" ).replace( "\n", "\\n" ).replace( "\r", "\\r" ) )
				+ ",mass=" + mass + ",radius=" + radius + ",temperature=" + temperature
				+ ",xPosition=" + xPosition + ",yPosition=" + yPosition + ",xVelocity=" + xVelocity / currentStep + ",yVelocity=" + yVelocity / currentStep + "]";
	}
	
	
	//calculates black body radiation
	private double blackBodyRadiation( double wavelength ) {
		double radiance = OPT_BLACK_BODY_1 / ( Math.pow( wavelength, 5 ) ) 
				/ ( Math.pow( Math.E, OPT_BLACK_BODY_2 / wavelength / temperature ) - 1 );
		return radiance < 0 ? 0 : radiance;
	}
	
	//updates the timestep the object operates at
	private void updateTimeStep( double newStep ) {
		if( currentStep != newStep ) {
			double changeFactor = newStep / currentStep;
			xVelocity *= changeFactor;
			yVelocity *= changeFactor;
			currentStep = newStep;
		}
	}
	
	
	//static utility methods
	//makes obj1 and obj2 orbit each other in a circular orbit
	public static void orbit( SpaceObject obj1, SpaceObject obj2 ) {
		orbit( obj1, obj2, 0 );
	}
	
	//makes obj1 and obj2 orbit each other with a certain eccentricity
	public static void orbit( SpaceObject obj1, SpaceObject obj2, double eccentricity ) {
		if( 0 <= eccentricity && eccentricity < 1 ) {
			obj1.updateTimeStep( 1 );
			obj2.updateTimeStep( 1 );
			eccentricity = Math.sqrt( 1 - eccentricity );
			double totalMass = obj1.mass + obj2.mass;
			double xDiff = obj1.xPosition - obj2.xPosition;
			double yDiff = obj1.yPosition - obj2.yPosition;
			obj2.updateTimeStep( obj1.currentStep );
			double xVeloc = obj1.xVelocity;
			double yVeloc = obj1.yVelocity;
			double distance = Math.sqrt( xDiff * xDiff + yDiff * yDiff );
			double moment = Math.sqrt( totalMass / distance * OPT_GRAVITY_PULL ) / totalMass * eccentricity;
			double veloc = moment * obj2.mass;
			xDiff /= distance;
			yDiff /= distance;
			obj1.xVelocity = xVeloc - yDiff * veloc;
			obj1.yVelocity = yVeloc + xDiff * veloc;
			veloc = moment * obj1.mass;
			obj2.xVelocity = xVeloc + yDiff * veloc;
			obj2.yVelocity = yVeloc - xDiff * veloc;
		} else {
			throw new IllegalArgumentException( "Eccentricity must be between 0 and 1.0" );
		}
	}
	
	/*//writes an array of spaceobjects to a file
	public static void writeFile( String fileName, SpaceObject[] objList ) throws IOException {
		writeStream( new FileOutputStream( fileName ), objList );
	}
	
	//reads an array of spaceobjects from a file
	public static SpaceObject[] readFile( String fileName ) throws IOException, ClassNotFoundException {
		return readStream( new FileInputStream( fileName ) );
	}*/
	
	//writes an array of spaceobjects to an outputstream
	public static void writeStream( OutputStream ostream, SpaceObject[] objList ) throws IOException {
		DeflaterOutputStream dStream = new DeflaterOutputStream( ostream );
		ObjectOutputStream stream = new ObjectOutputStream( dStream );
		stream.writeObject( objList );
		stream.close();
		dStream.close();
		ostream.close();
	}
	
	//reads an array of spaceobjects from an inputstream
	public static SpaceObject[] readStream( InputStream istream ) throws IOException, ClassNotFoundException {
		InflaterInputStream dStream = new InflaterInputStream( istream );
		ObjectInputStream stream = new ObjectInputStream( dStream );
		SpaceObject[] objList = (SpaceObject[])stream.readObject();
		stream.close();
		dStream.close();
		istream.close();
		return objList;
	}
	
	
	//at some point in future more data might be added to a spaceobject, which will be added to this class
	/*public final class ExternData {
		private String name;
		private Color trailColor;
		private double[] xValues;
		private double[] yValues;
		private int callCoolDown;
		private int firstIndex;
		private int callsIgnored;
		
		public ExternData( String n, Color c, double initX, double initY, int len, int coolDown ) {
			name = n;
			trailColor = c;
			
			xValues = new double[len];
			yValues = new double[len];
			callCoolDown = coolDown;
			firstIndex = 0;
			callsIgnored = coolDown;
			Arrays.fill( xValues, initX );
			Arrays.fill( xValues, initY );
			
			data = this;
		}
		
		public synchronized void simulate() {
			if( callsIgnored >= callCoolDown ) {
				xValues[firstIndex] = xPosition;
				yValues[firstIndex] = yPosition;
				firstIndex = ( firstIndex + 1 ) % xValues.length;
				callsIgnored = 0;
			} else {
				callsIgnored++;
			}
		}
		
		public synchronized void render( Graphics g, SpaceObject.EnvInfo env, int len, boolean drawName ) {
			g.setColor( trailColor );
			int prevX = env.translateX( xValues[firstIndex] );
			int prevY = env.translateY( yValues[firstIndex] );
			len = len < 0 ? xValues.length : Math.min( xValues.length, len );
			if( drawName ) {
				g.drawString( name, prevX, prevY );
			}
			for( int i = 1; i < len; i++ ) {
				int x = env.translateX( xValues[ ( i + firstIndex ) % xValues.length ] );
				int y = env.translateY( yValues[ ( i + firstIndex ) % xValues.length ] );
				g.drawLine( prevX, prevY, x, y );
				prevX = x;
				prevY = y;
			}
		}
	}*/
	
	
	//the environment info the spaceobject being simulated is in
	public static final class EnvInfo {
		//environment variables
		public double posX;
		public double posY;
		public double zoom;
		public double timeStep;
		public int centerX;
		public int centerY;
		public int numID;
		
		//method for translating simulation coordinates into screen coordinates
		public int translateX( double x ) {
			return (int)( ( x - posX ) * zoom ) + centerX;
		}

		public int translateY( double y ) {
			return (int)( ( posY - y ) * zoom ) + centerY;
		}
	}
}
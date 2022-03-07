package spcsim;

/* Author: Kent Fukuda
 * Description: A library used to convert between different units
 * Created: 8-25-31
 * Status: library class, finished
 * Dependencies: none
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;


public class Units {
	
	//function for default and unknown converters
	public static final BiFunction<Double,Boolean,Double> DEFAULT;
	public static final BiFunction<Double,Boolean,Double> UNKNOWN;
	//static unit converters for time, length, mass, temperature, speed, and degree units
	public static final Map<String,BiFunction<Double,Boolean,Double>> TIME;
	public static final Map<String,BiFunction<Double,Boolean,Double>> LENGTH;
	public static final Map<String,BiFunction<Double,Boolean,Double>> MASS;
	public static final Map<String,BiFunction<Double,Boolean,Double>> TEMPERATURE;
	public static final Map<String,BiFunction<Double,Boolean,Double>> SPEED;
	public static final Map<String,BiFunction<Double,Boolean,Double>> DEGREE;
	
	
	//private constructor
	private Units() { }
	
	//static initializes
	static {
		DEFAULT = ( val, to ) -> val;
		UNKNOWN = ( val, to ) -> Double.NaN;
		Map<String,BiFunction<Double,Boolean,Double>> timeConverter = new HashMap<>();
		Map<String,BiFunction<Double,Boolean,Double>> lengthConverter = new HashMap<>();
		Map<String,BiFunction<Double,Boolean,Double>> massConverter = new HashMap<>();
		Map<String,BiFunction<Double,Boolean,Double>> tempConverter = new HashMap<>();
		Map<String,BiFunction<Double,Boolean,Double>> speedConverter = new HashMap<>();
		Map<String,BiFunction<Double,Boolean,Double>> degreeConverter = new HashMap<>();
		
		double hour = 60;
		double day = 24;
		double year = 1 / 365.2425;
		double kilometer = SpaceObject.EARTH_RADIUS / 1000;
		double miles = SpaceObject.EARTH_RADIUS / 1607;
		double au = SpaceObject.EARTH_RADIUS / 149597870700.0;
		double lightday = SpaceObject.EARTH_RADIUS / 2.592e19;
		double lightyear = SpaceObject.EARTH_RADIUS / 9.46e21;
		double degree = 180 / Math.PI;
		
		timeConverter.put( "milliseconds", makeConvert( SpaceObject.DAY_LENGTH * 1000 ) );
		timeConverter.put( "seconds", makeConvert( SpaceObject.DAY_LENGTH ) );
		timeConverter.put( "minutes", makeConvert( day * hour ) );
		timeConverter.put( "hours", makeConvert( day ) );
		timeConverter.put( "days", DEFAULT );
		timeConverter.put( "weeks", makeConvert( 1.0 / 7 ) );
		timeConverter.put( "months", makeConvert( 1 / 30.43 ) );
		timeConverter.put( "years", makeConvert( year ) );
		timeConverter.put( "decades", makeConvert( 0.1 * year ) );
		timeConverter.put( "centuries", makeConvert( 0.01 * year ) );
		
		lengthConverter.put( "meters", makeConvert( SpaceObject.EARTH_RADIUS ) );
		lengthConverter.put( "kilometers", makeConvert( kilometer ) );
		lengthConverter.put( "miles", makeConvert( miles ) );
		lengthConverter.put( "earth radii", DEFAULT );
		lengthConverter.put( "solar radii", makeConvert( 1 / 108.968 ) );
		lengthConverter.put( "au", makeConvert( au ) );
		lengthConverter.put( "light days", makeConvert( lightday ) );
		lengthConverter.put( "light years", makeConvert( lightyear ) );
		lengthConverter.put( "parsecs", makeConvert( lightyear / 3.26 ) );
		
		massConverter.put( "kilograms", makeConvert( SpaceObject.EARTH_MASS ) );
		massConverter.put( "tons", makeConvert( SpaceObject.EARTH_MASS / 1000 ) );
		massConverter.put( "earth masses", DEFAULT );
		massConverter.put( "jovian masses", makeConvert( 1.0 / 318 ) );
		massConverter.put( "solar masses", makeConvert( 1.0 / 330000 ) );
		
		tempConverter.put( "kelvin", ( val, to ) -> val );
		tempConverter.put( "celsius", ( val, to ) -> to ? val - 273.15 : val + 273.15 );
		tempConverter.put( "fahrenheit", ( val, to ) -> to ? val * 1.8 - 459.67 : val / 1.8 + 255.37 );
		
		speedConverter.put( "km/year", makeConvert( kilometer / year ) );
		speedConverter.put( "km/h", makeConvert( kilometer / hour ) );
		speedConverter.put( "mph", makeConvert( miles / hour ) );
		speedConverter.put( "m/s", makeConvert( SpaceObject.EARTH_RADIUS / SpaceObject.DAY_LENGTH ) );
		speedConverter.put( "km/s", makeConvert( kilometer / SpaceObject.DAY_LENGTH ) );
		speedConverter.put( "earth radii/year", makeConvert( 1 / year ) );
		speedConverter.put( "earth radii/day", DEFAULT );
		speedConverter.put( "au/h", makeConvert( au / hour ) );
		speedConverter.put( "au/day", makeConvert( au / day ) );
		speedConverter.put( "au/year", makeConvert( au / year ) );
		speedConverter.put( "lightspeed", makeConvert( lightday ) );

		degreeConverter.put( "seconds", makeConvert( degree * 60 * 60 ) );
		degreeConverter.put( "minutes", makeConvert( degree * 60 ) );
		degreeConverter.put( "degrees", makeConvert( degree ) );
		degreeConverter.put( "radians", DEFAULT );
		
		TIME = Collections.unmodifiableMap( timeConverter );
		LENGTH = Collections.unmodifiableMap( lengthConverter );
		MASS = Collections.unmodifiableMap( massConverter );
		TEMPERATURE = Collections.unmodifiableMap( tempConverter );
		SPEED = Collections.unmodifiableMap( speedConverter );
		DEGREE = Collections.unmodifiableMap( degreeConverter );
	}
	
	
	//parses a string to a double using a converter
	public static double parseDouble( Map<String,BiFunction<Double,Boolean,Double>> converter, String string, String unit ) {
		try {
			return converter.get( unit ).apply( Double.parseDouble( string ), false );
		} catch( IndexOutOfBoundsException|NullPointerException e ) {
			throw new NumberFormatException();
		}
	}
	
	//makes a string with a unit from a value using a converter
	public static String toString( Map<String,BiFunction<Double,Boolean,Double>> converter, double value, String unit ) {
		return Double.toString( converter.getOrDefault( unit.toLowerCase(), UNKNOWN ).apply( value, true ) );
	}
	
	//makes a string with a unit from a value using a converter, rounded
	public static String toStringRound( Map<String,BiFunction<Double,Boolean,Double>> converter, double value, String unit ) {
		return converter.getOrDefault( unit.toLowerCase(), UNKNOWN ).apply( value, true ).longValue() + " " + unit;
	}
	
	
	//creates a ratio conversion function
	private static BiFunction<Double,Boolean,Double> makeConvert( double factor ) {
		return ( val, to ) -> to ? val * factor : val / factor;
	}
}

package spcsim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*
 * @author K. A. F.
 * 
 * Description: A library used to convert between different units
 * Created: 8-25-31
 * Status: library class, finished
 * Dependencies: none
 */

public class Units {
	
	//static unit converters for time, length, and mass units
	public static final Map<String,Double> TIME;
	public static final Map<String,Double> LENGTH;
	public static final Map<String,Double> MASS;
	
	
	//private constructor
	private Units() { }
	
	//static initializes
	static {
		Map<String,Double> timeConverter = new HashMap<>();
		Map<String,Double> lengthConverter = new HashMap<>();
		Map<String,Double> massConverter = new HashMap<>();
		
		timeConverter.put( "milliseconds", SpaceObject.DAY_LENGTH * 1000 );
		timeConverter.put( "seconds", SpaceObject.DAY_LENGTH );
		timeConverter.put( "minutes", 24.0 * 60 );
		timeConverter.put( "hours", 24.0 );
		timeConverter.put( "days", 1.0 );
		timeConverter.put( "weeks", 1.0 / 7 );
		timeConverter.put( "months", 1 / 30.43 );
		timeConverter.put( "years", 1 / 365.2425 );
		timeConverter.put( "decades", 1 / 3652.425 );
		timeConverter.put( "centuries", 1 / 365242.5 );
		
		lengthConverter.put( "meters", SpaceObject.EARTH_RADIUS );
		lengthConverter.put( "kilometers", SpaceObject.EARTH_RADIUS / 1000 );
		lengthConverter.put( "miles", SpaceObject.EARTH_RADIUS / 1607 );
		lengthConverter.put( "earth radii", 1.0 );
		lengthConverter.put( "solar radii", 1 / 108.968 );
		lengthConverter.put( "au", SpaceObject.EARTH_RADIUS / 149597870700.0 );
		lengthConverter.put( "light years", SpaceObject.EARTH_RADIUS / 9.46e21 );
		lengthConverter.put( "parsecs", SpaceObject.EARTH_RADIUS / 9.46e21 / 3.26 );
		
		massConverter.put( "kilograms", SpaceObject.EARTH_MASS );
		massConverter.put( "tons", SpaceObject.EARTH_MASS / 1000 );
		massConverter.put( "earth masses", 1.0 );
		massConverter.put( "solar masses", 1.0 / 330000 );
		
		
		TIME = Collections.unmodifiableMap( timeConverter );
		LENGTH = Collections.unmodifiableMap( lengthConverter );
		MASS = Collections.unmodifiableMap( massConverter );
	}
	
	
	//parses a string to a double using a converter
	public static double parseDouble( Map<String,Double> converter, String string ) {
		int unitIndex = string.strip().indexOf( ' ' );
		if( unitIndex == -1 ) {
			throw new NumberFormatException();
		}
		String unit = string.substring( unitIndex ).strip().toLowerCase();
		string = string.substring( 0, unitIndex );
		Double divVal = converter.get( unit );
		if( divVal == null ) {
			throw new NumberFormatException();
		}
		return Double.parseDouble( string ) / divVal;
	}
	
	//makes a string with a unit from a value using a converter
	public static String toString( Map<String,Double> converter, double val, String unit ) {
		return converter.getOrDefault( unit.toLowerCase(), Double.NaN ) * val + " " + unit;
	}
	
	//makes a string with a unit from a value using a converter, rounded
	public static String toStringR( Map<String,Double> converter, double val, String unit ) {
		return (long)( converter.getOrDefault( unit.toLowerCase(), Double.NaN ) * val ) + " " + unit;
	}
}

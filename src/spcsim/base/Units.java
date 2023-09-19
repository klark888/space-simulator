package spcsim.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.function.Consumer;

/* Author: Kent Fukuda
 * Description: A library used to convert between different units
 * Created: 8-25-31
 * Status: constants class, finished
 * Dependencies: none
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

public final class Units implements Serializable {
    
    //serialversionuid
    private static final long serialVersionUID = 4249163793471391509L;
    //si unit convert chart
    private static final String[] SI_UNIT_NAME =  { "giga", "mega", "kilo", "deci", "centi", "milli", "micro", "nano", "pico" };
    private static final char[] SI_UNIT_ABBREV =  { 'G',    'M',    'k',    'd',    'c',     'm',     'u',     'n',    'p' };
    private static final double[] SI_UNIT_VALUE = { 1e9,    1e6,    1e3,    1e-1,   1e-2,    1e-3,    1e-6,    1e-9,   1e-12 };
    //length of si units
    public static final int MAX_SI = SI_UNIT_NAME.length;
    //constants
    public static final double YEAR_LENGTH = 365.24,//days
            DAY_LENGTH = 86400,//seconds
            LIGHT_YEAR = 9.46e21,//meters
            EARTH_RADIUS = 6.371e6,//meters
            EARTH_MASS = 5.9742e24,//kilograms
            RADIANS = Math.PI / 180,//degrees
            GRAVITY_CONSTANT = -6.67430e-11;//m^3/kg/s^2
    //default units
    public static final Units TIME = new Units();
    public static final Units LENGTH = new Units();
    public static final Units MASS = new Units();
    public static final Units TEMPERATURE = new Units();
    public static final Units DEGREE = new Units();
    
    //unit conversion information
    private final HashMap<String,double[]> unitsData;
    //indicates if the units object is modifiable or not
    private boolean modifiable;
    
    //static initializaer
    static {
        //time
        TIME.addUnit( "billions of years", "gya", YEAR_LENGTH * 10e9 );
        TIME.addUnit( "millions of years", "mya", YEAR_LENGTH * 10e6 );
        TIME.addUnit( "centuries", YEAR_LENGTH * 100 );
        TIME.addUnit( "decades", YEAR_LENGTH * 10 );
        TIME.addUnit( "years", "yr", YEAR_LENGTH );
        TIME.addUnit( "months", "mo", 30.43 );
        TIME.addUnit( "weeks", 7 );
        TIME.addUnit( "days", 1 );
        TIME.addUnit( "hours", "hr", 1 / 24.0 );
        TIME.addUnit( "minutes", "m", 1 / 144.0 );
        TIME.addSIUnit( "seconds", "s", 1 / DAY_LENGTH, 6, MAX_SI );
        //length
        LENGTH.addSIUnit( "parsecs", "pc", LIGHT_YEAR * 3.26 / EARTH_RADIUS, 0, 3 );
        LENGTH.addUnit( "light years", "lya", LIGHT_YEAR / EARTH_RADIUS );
        LENGTH.addUnit( "light days", 2.592e19 / EARTH_RADIUS );
        LENGTH.addUnit( "astronomical units", "AU", 149597870700.0 / EARTH_RADIUS );
        LENGTH.addUnit( "solar radii", "OR", 108.968 );
        LENGTH.addUnit( "earth radii", "ER", 1 );
        LENGTH.addUnit( "miles", "mi", 1.607 / EARTH_RADIUS );
        LENGTH.addSIUnit( "meters", "m", 1 / EARTH_RADIUS );
        //mass
        MASS.addSIUnit( "grams", "g", 1 / EARTH_MASS );
        MASS.addSIUnit( "tons", "t", 1000 / EARTH_MASS, 0, 3 );
        MASS.addUnit( "earth masses", "EM", 1 );
        MASS.addUnit( "jovian masses", "JM", 318 );
        MASS.addUnit( "solar masses", "SM", 330000 );
        //temperature
        TEMPERATURE.addUnit( "fahrenheit", "F", 1.8, -459.67 );
        TEMPERATURE.addUnit( "kelvin", "K", 1 );
        TEMPERATURE.addUnit( "celsius", "C", 1, -273.15 );
        //degree
        DEGREE.addUnit( "seconds", "\"", RADIANS * 3600 );
        DEGREE.addUnit( "minutes", "'", RADIANS * 60 );
        DEGREE.addUnit( "degrees", "o", RADIANS );
        DEGREE.addUnit( "radians", "", 1 );
        //making defaults unmodifiable
        TIME.makeUnmodifiable();
        LENGTH.makeUnmodifiable();
        MASS.makeUnmodifiable();
        TEMPERATURE.makeUnmodifiable();
        DEGREE.makeUnmodifiable();
    }
    
    //constructor
    public Units() {
        unitsData = new HashMap<>();
        modifiable = true;
    }
    
    //returns if the units object is currently modifiable
    public boolean modifiable() {
        return modifiable;
    }
    
    //makes the units unmodifiable - this cannot be reversed
    public void makeUnmodifiable() {
        modifiable = false;
    }
    
    //adds a si unit to the units object
    public void addSIUnit( String name, String abbrev, double multiplier, int low, int high ) {
        checkModifiability();
        unitsData.put( name, new double[]{ multiplier, 0, 1 } );
        unitsData.put( abbrev, new double[]{ multiplier, 0, -1 } );
        for( int i = low; i < high; i++ ) {
            unitsData.put( SI_UNIT_NAME[i] + name, new double[]{ multiplier * SI_UNIT_VALUE[i], 0, 1 } );
            unitsData.put( SI_UNIT_ABBREV[i] + abbrev, new double[]{ multiplier * SI_UNIT_VALUE[i], 0, -1 } );
        }
    }
    
    //adds a si unit to the units object
    public void addSIUnit( String name, String abbrev, double multiplier ) {
        addSIUnit( name, abbrev, multiplier, 0, MAX_SI );
    }
    
    //adds unit to object
    public void addUnit( String name, double multiplier, double offset ) throws IllegalStateException {
        checkModifiability();
        unitsData.put( name, new double[]{ multiplier, offset, 1 } );
    }
    
    //adds unit to object
    public void addUnit( String name, double multiplier ) throws IllegalStateException {
        checkModifiability();
        unitsData.put( name, new double[]{ multiplier, 0, 1 } );
    }
    
    //adds unit to object
    public void addUnit( String name, String abbreviation, double multiplier, double offset ) throws IllegalStateException {
        checkModifiability();
        unitsData.put( name, new double[]{ multiplier, offset, 1 } );
        unitsData.put( abbreviation, new double[]{ multiplier, offset, 0 } );
    }
    
    //adds unit to object
    public void addUnit( String name, String abbrev, double multiplier ) {
        checkModifiability();
        unitsData.put( name, new double[]{ multiplier, 0, 1 } );
        unitsData.put( abbrev, new double[]{ multiplier, 0, 0 } );
    }
    
    //iterates through visible units
    public void forEach( Consumer<String> c ) throws NullPointerException {
        unitsData.forEach( ( s, d ) -> {
            if( d[2] > 0 ) {
                c.accept( s );
            }
        } );
    }
    
    //checks if argument is a valid measurement for this unit
    public boolean valid( String unit ) {
        return unitsData.containsKey( unit );
    }
    
    //converts a value of current unit to new unit
    public double convert( double value, String oldUnit, String newUnit ) {
        double[] oldU = unitsData.get( oldUnit );
        double[] newU = unitsData.get( newUnit );
        return oldU == null || newU == null ? Double.NaN : ( value + oldU[1] ) * oldU[0] / newU[0] - newU[1];
    }
    
    //checks if units is modifiable, if false throws exception
    private void checkModifiability() {
        if( !modifiable ) {
            throw new IllegalStateException( "This units object is unmodifiable" );
        }
    }
}
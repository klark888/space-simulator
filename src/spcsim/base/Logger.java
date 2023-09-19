package spcsim.base;

/* Author: Kent Fukuda
 * Description: a class to log past events in the program for debugging
 * Created: 2-19-23
 * Status: singleton class, finished
 * Dependencies: none
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public final class Logger {
    
    //records the time the logger was first loaded
    private static final long TIME_ACTIVATED;
    //stores the outputstream the loggers writes data to
    private static PrintStream OUT;
    
    //private constructor
    private Logger() {
        throw new AssertionError();
    }
    
    //static initializer
    static {
        TIME_ACTIVATED = System.currentTimeMillis();
        OUT = System.out;
        logMessage( "Logger initiated" );
    }
    
    //method for setting the output of the logger to a file
    public static boolean setOut( String fileName ) {
        try {
            OUT = new PrintStream( new FileOutputStream( new File( fileName ).getAbsolutePath() ) );
            logMessage( "Set logger output to " + OUT );
            return true;
        } catch( IOException|SecurityException e1 ) {
            logThrowable( e1 );
            return false;
        } catch( NullPointerException e2 ) {
            return false;
        }
    }
    
    //method for setting the output of the logger to a printstream
    public static void setOut( PrintStream stream ) {
        if( stream != null ) {
            OUT = stream;
            logMessage( "Set logger output to " + OUT );
        }
    }
    
    //logs a throwable exception or error with an error message
    public static void logThrowable( Throwable throwable, String message ) {
        logTime();
        logThread();
        OUT.print( throwable.toString() );
        OUT.print( " - " );
        OUT.println( message );
    }
    
    //logs a  throwable exception or error
    public static void logThrowable( Throwable throwable ) {
        logTime();
        logThread();
        OUT.println( throwable.toString() );
    }
    
    //logs a message from a particular thread
    public static void logThreadMessage( String message ) {
        logTime();
        logThread();
        OUT.println( message );
    }
    
    //logs the creation of an object
    public static void logCreation( Object object ) {
        logTime();
        OUT.print( object );
        OUT.println( " has been created" );
    }
    
    //logs an error message
    public static void logError( String error ) {
        logTime();
        OUT.print( "(ERROR!!!)" );
        OUT.println( error );
    }
    
    //logs a message
    public static void logMessage( String message ) {
        logTime();
        OUT.println( message );
    }
    
    //utility method for logging the time component
    private static void logTime() {
        int seconds = (int)( ( System.currentTimeMillis() - TIME_ACTIVATED ) / 1000 );
        int minutes = seconds / 60;
        seconds %= 60;
        OUT.print( '[' );
        if( minutes < 100 ) {
            OUT.print( '0' );
            if( minutes < 10 )
                OUT.print( '0' );
        }
        OUT.print( minutes );
        OUT.print( ':' );
        if( seconds < 10 )
            OUT.print( '0' );
        OUT.print( seconds );
        OUT.print( "] " );
    }
    
    //utility method for logging the thread component
    private static void logThread() {
        OUT.print( "<@" );
        String name = Thread.currentThread().getName();
        int len = name.length();
        if( len > 10 )
            name = name.substring( 0, 10 );
        OUT.print( name );
        for( int i = len; i < 10; i++ )
            OUT.print( '-' );
        OUT.print( "> " );
    }
}
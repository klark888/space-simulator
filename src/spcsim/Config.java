//Licensed under GNU v3.0, src/spcsim/SpaceSim.java for more details
package spcsim;

/* Author: Kent Fukuda
 * Description: Config class that reads and writes configuration variables into and from a .cfg file
 * Created: 8-26-21
 * Status: generic class, finished
 * Dependencies: none
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;


public class Config {
	
	//formatter indicates the characters that are replaces with holders in a string
	private static final String[][] FORMATTER = { { "##", "#" }, { "#d", ";" }, { "#a", "=" }, { "#o", "{" }, { "#c", "}" },
			{ "#s", " " }, { "#t", "\t" }, { "#n", "\n" }, { "#f", "\f" }, { "#r", "\r" }, 
			{ "#u0", "\u00A0" }, { "#u1", "\u2007" }, { "#u2", "\u202F" }, { "#u3", "\u000B" }, 
			{ "#u4", "\u001C" }, { "#u5", "\u001D" }, { "#u6", "\u001E" }, { "#u7", "\u001F" } };
	
	
	//default path the config reads and writes from when saveConfig() or loadConfig() is called
	private final String defaultSavePath;
	//holds the static fields that have config variables and methods to access/write them
	private final Map<Field,Method[]> monitoredFields;
	
	
	//constructor, taking in the name of the default file and directory path
	public Config( String name, String path ) {
		defaultSavePath = path + "config." + name + ".kaf.cfg";
		monitoredFields = new HashMap<>();
	}

	
	//saves config variables to default path
	public boolean saveConfig() {
		return saveConfig( defaultSavePath );
	}
	
	//loads config variables from default path
	public boolean loadConfig() {
		return loadConfig( defaultSavePath );
	}
	
	//saves config to a path. returns if saved successfully
	public boolean saveConfig( String filePath ) {
		try {
			//writes the variables in a string buffer before printing
			StringBuilder bldr = new StringBuilder();
			monitoredFields.forEach( ( monitor, methods ) -> {
				try {
					//records the identifier for the monitoring fields as [classname].[fieldname]
					Object obj = monitor.get( null );
					bldr.append( monitor.getDeclaringClass().getName() ).append( "." ).append( monitor.getName() ).append( " {\n" );
					for( int i = 0; i < methods.length; i += 2 ) {
						//invokes the accessor method to get config field's variable value
						String baseName = methods[i].getName();
						Object var = methods[i].invoke( obj );
						String value = var.toString();
						if( var.getClass().equals( String.class )||var.getClass().equals( Character.class ) ) {
							for( String[] replace : FORMATTER ) {
								value = value.replace( replace[1], replace[0] );
							}
						}
						bldr.append( "\t" ).append( baseName.substring( 3 ) ).append( " = " ).append( value ).append( ";\n" );
					}
					bldr.append( "}\n" );
				} catch( IllegalAccessException|InvocationTargetException e ) {
					throw new UnsupportedOperationException();
				}
			} );
			//writes to file
			FileOutputStream stream = new FileOutputStream( filePath );
			stream.write( bldr.toString().getBytes() );
			stream.close();
			return true;
		} catch( IOException|UnsupportedOperationException|SecurityException e ) {
			return false;
		}
	}
	
	//loads config to a path. returns if loaded successfully
	public boolean loadConfig( String filePath ) {
		try {
			//reads from file  and create string
			FileInputStream stream = new FileInputStream( filePath );
			byte[] bytes = new byte[ stream.available() ];
			stream.read( bytes );
			stream.close();
			//splits each monitored field
			for( String fieldText : new String( bytes ).replaceAll( "\\s", "" ).split( "}" ) ) {
				//identifies the monitoring field and its mutator config variable methods
				String[] monitorSplit = fieldText.split( "\\x7b" );
				Field monitoredObj = parse( monitorSplit[0] );
				String[] fields = monitorSplit[1].split( ";" );
				Object obj = monitoredObj.get( null );
				Method[] methods = monitoredFields.get( monitoredObj );
				if( methods != null ) {
					for( int j = 0; j < fields.length; j++ ) {
						//gets the value of the config variable from parsing
						String[] varNames = fields[j].split( "=" );
						for( int i = FORMATTER.length - 1; i >= 0; i-- ) {
							varNames[1] = varNames[1].replace( FORMATTER[i][0], FORMATTER[i][1] );
						}
						for( int i = 1; i < methods.length; i += 2 ) {
							if( methods[i].getName().equals( "set" + varNames[0] ) ) {
								Object arg;
								switch( methods[i - 1].getReturnType().getName() ) {
									case "java.lang.Byte" : case "byte" :
										arg = Byte.parseByte( varNames[1] );
										break;
									case "java.lang.Short" : case "short" :
										arg = Short.parseShort( varNames[1] );
										break;
									case "java.lang.Integer" : case "int" :
										arg = Integer.parseInt( varNames[1] );
										break;
									case "java.lang.Long" : case "long" :
										arg = Long.parseLong( varNames[1] );
										break;
									case "java.lang.Float" : case "float" :
										arg = Float.parseFloat( varNames[1] );
										break;
									case "java.lang.Double" : case "double" :
										arg = Double.parseDouble( varNames[1] );
										break;
									case "java.lang.Boolean" : case "boolean" :
										arg = Boolean.parseBoolean( varNames[1] );
										break;
									case "java.lang.Character" : case "char" :
										arg = varNames[1].charAt( 0 );
										break;
									case "java.lang.String" :
										arg = varNames[1];
										break;
									default :
										return false;
								}
								methods[i].invoke( obj, arg );
								break;
							}
						}
					}
				}
			}
			return true;
		} catch( IOException|ClassNotFoundException|NoSuchFieldException|IllegalAccessException|InvocationTargetException|StringIndexOutOfBoundsException e ) {
			return false;
		}
	}
	
	//add an field and the name of the config variables to monitor
	public boolean addMonitorObject( String fieldName, String... fields ) {
		try {
			Field monitor = parse( fieldName );
			Method[] methods = new Method[ fields.length * 2 ];
			for( int i = 0; i < fields.length; i++ ) {
				//gets access and mutator methods for the field. these are: set[field name]( type ), get[field name]( type ),
				Method access = monitor.getType().getMethod( "get" + fields[i] );
				Class<?> retType = access.getReturnType();
				Method mutate = monitor.getType().getMethod( "set" + fields[i], retType );
				//config variable must be primitive (as of this version)
				switch( retType.getName() ) {
					case "java.lang.Byte" : case "byte" :
					case "java.lang.Short" : case "short" :
					case "java.lang.Integer" : case "int" :
					case "java.lang.Long" : case "long" :
					case "java.lang.Float" : case "float" :
					case "java.lang.Double" : case "double" :
					case "java.lang.Boolean" : case "boolean" :
					case "java.lang.Character" : case "char" :
					case "java.lang.String" :
						if( ( access.getModifiers() & Modifier.PUBLIC ) == Modifier.PUBLIC &&
								( mutate.getModifiers() & Modifier.PUBLIC ) == Modifier.PUBLIC ) {
							methods[i * 2] = access;
							methods[i * 2 + 1] = mutate;
							break;
						} else {
							return false;
						}
					default :
						return false;
				}
			}
			monitoredFields.put( monitor, methods );
			return true;
		} catch( NoSuchFieldException|NoSuchMethodException|SecurityException|ClassNotFoundException e ) {
			return false;
		}
	}
	
	//removes field from being monitored by the config
	public boolean removeMonitorObject( String fieldName ) {
		try {
			return monitoredFields.remove( parse( fieldName ) ) == null;
		} catch ( NoSuchFieldException|SecurityException|ClassNotFoundException e ) {
			return false;
		}
	}
	
	
	//parses the field name as [classname].[field name]
	private Field parse( String str ) throws NoSuchFieldException, SecurityException, ClassNotFoundException {
		int index = str.lastIndexOf( '.' );
		Field field = Class.forName( str.substring( 0, index ) ).getDeclaredField( str.substring( index + 1 ) );
		int mod = Modifier.STATIC & Modifier.PUBLIC;
		if( ( field.getModifiers() & mod ) == mod ) {
			return field;
		}
		throw new IllegalArgumentException( "Incorrect modifiers" );
	}
}

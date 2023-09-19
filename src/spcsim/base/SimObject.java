package spcsim.base;

/* Author: Kent Fukuda
 * Description: Super class of all simulation object entities
 * Created: 7-13-21
 * Status: entity inteface, finished
 * Dependencies: none
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public interface SimObject extends Externalizable, Supplier<String>, Consumer<String> {
    
    //magic and version of simobject storage files for io
    public static final int MAGIC = 0x9A471C1E, VERSION = 02000101, NAME_LENGTH = 6;
    
    
    //write method for implementing io. default throws error
    public default void write( DataOutput out ) throws IOException {
        throw new IOException();
    }
    
    //read method for implementing io. default throws error
    public default void read( DataInput in ) throws IOException {
        throw new IOException();
    }
    
    //name of simobject instance
    public default String name() {
        return "Unnamed";
    }
    
    //new instance of simobject of the same class
    public default SimObject newInstance() throws IllegalStateException {
        return infoWithClass( getClass() ).instance.get();
    }
    
    //returns the simobject type name of particle
    public default String typeName() throws IllegalStateException {
        return infoWithClass( getClass() ).name;
    }
    
    //sets attribute data of simobject from a string format
    public default void parseString( String stringForm ) throws IllegalStateException, IllegalArgumentException {
        try {
            var info = infoWithClass( getClass() );
            int index = stringForm.indexOf( '[' ), end = stringForm.lastIndexOf( ']' );
            if( end != stringForm.length() -1 || !stringForm.substring( 0, index ).equals( info.name ) )
                throw new IllegalArgumentException( "Invalid format" );
            if( index + 1 == end ) return;
            synchronized( this ) {
                while( index < end ) {
                    var start = ++index;
                    while( stringForm.charAt( ++index ) != '=' ) { }
                    var name = stringForm.substring( start, start = index );
                    while( stringForm.charAt( ++index ) != ',' && index < end ) { }
                    var value = stringForm.substring( start + 1, index ).replaceAll( "\\\\o", "[" ).replaceAll( "\\\\c", "]" )
                            .replaceAll( "\\\\e", "=" ).replaceAll( "\\\\d", "," ).replaceAll( "\\\\n", "\n" ).replaceAll( "\\\\", "\\" );
                    info.fields[ Arrays.binarySearch( info.fields, name ) ].strSet.accept( this, value );
                }
            }
        } catch( IndexOutOfBoundsException|NullPointerException e ) {
            throw new IllegalArgumentException( "Invalid format", e );
        }
    }
    
    //retreives the string format of the attribute data of simobject
    public default String formatString() throws IllegalStateException {
        var bldr = new StringBuilder();
        var info = infoWithClass( getClass() );
        bldr.append( info.name ).append( '[' );
        for( var field : info.fields ) {
            String value = field.strGet.apply( this ).replaceAll( "\\\\", "\\\\" ).replaceAll( "\\[", "\\o" )
                    .replaceAll( "\\]", "\\c" ).replaceAll( "=", "\\e" ).replaceAll( ",", "\\d" ).replaceAll( "\n", "\\n" );
            bldr.append( field.name ).append( '=' ).append( value ).append( ',' );
        }
        bldr.setCharAt( bldr.length() - 1, ']' );
        return bldr.toString();
    }

    //overridden write external method
    @Override
    public default void writeExternal( ObjectOutput o ) throws IOException {
        write( o );
    }
    
    //overridden read external method
    @Override
    public default void readExternal( ObjectInput o ) throws IOException {
        read( o );
    }
    
    //overridden get method
    @Override
    public default String get() throws IllegalStateException {
        return formatString();
    }
    
    //overridden accept method
    @Override
    public default void accept( String t ) throws IllegalStateException, IllegalArgumentException {
        parseString( t );
    }
    
    
    //static methods
    //creates new instance with simobj subclass cls
    public static <Type extends SimObject> Type newInstance( Class<Type> cls ) throws IllegalStateException {
        return (Type)infoWithClass( cls ).instance.get();
    }
    
    //parses string format of simobjs back into a object
    public static SimObject valueOf( String stringForm ) throws IllegalArgumentException, IllegalArgumentException {
        var index = stringForm.indexOf( '[' );
        if( index < 0 )
            throw new IllegalArgumentException( "Invalid format" );
        var info = SimObjInfo.ID_MAP.get( createID( stringForm.substring( 0, index ) ) );
        if( info == null )
            throw new IllegalArgumentException( "Invalid format" );
        var obj = info.instance.get();
        obj.parseString( stringForm );
        return obj;
    }
    
    //reads a collection of simobjs from an iostream
    public static SimObject[] read( InputStream stream ) throws IOException, NullPointerException {
        int identifier = 0, current;
        while( ( identifier = ( identifier << 8 ) | ( current = stream.read() ) ) != MAGIC )
            if( current < 0 )
                throw new IOException();
        if( VERSION != ( ( stream.read() << 24 ) | ( stream.read() << 16 ) | ( stream.read() << 8 ) | stream.read() ) )
            throw new IOException();
        var inflater = new InflaterInputStream( stream );
        var data = new DataInputStream( inflater );
        int len = data.readInt();
        var partList = new SimObject[len];
	for( int i = 0; i < len; i++ ) {
            var obj = SimObjInfo.ID_MAP.get( data.readInt() ).instance.get();
            obj.read( (DataInput)data );
            partList[i] = obj;
        }
        return partList;
    }
    
    //writes a collection of simobjs to an iostream
    public static void write( SimObject[] partList, OutputStream stream ) throws IOException, NullPointerException, IllegalStateException {
        writeInt( stream, MAGIC );
        writeInt( stream, VERSION );
        var deflater = new DeflaterOutputStream( stream );
        var data = new DataOutputStream( deflater );
        data.writeInt( partList.length );
	for( SimObject obj : partList ) {
            data.writeInt( infoWithClass( obj.getClass() ).id );
            obj.write( data );
        }
        data.flush();
        deflater.finish();
        stream.flush();
    }
    
    //returns the simobject class of a type name
    public static Class<? extends SimObject> classType( String typeName ) throws IllegalStateException {
        var info = SimObjInfo.ID_MAP.get( createID( typeName ) );
        if( info == null )
            throw new IllegalStateException();
        return (Class <? extends SimObject >)info.particleClass;
    }
    
    //returns the simobject type name of a class
    public static String typeName( Class<? extends SimObject> cls ) throws IllegalStateException {
        return infoWithClass( cls ).name;
    }
    
    //returns the loaded subclasses of a class
    public static <T extends SimObject> Class<? extends T>[] subClassesOf( Class<T> cls ) throws IllegalStateException {
        return infoWithClass( cls ).subclasses.toArray( Class[]::new );
    }
    
    //ensured thats the simobject class is loaded and returns its serialVersionUID
    public static void ensureLoaded( Class<? extends SimObject> cls ) throws IllegalStateException {
        try {
            MethodHandles.lookup().findStaticGetter( cls, "serialVersionUID", long.class ).invoke();
        } catch( IllegalStateException e ) {
            throw e;
        } catch( Throwable t ) {
            throw new IllegalStateException( "An error occurred while the class was loading", t );
        }
    }
    
    //registers particle information  [\]^_`
    public static long registerParticleClass( MethodHandles.Lookup lookup, String name ) throws UnsupportedOperationException {
        Class<?> cls = lookup.lookupClass();
        boolean isClass = ( cls.getModifiers() & Modifier.ABSTRACT ) == 0;
        //condition checks
        if( lookup == null )
            throw new UnsupportedOperationException( "Lookup is null" );
        int id = createID( name );
        if( id == -1 && isClass )
            throw new UnsupportedOperationException( "Invalid name argument" );
        if( !SimObject.class.isAssignableFrom( cls ) )
            throw new UnsupportedOperationException( "Class does not implement simulation object" );
        int clsIndex = Collections.binarySearch( SimObjInfo.PARTICLE_INFO, cls );
        if( clsIndex >= 0 )
            throw new UnsupportedOperationException( "Class already registered" );
        if( SimObjInfo.ID_MAP.containsKey( id ) )
            throw new UnsupportedOperationException( "Class with same id already exists" );
        try {
            ArrayList<SimObjInfo.FieldInfo> fieldInfos = new ArrayList<>();
            //iterates through each field of sim object
            for( Field field : cls.getDeclaredFields() ) {
                Class<?> type = field.getType();
                //handles for field
                MethodHandle setHandle, getHandle;
                BiConsumer<SimObject,String> strSet;
                Function<SimObject,String> strGet;
                if( ( field.getModifiers() & ( Modifier.TRANSIENT|Modifier.FINAL|Modifier.STATIC ) ) != 0 ) continue;
                try {
                    setHandle = lookup.unreflectSetter( field );
                    getHandle = lookup.unreflectGetter( field );
                } catch( IllegalAccessException e ) {
                    continue;
                }
                //special case handing for int, long, double, and string (proper lambda functions already exist)
                if( type == int.class ) {
                    ObjIntConsumer<SimObject> set = MethodHandleProxies.asInterfaceInstance( ObjIntConsumer.class, setHandle );
                    ToIntFunction<SimObject> get = MethodHandleProxies.asInterfaceInstance( ToIntFunction.class, getHandle );
                    strSet = ( p, s ) -> set.accept( p, Integer.parseInt( s ) );
                    strGet = p -> String.valueOf( get.applyAsInt( p ) );
                } else if( type == long.class ) {
                    ObjLongConsumer<SimObject> set = MethodHandleProxies.asInterfaceInstance( ObjLongConsumer.class, setHandle );
                    ToLongFunction<SimObject> get = MethodHandleProxies.asInterfaceInstance( ToLongFunction.class, getHandle );
                    strSet = ( p, s ) -> set.accept( p, Long.parseLong( s ) );
                    strGet = p -> String.valueOf( get.applyAsLong( p ) );
                } else if( type == double.class ) {
                    ObjDoubleConsumer<SimObject> set = MethodHandleProxies.asInterfaceInstance( ObjDoubleConsumer.class, setHandle );
                    ToDoubleFunction<SimObject> get = MethodHandleProxies.asInterfaceInstance( ToDoubleFunction.class, getHandle );
                    strSet = ( p, s ) -> set.accept( p, Double.parseDouble( s ) );
                    strGet = p -> String.valueOf( get.applyAsDouble( p ) );
                } else if( type == char.class || type == Character.class ) {
                    BiConsumer<SimObject,Character> set = MethodHandleProxies.asInterfaceInstance( BiConsumer.class, setHandle );
                    strSet = ( p, s ) -> {
                        if( s.length() == 1 ) {
                            set.accept( p, s.charAt( 0 ) );
                        } else {
                            throw new IllegalArgumentException();
                        }
                    };
                    strGet = p -> MethodHandleProxies.asInterfaceInstance( Function.class, getHandle ).apply( p ).toString();
                } else if( type == String.class ) {
                    strSet = MethodHandleProxies.asInterfaceInstance( BiConsumer.class, setHandle );
                    strGet = MethodHandleProxies.asInterfaceInstance( Function.class, getHandle );
                } else {
                    //if primitive, convert to box class
                    if( type.isPrimitive() ) {
                        switch( type.getName() ) {
                            case "short" : type = Short.class; break;
                            case "byte" : type = Byte.class; break;
                            case "boolean" : type = Boolean.class; break;
                            case "float" : type = Float.class; break;
                            default :
                        }
                    }
                    //for other instances, obj( String s ) constructor is used to convert from string to object
                    var constructor = (Function<String,Object>)LambdaMetafactory.metafactory( lookup, "apply", 
                            MethodType.methodType( Function.class ), MethodType.methodType( Object.class, Object.class ),  
                            lookup.findConstructor( type, MethodType.methodType( void.class, String.class ) ), 
                            MethodType.methodType( type, String.class ) ).getTarget().invoke();
                    BiConsumer<SimObject,Object> set = MethodHandleProxies.asInterfaceInstance( BiConsumer.class, setHandle );
                    strSet = ( p, s ) -> set.accept( p, constructor.apply( s ) );
                    strGet = p -> MethodHandleProxies.asInterfaceInstance( Function.class, getHandle ).apply( p ).toString();
                }
                //add fieldinfo to list
                var fieldName = field.getName();
                fieldInfos.add( new SimObjInfo.FieldInfo( fieldName, strSet, strGet ) );
            }
            var superCls = cls.getSuperclass();
            if( SimObject.class.isAssignableFrom( superCls ) ) {
                var info = infoWithClass( (Class<? extends SimObject>)superCls );
                fieldInfos.addAll( 0, Arrays.asList( info.fields ) );
                info.subclasses.add( (Class<? extends SimObject>)cls );
            }
            fieldInfos.sort( ( a1, a2 ) -> a1.name.compareTo( a2.name ) );
            //finish creating simobjinfo
            Supplier<? extends SimObject> instance;
            if( isClass )
                instance = (Supplier<? extends SimObject>)LambdaMetafactory.metafactory( lookup, "get", 
                        MethodType.methodType( Supplier.class ), MethodType.methodType( Object.class ),  
                        lookup.findConstructor( cls, MethodType.methodType( void.class ) ), MethodType.methodType( cls ) ).getTarget().invoke();
            else
                instance = null;
            var pInfo = new SimObjInfo( id, cls, name, instance, fieldInfos.toArray( SimObjInfo.FieldInfo[]::new ) );
            synchronized( SimObjInfo.PARTICLE_INFO )  {
                SimObjInfo.PARTICLE_INFO.add( -clsIndex - 1, pInfo );
                if( isClass )
                    SimObjInfo.ID_MAP.put( id, pInfo );
            }
        } catch( Error e ) {
            throw e;
        } catch( Throwable t ) {
            throw new UnsupportedOperationException( "Error occurred while registering simulation object", t );
        }
        return SimObjInfo.SERIAL_START | id;
    }
    
    
    //pirvate utility methods
    //writes int
    private static void writeInt( OutputStream stream, int val ) throws IOException {
        for( var i = 24; i >= 0; i -= 8 )
            stream.write( val >>> i );
    }
    
    //creates lossless id from name. returns -1 for invalid strings
    private static int createID( String name ) {
        if( name == null || name.length() > NAME_LENGTH )
            return -1;
        var id = 0;
        for( int i = 0; i < name.length(); i++ ) {
            int c = name.charAt( i ) - 'A';
            if( c >= 32 )
                return -1;
            id = ( id << 5 ) | c;
        }
        return id;
    }
    
    //retrieves simobj info for subclass cls
    private static SimObjInfo infoWithClass( Class<? extends SimObject> cls ) {
        int index;
        if( cls == null ) {
            throw new IllegalStateException( "Class argument is null" );
        } else if( ( index = Collections.binarySearch( SimObjInfo.PARTICLE_INFO, cls ) ) < 0 ) {
            ensureLoaded( cls );
            if( ( index = Collections.binarySearch( SimObjInfo.PARTICLE_INFO, cls ) ) < 0 ) {
                throw new IllegalStateException( "Class not registered as SimObject" );
            }
        }
        return SimObjInfo.PARTICLE_INFO.get( index );
    }
    
    
    //class for storing information about each registered subclass of simobj
    static final class SimObjInfo implements Comparable<Class<?>> {
        
        //first four bytes of all simobject serialVersionUIDs
        private static final long SERIAL_START = ( (long)MAGIC ) << 32;
        //list storing info of all subclasses of simobject that are registered
        private static final ArrayList<SimObjInfo> PARTICLE_INFO = new ArrayList<>();
        //hashmap correlating simobject type ids with their respective particle information
        private static final HashMap<Integer,SimObjInfo> ID_MAP = new HashMap<>();
        
        //final fields
        private final int id;
        private final Class<?> particleClass;
        private final String name;
        private final Supplier<? extends SimObject> instance;
        private final ArrayList<Class<? extends SimObject>> subclasses;
        private final FieldInfo[] fields;
        
        //constructor
        private SimObjInfo( int id, Class<?> particleClass, String name, Supplier<? extends SimObject> instance, FieldInfo[] fields ) {
            this.id = id;
            this.particleClass = particleClass;
            this.name = name;
            this.instance = instance;
            this.subclasses = new ArrayList<>();
            this.fields = fields;
        }
        
        //overridden comparable method
        @Override
        public int compareTo( Class<?> t ) {
            int val = particleClass.hashCode() - t.hashCode();
            return ( val == 0 && particleClass != t ) ? 1 : val;
        }
        
        
        //class for storing information about each field of subclass in simobjinfo
        private static final class FieldInfo implements Comparable<String> {
            //fields
            private final String name;
            private final BiConsumer<SimObject,String> strSet;
            private final Function<SimObject,String> strGet;
        
            //constructor
            private FieldInfo( String name, BiConsumer<SimObject,String> strSet, Function<SimObject,String> strGet ) {
                this.name = name;
                this.strSet = strSet;
                this.strGet = strGet;
            }
            
            //overridden comparable method
            @Override
            public int compareTo( String t ) {
                return name.compareTo( t );
            }
        }
    }
}
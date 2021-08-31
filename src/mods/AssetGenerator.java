package mods;

import java.io.File;
import java.io.IOException;

import spcsim.SpaceObject;

public class AssetGenerator {
	
	private AssetGenerator() { }
	
	
	public static void main( String[] args ) {
		args = new String[] { "assets" };
		
		
		if( args.length == 1 && args[0] == "-h" ) {
			System.out.println( "Usage: java AssetGenerator <directory name>" );
			return;
		} else if( args.length != 1 ) {
			System.err.println( "Please enter the correct number of arguments" );
			return;
		}
		
		File file = new File( args[0] );
		if( file.exists() ) {
			if( file.isFile() ) {
				System.err.println( "A file with the same name already exists." );
				return;
			} else if( file.list().length != 0 ) {
				System.err.println( "File is not empty." );
				return;
			}
		} else {
			file.mkdir();
		}
		
		try {
			String[] nameTable = { "Sol", "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune",
					"Luna", "Io", "Europa", "Ganymede", "Callisto", "Titan" };
			double[][] varTable = { //wikipedia
				//mass    radius  temp  distance            degree        eccentricity 
				{ 330060, 109,    6000, 0,                  0,            0          },//sun
				{ 0.0553, 0.3830, 700,  9089.503459664793,  77.45779628,  0.20563593 },//mercury
				{ 0.815,  0.9500, 750,  16984.692283374538, 131.60246718, 0.00677672 },//venus
				{ 1.000,  1.0000, 300,  23481.12716221041,  102.93768193, 0.01671123 },//earth
				{ 0.107,  0.5321, 240,  35778.34287043997,  -23.94362959, 0.09339410 },//mars
				{ 318,    10.973, 160,  122169.33239565387, 14.72847983,  0.04838624 },//jupiter
				{ 95.2,   9.1402, 130,  223931.31599119777, 92.59887831,  0.05386179 },//saturn
				{ 14.5,   3.9809, 70,   450582.03902930976, 170.95427630, 0.04725744 },//uranus
				{ 17.1,   3.8647, 70,   706073.8372326898,  44.96476227,  0.00859048 },//neptune
				
				{ 0.0123, 0.2731, 400,  63.632082875529744,  0,           0.0549     },//3 moon
				{ 0.015,  0.286,  110,  66.4573850258986030, 0,           0.0041     },//5 io
				{ 0.008,  0.245,  100,  106.253021503688589, 0,           0.009      },//5 europa
				{ 0.0247, 0.413,  160,  168.199654685292732, 0,           0.0013     },//5 ganymede
				{ 0.018,  0.378,  130,  297.755454402762518, 0,           0.0074     },//5 callisto
				{ 0.0255, 0.404,  130,  197.309684507926542, 0,           0.0288     },//6 titan
			};
			/*
			triton
			eris
			pluto
			titania
			rhea
			oberon
			sedna
			iapetus
			charon
			umbriel
			ariel
			dione
			tethys
			ceres*/
			
			//regular system
			
			//simple solar system
			SpaceObject[] system = new SpaceObject[9];
			system[0] = new SpaceObject( nameTable[0], varTable[0][0], varTable[0][1], varTable[0][2], 0, 0, 0, 0 );
			for( int i = 1; i < system.length; i++ ) {
				system[i] = new SpaceObject( nameTable[i], varTable[i][0], varTable[i][1], varTable[i][2],
						system[0].getXPosition() + Math.cos( varTable[i][4] * Math.PI / 180 ) * varTable[i][3], 
						system[0].getYPosition() + Math.sin( varTable[i][4] * Math.PI / 180 ) * varTable[i][3], 0, 0 );
				SpaceObject.orbit( system[0], system[i], varTable[i][5] );
			}
			SpaceObject.writeFile( file.getCanonicalPath() + "/Simple Solar System.spcobj", system );
			
			//spiral system
			system = new SpaceObject[302];
			system[300] = new SpaceObject( "Star 1", 30000, 30, 5000, 10000, 0, 0, 0 );
			system[301] = new SpaceObject( "Star 2", 30000, 30, 5000, -10000, 0, 0, 0 );
			SpaceObject.orbit( system[300], system[301] );
			for( int i = 0; i < 300; i++ ) {
				boolean even = i % 2 == 0;
				double rot = Math.random() * 2 * Math.PI;
				double dist = Math.random() * 13500 + 1500;
				double mass = Math.random() * 0.1;
				system[i] = new SpaceObject( null, mass, Math.pow( mass, 1.0/3 ), 3000,
						Math.sin( rot ) * dist + ( even ? 10000 : -10000 ), Math.cos( rot ) * dist, 0, 0 );
				SpaceObject.orbit( system[ even ? 300 : 301 ], system[i] );
			}
			SpaceObject.writeFile( file.getCanonicalPath() + "/Spiral System.spcobj", system );
			
			//proto system
			system = new SpaceObject[151];
			system[150] = new SpaceObject( "Host Star", 30000, 30, 5000, 0, 0, 0, 0 );
			for( int i = 0; i < 150; i++ ) {
				double rot = Math.random() * 2 * Math.PI;
				double dist = Math.random() * 28500 + 1500;
				double mass = Math.random() * 0.1;
				system[i] = new SpaceObject( null, mass, Math.pow( mass, 1.0/3 ), 3000,
						Math.sin( rot ) * dist, Math.cos( rot ) * dist, 0, 0 );
				SpaceObject.orbit( system[150], system[i] );
			}
			SpaceObject.writeFile( file.getCanonicalPath() + "/Proto System.spcobj", system );
			
			//default system
			system = new SpaceObject[] {
					new SpaceObject( "Cyzin", 62597.19924, 33, 3000, 0.0, 0.0, 0.0, 0.0 ),
					new SpaceObject( "Klarken", 1.3230927052799724, 1.1417300480576635, 300, -1398.3470005203105, -1230.7819421014865, 426.0673753564133, -462.4511267083711 ),
					new SpaceObject( "Kanna", 5.5171459763102915, 2.0143874621444717, 160, -1015.6161176168188, 7288.135669967801, -278.76353606381457, -28.329617790427832 ),
					new SpaceObject( "Yoriel", 3.778587713954922, 1.6821585404158164, 100, -7831.0278529975285, 24332.67811746573, -154.70234602144538, -53.22125741725264 ),
					new SpaceObject( "Eytfana", 0.004189185852675781, 0.15798018513263465, 40, 5108.7205302590455, -30044.68257991509, 181.40130870383396, 6.115744409024012 ),
					new SpaceObject( "Ouros", 0.27422998378310076, 0.6886694926160145, 40, 97776.11676742957, -38461.13067964423, 49.5554722245471, 58.29667008038443 )
			};
			SpaceObject.writeFile( file.getCanonicalPath() + "/Default System.spcobj", system );
			
			System.out.println( "Successfully created files." );
		} catch( IOException|NullPointerException e ) {
			System.err.println( "Error writing files" );
		}
	}
}
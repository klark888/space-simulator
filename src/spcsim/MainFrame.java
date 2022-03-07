package spcsim;

/* Author: Kent Fukuda
 * Description: The class that supports the main window gui of the program
 * Created: 8-27-21
 * Status: environment class, wip
 * Dependencies: Config, CustomLayout, EditorPane, Environment, GUIHandler, SpaceObject, Units
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;


public class MainFrame extends JFrame {
	
	private static final long serialVersionUID = 472314064308238191L;
	
	
	//constructor
	public MainFrame( Environment environment, Config config, String version ) {
		//creates frame and adds environment and config components to screen
		super( "Space Simulator - v" + version );
		super.setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
		super.setBounds( (int)( GUIHandler.getScreenWidth() * 0.1 ), (int)( GUIHandler.getScreenHeight() * 0.1 ), 
				(int)( GUIHandler.getScreenWidth() * 0.7 ), (int)( GUIHandler.getScreenHeight() * 0.7 ) );
		super.setBackground( Color.DARK_GRAY );
		EditorPane editPane = new EditorPane( this, environment );
		ExtensionLoader loader = new ExtensionLoader();
		super.add( environment );
		super.add( editPane );
		
		//message for confirming sim clearing
		String confirmMessage = "Do you want to destroy changes?";
		JDialog timeUnit = EditorPane.createUnitDialog( this, str -> environment.setTimeUnit( str ), Units.TIME );
		JDialog lengthUnit = EditorPane.createUnitDialog( this, str -> environment.setLengthUnit( str ), Units.LENGTH );
		
		//resource loading
		String[] files = {};
		try {
			InputStream stream = getClass().getResourceAsStream( "icon.png" );
			super.setIconImage( ImageIO.read( stream ) );
			stream.close();
			
			stream = getClass().getClassLoader().getResourceAsStream( "assets/assetlist.txt" );
			byte[] bytes = new byte[ stream.available() ];
			stream.read( bytes );
			stream.close();
			files = new String( bytes ).split( ";" );
			
		} catch( IOException|IllegalArgumentException|NullPointerException e ) { }
		
		
		//initialize actionlisteners and menu item labels for asset menu
		ActionListener[] assetListeners = new ActionListener[ files.length ];
		for( int i = 0; i < files.length; i++ ) {
			String file = files[i];
			files[i] = file.substring( 0, Math.max( file.length() - SpaceObject.SHORTHAND.length() - 1, 1 ) );
			assetListeners[i] = ( a ) -> {
				try {
					SpaceObject[] objList = SpaceObject.readStream( MainFrame.this.getClass().getClassLoader().getResourceAsStream( "assets/" + file ) );
					double x = environment.getXPosition();
					double y = environment.getYPosition();
					for( SpaceObject obj : objList ) {
						obj.setXPosition( obj.getXPosition() + x );
						obj.setYPosition( obj.getYPosition() + y );
					}
					environment.queueOperation( ( list ) -> list.addAll( Arrays.asList( objList ) ) );
				} catch( IOException|ClassNotFoundException|ClassCastException|NullPointerException e ) {
					GUIHandler.errorMessage( "Error opening asset" );
				}
			};
		}
		
		//initializes the rest of the actionlisteners, menu item labels, and shortcut keys for the menubar
		String[] menus = { "File", "Edit", "Simulation", "View", "Assets", "About" };
		String[][] items = { { "New", "Open", "Save", "Import", "Import Config", "Export Config", "Extensions", "Exit" }, 
			{ "Select", "Copy", "Paste", "Delete", "New Object", "Add Moon", "Random Planet" },
			{ "Start", "Stop", "Step", "Threading", "Simulation Speed", "Time Step", "Tick Speed", "Reset Simulation Counter" },
			{ "Zoom In", "Zoom Out", "Default Zoom", "Editor Pane", "FPS", "Time Units", "Length Units", "Toggle Name Labels", "Toggle Stats Boxs" }, 
			files, { "Information", "GitHub", "Help", "Changelog" } };
		int[][] keys = { { KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_S, KeyEvent.VK_O, 0, 0, 0, 0 },
				{ KeyEvent.VK_F, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_X, 0, 0, 0 },
				{ KeyEvent.VK_R, KeyEvent.VK_W, KeyEvent.VK_Q, 0, 0, 0, 0, 0 },
				{ KeyEvent.VK_EQUALS, KeyEvent.VK_MINUS, KeyEvent.VK_0, 0, 0, 0, 0, 0, 0 },
				new int[ files.length ], { 0, 0, 0, 0 } };
		boolean[][] shiftMod = { { false, false, false, true, false, false, false, false }, 
				{ false, true, true, true, false, false, false, false },
				new boolean[9], new boolean[7], new boolean[ files.length ], new boolean[4] };
		ActionListener[][] listeners = {
			{
				a -> {
					if( GUIHandler.confirmSelection( confirmMessage ) ) {
						environment.queueOperation( ( list ) -> list.clear() );
					}
				},
				a -> {
					if( GUIHandler.confirmSelection( confirmMessage ) ) {
						String path = GUIHandler.selectFile( SpaceObject.SHORTHAND, "Space Object File", "Open", false );
						if( path != null ) {
							try {
								SpaceObject[] objList = SpaceObject.readStream( new FileInputStream( path ) );
								environment.queueOperation( ( list ) -> {
									list.clear();
									list.addAll( Arrays.asList( objList ) );
								} );
							} catch( IOException|ClassNotFoundException|ClassCastException|NullPointerException|SecurityException e ) {
								GUIHandler.errorMessage( "Error opening file." );
							}
						}
					}
				},
				a -> {
					String path = GUIHandler.selectFile( SpaceObject.SHORTHAND, "Space Object File", "Save", true );
					if( path != null ) {
						if( !path.endsWith( "." + SpaceObject.SHORTHAND ) ) {
							path += "." + SpaceObject.SHORTHAND;
						}
						String filePath = path;
						environment.queueOperation( ( list ) -> {
							try {
								SpaceObject.writeStream( new FileOutputStream( filePath ), list.toArray( new SpaceObject[ list.size() ] ) );
							} catch( IOException|NullPointerException|SecurityException e ) {
								GUIHandler.errorMessage( "Error saving file." );
							}
						} );
					}
				},
				a -> {
					String path = GUIHandler.selectFile( SpaceObject.SHORTHAND, "Space Object File", "Import", false );
					if( path != null ) {
						try {
							SpaceObject[] objList = SpaceObject.readStream( new FileInputStream( path ) );
							double x = environment.getXPosition();
							double y = environment.getYPosition();
							for( SpaceObject obj : objList ) {
								obj.setXPosition( obj.getXPosition() + x );
								obj.setYPosition( obj.getYPosition() + y );
							}
							environment.queueOperation( ( list ) -> {
								list.addAll( Arrays.asList( objList ) );
							} );
						} catch( IOException|ClassNotFoundException|ClassCastException|NullPointerException|SecurityException e ) {
							GUIHandler.errorMessage( "Error importing file." );
						}
					}
				},
				a -> {
					String path = GUIHandler.selectFile( "cfg", "Config File", "Import", false );
					if( path != null ) {
						if( !config.loadConfig( path ) ) {
							GUIHandler.errorMessage( "Error importing config." );
						}
					}
				},
				a -> {
					String path = GUIHandler.selectFile( "cfg", "Config File", "Export", true );
					if( path != null ) {
						if( !path.endsWith( ".cfg" ) ) {
							path += ".cfg";
						}
						if( !config.saveConfig( path ) ) {
							GUIHandler.errorMessage( "Error exporting config." );
						}
					}
				},
				a -> {
					String path = GUIHandler.selectFile( "class", "Class File", "Add", false );
					if( path != null ) {
						Thread thread = new Thread( () -> {
							try {
								loader.addExtension( version, path );
							} catch( InvocationTargetException e ) {
		                        GUIHandler.errorMessage( "Exception in program extention.\n" + e.getTargetException() );
		                    } catch( Throwable t ) {
		                        GUIHandler.errorMessage( "Could not add program extention.\n" + t );
		                    }
						}, "extension-main" );
			            thread.setDaemon( true );
			            thread.start();
					}
				},
				a -> {
					if( GUIHandler.confirmSelection( confirmMessage ) ) {
						config.saveConfig();
						System.exit( 0 );
					}
				}
			},
			{
				a -> { 
					JDialog dialog = new JDialog( this, "Select Object" );
					java.awt.List slist = new java.awt.List();
					slist.addActionListener( ( b ) -> {
						int select = slist.getSelectedIndex();
						dialog.dispose();
						environment.queueOperation( list -> {
							if( select < list.size() ) {
								editPane.setSelected( list.get( select ) );
								EventQueue.invokeLater( () -> {
									editPane.setVisible( true );
									super.validate();
								} );
							}
						} );
					} );
					dialog.add( slist );
					dialog.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
					dialog.setBounds( super.getX() + super.getWidth() / 2 - 100, super.getY() + super.getHeight() / 2 - 100, 200, 200 );
					dialog.setResizable( false );
					dialog.setModal( true );
					environment.queueOperation( list -> {
						list.forEach( ( obj ) -> {
							String name = obj.getName();
							slist.add( name == null ? "Unnamed" : name );
						} );
						EventQueue.invokeLater( () -> dialog.setVisible( true ) );
					} );
				},
				a -> {
					SpaceObject copy = editPane.getSelected();
					if( copy != null ) {
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents( new StringSelection( String.valueOf( copy ) ), null );
					}
				},
				a -> environment.queueOperation( ( list ) -> {
					try {
						SpaceObject paste = new SpaceObject( (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData( DataFlavor.stringFlavor ) );
						if( paste != null ) {
							paste.setXPosition( environment.getXPosition() );
							paste.setYPosition( environment.getYPosition() );
							editPane.setSelected( paste );
							list.add( paste );
						}
					} catch( IllegalArgumentException|IOException|UnsupportedFlavorException e ) { }
				} ),
				a -> environment.queueOperation( list -> list.remove( editPane.getSelected() ) ),
				a -> environment.queueOperation( list -> {
					SpaceObject obj = new SpaceObject( "New SpaceObject", 1, 1, 300, environment.getXPosition(), environment.getYPosition(), 0, 0 );
					list.add( obj );
					editPane.setSelected( obj );
				} ),
				a -> editPane.addMoon(),
				a -> {
					String name = "-";
					for( int i = 0 ; i < 3; i++ ) {
						name = (char)( (int)( Math.random() * 26 ) + 'A' ) + name;
						name += (int)( Math.random() * 10 );
					}
					double mass = 0.3 / ( Math.random() + 0.000075 ) - 0.03;
					double radius = mass < 6 ? Math.pow( mass, 1.0 / 3 ) : Math.pow( -1.03, -mass + 80 ) + 11;
					radius *= 0.9 + Math.random() * 0.2;
					SpaceObject obj = new SpaceObject( name, mass, radius, 100 / ( Math.random() + 0.03 ), environment.getXPosition(), environment.getYPosition(), 0, 0 );
					environment.queueOperation( ( list ) -> list.add( obj ) );
					editPane.setSelected( obj );
				}
			},
			{
				a -> environment.setActive( true ),
				a -> environment.setActive( false ),
				a -> {
					if( !environment.getActive() ) {
						environment.queueOperation( ( list ) -> environment.setActive( false ) );
						environment.setActive( true );
					}
				},
				a -> environment.setSingleThread( !GUIHandler.confirmSelection( "Enable multi-threading?" ) ),
				a -> {
					try {
						String oldSpeed;
						long tickLen = environment.getTickLength();
						double step = environment.getTimeStep();
						if( tickLen == -1 ) {
							oldSpeed = "infinity";
						} else {
							oldSpeed = Units.toString( Units.TIME, step * 1000 / tickLen, environment.getTimeUnit() );
						}
						String timeU = environment.getTimeUnit();
						String newSpeed = GUIHandler.inquiryMessage( "Change Simulation Speed (simulation " + timeU + " per real-time second):", oldSpeed );
						if( newSpeed == null ) {
							return;
						} else if( newSpeed.toLowerCase().equals( "infinity" ) ) {
							environment.setTickLength( -1 );
						} else {
							if( tickLen == -1 ) {
								tickLen = environment.getFrameLength();
								environment.setTickLength( tickLen );
							}
							environment.setTimeStep( Units.parseDouble( Units.TIME, newSpeed, timeU ) * tickLen / 1000 );
						}
					} catch( NumberFormatException e ) {
						GUIHandler.errorMessage( "Invalid number." );
					}
				},
				a -> {
					try {
						String timeU = environment.getTimeUnit();
						String ans = GUIHandler.inquiryMessage( "Change time step per tick (simulation " + timeU + " per tick):", 
								Units.toString( Units.TIME, environment.getTimeStep(), timeU ) );
						if( ans != null ) {
							double newStep = Units.parseDouble( Units.TIME, ans, environment.getTimeUnit() );
							environment.setTimeStep( newStep );
						}
					} catch( NumberFormatException e ) {
						GUIHandler.errorMessage( "Invalid number." );
					}
				},
				a -> {
					try {
						long tickLen = environment.getTickLength();
						String def;
						if( tickLen == -1 ) {
							def = "infinity";
						} else {
							def = Long.toString( 1000 / tickLen );
						}
						String ans = GUIHandler.inquiryMessage( "Change ticks per second:", def );
						if( ans != null ) {
							if( ans.toLowerCase().equals( "infinity" ) ) {
								environment.setTickLength( -1 );
							} else {
								long tps = Long.parseLong( ans );
								if( 0 < tps && tps <= 1000 ) {
									environment.setTickLength( 1000 / tps );
								} else {
									GUIHandler.errorMessage( "TPS must be between 1 and 1000 or infinity." );
								}
							}
						}
					} catch( NumberFormatException e ) {
						GUIHandler.errorMessage( "Invalid number." );
					}
				},
				a -> {
					if( GUIHandler.confirmSelection( "Reset Simulation Counter?" ) ) {
						environment.setDaysPassed( 0 );
					}
				}
			},
			{
				a -> environment.setZoom( environment.getZoom() * 1.25 ),
				a -> environment.setZoom( environment.getZoom() * 0.75 ),
				a -> environment.setZoom( 1 ),
				a -> {
					editPane.setVisible( !editPane.isVisible() );
					super.validate();
				},
				a -> {
					String ans = GUIHandler.inquiryMessage( "Change maximum FPS:", Long.toString( 1000 / environment.getFrameLength() ) );
					if( ans != null ) {
						try {
							long ticks = Long.parseLong( ans );
							if( 0 < ticks && ticks <= 1000 ) {
								environment.setFrameLength( 1000 / ticks );
							} else {
								GUIHandler.errorMessage( "FPS must be between 1 and 1000." );
							}
						} catch( NumberFormatException e ) {
							GUIHandler.errorMessage( "Invalid number." );
						}
					}
				},
				a -> timeUnit.setVisible( true ),
				a -> lengthUnit.setVisible( true ),
				a -> environment.setShowNames( !environment.getShowNames() ),
				a -> environment.setShowEnvStatus( !environment.getShowEnvStatus() )
			},
			assetListeners,
			{
				a -> GUIHandler.regularMessage( 
							"Program: Space Simulation Program\n" + 
							"Author:  Kent Fukuda\n" + 
							"Version: " + version + "\n" +
							"Created: 8-31-2021\n" +
							"License: GNU Affero General Public License",
							"Information" ),
				a -> {
					String url = "https://github.com/klark888/space-simulator";
					try {
						Desktop.getDesktop().browse( new URI( url ) );
					} catch( UnsupportedOperationException|IOException|URISyntaxException e ) {
						GUIHandler.errorMessage( "Could not open browser. Please copy link:\n" + url );
					}
				},
				a -> GUIHandler.regularMessage( 
						"File - Open and save the simulation environment, program configuration, and add script classes.\n" +
						"Edit - Select or create a new object in the simulation or change the current selected object.\n" + 
						"Simulation - Start and stop simulation or change the speed to the simulation.\n" + 
						"View - Zoom in, out, change measurement units, or set FPS.\n" + 
						"Assets - Select an asset to import into the simulation.\n" +
						"About - Information about the program.\n" +
						"Editor Pane - Edit the current selected object.\n" + 
						"Simulation View - Drag mouse to move camera. Click on a object to select it.\n" +
						"Edit Object - Drag arrow around to change velocity. Drag camera to change position.",
						"Help" ),
				a -> GUIHandler.regularMessage( 
						"Version " + version + ":\n" +
						"Fixed multi-threading.\n" + 
						"Added simulation box toggle.\n" + 
						"Added space object name label.\n" +
						"Refactored code.",
						"Changelog" )
			}
		};
		
		//initializes the menubar
		MenuBar menuBar = new MenuBar();
		for( int i = 0; i < menus.length; i++ ) {
			Menu menu = new Menu( menus[i] );
			for( int k = 0; k < items[i].length; k++ ) {
				MenuItem item = new MenuItem( items[i][k] );
				item.addActionListener( listeners[i][k] );
				if( keys[i][k] != 0 ) {
					item.setShortcut( new MenuShortcut( keys[i][k], shiftMod[i][k] ) );
				}
				menu.add( item );
			}
			menuBar.add( menu );
		}
		super.setMenuBar( menuBar );
		
		//sets the lsyoutmanager of the frame
		super.setLayout( new CustomLayout( () -> {
			Dimension b = MainFrame.super.getSize();
			Insets i = MainFrame.super.getInsets();
			int mult = 0;
			if( editPane.isVisible() ) {
				mult = 300;
				editPane.setBounds( b.width - mult - i.left - i.right, 0, mult, b.height - i.top - i.bottom );
			}
			environment.setBounds( 0, 0, b.width - mult - i.left - i.right, b.height - i.top - i.bottom );
		} ) );
		
		//adds a windowlistener for exit handling
		super.addWindowListener( new WindowListener() {
			@Override
			public void windowClosing( WindowEvent e ) { 
				if( GUIHandler.confirmSelection( confirmMessage ) ) {
					config.saveConfig();
					System.exit( 0 );
				}
			}

			@Override
			public void windowOpened( WindowEvent e ) { }
			@Override
			public void windowClosed( WindowEvent e ) { }
			@Override
			public void windowIconified( WindowEvent e ) { }
			@Override
			public void windowDeiconified( WindowEvent e ) { }
			@Override
			public void windowActivated( WindowEvent e ) { }
			@Override
			public void windowDeactivated( WindowEvent e ) { }
		} );
	}
	
	
	//mutator and accessor methods - used for config
	public void setWidth( int width ) {
		super.setSize( width, super.getHeight() );
	}
	
	public void setHeight( int height ) {
		super.setSize( super.getWidth(), height );
	}
	
	public void setX( int x ) {
		super.setLocation( x, super.getY() );
	}
	
	public void setY( int y ) {
		super.setLocation( super.getX(), y );
	}
	
	public void setEditVisible( boolean val ) {
		super.getContentPane().getComponent( 1 ).setVisible( val );
		super.validate();
	}
	
	public boolean getEditVisible() {
		return super.getContentPane().getComponent( 1 ).isVisible();
	}
	
	public EditorPane getEditorPane() {
		return (EditorPane)super.getContentPane().getComponent( 1 );
	}
	
	
	//class used for loading extensions
	private static final class ExtensionLoader extends ClassLoader {
		
		private final List<String> paths;
		
		private ExtensionLoader() {
			paths = Collections.synchronizedList( new ArrayList<>() );
		}
		
		
		public void addExtension( String version, String path ) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
			Class<?> cls = loadPath( path );
			String sub = cls.getName().replace( '.', '/' ) + ".class";
			if( path.endsWith( sub ) ) {
				paths.add( path.substring( path.length() - sub.length() ) + '/' );
				super.resolveClass( cls );
				Method m = cls.getMethod( "main", String[].class );
                m.invoke( null, (Object)new String[] { "name=spcsim.SpaceSim", "version=" + version } );
			} else {
				throw new IllegalArgumentException();
			}
		}
		
		@Override
		public Class<?> findClass( String name ) throws ClassNotFoundException {
			for( String path : paths ) {
				try {
					return loadPath( path + name.replace( '.', '/' ) + ".class" );
				} catch( IOException e ) { }
			}
			throw new ClassNotFoundException();
		}
		
		private Class<?> loadPath( String path ) throws IOException {
			FileInputStream stream = new FileInputStream( path );
            byte[] bytes = new byte[ stream.available() ];
            stream.read( bytes );
            stream.close();
            return defineClass( null, bytes, 0, bytes.length );
		}
    }
}

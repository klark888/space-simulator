package spcsim;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;

/*
 * @author K. A. F.
 * 
 * Description: The class that supports the main window gui of the program
 * Created: 8-27-21
 * Status: environment class, wip
 * Dependencies: Config, CustomLayout, EditorPane, Environment, GUIHandler, SpaceObject, Units
 */

public class MainFrame extends JFrame {
	
	private static final long serialVersionUID = 472314064308238191L;
	
	
	//constructor
	public MainFrame( Environment environment, Config config ) {
		//creates frame and adds environment and config components to screen
		super( "Space Simulator" );
		super.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		super.setBounds( (int)( GUIHandler.getScreenWidth() * 0.1 ), (int)( GUIHandler.getScreenHeight() * 0.1 ), 
				(int)( GUIHandler.getScreenWidth() * 0.7 ), (int)( GUIHandler.getScreenHeight() * 0.7 ) );
		super.setBackground( Color.DARK_GRAY );
		EditorPane editPane = new EditorPane( environment );
		super.add( environment );
		super.add( editPane );
		//sets icon image
		try {
			InputStream stream = ClassLoader.getSystemResourceAsStream( "spcsim/icon.png" );
			super.setIconImage( ImageIO.read( stream ) );
			stream.close();
		} catch( IOException e ) {  }
		String confirmMessage = "Do you want to destroy changes?";
		
		
		//initialize actionlisteners and menu item labels for asset menu
		String[] files = new File( "assets" ).list( ( file, name ) -> {
			return name.endsWith( "." + SpaceObject.SHORTHAND );
		} );
		ActionListener[] assetListeners = new ActionListener[ files.length ];
		for( int i = 0; i < files.length; i++ ) {
			String file = files[i];
			files[i] = file.substring( 0, file.length() - SpaceObject.SHORTHAND.length() - 1 );
			assetListeners[i] = ( a ) -> {
				try {
					SpaceObject[] objList = SpaceObject.readFile( "assets/" + file );
					double x = environment.getXPosition();
					double y = environment.getYPosition();
					for( SpaceObject obj : objList ) {
						obj.setXPosition( obj.getXPosition() + x );
						obj.setYPosition( obj.getYPosition() + y );
					}
					environment.queueOperation( ( list ) -> {
						list.addAll( Arrays.asList( objList ) );
					} );
				} catch( IOException|ClassNotFoundException|ClassCastException|NullPointerException e ) {
					GUIHandler.errorMessage( "Error opening asset" );
				}
			};
		}
		
		//initializes the rest of the actionlisteners, menu item labels, and shortcut keys for the menubar
		String[] menus = { "File", "Edit", "Simulation", "View", "Assets", "About" };
		String[][] items = { { "New", "Open", "Save", "Import", "Import Config", "Export Config", "Scripts", "Exit" }, 
			{ "Select", "Copy", "Paste", "Delete", "New Object", "Add Moon", "Random Planet" },
			{ "Start", "Stop", "Simulation Speed", "Time Step", "Tick Speed" },
			{ "Zoom In", "Zoom Out", "Default Zoom", "Time Units", "Length Units", "Mass Units", "Editor Pane", "FPS" },
			files,
			{ "Information", "GitHub" } };
		int[][] keys = { { KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_S, KeyEvent.VK_1, 0, 0, 0, 0 },
				{ 0, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_D, 0, 0, 0 },
				{ KeyEvent.VK_4, KeyEvent.VK_5, 0, 0, 0 },
				{ KeyEvent.VK_6, KeyEvent.VK_7, 0, 0, 0, 0, 0, 0 },
				new int[ files.length ],
				{ 0, 0 } };
		ActionListener[][] listeners = {
			{
				( a ) -> {
					if( GUIHandler.confirmSelection( confirmMessage ) ) {
						environment.queueOperation( ( list ) -> list.clear() );
					}
				},
				( a ) -> {
					if( GUIHandler.confirmSelection( confirmMessage ) ) {
						String path = GUIHandler.selectFile( SpaceObject.SHORTHAND, "Space Object File", "Open", false );
						if( path != null ) {
							try {
								SpaceObject[] objList = SpaceObject.readFile( path );
								environment.queueOperation( ( list ) -> {
									list.clear();
									list.addAll( Arrays.asList( objList ) );
								} );
							} catch( IOException|ClassNotFoundException|ClassCastException|NullPointerException e ) {
								GUIHandler.errorMessage( "Error opening file." );
							}
						}
					}
				},
				( a ) -> {
					String path = GUIHandler.selectFile( SpaceObject.SHORTHAND, "Space Object File", "Save", true );
					if( path != null ) {
						if( !path.endsWith( "." + SpaceObject.SHORTHAND ) ) {
							path += "." + SpaceObject.SHORTHAND;
						}
						String filePath = path;
						environment.queueOperation( ( list ) -> {
							try {
								SpaceObject.writeFile( filePath, list.toArray( new SpaceObject[ list.size() ] ) );
							} catch( IOException|NullPointerException e ) {
								GUIHandler.errorMessage( "Error saving file." );
							}
						} );
					}
				},
				( a ) -> {
					String path = GUIHandler.selectFile( SpaceObject.SHORTHAND, "Space Object File", "Import", false );
					if( path != null ) {
						try {
							SpaceObject[] objList = SpaceObject.readFile( path );
							double x = environment.getXPosition();
							double y = environment.getYPosition();
							for( SpaceObject obj : objList ) {
								obj.setXPosition( obj.getXPosition() + x );
								obj.setYPosition( obj.getYPosition() + y );
							}
							environment.queueOperation( ( list ) -> {
								list.addAll( Arrays.asList( objList ) );
							} );
						} catch( IOException|ClassNotFoundException|ClassCastException|NullPointerException e ) {
							GUIHandler.errorMessage( "Error importing file." );
						}
					}
				},
				( a ) -> {
					String path = GUIHandler.selectFile( "cfg", "Config File", "Import", false );
					if( path != null ) {
						if( !config.loadConfig( path ) ) {
							GUIHandler.errorMessage( "Error importing config." );
						}
					}
				},
				( a ) -> {
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
				( a ) -> {
					String path = GUIHandler.selectFile( "class", "Class File", "Add", false );
					if( path != null ) {
						class ExtentionLoader extends ClassLoader implements Runnable {
			                @Override
			                public void run() {
			                    try {
			                    	FileInputStream stream = new FileInputStream( path );
			                        byte[] bytes = new byte[ stream.available() ];
			                        stream.read( bytes );
			                        stream.close();
			                        Class<?> cls = defineClass( null, bytes, 0, bytes.length );
			                        Method m = cls.getMethod( "main", String[].class );
			                        m.invoke( null, (Object)new String[] { "name=spcsim.SpaceSim", "version=1.0.0" } );
			                    } catch( InvocationTargetException e ) {
			                        GUIHandler.errorMessage( "Exception in program extention.\n" + e.getTargetException() );
			                    } catch( Throwable t ) {
			                        GUIHandler.errorMessage( "Could not add program extention.\n" + t );
			                    }
			                }
			            }
						Thread thread = new Thread( new ExtentionLoader(), "extention-main" );
			            thread.setDaemon( true );
			            thread.start();
					}
				},
				( a ) -> {
					if( GUIHandler.confirmSelection( confirmMessage ) ) {
						config.saveConfig();
						System.exit( 0 );
					}
				}
			},
			{
				( a ) -> { 
					JDialog dialog = new JDialog( this, "Select Object" );
					java.awt.List slist = new java.awt.List();
					boolean[] objGet = { true };
					environment.queueOperation( ( list ) -> {
						list.forEach( ( obj ) -> {
							String name = obj.getName();
							slist.add( name == null ? "Unnamed" : name );
						} );
						objGet[0] = false;
					} );
					while( objGet[0] ) {
						try {
							Thread.sleep( 10 );
						} catch( InterruptedException e ) { }
					}
					slist.addActionListener( ( b ) -> {
						int select = slist.getSelectedIndex();
						environment.queueOperation( ( list ) -> {
							if( select < list.size() ) {
								editPane.setSelected( list.get( select ), null );
								editPane.setVisible( true );
								super.validate();
							}
						} );
						dialog.dispose();
					} );
					dialog.add( slist );
					dialog.setBounds( super.getX() + super.getWidth() / 2 - 100, super.getY() + super.getHeight() / 2 - 100, 200, 200 );
					dialog.setModal( true );
					dialog.setVisible( true );
				},
				( a ) -> editPane.copy(),
				( a ) -> environment.queueOperation( ( list ) -> {
					SpaceObject paste = editPane.paste();
					if( paste != null ) {
						paste.setXPosition( paste.getXPosition() + environment.getXPosition() );
						paste.setYPosition( paste.getYPosition() + environment.getYPosition() );
						list.add( paste );
					}
				} ),
				( a ) -> environment.queueOperation( ( list ) -> list.remove( editPane.getSelected() ) ),
				( a ) -> environment.queueOperation( ( list ) -> {
					SpaceObject obj = new SpaceObject( "New SpaceObject", 1, 1, 300, environment.getXPosition(), environment.getYPosition(), 0, 0 );
					list.add( obj );
					editPane.setSelected( obj, null );
				} ),
				( a ) -> editPane.addMoon(),
				( a ) -> {
					String name = "-";
					for( int i = 0 ; i < 3; i++ ) {
						name = (char)( Math.random() * 26 + 'A' ) + name;
						name += (int)( Math.random() * 10 );
					}
					double mass = 0.3 / ( Math.random() + 0.000075 ) - 0.03;
					double radius = mass < 6 ? Math.pow( mass, 1.0 / 3 ) : Math.pow( -1.03, -mass + 80 ) + 11;
					radius *= 0.9 + Math.random() * 0.2;
					SpaceObject obj = new SpaceObject( name, mass, radius, 100 / ( Math.random() + 0.03 ), environment.getXPosition(), environment.getYPosition(), 0, 0 );
					environment.queueOperation( ( list ) -> list.add( obj ) );
					editPane.setSelected( obj, null );
				}
			},
			{
				( a ) -> environment.setActive( true ),
				( a ) -> environment.setActive( false ),
				( a ) -> {
					try {
						String oldSpeed;
						long tickLen = environment.getTickLength();
						double step = environment.getTimeStep();
						if( tickLen == -1 ) {
							oldSpeed = "infinity";
						} else {
							oldSpeed = Units.toString( Units.TIME, step * 1000 / tickLen, environment.getTimeUnit() );
						}
						String newSpeed = GUIHandler.inquiryMessage( "Change Simulation Speed (Time passed per second):", oldSpeed );
						if( newSpeed == null ) {
							return;
						} else if( newSpeed.toLowerCase().equals( "infinity" ) ) {
							environment.setTickLength( -1 );
						} else {
							if( tickLen == -1 ) {
								tickLen = environment.getFrameLength();
								environment.setTickLength( tickLen );
							}
							environment.setTimeStep( Units.parseDouble( Units.TIME, newSpeed ) * tickLen / 1000 );
						}
					} catch( NumberFormatException e ) {
						GUIHandler.errorMessage( "Invalid number." );
					}
				},
				( a ) -> {
					try {
						double newStep = Units.parseDouble( Units.TIME, 
								GUIHandler.inquiryMessage( "Change time step per tick:", 
								Units.toString( Units.TIME, environment.getTimeStep(), environment.getTimeUnit() ) ) );
						environment.setTimeStep( newStep );
					} catch( NumberFormatException e ) {
						GUIHandler.errorMessage( "Invalid number." );
					} catch( NullPointerException e ) { }
				},
				( a ) -> {
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
			},
			{
				( a ) -> environment.setZoom( environment.getZoom() * 1.25 ),
				( a ) -> environment.setZoom( environment.getZoom() * 0.75 ),
				( a ) -> environment.setZoom( 1 ),
				( a ) -> {
					JDialog dialog = new JDialog( this, "Display Time Units" );
					java.awt.List list = new java.awt.List();
					Units.TIME.forEach( ( str, val ) -> list.add( str ) );
					list.addActionListener( ( b ) -> {
						environment.setTimeUnit( list.getSelectedItem() );
						dialog.dispose();
					} );
					dialog.add( list );
					dialog.setBounds( super.getX() + super.getWidth() / 2 - 100, super.getY() + super.getHeight() / 2 - 100, 200, 200 );
					dialog.setModal( true );
					dialog.setVisible( true );
				},
				( a ) -> {
					JDialog dialog = new JDialog( this, "Display Length Units" );
					java.awt.List list = new java.awt.List();
					Units.LENGTH.forEach( ( str, val ) -> list.add( str ) );
					list.addActionListener( ( b ) -> {
						environment.setLengthUnit( list.getSelectedItem() );
						dialog.dispose();
					} );
					dialog.add( list );
					dialog.setBounds( super.getX() + super.getWidth() / 2 - 100, super.getY() + super.getHeight() / 2 - 100, 200, 200 );
					dialog.setModal( true );
					dialog.setVisible( true );
				},
				( a ) -> {
					JDialog dialog = new JDialog( this, "Display Mass Units" );
					java.awt.List list = new java.awt.List();
					Units.MASS.forEach( ( str, val ) -> list.add( str ) );
					list.addActionListener( ( b ) -> {
						environment.setMassUnit( list.getSelectedItem() );
						dialog.dispose();
					} );
					dialog.add( list );
					dialog.setBounds( super.getX() + super.getWidth() / 2 - 100, super.getY() + super.getHeight() / 2 - 100, 200, 200 );
					dialog.setModal( true );
					dialog.setVisible( true );
				},
				( a ) -> {
					boolean val = editPane.isVisible();
					editPane.setVisible( !val );
					super.validate();
				},
				( a ) -> {
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
							GUIHandler.errorMessage( "Invalid Number." );
						}
					}
				}
			},
			assetListeners,
			{
				( a ) -> GUIHandler.regularMessage( 
							"Program: Space Simulation Program\n" + 
							"Author:  Kent F.\n" + 
							"Version: 1.0.0\n",
							"Information" ),
				( a ) -> {
					String url = "https://github.com/klark888/space-simulator";
					try {
						Desktop.getDesktop().browse( new URI( url ) );
					} catch( UnsupportedOperationException|IOException|URISyntaxException e ) {
						GUIHandler.errorMessage( "Could not open browser. Please copy link:\n" + url );
					}
				}
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
					item.setShortcut( new MenuShortcut( keys[i][k], false ) );
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
	
	
	//mutator and accessor methods
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
}

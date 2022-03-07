package spcsim;

/* Author: Kent Fukuda
 * Description: Class that stores the simulation environment including the list of SpaceObjects and simulates them
 * Created: 7-13-21
 * Status: environment class, wip
 * Dependencies: SpaceObject
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

//NOTE: may merge EditorPane and MainFrame into this class into the future
public class Environment extends Component implements Runnable {
	
	private static final long serialVersionUID = -5549313027794796626L;
	
	
	//lists of spaceobjects in the simulation
	private final List<SpaceObject> spaceObjects;
	//queue of external operations queued to the spaceObjects list
	private final List<Consumer<List<SpaceObject>>> operationQueue;
	//main thread of the environment
	private final Thread mainThread;
	//thread pool for multithreading
	private final ForkJoinPool threadPool;
	
	//notifier (EditorPane) notified by events in the simulation
	private SelectionNotifier updateNotif;
	//environment variables
	private double posX;
	private double posY;
	private double zoom;
	//mouseevent storage
	private MouseEvent lastDrag;
	private MouseEvent lastPos;
	//unit to display simulation
	private String timeUnit;
	private String lengthUnit;
	//indicates days passed in simulation
	private double daysPassed;
	//indicates the time passed per tick of simulation
	private double timeStep;
	//environment rendering settings
	private boolean showNames;
	private boolean showEnvStatus;
	//status checkers
	private long tickLength;//minumum length of each tick
	private long frameLength;//minimum length of each frame repaint
	private boolean simActive;//if simulation is active
	private boolean singleThread;//if multithreading is enabled
	
	
	//constructor
	public Environment() {
		//initializes final fields
		spaceObjects = Collections.synchronizedList( new ArrayList<>() );
		operationQueue = Collections.synchronizedList( new ArrayList<>() );
		mainThread = new Thread( this, "simulation-main" );
		threadPool = new ForkJoinPool();
		
		//initializes other fields
		posX = 0;
		posY = 0;
		zoom = 1;
		updateNotif = obj -> {};
		lastDrag = null;
		lastPos = new MouseEvent( this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 0, 0, 0, false );
		timeUnit = "days";
		lengthUnit = "au";
		daysPassed = 0;
		timeStep = 1.0 / 24;/// SpaceObject.DAY_LENGTH;//806 / SpaceObject.DAY_LENGTH;
		showNames = true;
		showEnvStatus = true;
		tickLength = 16;
		frameLength = 16;
		simActive = false;
		singleThread = true;
		
		
		super.setFocusable( true );
		mainThread.setPriority( Thread.MAX_PRIORITY );
		mainThread.setDaemon( true );
		
		//adds listeners to itself
		super.addMouseListener( new MouseListener() {
			@Override
			public void mouseClicked( MouseEvent e ) {
				Environment.super.requestFocus();
				operationQueue.add( list -> {
					for( SpaceObject obj : list ) {
						int xDiff = e.getX() - translateX( obj.getXPosition() );
						int yDiff = e.getY() - translateY( obj.getYPosition() );
						if( Math.sqrt( xDiff * xDiff + yDiff * yDiff ) < Math.max( obj.getRadius() * zoom, 5 ) ) {
							updateNotif.setSelected( obj );
							return;
						}
					}
					updateNotif.setSelected( null );
				} );
			}
			
			@Override
			public void mouseReleased( MouseEvent e ) { 
				lastDrag = null;
			}

			@Override
			public void mousePressed( MouseEvent e ) { }
			@Override
			public void mouseEntered( MouseEvent e ) { }
			@Override
			public void mouseExited( MouseEvent e ) { }
		} );
		
		super.addMouseMotionListener( new MouseMotionListener() {
			@Override
			public void mouseDragged( MouseEvent e ) {
				updateNotif.dragEvent( e );
				lastDrag = e;
				lastPos = e;
			}

			@Override
			public void mouseMoved( MouseEvent e ) {
				lastPos = e;
			}
		} );
		
		super.addMouseWheelListener( e -> {
			double mult = Math.pow( 1.1, -e.getPreciseWheelRotation() * e.getScrollAmount() );
			double x = posX - ( e.getX() - Environment.super.getWidth() / 2 ) / zoom;
			double y = posY - ( Environment.super.getHeight() / 2 - e.getY() ) / zoom;
			posX = x + ( posX - x ) * mult;
			posY = y + ( posY - y ) * mult;
			zoom = zoom * mult;
		} );
	}
	
	
	//mutator methods
	public void setUpdater( SelectionNotifier updateNotif ) {
		this.updateNotif = updateNotif;
	}
	
	public void setActive( boolean simActive ) {
		this.simActive = simActive;
	}
	
	public void setSingleThread( boolean singleThread ) {
		this.singleThread = singleThread;
	}
	
	public void setDaysPassed( double daysPassed ) {
		operationQueue.add( ( list ) -> this.daysPassed = daysPassed );
	}
	
	public void setTimeStep( double timeStep ) {
		operationQueue.add( ( list ) -> this.timeStep = timeStep );
	}
	
	public void setShowNames( boolean showNames ) {
		this.showNames = showNames;
	}
	
	public void setShowEnvStatus( boolean showEnvStatus ) {
		this.showEnvStatus = showEnvStatus;
	}
	
	public void setTickLength( long tickLength ) {
		this.tickLength = tickLength;
	}
	
	public void setFrameLength( long frameLength ) {
		this.frameLength = frameLength;
	}
	
	public void setXPosition( double posX ) {
		this.posX = posX;
	}
	
	public void setYPosition( double posY ) {
		this.posY = posY;
	}
	
	public void setZoom( double zoom ) {
		this.zoom = zoom;
	}
	
	public void setTimeUnit( String timeUnit ) {
		this.timeUnit = timeUnit;
	}
	
	public void setLengthUnit( String lengthUnit ) {
		this.lengthUnit = lengthUnit;
	}
	
	
	//accessor methods
	public Thread getMainThread() {
		return mainThread;
	}
	
	public SelectionNotifier getRepaintComponent() {
		return updateNotif;
	}
	
	public boolean getActive() {
		return simActive;
	}
	
	public boolean getSingleThread() {
		return singleThread;
	}
	
	public double getTimeStep() {
		return timeStep;
	}
	
	public boolean getShowNames() {
		return showNames;
	}
	
	public boolean getShowEnvStatus() {
		return showEnvStatus;
	}
	
	public long getTickLength() {
		return tickLength;
	}
	
	public long getFrameLength() {
		return frameLength;
	}
	
	public double getXPosition() {
		return posX;
	}
	
	public double getYPosition() {
		return posY;
	}
	
	public double getZoom() {
		return zoom;
	}
	
	public String getTimeUnit() {
		return timeUnit;
	}
	
	public String getLengthUnit() {
		return lengthUnit;
	}
	
	public MouseEvent getLastDrag() {
		return lastDrag;
	}
	
	public MouseEvent getLastPos() {
		return lastPos;
	}
	
	
	//method for translating simulation coordinates into screen coordinates
	int translateX( double x ) {
		return (int)( ( x - posX ) * zoom ) + super.getWidth() / 2;
	}

	int translateY( double y ) {
		return (int)( ( posY - y ) * zoom ) + super.getHeight() / 2;
	}
	
	
	//queues an operation to spaceObject list
	public void queueOperation( Consumer<List<SpaceObject>> operation ) {
		operationQueue.add( operation );
	}
	
	
	//overridden methods
	@Override
	public void paint( Graphics g ) {
		//paints spaceobject objects
		g.setColor( Color.GRAY );
		g.fillRect( 0, 0, super.getWidth(), super.getHeight() );
		synchronized( spaceObjects ) {
			updateNotif.render( g, spaceObjects );
			spaceObjects.forEach( ( obj ) -> {
				if( obj != null ) {
					obj.render( g, this );
				}
			} );
		}
		//paints spaceobject names
		g.setColor( Color.WHITE );
		if( showNames ) {
			g.setColor( Color.WHITE );
			spaceObjects.forEach( obj -> {
				String name;
				if( obj != null && ( name = obj.getName() ) != null ) {
					g.drawString( name, translateX( obj.getXPosition() ), translateY( obj.getYPosition() ) );
				}
			} );
		}
		//paints environment status
		if( showEnvStatus ) {
			g.drawString( "Coordinates: ( " + 
					Units.toStringRound( Units.LENGTH, posX + ( lastPos.getX() - super.getWidth() / 2 ) / zoom, lengthUnit )
					+ ", " + Units.toStringRound( Units.LENGTH, posY - ( lastPos.getY() - super.getHeight() / 2 ) / zoom, lengthUnit )
					+ ") --- Simulation Time: " + Units.toStringRound( Units.TIME, daysPassed, timeUnit )
					+ " --- Zoom Magnitude: " + (int)( zoom * 100 ) + "%", 0, 10 );
		}
	}
	
	@Override
	public void run() {
		long repaintTime = 0;
		long simTime = 0;
		long currentTime = 0;
		while( true ) {
			//for single thread simulations
			while( singleThread ) {
				currentTime = System.currentTimeMillis();
				repaintTime = paintOpQueue( currentTime, repaintTime );
				//simulates a single tick of the simulation
				if( simActive && currentTime > simTime + tickLength ) {
					int size = spaceObjects.size();
					for( int i = 0; i < size; i++ ) {
						SpaceObject obj = spaceObjects.get( i );
						for( int j = i + 1; j < size; j++ ) {
							if( obj.interact( spaceObjects.get( j ) ) ) {
								spaceObjects.remove( j );
								size--;
								j--;
							}
						}
						obj.simulate();
					}
					daysPassed += timeStep;
					simTime = currentTime;
				}
			}
			
			//for multi thread simulations
			while( !singleThread ) {
				currentTime = System.currentTimeMillis();
				repaintTime = paintOpQueue( currentTime, repaintTime );
				//simulates a single tick of the simulation
				if( simActive && currentTime > simTime + tickLength ) {
					int size = spaceObjects.size();
					for( int i = 0; i < size; i++ ) {
						SpaceObject obj = spaceObjects.get( i );
						if( obj != null ) {
							for( int j = i + 1; j < size; j++ ) {
								int id = j;
								threadPool.submit( () -> {
									SpaceObject interact = spaceObjects.get( id );
									if( interact != null && obj.interact( interact ) ) {
										spaceObjects.set( id, null );
									}
								} );
							}
						}
					}
					threadPool.awaitQuiescence( Long.MAX_VALUE, TimeUnit.DAYS );
					for( int i = 0; i < size; i++ ) {
						SpaceObject obj = spaceObjects.get( i );
						if( obj == null ) {
							spaceObjects.remove( i-- );
							size--;
						} else {
							threadPool.submit( obj::simulate );
						}
					}
					threadPool.awaitQuiescence( Long.MAX_VALUE, TimeUnit.DAYS );
					daysPassed += timeStep;
					simTime = currentTime;
				}
			}
		}
	}
	
	
	//does operations on spaceObjects and repaints
	private long paintOpQueue( long currentTime, long repaintTime ) {
		if( currentTime > repaintTime + frameLength ) {
			super.repaint();
			if( !operationQueue.isEmpty() ) {
				synchronized( spaceObjects ) {
					do {
						operationQueue.remove( 0 ).accept( spaceObjects );
					} while( !operationQueue.isEmpty() );
				}
				spaceObjects.forEach( obj -> obj.updateTimeStep( timeStep ) );
			}
			repaintTime = currentTime;
		}
		return repaintTime;
	}
	
	
	//interface used to listen to the various events happening in the simulation environment
	public static interface SelectionNotifier {
		public void setSelected( SpaceObject obj );
		public default void dragEvent( MouseEvent current ) {};
		public default void render( Graphics g, List<SpaceObject> list ) {};
	}
}
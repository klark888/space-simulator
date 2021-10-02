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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

//NOTE: may merge EditorPane and MainFrame into this class into the future
public class Environment extends Component implements Runnable {
	
	private static final long serialVersionUID = -5549313027794796626L;
	
	
	//lists of spaceobjects in the simulation
	private final List<SpaceObject> spaceObjects;
	//queue of external operations queued to the spaceObjects list
	private final List<Consumer<List<SpaceObject>>> operationQueue;
	//queue of operations the worker threads do (only used during multithread)
	private final List<Runnable> workerQueue;
	//objects to lock to indicate the worker thread is currently processing items
	private final List<Object> threadLockers;
	//environment information for the simulation
	private final SpaceObject.EnvInfo envInfo;
	//main thread of the environment
	private final Thread mainThread;
	
	//notifier (EditorPane) notified by events in the simulation
	private SelectionNotifier updateNotif;
	//mouseevent storage
	private MouseEvent lastDrag;
	private MouseEvent lastPos;
	//unit to display simulation
	private String timeUnit;
	private String lengthUnit;
	private String massUnit;
	private String tempUnit;
	private String speedUnit;
	private String degUnit;
	//indicates days passed in simulation
	private double daysPassed;
	//indicates the time passed per tick of simulation
	private double timeStep;
	//status checkers
	private long tickLength;//minumum length of each tick
	private long frameLength;//minimum length of each frame repaint
	private int numThreads;//number of worker threads
	private boolean simActive;//if simulation is active
	private boolean validRunner;//if current control thread is valid
	
	
	//constructor
	public Environment() {
		//initializes final fields
		spaceObjects = Collections.synchronizedList( new ArrayList<>() );
		operationQueue = Collections.synchronizedList( new ArrayList<>() );
		workerQueue = Collections.synchronizedList( new ArrayList<>() );
		threadLockers = Collections.synchronizedList( new ArrayList<>() );
		envInfo = new SpaceObject.EnvInfo();
		mainThread = new Thread( this, "simulation-main" );
		
		//initializes other fields
		envInfo.posX = 0;
		envInfo.posY = 0;
		envInfo.zoom = 1;
		updateNotif = ( obj, envInfo ) -> {};
		lastDrag = null;
		lastPos = new MouseEvent( this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 0, 0, 0, false );
		timeUnit = "days";
		lengthUnit = "au";
		massUnit = "earth masses";
		tempUnit = "kelvin";
		speedUnit = "km/s";
		degUnit = "degrees";
		daysPassed = 0;
		timeStep = 1.0 / 24;/// SpaceObject.DAY_LENGTH;//806 / SpaceObject.DAY_LENGTH;
		tickLength = 16;
		frameLength = 16;
		numThreads = 0;
		simActive = false;
		validRunner = true;
		
		
		super.setFocusable( true );
		mainThread.setPriority( Thread.NORM_PRIORITY );
		
		//adds listeners to itself
		mainThread.setUncaughtExceptionHandler( ( thread, throwable ) -> {
			synchronized( this ) {
				numThreads = 0;
			}
			throwable.printStackTrace();
		} );
		
		super.addComponentListener( new ComponentListener() {
			@Override
			public void componentResized( ComponentEvent e ) {
				envInfo.centerX = Environment.super.getWidth() / 2;
				envInfo.centerY = Environment.super.getHeight() / 2;
			}
			
			@Override
			public void componentMoved( ComponentEvent e ) { }
			@Override
			public void componentShown( ComponentEvent e ) { }
			@Override
			public void componentHidden( ComponentEvent e ) { }
		} );
		
		super.addMouseListener( new MouseListener() {
			@Override
			public void mouseClicked( MouseEvent e ) {
				Environment.super.requestFocus();
				operationQueue.add( ( list ) -> {
					for( SpaceObject obj : list ) {
						int xDiff = e.getX() - envInfo.translateX( obj.getXPosition() );
						int yDiff = e.getY() - envInfo.translateY( obj.getYPosition() );
						if( Math.sqrt( xDiff * xDiff + yDiff * yDiff ) < Math.max( obj.getRadius() * envInfo.zoom, 5 ) ) {
							updateNotif.setSelected( obj, envInfo );
							return;
						}
					}
					updateNotif.setSelected( null, envInfo );
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
				updateNotif.dragEvent( e, envInfo );
				lastDrag = e;
				lastPos = e;
			}

			@Override
			public void mouseMoved( MouseEvent e ) {
				lastPos = e;
			}
		} );
		
		super.addMouseWheelListener( ( e ) -> {
			double mult = Math.pow( 1.1, -e.getPreciseWheelRotation() * e.getScrollAmount() );
			double x = envInfo.posX - ( e.getX() - envInfo.centerX ) / envInfo.zoom;
			double y = envInfo.posY - ( envInfo.centerY - e.getY() ) / envInfo.zoom;
			envInfo.posX = x + ( envInfo.posX - x ) * mult;
			envInfo.posY = y + ( envInfo.posY - y ) * mult;
			envInfo.zoom = envInfo.zoom * mult;
		} );
	}
	
	
	//mutator methods
	public void setUpdater( SelectionNotifier updateNotif ) {
		this.updateNotif = updateNotif;
	}
	
	public void setActive( boolean simActive ) {
		this.simActive = simActive;
	}
	
	public void setTimeStep( double timeStep ) {
		operationQueue.add( ( list ) -> this.timeStep = timeStep );
	}
	
	public void setTickLength( long tickLength ) {
		this.tickLength = tickLength;
	}
	
	public void setFrameLength( long frameLength ) {
		this.frameLength = frameLength;
	}
	
	public void setZoom( double zoom ) {
		envInfo.zoom = zoom;
	}
	
	public synchronized void setNumThreads( int num ) {
		if( num >= 0 ) {
			int prev = numThreads;
			numThreads = num;
			if( ( prev == 0 && num > 0 ) || ( prev > 0 && num == 0 ) ) {
				validRunner = false;
			}
			for( int i = prev; i < num; i++ ) {
				int id = i;
				Thread thread = new Thread( () -> workerRun( id ), "simulation-worker-" + i );
				thread.setPriority( Thread.NORM_PRIORITY );
				thread.start();
			}
		} else {
			throw new IllegalArgumentException( "Number of threads must be positive or 0" );
		}
	}
	
	public void setTimeUnit( String timeUnit ) {
		this.timeUnit = timeUnit;
	}
	
	public void setLengthUnit( String lengthUnit ) {
		this.lengthUnit = lengthUnit;
	}
	
	public void setMassUnit( String massUnit ) {
		this.massUnit = massUnit;
	}
	
	public void setTempUnit( String tempUnit ) {
		this.tempUnit = tempUnit;
	}
	
	public void setSpeedUnit( String speedUnit ) {
		this.speedUnit = speedUnit;
	}
	
	public void setDegUnit( String degUnit ) {
		this.degUnit = degUnit;
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
	
	public double getTimeStep() {
		return timeStep;
	}
	
	public long getTickLength() {
		return tickLength;
	}
	
	public long getFrameLength() {
		return frameLength;
	}
	
	public double getXPosition() {
		return envInfo.posX;
	}
	
	public double getYPosition() {
		return envInfo.posY;
	}
	
	public double getZoom() {
		return envInfo.zoom;
	}
	
	public int getNumThreads() {
		return numThreads;
	}
	
	public String getTimeUnit() {
		return timeUnit;
	}
	
	public String getLengthUnit() {
		return lengthUnit;
	}
	
	public String getMassUnit() {
		return massUnit;
	}
	
	public String getTempUnit() {
		return tempUnit;
	}
	
	public String getSpeedUnit() {
		return speedUnit;
	}
	
	public String getDegUnit() {
		return degUnit;
	}
	
	public MouseEvent getLastDrag() {
		return lastDrag;
	}
	
	public MouseEvent getLastPos() {
		return lastPos;
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
		g.fillRect( 0, 0, envInfo.centerX * 2, envInfo.centerY * 2 );
		synchronized( spaceObjects ) {
			updateNotif.render( g, spaceObjects, envInfo );
			spaceObjects.forEach( ( obj ) -> {
				if( obj != null ) {
					obj.render( g, envInfo );
				}
			} );
		}
		//paints environment status
		g.setColor( Color.WHITE );
		g.drawString( "Coordinates: ( " + 
				Units.toStringR( Units.LENGTH, envInfo.posX + ( lastPos.getX() - envInfo.centerX ) / envInfo.zoom, lengthUnit )
				+ ", " + Units.toStringR( Units.LENGTH, envInfo.posY - ( lastPos.getY() - envInfo.centerY ) / envInfo.zoom, lengthUnit )
				+ ") --- Simulation Time: " + Units.toStringR( Units.TIME, daysPassed, timeUnit )
				+ " --- Zoom Magnitude: " + (int)( envInfo.zoom * 100 ) + "%", 0, 10 );
	}
	
	@Override
	public void run() {
		long repaintTime = 0;
		long simTime = 0;
		long currentTime = 0;
		while( true ) {
			validRunner = true;
			if( numThreads == 0 ) {
				//for single thread simulations
				while( validRunner ) {
					currentTime = System.currentTimeMillis();
					//does operations on spaceObjects and repaints
					if( currentTime > repaintTime + frameLength ) {
						super.repaint();
						opQueue();
						repaintTime = currentTime;
					}
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
				
			} else {
				//for multi thread simulations
				while( validRunner ) {
					currentTime = System.currentTimeMillis();
					//does operations on spaceObjects list and repaints
					if( currentTime > repaintTime + frameLength ) {
						super.repaint();
						opQueue();
						repaintTime = currentTime;
					}
					//simulates a single tick of the simulation
					if( simActive && currentTime > simTime + tickLength ) {
						//breaks down interaction tasks into smaller tasks and pushes them into the workerQueue
						int listSize = spaceObjects.size();
						int halfSize = listSize / 2;
						int threads = numThreads;
						for( int i = 0; i < threads; i++ ) {
							int start = i;
							workerQueue.add( () -> {
								for( int j = start; j < halfSize; j += threads ) {
									interact( j );
									interact( listSize - j - 1 );
								}
							} );
						}
						if( listSize % 2 == 1 ) {
							workerQueue.add( () -> {
								interact( halfSize );
								simulate( halfSize );
							} );
						}
						//independent simulation tasks broken up and put to worker queue
						for( int i = 0; i < threads; i++ ) {
							int start = i;
							workerQueue.add( () -> {
								for( int j = start; j < listSize; j += threads ) {
									simulate( j );
								}
							} );
						}
						//waits until worker queue is empty for next tick
						while( !workerQueue.isEmpty() );
						threadLockers.forEach( ( locker ) -> { synchronized( locker ) { } } );
						spaceObjects.removeIf( ( obj ) -> obj == null );
						daysPassed += timeStep;
						simTime = currentTime;
					}
				}
			}
		}
	}
	
	
	//private utility methods
	//interact spaceObjects.get( i ) and subsequent spaceobjects in that list
	private void interact( int i ) {
		SpaceObject obj1 = spaceObjects.get( i );
		if( obj1 != null ) {
			for( int j = i + 1; j < spaceObjects.size(); j++ ) {
				SpaceObject obj2 = spaceObjects.get( j );
				if( obj2 != null && obj1.interact( obj2 ) ) {
					spaceObjects.set( j, null );
				}
			}
		}
	}
	
	//simulate spaceObjects.get( i )
	private void simulate( int i ) {
		SpaceObject obj = spaceObjects.get( i );
		if( obj != null ) {
			obj.simulate();
		}
	}
	
	//does all operations queued in the operation queue
	private void opQueue() {
		if( !operationQueue.isEmpty() ) {
			do {
				try {
					operationQueue.remove( 0 ).accept( spaceObjects );
				} catch( ThreadDeath e ) {
					synchronized( this ) {
						numThreads = 0;
					}
					throw e;
				} catch( Throwable t ) {
					t.printStackTrace();
				}
			} while( !operationQueue.isEmpty() );
			spaceObjects.forEach( ( obj ) -> {
				obj.updateTimeStep( timeStep );
			} );
		}
	}
	
	//method for worker threads
	private void workerRun( int id ) {
		Object threadLocker = new Object();
		threadLockers.add( threadLocker );
		while( id < numThreads ) {
			Runnable r = null;
			synchronized( workerQueue ) {
				if( !workerQueue.isEmpty() ) {
					r = workerQueue.remove( 0 );
				}
			}
			if( r != null ) {
				synchronized( threadLocker ) {
					r.run();
				}
			}
		}
		threadLockers.remove( threadLocker );
	}
	
	
	//interface used to listen to the various events happening in the simulation environment
	public static interface SelectionNotifier {
		public void setSelected( SpaceObject obj, SpaceObject.EnvInfo env );
		public default void dragEvent( MouseEvent current, SpaceObject.EnvInfo env ) {};
		public default void render( Graphics g, List<SpaceObject> list, SpaceObject.EnvInfo env ) {};
	}
}
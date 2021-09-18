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
	//queue of external operations cast onto spaceObjects list
	private final List<Consumer<List<SpaceObject>>> operationQueue;
	//environment information for the simulation
	private final SpaceObject.EnvInfo envInfo;
	//notifier (EditorPane) notified by events in the simulation
	private SelectionNotifier updateNotif;
	//other internal variables
	private MouseEvent lastDrag;
	private MouseEvent lastPos;
	private String timeUnit;
	private String lengthUnit;
	private String massUnit;
	private String tempUnit;
	private String speedUnit;
	private String degUnit;
	private double daysPassed;
	private long tickLength;
	private long frameLength;
	private boolean simActive;
	
	
	//constructor
	public Environment() {
		//initializes final fields
		spaceObjects = Collections.synchronizedList( new ArrayList<>() );
		operationQueue = Collections.synchronizedList( new ArrayList<>() );
		envInfo = new SpaceObject.EnvInfo();
		envInfo.timeStep = 1.0 / 24;/// SpaceObject.DAY_LENGTH;//806 / SpaceObject.DAY_LENGTH;
		envInfo.posX = 0;
		envInfo.posY = 0;
		envInfo.zoom = 1;
		//initializes other fields
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
		tickLength = 16;
		frameLength = 16;
		simActive = false;
		
		super.setFocusable( true );
		
		//adds listeners to itself
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
		operationQueue.add( ( list ) -> envInfo.timeStep = timeStep );
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
	public SelectionNotifier getRepaintComponent() {
		return updateNotif;
	}
	
	public boolean getActive() {
		return simActive;
	}
	
	public double getTimeStep() {
		return envInfo.timeStep;
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
	
	//overriddden methods
	@Override
	public void paint( Graphics g ) {
		//paints spaceobject objects
		g.setColor( Color.GRAY );
		g.fillRect( 0, 0, envInfo.centerX * 2, envInfo.centerY * 2 );
		synchronized( spaceObjects ) {
			updateNotif.render( g, spaceObjects, envInfo );
			spaceObjects.forEach( ( obj ) -> {
				obj.render( g, envInfo );
			} );
		}
		//paints environment status
		g.setColor( Color.WHITE );
		g.drawString( "Coordinates: ( " + 
				Units.toStringR( Units.LENGTH, envInfo.posX + ( lastPos.getX() - envInfo.centerX ) / envInfo.zoom, lengthUnit )
				+ ", " + Units.toStringR( Units.LENGTH, envInfo.posY - ( lastPos.getY() - envInfo.centerY ) / envInfo.zoom, lengthUnit )
				+ ") --- Simulation Time: " + Units.toStringR( Units.TIME, daysPassed, timeUnit ), 0, 10 );
	}

	@Override
	public void run() {
		long repaintTime = 0;
		long simTime = 0;
		long currentTime = 0;
		while( true ) {
			currentTime = System.currentTimeMillis();
			//does operations on spaceObjects list in the operationQueue
			if( currentTime > repaintTime + frameLength ) {
				super.repaint();
				while( !operationQueue.isEmpty() ) {
					operationQueue.remove( 0 ).accept( spaceObjects );
				}
				repaintTime = currentTime;
			}
			//simulates a single tick of the simulation
			if( simActive && currentTime > simTime + tickLength ) {
				for( envInfo.numID = 0; envInfo.numID < spaceObjects.size(); envInfo.numID++ ) {
					spaceObjects.get( envInfo.numID ).simulate( spaceObjects, envInfo );
				}
				daysPassed += envInfo.timeStep;
				simTime = currentTime;
			}
		}
	}
	
	
	
	//interface used to listen to the various events happening in the simulation environment
	public static interface SelectionNotifier {
		public void setSelected( SpaceObject obj, SpaceObject.EnvInfo env );
		public default void dragEvent( MouseEvent current, SpaceObject.EnvInfo env ) {};
		public default void render( Graphics g, List<SpaceObject> list, SpaceObject.EnvInfo env ) {};
	}
}
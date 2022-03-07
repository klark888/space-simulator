package spcsim;

/* Author: Kent Fukuda
 * Description: Component that edits the SpaceObjects in a particle Environment
 * Created: 8-29-21
 * Status: environment class, finished
 * Dependencies: CustomLayout, Environment, SpaceObject, Units
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JDialog;


public class EditorPane extends Container implements Environment.SelectionNotifier {

	private static final long serialVersionUID = -1316866962463841140L;

	
	//environment the editor pane mutates
	private final Environment environment;
	//component containing textfields used to manipulate fields in a spaceobject
	private final Container container;
	//default object when not selected
	private final SpaceObject nullSelect;
	//list of textfields to update
	private final ArrayList<Runnable> fieldUpdate;
	//list of buttons to update
	private final Button[] unitButtons;
	//units for editorpane display
	private final String[] units;
	//current object being edited
	private SpaceObject selectedObject;
	//environment variables
	private boolean isVelocDragMode;
	private boolean addMoonMode;
	
	
	//constructor
	public EditorPane( Frame frame, Environment env ) {
		//initiates fields
		environment = env;
		container = new Container();
		nullSelect = new SpaceObject();
		fieldUpdate = new ArrayList<>();
		unitButtons = new Button[9];
		units = new String[]{ "earth masses", "kilometers", "celsius", "au", "au", "km/s", "degrees", "km/s", "km/s" };
		selectedObject = new SpaceObject( "Default Planet", 5.5171459763102915, 3.745108970856094, 763.604205392109 );
		isVelocDragMode = false;
		addMoonMode = false;
		Label label = new Label( "Editor Pane" );
		ScrollPane pane = new ScrollPane();
		
		//adds object mutation textfields to container
		createEdit( "Name", v -> selectedObject.setName( v ), () -> selectedObject.getName() );
		createEdit( "Mass", frame, Units.MASS, 0, v -> selectedObject.setMass( v ), () -> selectedObject.getMass() );
		createEdit( "Radius", frame, Units.LENGTH, 1, v -> selectedObject.setRadius( v ), () -> selectedObject.getRadius() );
		createEdit( "Temperature", frame, Units.TEMPERATURE, 2, v -> selectedObject.setTemperature( v ), () -> selectedObject.getTemperature() );
		createEdit( "X - Position", frame, Units.LENGTH, 3, v -> selectedObject.setXPosition( v ), () -> selectedObject.getXPosition() );
		createEdit( "Y - Position", frame, Units.LENGTH, 4, v -> selectedObject.setYPosition( v ), () -> selectedObject.getYPosition() );
		createEdit( "Speed", frame, Units.SPEED, 5, v -> {
			double deg = Math.atan2( selectedObject.getYVelocity(), selectedObject.getXVelocity() );
			selectedObject.setXVelocity( v * Math.cos( deg ) );
			selectedObject.setYVelocity( v * Math.sin( deg ) );
		}, () -> {
			double xVeloc = selectedObject.getXVelocity();
			double yVeloc = selectedObject.getYVelocity();
			return Math.sqrt( xVeloc * xVeloc + yVeloc * yVeloc );
		} );
		createEdit( "Direction", frame, Units.DEGREE, 6,  v -> {
			double xVeloc = selectedObject.getXVelocity();
			double yVeloc = selectedObject.getYVelocity();
			double mult = Math.sqrt( xVeloc * xVeloc + yVeloc * yVeloc );
			selectedObject.setXVelocity( mult * Math.cos( v ) );
			selectedObject.setYVelocity( mult * Math.sin( v ) );
		}, () -> Math.atan2( selectedObject.getYVelocity(), selectedObject.getXVelocity() ) );
		createEdit( "X - Velocity", frame, Units.SPEED, 7, v -> selectedObject.setXVelocity( v ), () -> selectedObject.getXVelocity() );
		createEdit( "Y - Velocity", frame, Units.SPEED, 8, v -> selectedObject.setYVelocity( v ), () -> selectedObject.getYVelocity() );
		
		//click-focus mouse listener
		MouseListener requestFocus = new MouseListener() {
			@Override
			public void mouseClicked( MouseEvent e ) { 
				e.getComponent().requestFocus();
			}
			@Override
			public void mousePressed( MouseEvent e ) { }
			@Override
			public void mouseReleased( MouseEvent e ) { }
			@Override
			public void mouseEntered( MouseEvent e ) { }
			@Override
			public void mouseExited( MouseEvent e ) { } 
		};
		
		//link fields and other components
		for( Component c : container.getComponents() ) {
			c.setBackground( Color.DARK_GRAY );
			c.setForeground( Color.WHITE );
			c.addMouseListener( requestFocus );
		}
		label.setBackground( Color.DARK_GRAY );
		label.setForeground( Color.WHITE );
		pane.setBackground( Color.DARK_GRAY );
		pane.setForeground( Color.WHITE );
		pane.add( container );
		super.add( label );
		super.add( pane );
		super.addMouseListener( requestFocus );
		container.addMouseListener( requestFocus );
		
		//sets layout of container and this container 
		super.setLayout( new CustomLayout( () -> {
			Dimension d = EditorPane.super.getSize();
			label.setBounds( 0, 0, d.width, 20 );
			pane.setBounds( 0, 20, d.width, d.height - 20 );
		} ) );
		container.setLayout( new CustomLayout( null ) {
			@Override
			public void layoutContainer( Container parent ) {
				int w = container.getWidth();
				for( int i = 0 ; i < container.getComponentCount(); i++ ) {
					container.getComponent( i ).setBounds( 0, i * 45, w, 45 );
				}
			}

			@Override
			public Dimension minimumLayoutSize( Container parent ) {
				return new Dimension( 0, container.getComponentCount() * 45 );
			}
		} );
		
		environment.setUpdater( this );
		environment.queueOperation( ( list ) -> list.add( selectedObject ) );
	}
	
	
	//gets currently selected object
	public SpaceObject getSelected() {
		return selectedObject == nullSelect ? null : selectedObject;
	}
	
	//adds moon to currently selected object
	public void addMoon() {
		addMoonMode = selectedObject != nullSelect;
	}
	
	
	//units accessor and mutator for config
	public void setUnits( String units ) {
		String[] uArr = units.split( "," );
		System.arraycopy( uArr, 0, this.units, 0, uArr.length );
		for( int i = 0; i < unitButtons.length; i++ ) {
			unitButtons[i].setLabel( this.units[i] );
		}
	}
	
	public String getUnits() {
		StringBuilder bldr = new StringBuilder();
		for( String unit : units ) {
			bldr.append( unit ).append( "," );
		}
		return bldr.substring( 0, bldr.length() - 1 );
	}
	
	
	//overridden methods
	@Override
	public void setSelected( SpaceObject obj ) {
		if( addMoonMode ) {
			//creates a moon of selectedObject
			MouseEvent loc = environment.getLastPos();
			double zoom = environment.getZoom();
			SpaceObject moon = new SpaceObject( "Moon of " + selectedObject.getName(), 
					selectedObject.getMass() / 100, selectedObject.getRadius() / 5, selectedObject.getTemperature(),
					environment.getXPosition() + ( loc.getX() - environment.getWidth() / 2 ) / zoom, 
					environment.getYPosition() - ( loc.getY() - environment.getHeight() / 2 ) / zoom, 0, 0 );
			environment.queueOperation( list -> {
				SpaceObject.orbit( selectedObject, moon );
				list.add( moon );
			} );
		} else {
			//selects new object
			selectedObject = obj == null ? nullSelect : obj;
		}
		addMoonMode = false;
	}
	
	@Override
	public void dragEvent( MouseEvent current ) {
		MouseEvent previous = environment.getLastDrag();
		double zoom = environment.getZoom();
		if( previous != null ) {
			double dragX = -( current.getX() - previous.getX() ) / zoom;
			double dragY = ( current.getY() - previous.getY() ) / zoom;
			if( nullSelect != selectedObject ) {
				if( isVelocDragMode ) {
					//drags the velocity vector of selectedObject
					double mult = Math.sqrt( zoom );
					selectedObject.setXVelocity( selectedObject.getXVelocity()  - dragX * mult );
					selectedObject.setYVelocity( selectedObject.getYVelocity() - dragY * mult );
				} else {
					//drags the position of selectedObject
					selectedObject.setXPosition( selectedObject.getXPosition() + dragX );
					selectedObject.setYPosition( selectedObject.getYPosition() + dragY );
				}
			} else {
				//drags the position of camera
				environment.setXPosition( environment.getXPosition() + dragX );
				environment.setYPosition( environment.getYPosition() + dragY );
			}
		} else if( nullSelect != selectedObject ) {
			//determines if the current mouse dragging movement is dragging the velocity vector arrow or position
			double mult = Math.sqrt( zoom );
			int xDiff = (int)( selectedObject.getXVelocity() * mult ) + environment.getWidth() / 2 - current.getX();
			int yDiff = -(int)( selectedObject.getYVelocity() * mult ) + environment.getHeight() / 2 - current.getY();
			isVelocDragMode = Math.sqrt( xDiff * xDiff + yDiff * yDiff ) < 20;
		}
	}
	
	@Override
	public void render( Graphics g, List<SpaceObject> spaceObjects ) {
		selectedObject = spaceObjects.contains( selectedObject ) ? selectedObject : nullSelect;
		if( selectedObject != nullSelect ) {
			//renders velocity vector
			environment.setXPosition( selectedObject.getXPosition() );
			environment.setYPosition( selectedObject.getYPosition() );
			int centerX = environment.getWidth() / 2;
			int centerY = environment.getHeight() / 2;
			double mult = Math.sqrt( environment.getZoom() );
			int velX = (int)( selectedObject.getXVelocity() * mult ) + centerX;
			int velY = -(int)( selectedObject.getYVelocity() * mult ) + centerY;
			g.setColor( Color.YELLOW );
			g.drawLine( centerX, centerY, velX, velY );
			g.drawOval( velX - 10, velY - 10, 20, 20 );
			//renders moon orbit
			if( addMoonMode ) {
				MouseEvent pos = environment.getLastPos();
				int mx = centerX - pos.getX();
				int my = centerY - pos.getY();
				int dist = (int)Math.sqrt( mx * mx + my * my );
				g.setColor( Color.WHITE );
				g.drawOval( -dist + centerX, -dist + centerY, dist * 2, dist * 2 );
			}
		}
		//updates container's textfields to show correct values
		if( super.isVisible() ) {
			fieldUpdate.forEach( r -> r.run() );
		}
	}
	
	

	//private methods for intializing edit pane components
	private Container createEdit( String name, Consumer<String> in, Supplier<String> out ) {
		TextField field = new TextField();
		Container cnt = new Container();
		Label label = new Label( name + ": " );
		cnt.add( label );
		field.setBackground( Color.DARK_GRAY );
		field.addActionListener( a -> {
			try {
				in.accept( field.getText() );
			} catch( NumberFormatException e ) {
				field.setText( out.get() );
			}
		} );
		field.addFocusListener( new FocusListener() {
			@Override
			public void focusGained( FocusEvent e ) {
				field.setBackground( Color.BLACK );
			}
			@Override
			public void focusLost( FocusEvent f ) {
				try {
					in.accept( field.getText() );
				} catch( NumberFormatException e ) {
					field.setText( out.get() );
				}
				field.setBackground( Color.DARK_GRAY );
			}
		} );
		cnt.add( field );
		cnt.setLayout( new CustomLayout( () -> {
			int w = cnt.getWidth();
			int h = ( cnt.getHeight() - 5 ) / 2;
			cnt.getComponent( 0 ).setBounds( 0, 0, w, h );
			int len = cnt.getComponentCount();
			int sz = w / ( len - 1 );
			for( int i = 1; i < len; i++ ) {
				cnt.getComponent( i ).setBounds( sz * ( i - 1 ), h, sz, h );
			}
		} ) );
		fieldUpdate.add( () -> {
			if( !field.isFocusOwner() ) {
				field.setText( out.get() );
			}
		} );
		container.add( cnt );
		return cnt;
	}
	
	//create field for editing spaceobjects
	private void createEdit( String name, Frame frame, 
			Map<String,BiFunction<Double,Boolean,Double>> map, int id, Consumer<Double> in, Supplier<Double> out ) {
		Container cnt = createEdit( name, str -> in.accept( Units.parseDouble( map, str, units[id] ) ), 
				() -> Units.toString( map, out.get(), units[id] ) );
		Button button = new Button( units[id] );
		button.setBackground( Color.DARK_GRAY );
		JDialog dialog = createUnitDialog( frame, str -> {
			units[id] = str;
			button.setLabel( str );
		}, map );
		unitButtons[id] = button;
		button.addActionListener( a -> dialog.setVisible( true ) );
		cnt.add( button );
		cnt.addPropertyChangeListener( "units", p -> button.setLabel( units[id] ) );
	}
	
	
	//static utility
	//function for creating unit dialog selector
	public static JDialog createUnitDialog( Frame frame, Consumer<String> change, Map<String,BiFunction<Double,Boolean,Double>> map ) {
		JDialog dialog = new JDialog( frame, "Select Units" );
		java.awt.List list = new java.awt.List();
		map.forEach( ( name, val ) -> list.add( name ) );
		list.addActionListener( b -> {
			String unit = list.getSelectedItem();
			if( unit != null ) {
				change.accept( unit );
			}
			dialog.setVisible( false );
		} );
		dialog.add( list );
		dialog.setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
		dialog.setBounds( frame.getX() + frame.getWidth() / 2 - 100, frame.getY() + frame.getHeight() / 2 - 100, 200, 200 );
		dialog.setModal( true );
		dialog.setResizable( false );
		return dialog;
	}
}

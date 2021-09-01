package spcsim;

/* Author: Kent Fukuda
 * Description: Component that edits the SpaceObjects in a particle Environment
 * Created: 8-29-21
 * Status: environment class, finished
 * Dependencies: CustomLayout, Environment, SpaceObject, Units
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class EditorPane extends Container implements Environment.SelectionNotifier {

	private static final long serialVersionUID = -1316866962463841140L;

	
	//environment the editor pane mutates
	private final Environment environment;
	//component containing textfields used to manipulate fields in a spaceobject
	private final Container container;
	//current object being edited
	private SpaceObject selectedObject;
	//current object in pastebin
	private SpaceObject copied;
	//environment variables
	private boolean isNullSelect;
	private boolean isVelocDragMode;
	private boolean addMoonMode;
	
	
	//constructor
	public EditorPane( Environment env ) {
		//initiates fields
		environment = env;
		container = new Container();
		selectedObject = new SpaceObject( "Default Planet", 5.5171459763102915, 3.745108970856094, 763.604205392109 );
		copied = null;
		isNullSelect = false;
		isVelocDragMode = false;
		addMoonMode = false;
		Label label = new Label( "Editor Pane" );
		ScrollPane pane = new ScrollPane();
		
		//ads object mutation textfields to container
		container.add( new Label( "Name: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setName( t ), 
				() -> selectedObject.getName() ) );
		container.add( new Label( "Mass: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setMass( Units.parseDouble( Units.MASS, t ) ), 
				() -> Units.toString( Units.MASS, selectedObject.getMass(), environment.getMassUnit() ) ) );
		container.add( new Label( "Radius: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setRadius( Units.parseDouble( Units.LENGTH, t ) ), 
				() -> Units.toString( Units.LENGTH, selectedObject.getRadius(), environment.getLengthUnit() ) ) );
		container.add( new Label( "Temperature: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setTemperature( Double.parseDouble( t.substring( 0, t.length() - 6 ).strip() ) ), 
				() -> selectedObject.getTemperature() + " Kelvin" ) );
		container.add( new Label( "X - Position: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setXPosition( Units.parseDouble( Units.LENGTH, t ) ), 
				() -> Units.toString( Units.LENGTH, selectedObject.getXPosition(), environment.getLengthUnit() ) ) );
		container.add( new Label( "Y - Position: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setYPosition( Units.parseDouble( Units.LENGTH, t ) ), 
				() -> Units.toString( Units.LENGTH, selectedObject.getYPosition(), environment.getLengthUnit() ) ) );
		container.add( new Label( "Speed: " ) );
		container.add( createEditField( ( t ) -> {
			double mult = Units.parseDouble( Units.LENGTH, t );
			double deg = Math.atan2( selectedObject.getYVelocity(), selectedObject.getXVelocity() );
			selectedObject.setXVelocity( mult * Math.cos( deg ) );
			selectedObject.setYVelocity( mult * Math.sin( deg ) );
		}, () -> {
			double xVeloc = selectedObject.getXVelocity();
			double yVeloc = selectedObject.getYVelocity();
			return Units.toString( Units.LENGTH, Math.sqrt( xVeloc * xVeloc + yVeloc * yVeloc ), environment.getLengthUnit() );
		} ) );
		container.add( new Label( "Direction: " ) );
		container.add( createEditField( ( t ) -> {
			double deg = Math.PI / 180 * Double.parseDouble( t );
			double xVeloc = selectedObject.getXVelocity();
			double yVeloc = selectedObject.getYVelocity();
			double mult = Math.sqrt( xVeloc * xVeloc + yVeloc * yVeloc );
			selectedObject.setXVelocity( mult * Math.cos( deg ) );
			selectedObject.setYVelocity( mult * Math.sin( deg ) );
		}, () -> {
			return Double.toString( 180 / Math.PI * Math.atan2( selectedObject.getYVelocity(), selectedObject.getXVelocity() ) );
		} ) );
		container.add( new Label( "X - Velocity: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setXVelocity( Units.parseDouble( Units.LENGTH, t ) ), 
				() -> Units.toString( Units.LENGTH, selectedObject.getXVelocity(), environment.getLengthUnit() ) ) );
		container.add( new Label( "Y - Velocity: " ) );
		container.add( createEditField( ( t ) -> selectedObject.setYVelocity( Units.parseDouble( Units.LENGTH, t ) ), 
				() -> Units.toString( Units.LENGTH, selectedObject.getYVelocity(), environment.getLengthUnit() ) ) );
		
		//sets color of fields
		for( Component c : container.getComponents() ) {
			c.setBackground( Color.DARK_GRAY );
			c.setForeground( Color.WHITE );
		}
		label.setBackground( Color.DARK_GRAY );
		label.setForeground( Color.WHITE );
		pane.setBackground( Color.DARK_GRAY );
		pane.setForeground( Color.WHITE );
		pane.add( container );
		super.add( label );
		super.add( pane  );
		
		//sets layout of container and this 
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
					container.getComponent( i ).setBounds( 0, i * 20, w, 20 );
				}
			}

			@Override
			public Dimension minimumLayoutSize( Container parent ) {
				return new Dimension( 0, container.getComponentCount() * 20 );
			}
		} );
		
		environment.setUpdater( this );
		environment.queueOperation( ( list ) -> list.add( selectedObject ) );
	}
	
	
	
	public SpaceObject getSelected() {
		return isNullSelect ? null : selectedObject;
	}
	
	public void copy() {
		if( !isNullSelect ) {
			copied = selectedObject;
			String name = copied.toString();
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents( new StringSelection( name == null ? "Unnamed SpaceObject" : name ), null );
		}
	}
	
	public SpaceObject paste() {
		return copied == null ? null : (SpaceObject)copied.clone();
	}
	
	public void addMoon() {
		addMoonMode = !isNullSelect;
	}
	
	
	//overridden methods
	@Override
	public void setSelected( SpaceObject obj, SpaceObject.EnvInfo env ) {
		if( addMoonMode && env != null ) {
			//creates a moon of selectedObject
			MouseEvent loc = environment.getLastPos();
			SpaceObject moon = new SpaceObject( "Moon of " + selectedObject.getName(), 
					selectedObject.getMass() / 100, selectedObject.getMass() / 5, selectedObject.getTemperature(),
					env.posX + ( loc.getX() - env.centerX ) / env.zoom, 
					env.posY - ( loc.getY() - env.centerY ) / env.zoom, 0, 0 );
			environment.queueOperation( ( list ) -> {
				SpaceObject.orbit( selectedObject, moon );
				list.add( moon );
			} );
		} else {
			//selects new object
			isNullSelect = obj == null;
			selectedObject = isNullSelect ? new SpaceObject() : obj;
		}
		addMoonMode = false;
	}
	
	@Override
	public void dragEvent( MouseEvent current, SpaceObject.EnvInfo env ) {
		MouseEvent previous = environment.getLastDrag();
		if( previous != null ) {
			double dragX = -( current.getX() - previous.getX() ) / env.zoom;
			double dragY = ( current.getY() - previous.getY() ) / env.zoom;
			if( !isNullSelect ) {
				if( isVelocDragMode ) {
					//drags the velocity vector of selectedObject
					selectedObject.setXVelocity( selectedObject.getXVelocity() - dragX * 2 * env.zoom );
					selectedObject.setYVelocity( selectedObject.getYVelocity() - dragY * 2 * env.zoom );
				} else {
					//drags the position of selectedObject
					selectedObject.setXPosition( selectedObject.getXPosition() + dragX );
					selectedObject.setYPosition( selectedObject.getYPosition() + dragY );
				}
			} else {
				//drags the position of camera
				env.posX += dragX;
				env.posY += dragY;
			}
		} else if( !isNullSelect ) {
			//determines if the current mouse dragging movement is dragging the velocity vector arrow or position
			int xDiff = env.centerX + (int)selectedObject.getXVelocity() / 2 - current.getX();
			int yDiff = env.centerY - (int)selectedObject.getYVelocity() / 2 - current.getY();
			isVelocDragMode = Math.sqrt( xDiff * xDiff + yDiff * yDiff ) < 20;
		}
	}
	
	@Override
	public void render( Graphics g, List<SpaceObject> spaceObjects, SpaceObject.EnvInfo env ) {
		isNullSelect = isNullSelect || !spaceObjects.contains( selectedObject );
		if( !isNullSelect ) {
			//renders velocity vector
			double x = selectedObject.getXPosition();
			double y = selectedObject.getYPosition();
			env.posX = x;
			env.posY = y;
			int velX = env.centerX + (int)selectedObject.getXVelocity() / 2;
			int velY = env.centerY - (int)selectedObject.getYVelocity() / 2;
			g.setColor( Color.YELLOW );
			g.drawLine( env.centerX, env.centerY, velX, velY );
			g.drawOval( velX - 10, velY - 10, 20, 20 );
			//renders moon orbit
			if( addMoonMode ) {
				MouseEvent pos = environment.getLastPos();
				int mx = env.centerX - pos.getX();
				int my = env.centerY - pos.getY();
				int dist = (int)Math.sqrt( mx * mx + my * my );
				g.setColor( Color.WHITE );
				g.drawOval( -dist + env.centerX, -dist + env.centerY, dist * 2, dist * 2 );
			}
		}
		//updates container's textfields to show correct values
		if( super.isVisible() ) {
			for( int i = 0 ; i < container.getComponentCount(); i++ ) {
				Component c = container.getComponent( i );
				if( c instanceof TextField && !c.isFocusOwner() ) {
					( (TextField)c ).setText( null );
				}
			}
		}
	}
	
	
	//creates a textfield for manipulating selectedObject
	private TextField createEditField( Consumer<String> out, Supplier<String> in ) {
		TextField field = new TextField( in.get() ) {
			private static final long serialVersionUID = 3205908324902273382L;
			public void setText( String text ) {
				super.setText( in.get() );
			}
		};
		field.addActionListener( ( a ) -> {
			try {
				out.accept( field.getText() );
			} catch( NumberFormatException e ) {
				field.setText( null );
			}
		} );
		field.addFocusListener( new FocusListener() {
			@Override
			public void focusGained( FocusEvent e ) {
				field.setText( null );
			}
			@Override
			public void focusLost( FocusEvent f ) {
				try {
					out.accept( field.getText() );
				} catch( NumberFormatException e ) {
					field.setText( null );
				}
			}
		} );
		return field;
	}
}

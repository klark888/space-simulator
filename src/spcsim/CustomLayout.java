package spcsim;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

/*
 * @author K. A. F.
 * 
 * Description: LayoutManager that notifies the layout Runnable when layout even occurs
 * Created: 8-29-21
 * Status: generic class, finished
 * Dependencies: none
 */

public class CustomLayout implements LayoutManager {

	//runnable to notify
	private final Runnable layout;
	
	//constructor
	public CustomLayout( Runnable layout ) {
		this.layout = layout;
	}
	
	
	//overridden methods
	@Override
	public void layoutContainer( Container parent ) {
		layout.run();
	}
	
	@Override
	public Dimension preferredLayoutSize( Container parent ) {
		return minimumLayoutSize( parent );
	}

	@Override
	public Dimension minimumLayoutSize( Container parent ) {
		return new Dimension();
	}
	
	@Override
	public void addLayoutComponent( String name, Component comp ) { }
	@Override
	public void removeLayoutComponent( Component comp ) { }
}

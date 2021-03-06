/**
Space Simulation Program: simulates the movement of objects in space with newtonian physics
Copyright (C) 2021 Kent Fukuda

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package spcsim;

/* Author: Kent Fukuda
 * Description: Main class for Space Simulator Program
 * Created: 7-7-21
 * Status: main class, finished
 * Dependencies: Environment, MainFrame, Config, EditorPane
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

/* Author: [Author]
 * Description: [Description]
 * Created: [Date]
 * Status: [Status]
 * Dependencies: [Dependencies]
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

public class SpaceSim {
	
	//global fields
	public static final Environment environment;
	public static final MainFrame mainFrame;
	public static final EditorPane editorPane;
	public static final Config config;
	
	//private constructor
	private SpaceSim() { }
	
	
	//static initializer
	static {
		environment = new Environment();
		config = new Config( "spcsim.acad", System.getProperty( "user.home" ) );
		mainFrame = new MainFrame( environment, config, "1.4.0" );
		editorPane = mainFrame.getEditorPane();
		config.addMonitorObject( SpaceSim.class.getName() + ".environment",
				"TimeStep", "TickLength", "FrameLength", "Zoom", 
				"SingleThread", "TimeUnit", "LengthUnit", "ShowNames", "ShowEnvStatus" );
		config.addMonitorObject( SpaceSim.class.getName() + ".mainFrame",
				"Width", "Height", "X", "Y", "EditVisible" );
		config.addMonitorObject( SpaceSim.class.getName() + ".editorPane", "Units" );
	}
	
	
	//main method
	public static void main( String[] args ) {
		config.loadConfig();
		environment.getMainThread().start();
		mainFrame.setVisible( true );
	}
}

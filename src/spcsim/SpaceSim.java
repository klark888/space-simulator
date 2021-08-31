package spcsim;

/*
 * @author K. A. F.
 * 
 * Description: [Description]
 * Created: [Date]
 * Status: [Status]
 * Dependencies: [Dependencies]
 */

/*
 * @author K. A. F.
 * 
 * Description: Main class for Space Simulator Program
 * Created: 7-7-21
 * Status: main class, finished
 * Dependencies: Environment, MainFrame, Config
 */

public class SpaceSim {
	
	//global fields
	public static Environment environment;
	public static MainFrame mainFrame;
	public static Config config;
	
	//private constructor
	private SpaceSim() { }
	
	
	//main method
	public static void main( String[] args ) {
		environment = new Environment();
		config = new Config( "spcsim.acad", "" );
		mainFrame = new MainFrame( environment, config );
		config.addMonitorObject( SpaceSim.class.getName() + ".environment",
				"Active", "TimeStep", "TickLength", "FrameLength", "Zoom", "TimeUnit", "LengthUnit", "MassUnit" );
		config.addMonitorObject( SpaceSim.class.getName() + ".mainFrame",
				"Width", "Height", "X", "Y", "EditVisible" );
		config.loadConfig();
		new Thread( environment ).start();
		mainFrame.setVisible( true );
	}
}

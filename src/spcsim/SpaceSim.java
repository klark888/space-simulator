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
 * Dependencies: MainFrame, Logger, SimObject
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

/* Author: [Author]
 * Description: [Description]
 * Created: [Date]
 * Status: [Status]
 * Dependencies: [Dependencies]
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import spcsim.impl.MainFrame;
import spcsim.base.Logger;
import spcsim.base.SimObject;

public class SpaceSim {
    
    //private constructor
    private SpaceSim() {
        throw new AssertionError();
    }
    
    //main function. program entry
    public static void main( String[] args ) {
        Logger.logMessage( "Starting Space Simulation Program" );
        SimObject.ensureLoaded( spcsim.grav2d.Simple.class );
        SimObject.ensureLoaded( spcsim.grav2d.MultiThread.class );
        SimObject.ensureLoaded( spcsim.grav2d.EnsureStable.class );
        SimObject.ensureLoaded( spcsim.part2d.EnsureStable.class );
        SimObject.ensureLoaded( spcsim.part2d.Simple.class );
        Logger.logMessage( "Initializing MainFrame" );
        var frame = new MainFrame();
        frame.readConfig();
        frame.setSimulation( frame.getSimulation() == null ? spcsim.grav2d.Simple.class : null );
        Logger.logMessage( "Program startup complete" );
        frame.setVisible( true );
    }
}
package spcsim.grav2d;

/* Author: Kent Fukuda
 * Description: Implementation of the Gravity2D simulation environment that prevents unstable integration calculations
 * Created: 9-1-22
 * Status: environment class, finished
 * Dependencies: SimObject, Gravity2D, SpaceObject2D
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.lang.invoke.MethodHandles;
import spcsim.impl.MainFrame;
import spcsim.base.EditPane;
import spcsim.base.SimObject;

public final class EnsureStable extends Gravity2D {
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "GRZDES" );
    private double ratioThresh;
    
    //private constructor
    private EnsureStable() {
        ratioThresh = 0.3;
    }
    
    //overridden gui method to add threshold for instability settings
    @Override
    protected void generateGUI( EditPane editPane, MainFrame application ) {
        super.generateGUI( editPane, application );
        editPane.addValueMenuItem( EditPane.CONTROL_TYPE, "Accuracy Threshold", "Set Threshold", () -> ratioThresh, val -> {
                if( val <= 0 )
                    throw new IllegalArgumentException( "Threshold must be larger than 0" );
                ratioThresh = val;
            } );
    }
    
    //implemented simulation method
    @Override
    protected void simulate() {
        super.simulateStable( ratioThresh, true );
    }
}
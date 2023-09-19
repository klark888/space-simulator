package spcsim.part2d;

/* Author: Kent Fukuda
 * Description: Implementation of the Particles2D simulation environment that prevents unstable integration calculations
 * Created: 9-17-23
 * Status: environment class, finished
 * Dependencies: SimObject, Particles2D, AxiomObject2D
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.lang.invoke.MethodHandles;
import spcsim.impl.MainFrame;
import spcsim.base.EditPane;
import spcsim.base.SimObject;

public final class EnsureStable extends Particles2D {
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "PTZDES" );
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
        super.simulateStable( ratioThresh, false );
    }
}

package spcsim.part2d;

/* Author: Kent Fukuda
 * Description: Implementation of the Particles2D simulation environment with minimal functional coding
 * Created: 9-16-23
 * Status: environment class, finished
 * Dependencies: SimObject, Particles2D, AxiomObject2D
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.lang.invoke.MethodHandles;
import spcsim.base.SimObject;

public class Simple extends Particles2D {
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "PTZDSP" );
    
    //private constructor
    private Simple() { }
    
    //implemented simulation method
    @Override
    protected void simulate() {
        int size = particles.size();
        for( int i = 0; i < size; i++ ) {
            AxiomObject2D obj = particles.get( i );
            for( int j = i + 1; j < size; j++ )
                obj.interact( particles.get( j ) );
            obj.update( timeStep );
        }
    }
}

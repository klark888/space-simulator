package spcsim.grav2d;

/* Author: Kent Fukuda
 * Description: Implementation of the Gravity2D simulation environment with minimal functional coding
 * Created: 9-1-22
 * Status: environment class, finished
 * Dependencies: SimObject, Gravity2D, SpaceObject2D
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.lang.invoke.MethodHandles;
import spcsim.base.SimObject;

public final class Simple extends Gravity2D {
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "GRZDSP" );
    
    //private constructor
    private Simple() { }
    
    //implemented simulation method
    @Override
    protected void simulate() {
        int size = particles.size();
        for( int i = 0; i < size; i++ ) {
            SpaceObject2D obj = particles.get( i );
            for( int j = i + 1; j < size; j++ ) {
                if( obj.interact( particles.get( j ) ) < 0 ) {
                    synchronized( particles ) {
                        particles.remove( j-- );
                    }
                    size--;
                }
            }
            obj.update( timeStep );
        }
    }
}

package spcsim.grav2d;

/* Author: Kent Fukuda
 * Description: Implementation of the Gravity2D simulation environment that runs on multiple threads
 * Created: 9-1-22
 * Status: environment class, finished
 * Dependencies: SimObject, Gravity2D, SpaceObject2D
 * Licensed under GNU v3, see src/spcsim/SpaceSim.java for more details
 */

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JOptionPane;
import spcsim.impl.MainFrame;
import spcsim.base.EditPane;
import spcsim.base.SimObject;

public final class MultiThread extends Gravity2D {
    //serialversionuid
    public static final long serialVersionUID = SimObject.registerParticleClass( MethodHandles.lookup(), "GRZDMT" );
    
    private final Object lock;
    private final ArrayList<Integer> removeQueue;
    private volatile int numThreads;
    private transient volatile int activeThreads, simStep, tasksTodo, tasksToComplete, blockSize;
    
    //private constructor
    private MultiThread() {
        lock = new Object();
        removeQueue = new ArrayList<>();
        activeThreads = 0;
        numThreads = Math.max( Runtime.getRuntime().availableProcessors() - Thread.activeCount() - 1, 0 );
        simStep = 0;
        tasksTodo = 0;
        tasksToComplete = 0;
        blockSize = 0;
    }
    
    //overridden gui method to add worker count settings
    @Override
    protected void generateGUI( EditPane editPane, MainFrame application ) {
        super.generateGUI( editPane, application );
        editPane.addMenuItem( EditPane.CONTROL_TYPE, "Number of Threads", a -> {
            String response = JOptionPane.showInputDialog( "Set Worker Thread Count", Integer.toString( numThreads ) );
            try {
                if( response != null ) {
                    numThreads = Integer.parseInt( response );
                }
            } catch( IllegalArgumentException e ) {
                JOptionPane.showMessageDialog( null, "Could not parse invalid integer", "Error", JOptionPane.ERROR_MESSAGE );
            }
        } );
    }

    //implemented simulation method
    @Override
    protected void simulate() { 
        synchronized( lock ) {
            while( activeThreads < numThreads ) {
                var thread = new Thread( this::runWorker, "Simulator-" + super.typeName() + "-Worker-" + activeThreads );
                thread.setPriority( Thread.MAX_PRIORITY );
                thread.setDaemon( true );
                thread.start();
                activeThreads++;
            }
        }
        tasksTodo = particles.size() * ( particles.size() + 1 ) / 2;
        tasksToComplete = tasksTodo;
        simStep++;
        doTasks();
        synchronized( particles ) {
            while( !removeQueue.isEmpty() )
                particles.remove( (int)removeQueue.remove( 0 ) );
        }
    }

    //runnable method each worker thread runs
    private void runWorker() {
        int threadStep = simStep;
        while( simActive )
            if( threadStep < simStep ) {
                doTasks();
                threadStep++;
            }
        synchronized( lock ) {
            activeThreads--;
        }
    }
    
    //method completed when tasks are executed
    private void doTasks() {
        int start, end;
        do {
            synchronized( lock ) {
                start = tasksTodo;
                end = ( tasksTodo -= blockSize );
            }
            end = end < 0 ? 0 : end;
            int size = particles.size();
            int simSize = Math.max( start - size, 0 );
            int updateSize = blockSize - simSize;
            for( int i = start; i > size; i-- ) {
                int index1 = (int)( -Math.sqrt( 2 * ( i - size ) + 2.25 ) + size - 1.499 );
                int a = -index1 + size - 3;
                int index2 = -i + a * ( a + 1 ) / 2 + 2 * i - 2;
                SpaceObject2D obj1 = particles.get( index1 ), obj2 = particles.get( index2 );
                double dist;
                synchronized( obj1 ) {
                    synchronized( obj2 ) {
                        dist = obj1.interact( obj2 );
                    }
                }
                if( dist < 0 )
                    synchronized( removeQueue ) {
                        int removeIndex = Collections.binarySearch( removeQueue, index2 );
                        if( removeIndex < 0 ) {
                            obj2.mass = 0;
                            removeQueue.add( -removeIndex - 1, index2 );
                        }
                    }
            }
            synchronized( lock ) {
                tasksToComplete -= simSize;
            }
            for( int i = start - simSize; i > end; i-- )
                particles.get( i ).update( timeStep );
            synchronized( lock ) {
                tasksToComplete -= updateSize;
            }
        } while( end > 0 );
        try {
            while( tasksToComplete > 0 ) {
                Thread.sleep( 1 );
            }
        } catch( InterruptedException e ) { }
    }
}

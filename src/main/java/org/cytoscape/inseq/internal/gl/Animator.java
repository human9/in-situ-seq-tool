package org.cytoscape.inseq.internal.gl;

import java.util.concurrent.ArrayBlockingQueue;

import com.jogamp.opengl.GLAutoDrawable;

/**
 * A simple animator that can be called from anywhere.
 *
 * @author John Salamon
 */
public class Animator implements Runnable {

    private final GLAutoDrawable drawable;

    private Thread thread;

    private ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(1);

    private boolean running;

    public Animator(GLAutoDrawable drawable) {
        this.drawable = drawable;
        this.running = true;
    }

    public void run() {
        while(running) {
            try {
                queue.take();
            }
            catch (InterruptedException e) { 
                running = false;
            }
            drawable.display();
        }
    }

    /**
     * Calls display unless already pending and returns immediately.
     * If a call to display is already pending, returns false.
     */
    public boolean go() {
        return queue.offer(1);
    }
    
    /**
     * Start the animator by creating a new thread.
     */
    public void start() {
        if(thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        running = false;
    }
}

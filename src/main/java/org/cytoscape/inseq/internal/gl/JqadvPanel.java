package org.cytoscape.inseq.internal.gl;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.gl.JqadvGL.UpdateEngine;
import org.cytoscape.inseq.internal.tissueimage.SelectionPanel;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * JOGL data viewer.
 *
 * @author John Salamon
 */
public class JqadvPanel extends JPanel {
    
    private JqadvGL jqadvgl;
    private GLCanvas canvas;
    private Point origin;
    private InseqSession session;
    private SelectionPanel sp;

    public JqadvPanel(InseqSession s, SelectionPanel p) {
        
        GLProfile profile = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(profile);
        canvas = new GLCanvas(capabilities);
        jqadvgl = new JqadvGL(s, canvas);
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        origin = new Point();
        session = s;
        sp = p;

        canvas.addGLEventListener(new GLEventListener() {
            
            public void reshape(GLAutoDrawable glautodrawable, 
                                int x, int y, 
                                int width, int height) {
                jqadvgl.setup(glautodrawable.getGL().getGL2(),
                              width, height);
            }
            
            public void init(GLAutoDrawable drawable) {
                jqadvgl.init(drawable.getGL().getGL2());
            }
            
            public void dispose(GLAutoDrawable drawable) {
            }
            
            public void display(GLAutoDrawable drawable) {
                jqadvgl.render(drawable.getGL().getGL2(),
                               drawable.getSurfaceWidth(),
                               drawable.getSurfaceHeight());
            }
        });


        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);

        canvas.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if(jqadvgl.scale(e.getWheelRotation(), 
                              glX(e.getX()),
                              glY(e.getY()))
                        ) {
                    sp.updateZoom(jqadvgl.getScale());

        //statusBar.setZoom(df.format(imagePane.getScale()*100)+"%");
                    canvas.display();
                }
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origin.setLocation(e.getPoint());
                float[] p = jqadvgl.glToGraph(glX(origin.x)/canvas.getWidth(),
                                             glY(origin.y)/canvas.getHeight());
                //System.out.println(p[0] +", "+ p[1]);
                select(p);
            }
        });
        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                jqadvgl.move(2f*((origin.x - e.getX())),
                             2f*((origin.y - e.getY())));
                origin.setLocation(e.getPoint());
                canvas.display();
            }
        });

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                canvas.display();
            }
        });
    }

    /**
     * Called when user clicks on screen.
     * If a point is found near to that click it is selected.
     */
    private void select(float[] p) {
        List<Transcript> list;

        // This is calculating the distance in graph coordinates that we want
        // to search, required because this distance changes with zoom level.
        double dist = euclideanDistance(
                jqadvgl.glToGraph(glX(0)/ canvas.getWidth(), glY(0)/canvas.getHeight()),
                jqadvgl.glToGraph(glX(10)/ canvas.getWidth(), glY(0)/canvas.getHeight()) );
        try {
            list = session.tree.nearestEuclidean(
                    new double[]{p[0], p[1]}, Math.pow(dist, 2));
            Collections.reverse(list);
            for(Transcript t : list) {
                if(session.isActive(t)) {
                    jqadvgl.selectTranscript(t);
                    sp.setSelected(t);
                    canvas.display();
                    return;
                }
            }
            sp.setSelected(null);
            canvas.display();
        }
        catch (KeySizeException exc) {
            exc.printStackTrace();
        }
    }
    
    /**
     * Calculate a simple euclidean distance between two points.
     */
    public static double euclideanDistance(float[] a, float[] b) {
        double sqrdist = Math.pow((a[0] - b[0]) , 2) + Math.pow((a[1] - b[1]), 2);
        return Math.sqrt(sqrdist);
    }

    /**
     * Convert from AWT x to GL x coordinate.
     */
    public float glX(int x) {
        return 2f * x - canvas.getWidth();
    }
    
    /**
     * Convert from AWT y to GL y coordinate.
     */
    public float glY(int y) {
        return 2f * y - canvas.getHeight();
    }

    public void setPointScale(float value) {
        jqadvgl.setPointScale(value);
        canvas.display();
    }

    public void largePoints(boolean e) {
        jqadvgl.largePoints(e);
        canvas.display();
    }

    public UpdateEngine getUpdater() {
        return jqadvgl.engine;
    }
}

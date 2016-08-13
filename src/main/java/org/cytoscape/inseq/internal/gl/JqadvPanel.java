package org.cytoscape.inseq.internal.gl;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqSession;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

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

    public JqadvPanel(InseqSession s) {
        
        GLProfile profile = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(profile);
        canvas = new GLCanvas(capabilities);
        jqadvgl = new JqadvGL(s.getRaw());
        origin = new Point();
        session = s;

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
                jqadvgl.scale(e.getWheelRotation(), 
                              2f*e.getX() - canvas.getWidth(),
                              2f*e.getY() - canvas.getHeight());
                canvas.display();
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
               origin.setLocation(e.getPoint());
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
}

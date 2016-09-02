package org.cytoscape.inseq.internal.gl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.panel.SelectionPanel;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * JOGL data viewer.
 *
 * @author John Salamon
 */
public class JqadvPanel extends NewtCanvasAWT implements GLEventListener {

    static final long serialVersionUID = 22l;
    
    private JqadvGL jqadvgl;
    private JqadvListener jqadvListener;
    private GLWindow window;
    private InseqSession session;
    private SelectionPanel panel;

    PointerIcon cursor;
    Display display;

    public JqadvPanel(InseqSession s, SelectionPanel p) {

        // Initialise OpenGL with stencil buffer
        GLProfile profile = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setStencilBits(8);
        window = GLWindow.create(capabilities);
        jqadvgl = new JqadvGL(s, window);
        panel = p;

        // Set GLWindow cursor to crosshair
        IOUtil.ClassResources iocr
            = new IOUtil.ClassResources(new String[]{"/texture/crosshair.png"},
                                        JqadvPanel.class.getClassLoader(),
                                        JqadvPanel.class);
        try {
            display = window.getScreen().getDisplay();
            display.createNative();
            cursor = display.createPointerIcon(iocr, 12, 12);
            window.setPointerIcon(cursor);
        } catch (IOException e) {
            window.setPointerIcon(null);
        }

        session = s;

        window.addGLEventListener(this);

        jqadvListener = new JqadvListener(session, jqadvgl, this);
        window.addMouseListener(jqadvListener);

        this.setNEWTChild(window);
    }

    public void reshape(GLAutoDrawable drawable, 
                        int x, int y, 
                        int width, int height) {
        jqadvgl.setup(drawable.getGL().getGL2(),
                      width, height);
    }
    
    public void init(GLAutoDrawable drawable) {
        jqadvgl.init(drawable.getGL().getGL2());
    }
    
    public void dispose(GLAutoDrawable drawable) {
        jqadvgl.core.stop();
    }
    
    public void display(GLAutoDrawable drawable) {
        jqadvgl.render(drawable.getGL().getGL2(),
                       drawable.getSurfaceWidth(),
                       drawable.getSurfaceHeight());
    }

    public void center() {
        jqadvgl.centerView();
        jqadvgl.core.resume();
    }

    /**
     * Called when user clicks on screen.
     * If a point is found near to that click it is selected.
     */
    public void select(float[] p) {
        List<Transcript> list;

        // This is calculating the distance in graph coordinates that we want
        // to search, required because this distance changes with zoom level.
        double dist = Util.euclideanDistance(
                jqadvgl.pixelToGraph(new float[]{0,0}),
                jqadvgl.pixelToGraph(new float[]{10,0}));
        try {
            list = session.tree.nearestEuclidean(
                    new double[]{p[0], p[1]}, Math.pow(dist, 2));
            Collections.reverse(list);
            Transcript tr = null;
            for(Transcript t : list) {
                if(session.isActive(t)) {
                    tr = t;
                    break;
                }
            }
            jqadvgl.selectTranscript(tr);
            panel.setSelected(tr);
        }
        catch (KeySizeException exc) {
            exc.printStackTrace();
        }
    }
    public JqadvListener getListener() {
        return jqadvListener;
    }

    public JqadvGL getGL() {
        return jqadvgl;
    }

}


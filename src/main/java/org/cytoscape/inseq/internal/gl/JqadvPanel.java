package org.cytoscape.inseq.internal.gl;

import java.io.IOException;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.gl.JqadvGL.UpdateEngine;
import org.cytoscape.inseq.internal.tissueimage.SelectionPanel;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

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

    PointerIcon cursor;
    Display display;

    public JqadvPanel(InseqSession s, SelectionPanel p) {

        // Initialise OpenGL with stencil buffer
        GLProfile profile = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setStencilBits(8);
        window = GLWindow.create(capabilities);
        jqadvgl = new JqadvGL(s, window);

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

        jqadvListener = new JqadvListener(session, jqadvgl, window);
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
        jqadvgl.engine.core.stop();
    }
    
    public void display(GLAutoDrawable drawable) {
        jqadvgl.render(drawable.getGL().getGL2(),
                       drawable.getSurfaceWidth(),
                       drawable.getSurfaceHeight());
    }

    public void center() {
        jqadvgl.centerView();
    }

    public JqadvListener getListener() {
        return jqadvListener;

    }

    public void setPointScale(float value) {
        jqadvgl.setPointScale(value);
    }

    public void largePoints(boolean e) {
        jqadvgl.largePoints(e);
    }

    public UpdateEngine getUpdater() {
        return jqadvgl.engine;
    }

}


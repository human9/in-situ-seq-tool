package org.cytoscape.inseq.internal.gl;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.gl.JqadvGL.UpdateEngine;
import org.cytoscape.inseq.internal.tissueimage.SelectionPanel;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
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
public class JqadvPanel extends NewtCanvasAWT {

    static final long serialVersionUID = 22l;
    
    private JqadvGL jqadvgl;
    private GLWindow window;
    private Point origin;
    private Point leftClick;
    private InseqSession session;
    private SelectionPanel sp;

    private GeneralPath polygon;
    private Rectangle2D.Float rectangle;
    private Point start;
    private boolean selectButton;
    private boolean dragButton;
    private boolean usePolygon;
    private boolean initPolygon;
    private float[] polyOrigin;

    PointerIcon cursor;
    Display display;

    public JqadvPanel(InseqSession s, SelectionPanel p) {

        GLProfile profile = GLProfile.getDefault();
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setStencilBits(8);
        window = GLWindow.create(capabilities);
        jqadvgl = new JqadvGL(s, window);

        IOUtil.ClassResources iocr = new IOUtil.ClassResources(new String[] {"/texture/crosshair.png"}, JqadvPanel.class.getClassLoader(), JqadvPanel.class);
        try {
            display = window.getScreen().getDisplay();
            display.createNative();
            cursor = display.createPointerIcon(iocr, 12, 12);
            window.setPointerIcon(cursor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        origin = new Point();
        session = s;
        sp = p;

        window.addGLEventListener(new GLEventListener() {
            
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
            }
            
            public void display(GLAutoDrawable drawable) {
                jqadvgl.render(drawable.getGL().getGL2(),
                               drawable.getSurfaceWidth(),
                               drawable.getSurfaceHeight());
            }
        });

        window.addMouseListener(new JqadvListener());

        this.setNEWTChild(window);
    }

    public void enablePoly() {
        usePolygon = true;
        initPolygon = false;
        session.setSelection(null);
    }
    
    public void enableRect() {
        usePolygon = false;
        session.setSelection(null);
    }

    public void center() {
        jqadvgl.centerView();
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
                jqadvgl.glToGraph(glX(0)/ window.getWidth(), glY(0)/window.getHeight()),
                jqadvgl.glToGraph(glX(10)/ window.getWidth(), glY(0)/window.getHeight()) );
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
            sp.setSelected(tr);
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
     * Get exact pixel coordinates of where a graph point is.
     */
    private float[] toPixel(float[] p) {
        float[] pixel = jqadvgl.graphToGL(p[0], p[1]);
        pixel[0] = (window.getWidth() * pixel[0] + window.getWidth()) / 2f;
        pixel[1] = (window.getHeight() * pixel[1] + window.getHeight()) / 2f;
        return pixel;
    }

    /**
     * Get graph coordinates of where a pixel point is.
     */
    private float[] toGraph(Point p) {
        float[] graph = jqadvgl.glToGraph(glX(p.x) / window.getWidth(),
                glY(p.y) / window.getHeight());
        return graph;
    }

    /**
     * Convert from AWT x to GL x coordinate.
     */
    public float glX(int x) {
        return 2f * x - window.getWidth();
    }
    
    /**
     * Convert from AWT y to GL y coordinate.
     */
    public float glY(int y) {
        return 2f * y - window.getHeight();
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

    
    class JqadvListener extends MouseAdapter {

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            jqadvgl.engine.updateScale(-e.getRotation()[1], 
                          glX(e.getX()),
                          glY(e.getY()));
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Point ePoint = new Point(e.getX(), e.getY());
            float[] p = toGraph(ePoint);
            if(e.getButton() == MouseEvent.BUTTON1) {
                // Left mouse button
                dragButton = true;
                start = ePoint;
                leftClick = new Point(e.getX(), e.getY());
            }
            else if(e.getButton() == MouseEvent.BUTTON3) {
                // Right mouse button

                boolean pathClosed = false;
                if(!usePolygon) {
                    selectButton = true;
                    rectangle = new Rectangle2D.Float();
                    session.setSelection(null);
                    origin.setLocation(new Point2D.Float(p[0], p[1]));
                    pathClosed = true;
                }
                else {
                    if(!initPolygon) {
                        polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 8);
                        polyOrigin = p;
                        polygon.moveTo(p[0], p[1]);
                        initPolygon = true;
                        session.setSelection(null);
                    }
                    else {
                        if(euclideanDistance(toPixel(polyOrigin),
                            new float[] {e.getX(), e.getY()}) < 20) 
                        {
                            polygon.closePath();
                            session.setSelection(polygon);
                            pathClosed = true;
                            initPolygon = false;
                        } else {
                            polygon.lineTo(p[0], p[1]);
                            GeneralPath current = (GeneralPath) polygon.clone();
                            session.setSelection(current);
                        }
                    }
                }
                getUpdater().selectionChanged(pathClosed);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Point ePoint = new Point(e.getX(), e.getY());
                float[] p = toGraph(ePoint);
                if(ePoint.equals(leftClick))
                {
                    select(p);
                }
                dragButton = false;
            }
            if (e.getButton() == MouseEvent.BUTTON3) {
                selectButton = false;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point ePoint = new Point(e.getX(), e.getY());
            float[] p = toGraph(ePoint);
            if(initPolygon && usePolygon) {
                GeneralPath current = (GeneralPath) polygon.clone();
                boolean pathClosed = false;
                if(euclideanDistance(toPixel(polyOrigin),
                            new float[] {e.getX(), e.getY()}) < 20)
                {
                    current.closePath();
                    pathClosed = true;
                } else {
                    current.lineTo(p[0], p[1]);
                }
                session.setSelection(current);
                getUpdater().selectionChanged(pathClosed);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point ePoint = new Point(e.getX(), e.getY());
            float[] p = toGraph(ePoint);
            if(dragButton) {
                jqadvgl.engine.move(2f*((start.x - e.getX())),
                             2f*((start.y - e.getY())));
                start.setLocation(ePoint);
            }
            if (selectButton && !usePolygon) {
                rectangle.setFrameFromDiagonal(
                        origin,
                        new Point2D.Float(p[0], p[1]));
                session.setSelection(rectangle);
                getUpdater().selectionChanged(true);
            }
            if (initPolygon && usePolygon) {
                GeneralPath current = (GeneralPath) polygon.clone();
                boolean pathClosed = false;
                if(euclideanDistance(toPixel(polyOrigin),
                            new float[] {e.getX(), e.getY()}) < 20) {
                    current.closePath();
                    pathClosed = true;
                } else {
                    current.lineTo(p[0], p[1]);
                }
                session.setSelection(current);
                getUpdater().selectionChanged(pathClosed);
            }
        }

    }

}


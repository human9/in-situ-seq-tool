package org.cytoscape.inseq.internal.gl;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.gl.JqadvGL.UpdateEngine;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;

import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * JOGL data viewer.
 *
 * @author John Salamon
 */
public class JqadvListener extends MouseAdapter {

    static final long serialVersionUID = 22l;
    
    private JqadvGL jqadvgl;
    private GLWindow window;
    private Point origin;
    private Point leftClick;
    private InseqSession session;

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

    public JqadvListener(InseqSession s, JqadvGL gl, GLWindow w) {
        
        origin = new Point();
        jqadvgl = gl;
        window = w;
        session = s;

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

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        jqadvgl.engine.updateScale(-e.getRotation()[1], 
                      e.getX(),
                      e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point ePoint = new Point(e.getX(), e.getY());
        float[] p = jqadvgl.pixelToGraph(new float[]{e.getX(), e.getY()});
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
                    if(euclideanDistance(jqadvgl.graphToPixel(polyOrigin),
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
            float[] p = jqadvgl.pixelToGraph(new float[]{e.getX(), e.getY()});
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
        float[] p = jqadvgl.pixelToGraph(new float[]{e.getX(), e.getY()});
        if(initPolygon && usePolygon) {
            GeneralPath current = (GeneralPath) polygon.clone();
            boolean pathClosed = false;
            if(euclideanDistance(jqadvgl.graphToPixel(polyOrigin),
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
        float[] p = jqadvgl.pixelToGraph(new float[]{e.getX(), e.getY()});
        if(dragButton) {
            jqadvgl.engine.move(start.x - e.getX(),
                         start.y - e.getY());
            start.setLocation(ePoint);
        }
        else {
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
                if(euclideanDistance(jqadvgl.graphToPixel(polyOrigin),
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


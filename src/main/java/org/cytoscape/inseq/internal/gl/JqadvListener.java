package org.cytoscape.inseq.internal.gl;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.cytoscape.inseq.internal.InseqSession;

import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

/**
 * JOGL data viewer.
 *
 * @author John Salamon
 */
public class JqadvListener extends MouseAdapter {

    static final long serialVersionUID = 22l;
    
    private JqadvGL jqadvgl;
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
    private JqadvPanel panel;

    public JqadvListener(InseqSession s, JqadvGL gl, JqadvPanel panel) {
        
        origin = new Point();
        jqadvgl = gl;
        session = s;
        this.panel = panel;

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

    
    @Override
    public void mouseWheelMoved(MouseEvent e) {
        jqadvgl.updateScale(-e.getRotation()[1], 
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
                    if(Util.euclideanDistance(jqadvgl.graphToPixel(polyOrigin),
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
            jqadvgl.selectionChanged(pathClosed);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point ePoint = new Point(e.getX(), e.getY());
            float[] p = jqadvgl.pixelToGraph(new float[]{e.getX(), e.getY()});
            if(ePoint.equals(leftClick))
            {
                panel.select(p);
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
            if(Util.euclideanDistance(jqadvgl.graphToPixel(polyOrigin),
                        new float[] {e.getX(), e.getY()}) < 20)
            {
                current.closePath();
                pathClosed = true;
            } else {
                current.lineTo(p[0], p[1]);
            }
            session.setSelection(current);
            jqadvgl.selectionChanged(pathClosed);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point ePoint = new Point(e.getX(), e.getY());
        float[] p = jqadvgl.pixelToGraph(new float[]{e.getX(), e.getY()});
        if(dragButton) {
            jqadvgl.move(start.x - e.getX(),
                         start.y - e.getY());
            start.setLocation(ePoint);
        }
        else {
            if (selectButton && !usePolygon) {
                rectangle.setFrameFromDiagonal(
                        origin,
                        new Point2D.Float(p[0], p[1]));
                session.setSelection(rectangle);
                jqadvgl.selectionChanged(true);
            }
            if (initPolygon && usePolygon) {
                GeneralPath current = (GeneralPath) polygon.clone();
                boolean pathClosed = false;
                if(Util.euclideanDistance(jqadvgl.graphToPixel(polyOrigin),
                            new float[] {e.getX(), e.getY()}) < 20) {
                    current.closePath();
                    pathClosed = true;
                } else {
                    current.lineTo(p[0], p[1]);
                }
                session.setSelection(current);
                jqadvgl.selectionChanged(pathClosed);
            }
        }
    }

}


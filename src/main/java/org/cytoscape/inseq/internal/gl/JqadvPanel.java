package org.cytoscape.inseq.internal.gl;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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

    static final long serialVersionUID = 22l;
    
    private JqadvGL jqadvgl;
    private GLCanvas canvas;
    private Point origin;
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
                    canvas.display();
                }
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                float[] p = toGraph(e.getPoint());
                if(e.getButton() == MouseEvent.BUTTON1) {
                    // Left mouse button
                    dragButton = true;
                    start = e.getPoint();
                }
                else if(e.getButton() == MouseEvent.BUTTON3) {
                    // Right mouse button

                    boolean pathClosed = false;
                    if(!usePolygon) {
                        selectButton = true;
                        rectangle = new Rectangle2D.Float();
                        session.setSelection(null);
                        origin.setLocation(new Point2D.Float(p[0], p[1]));
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
                    float[] p = toGraph(e.getPoint());
					if(e.getPoint().equals(start))
					{
                        select(p);
                    }
					dragButton = false;
				}
				if (e.getButton() == MouseEvent.BUTTON3) {
					selectButton = false;
				}
			}
        });
        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                float[] p = toGraph(e.getPoint());
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
                float[] p = toGraph(e.getPoint());
                if(dragButton) {
                    jqadvgl.move(2f*((start.x - e.getX())),
                                 2f*((start.y - e.getY())));
                    start.setLocation(e.getPoint());
                    canvas.display();
                }
				if (selectButton && !usePolygon) {
                    rectangle.setFrameFromDiagonal(
                            origin,
                            new Point2D.Float(p[0], p[1]));
                    session.setSelection(rectangle);
                    getUpdater().selectionChanged(false);
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
        });

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                canvas.display();
            }
        });
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
            Transcript tr = null;
            for(Transcript t : list) {
                if(session.isActive(t)) {
                    tr = t;
                    break;
                }
            }
            jqadvgl.selectTranscript(tr);
            sp.setSelected(tr);
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
     * Get exact pixel coordinates of where a graph point is.
     */
    private float[] toPixel(float[] p) {
        float[] pixel = jqadvgl.graphToGL(p[0], p[1]);
        pixel[0] = (canvas.getWidth() * pixel[0] + canvas.getWidth()) / 2f;
        pixel[1] = (canvas.getHeight() * pixel[1] + canvas.getHeight()) / 2f;
        return pixel;
    }

    /**
     * Get graph coordinates of where a pixel point is.
     */
    private float[] toGraph(Point p) {
        float[] graph = jqadvgl.glToGraph(glX(p.x) / canvas.getWidth(),
                glY(p.y) / canvas.getHeight());
        return graph;
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

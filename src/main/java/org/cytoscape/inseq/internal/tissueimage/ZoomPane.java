package org.cytoscape.inseq.internal.tissueimage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.inseq.internal.InseqSession;

public class ZoomPane extends JScrollPane {
	static final long serialVersionUID = 355635l;

	private Point mouseClick;
	private boolean dragButton;
	private boolean selectButton;
	private ImagePane imagePane;
	private double ratioX, ratioY;
	private Dimension imgDims;
	private JViewport vp;
	private Point start;
    private GeneralPath polygon;
    private Rectangle rectangle;
    private Point origin;
    private InseqSession session;

    private boolean usePolygons;
    private boolean initPolygon;

    private Point2D.Double clickOrigin;

	public void resizeEvent() {
		imagePane.setMinimumSize(vp.getExtentSize());
		imagePane.setSize();
		imagePane.repaint();
	}

    private Point translatePoint(MouseEvent e) {
        Point view = new Point(getViewport().getViewPosition());
        return new Point(
            (int) ((view.x + e.getX() - imagePane.offset.width)
                / imagePane.getScale()),
            (int) ((view.y + e.getY() - imagePane.offset.height)
                / imagePane.getScale())
        );
    }


	public ZoomPane(final SelectionPanel sp, InseqSession s) {
		this.imagePane = sp.imagePane;
        this.session = s;
		imagePane.sp = sp;
        origin = new Point();
		imagePane.zp = this;
		this.imgDims = session.min;
		setViewportView(imagePane);
		setWheelScrollingEnabled(false);
		this.vp = getViewport();
		vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		vp.setBackground(Color.BLACK);

        imagePane.makeDist(); 

        vp.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                imagePane.invalidateCache();
            }
        });
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				imagePane.setMinimumSize(vp.getExtentSize());
				imagePane.setSize();
				imagePane.repaint();
                imagePane.invalidateCache();
			}
		});
		addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoomImage(e.getPoint(), e.getWheelRotation());
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					dragButton = true;
					mouseClick = e.getPoint();
					start = e.getPoint();
				}
				if (e.getButton() == MouseEvent.BUTTON3) {
                    Point p = translatePoint(e);
                    if(!usePolygons) {
                        selectButton = true;
                        rectangle = new Rectangle();
                        session.setSelection(null);
                        origin.move(p.x, p.y);
                        imagePane.repaint();
                    }
                    else {
                        if(!initPolygon) {
                            polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 8);
                            clickOrigin = imagePane.viewportPixelPointToActual(e.getPoint());
                            polygon.moveTo(p.x, p.y);
                            initPolygon = true;
                            session.setSelection(null);
                        }
                        else {
                            if(imagePane.euclideanDistance(
                                        imagePane.actualPointToViewportPixel(clickOrigin), 
                                        e.getPoint()) < 20) 
                            {
                                polygon.closePath();
                                session.setSelection(polygon);
                                initPolygon = false;
                            } else {
                                polygon.lineTo(p.x, p.y);
                                GeneralPath current = (GeneralPath) polygon.clone();
                                session.setSelection(current);
                            }
                        }
                        imagePane.repaint();
                    }
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					if(e.getPoint().equals(start))
					{
						// Run point selection methods
						Point pix = e.getPoint();
						imagePane.clickAtPoint(pix);

					}
					dragButton = false;
				}
				if (e.getButton() == MouseEvent.BUTTON3) {
					selectButton = false;
				}
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				imagePane.ratioIsCurrent = false;
                Point p = translatePoint(e);
                if (initPolygon && usePolygons) {
                    GeneralPath current = (GeneralPath) polygon.clone();
                    if(imagePane.euclideanDistance(
                                imagePane.actualPointToViewportPixel(clickOrigin), 
                                e.getPoint()) < 20) {
                        current.closePath();
                    } else {
                        current.lineTo(p.x, p.y);
                    }
                    session.setSelection(current);
                    imagePane.repaint();
                }
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				imagePane.ratioIsCurrent = false;
                Point p = translatePoint(e);
				if (dragButton) {
					Point move = new Point(mouseClick.x - e.getX(), mouseClick.y - e.getY());
					Rectangle r = new Rectangle(move, vp.getExtentSize());
					vp.scrollRectToVisible(r);
					mouseClick.setLocation(e.getPoint());
					//repaint();
				}
				if (selectButton && !usePolygons) {
                    rectangle.setFrameFromDiagonal(
                            origin,
                            new Point(p.x, p.y));
                    session.setSelection(rectangle);
                    imagePane.repaint();
				}
                if (initPolygon && usePolygons) {
                    GeneralPath current = (GeneralPath) polygon.clone();
                    if(imagePane.euclideanDistance(
                                imagePane.actualPointToViewportPixel(clickOrigin), 
                                e.getPoint()) < 20) {
                        current.closePath();
                    } else {
                        current.lineTo(p.x, p.y);
                    }
                    session.setSelection(current);
                    imagePane.repaint();
                }
			}
		});
	}

	public Rectangle getView()
	{
		return getViewport().getViewRect();
	}

    public void enablePoly() {
        usePolygons = true;
        initPolygon = false;
        session.setSelection(null);
    }
    
    public void enableRect() {
        usePolygons = false;
        session.setSelection(null);
    }

	void zoomImage(Point mouse, int direction) {
		JViewport vp = getViewport();
		Point current = new Point(getViewport().getViewPosition());
		int x, y;

		double oldScale = imagePane.getScale();
		if (!imagePane.ratioIsCurrent) {
			ratioX = ((mouse.x + current.x)) / (imgDims.width * oldScale);
			ratioY = ((mouse.y + current.y)) / (imgDims.height * oldScale);
			imagePane.ratioIsCurrent = true;
		}

		imagePane.zoomAltered = true;
		if (direction < 0) {
			imagePane.scaleUp();
		} else {
			imagePane.scaleDown();
		}
		double newScale = imagePane.getScale();
		imagePane.sp.updateZoom();

		x = (int) (Math.round(ratioX * newScale * imgDims.width) - Math.round(ratioX * oldScale * imgDims.width));
		y = (int) (Math.round(ratioY * newScale * imgDims.height) - Math.round(ratioY * oldScale * imgDims.height));

		Rectangle r = new Rectangle(new Point(x, y), vp.getExtentSize());
		imagePane.revalidate();
		if (direction < 0)
			imagePane.setSize();
		vp.scrollRectToVisible(r);
		if (direction >= 0)
			imagePane.setSize();

		imagePane.repaint();
	}
/*
	// NOTE TO ME USE THIS TO GET REGION BLAG BGLAG
	public ArrayList<Point> getSelectedPoints()
	{
		ArrayList<Point> points = new ArrayList<Point>();

		Point p1 = new Point(imagePane.rect.x, imagePane.rect.y);	
		Point p2 = new Point(imagePane.rect.width + p1.x, imagePane.rect.height + p1.y);	

		return points; 
	}

	public ArrayList<Integer> getSelectedGridNumbers(Dimension gridSize) {
		int stepX = (int) Math.round(imagePane.image.getWidth() / gridSize.width);
		int stepY = (int) Math.round(imagePane.image.getHeight() / gridSize.height);
		int x = (int) Math.round(stepX / 2);
		int y = (int) Math.round(stepY / 2);
		ArrayList<Integer> points = new ArrayList<Integer>();
		int lx = imagePane.selectedOrigin.x;
		int ly = imagePane.selectedOrigin.y;
		int rx = imagePane.selectedFinish.x;
		int ry = imagePane.selectedFinish.y;
		Rectangle rect = new Rectangle(Math.min(lx, rx), Math.min(ly, ry), Math.abs(lx - rx), Math.abs(ly - ry));
		for (int i = 0; i < gridSize.width * gridSize.height; i++) {
			if (rect.contains(new Point(x, y)))
				points.add(i);
			y += stepY;
			if (i % gridSize.height == 0) {
				y = (int) Math.round(stepY / 2);
				x += stepX;
			}

		}
		return points;
	}
*/
	public void updateViewport(ImagePane view) {
		this.imagePane = view;
		view.setMinimumSize(getViewport().getExtentSize());
		setViewportView(view);
		this.vp = getViewport();
		imagePane.zp = this;
		view.setSize();
        imagePane.makeDist();
	}
	
	public void updateViewport() {
		imagePane.setMinimumSize(getViewport().getExtentSize());
		imagePane.setSize();
	}
		
		

	/**
	 *  The following overrides are to hijack the normal scroll behaviour of
	 *  JScrollPane. Without this, moving with the scrollbars causes graphical
	 *  glitches. Basically unless the call is made to scrollViewToRect, things
	 *  break. Especially text. BACKINGSTORE_SCROLL_MODE is responsible for this.
	 *  However, the other ways introduce other unfixable problems.
	 */
	@Override
	public JScrollBar createHorizontalScrollBar() {
		return new ScrollBar(JScrollBar.HORIZONTAL) {
			@Override	
			public void setValue(int i) {
				//super.setValue(i);
				// REMINDER TO SELF: scrollRectToVisible XY coordinates are RELATIVE!!!!
				Point p = new Point(i - getViewport().getViewPosition().x, 0);
				Dimension d = new Dimension(vp.getExtentSize());
				Rectangle r = new Rectangle(p,d);
				vp.scrollRectToVisible(r);
				return;
			}
		};
	}
	
	@Override
	public JScrollBar createVerticalScrollBar() {
		return new ScrollBar(JScrollBar.VERTICAL) {
			@Override	
			public void setValue(int i) {
				Point p = new Point(0, i - getViewport().getViewPosition().y);
				Dimension d = new Dimension(vp.getExtentSize());
				Rectangle r = new Rectangle(p,d);
				vp.scrollRectToVisible(r);
				return;
			}
		};
	}
}

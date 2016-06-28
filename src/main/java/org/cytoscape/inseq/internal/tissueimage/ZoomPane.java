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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

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

	private Timer imageTimer = new Timer();
	boolean taskdone = true;
	TimerTask task;
	
		/*
	@Override
	public void setLayout(LayoutManager layout) {
		if (layout instanceof ScrollPaneLayout) {
			super.setLayout(layout);
			((ScrollPaneLayout)layout).syncWithScrollPane
	}*/
	public void restartTimer() {

		imageTimer.purge();
		if(!taskdone) {
			task.cancel();
		}
		taskdone = false;
		task = new TimerTask() {
			public void run() {
				imagePane.cacheImage();
				taskdone = true;
			}

			@Override
			public boolean cancel() {
				super.cancel();
				imagePane.stopCache();
				return true;
			}
		};

		try { 
			imageTimer.schedule(task, 500);
		}
		catch(IllegalStateException x) {
			imageTimer = new Timer();
			imageTimer.schedule(task, 500);
		}
	}

	public void resizeEvent() {
		imagePane.setMinimumSize(vp.getExtentSize());
		imagePane.setSize();
		imagePane.repaint();
	}

	public ZoomPane(final ImagePane ip) {
		this.imagePane = ip;
		ip.zp = this;
		this.imgDims = new Dimension(ip.image.getWidth(), ip.image.getHeight());
		setViewportView(imagePane);
		setWheelScrollingEnabled(false);
		this.vp = getViewport();
		vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		vp.setBackground(Color.BLACK);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				imagePane.setMinimumSize(vp.getExtentSize());
				imagePane.setSize();
				imagePane.repaint();
			}
		});
		addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoomImage(e.getPoint(), e.getWheelRotation());
				restartTimer();
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
					selectButton = true;
					Point view = new Point(getViewport().getViewPosition());
					imagePane.selectedOrigin.move(
							(int) ((view.x + e.getX() - imagePane.offset.width) / imagePane.getScale()),
							(int) ((view.y + e.getY() - imagePane.offset.height) / imagePane.getScale()));
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
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				imagePane.ratioIsCurrent = false;
				if (dragButton) {
					Point move = new Point(mouseClick.x - e.getX(), mouseClick.y - e.getY());
					Rectangle r = new Rectangle(move, vp.getExtentSize());
					vp.scrollRectToVisible(r);
					mouseClick.setLocation(e.getPoint());
					//repaint();
				}
				if (selectButton) {
					Point view = new Point(getViewport().getViewPosition());
					imagePane.selectedFinish.move(
							(int) ((view.x + e.getX() - imagePane.offset.width) / imagePane.getScale()),
							(int) ((view.y + e.getY() - imagePane.offset.height) / imagePane.getScale()));
					repaint();
				}
			}
		});
	}

	public Rectangle getView()
	{
		return getViewport().getViewRect();
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
	*/

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

	public void updateViewport(ImagePane view) {
		this.imagePane = view;
		view.setMinimumSize(getViewport().getExtentSize());
		setViewportView(view);
		this.vp = getViewport();
		imagePane.zp = this;
		view.setSize();
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

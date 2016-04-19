package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;

class ZoomPane extends JScrollPane
{
	static final long serialVersionUID = 355635l; 
	
	private Point mouseClick;
	private boolean dragButton;
	private ImagePane imagePane;
	private double ratioX, ratioY;
	private Dimension imgDims;
	private JViewport vp;

	public ZoomPane(final ImagePane ip)
	{
		this.imagePane = ip;
		this.imgDims = new Dimension(ip.image.getWidth(), ip.image.getHeight());
		ip.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		setViewportView(imagePane);
		setWheelScrollingEnabled(false);
		this.vp = getViewport();
		vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		vp.setBackground(Color.BLACK);
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				imagePane.setMinimumSize(vp.getExtentSize());
				imagePane.setSize();
				imagePane.repaint();
			}
		});
		addMouseWheelListener(new MouseAdapter() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoomImage(e.getPoint(), e.getWheelRotation());
			}
		});
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					dragButton = true;
					mouseClick = new Point(e.getX(), e.getY());
				}
			}
			public void mouseReleased(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					dragButton = false;
				}
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			public void mouseMoved(MouseEvent e) {
				imagePane.ratioIsCurrent = false;
			}
			public void mouseDragged(MouseEvent e) {
				imagePane.ratioIsCurrent = false;
				if(dragButton) {
					Point move = new Point(mouseClick.x - e.getX(), mouseClick.y - e.getY());
					Rectangle r = new Rectangle(move, vp.getExtentSize());
					vp.scrollRectToVisible(r);
					mouseClick.setLocation(e.getPoint());
				}
			}
		});
	}

	void zoomImage(Point mouse, int direction)
	{
		JViewport vp = getViewport();
		Point current = new Point(getViewport().getViewPosition());
		int x, y;

		double oldScale = imagePane.getScale();
		if(!imagePane.ratioIsCurrent)
		{
			ratioX = ((mouse.x+current.x)) / (imgDims.width*oldScale);
			ratioY = ((mouse.y+current.y)) / (imgDims.height*oldScale);
			imagePane.ratioIsCurrent = true;
		}

		if(direction < 0)
		{
			imagePane.scaleUp();
		}
		else
		{
			imagePane.scaleDown();
		}
		double newScale = imagePane.getScale();
	
		x = (int) (Math.round(ratioX*newScale*imgDims.width) - Math.round(ratioX*oldScale*imgDims.width));
		y = (int) (Math.round(ratioY*newScale*imgDims.height) - Math.round(ratioY*oldScale*imgDims.height));

		Rectangle r = new Rectangle(new Point(x,y), vp.getExtentSize());
		if(direction < 0)
			imagePane.setSize();
		vp.scrollRectToVisible(r);
		if(direction >= 0)	
			imagePane.setSize();
		
		imagePane.revalidate();
		imagePane.repaint();
	}

	public void updateViewport(ImagePane view)
	{
		this.imagePane = view;
		view.setMinimumSize(getViewport().getExtentSize());
		setViewportView(view);
		this.vp = getViewport();
		view.setSize();
	}
}

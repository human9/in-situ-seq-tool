package org.cytoscape.inseq.internal.imageselection;

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
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

class ZoomPane extends JScrollPane
{
	static final long serialVersionUID = 355635l; 
	
	private Point mouseClick;
	private boolean dragButton;
	private boolean selectButton;
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
			}
		});
		addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					dragButton = true;
					mouseClick = e.getPoint();
				}
				if(e.getButton() == MouseEvent.BUTTON3) {
					selectButton = true;
					Point view = new Point(getViewport().getViewPosition());
					imagePane.selectedOrigin.move((int)((view.x + e.getX() - imagePane.offset.width)/imagePane.getScale()), (int)((view.y + e.getY() - imagePane.offset.height)/imagePane.getScale()));
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					dragButton = false;
				}
				if(e.getButton() == MouseEvent.BUTTON3) {
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
				if(dragButton) {
					Point move = new Point(mouseClick.x - e.getX(), mouseClick.y - e.getY());
					Rectangle r = new Rectangle(move, vp.getExtentSize());
					vp.scrollRectToVisible(r);
					mouseClick.setLocation(e.getPoint());
				}
				if(selectButton) {
					Point view = new Point(getViewport().getViewPosition());
					imagePane.selectedFinish.move((int)((view.x + e.getX() - imagePane.offset.width)/imagePane.getScale()), (int)((view.y + e.getY() - imagePane.offset.height)/imagePane.getScale()));
					repaint();
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
		imagePane.revalidate();
		if(direction < 0)
			imagePane.setSize();
		vp.scrollRectToVisible(r);
		if(direction >= 0)	
			imagePane.setSize();
		
		imagePane.repaint();
	}
	
	public ArrayList<Integer> getSelectedGridNumbers(Dimension gridSize)
	{
		int stepX = (int)Math.round(imagePane.image.getWidth() / gridSize.width);
		int stepY = (int)Math.round(imagePane.image.getHeight() / gridSize.height);
		int x = (int) Math.round(stepX / 2); 
		int y = (int) Math.round(stepY / 2);
		ArrayList<Integer> points = new ArrayList<Integer>();
		int lx = imagePane.selectedOrigin.x;
		int ly = imagePane.selectedOrigin.y;
		int rx = imagePane.selectedFinish.x;
		int ry = imagePane.selectedFinish.y;
		Rectangle rect = new Rectangle(Math.min(lx,rx), Math.min(ly,ry), Math.abs(lx-rx), Math.abs(ly-ry));
		for(int i = 0; i < gridSize.width*gridSize.height; i++)
		{
			if(rect.contains(new Point(x,y)))
				points.add(i);	
			y += stepY;
			if (i % gridSize.height == 0) 
			{
				y = (int)Math.round(stepY / 2);
				x += stepX;
			}

		}
		return points;
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
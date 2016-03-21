package org.cytoscape.myapp.internal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

class ZoomPanel extends JPanel
{
	private double scale;
	private Image img;
	private int xoff, yoff; 
	private int clickx, clicky; 
	private int dragx, dragy; 
	private int zx, zy; 

	public ZoomPanel(String path, final Component parent)
	{
		BufferedImage image = getImage(path);
		this.img = image;
		this.scale = 400.0/(double)image.getWidth();
		this.setPreferredSize(new Dimension(400, 400));	
		
		this.addMouseWheelListener(new MouseAdapter(){
			public void mouseWheelMoved(MouseWheelEvent e){
				int notches = e.getWheelRotation();
				if(notches < 0) {
					zoomIn(e.getX(), e.getY());
				}
				else {
					zoomOut(e.getX(), e.getY());
				}
				parent.repaint();
			}
		});
		this.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				clickx = e.getX();
				clicky = e.getY();
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter(){
			public void mouseDragged(MouseEvent e){
				int x = e.getX();
				int y = e.getY();
				dragx -= clickx - x; 
				dragy -= clicky - y; 
				clickx = x; 
				clicky = y;
				parent.repaint();
			}
		});
	}

	public void paintComponent(Graphics g)
	{
		int width = (int) (img.getWidth(null)*scale);
		int height = (int) (img.getHeight(null)*scale);

		int defx = (int) (getWidth() - width)/2;
		int defy = (int) (getHeight() - height)/2;
		
		if(width < getWidth())
		{
			/* We're zoomed out a bit so keep it centered */
			dragx = defx;
			zx = 0;
		}
		else
		{
			if(dragx + zx > 0)
			{
				dragx = 0;
				zx = 0;
			}
			if(dragx + zx + width < getWidth())
			{
				dragx = (width - getWidth()) * -1; 
				zx = 0;
			}
		}
		if (height < getHeight()) {
			dragy = defy;
			zy = 0;
		}
		else
		{
			if(dragy + zy > 0)
			{
				dragy = 0;
				zy = 0;
			}			
			if(dragy + zy + height < getHeight())
			{
				dragy = (height - getHeight()) * -1; 
				zy = 0;
			}
		}
		/* Check each border to see if we should stop */
		xoff = dragx + zx;
		yoff = dragy + zy;

		Graphics2D gr = (Graphics2D) g;
		gr.drawImage(img, xoff, yoff, width, height, this);
	}

	public void zoomIn(int x, int y) {
		double xper = (xoff - x) / (img.getWidth(null)*scale);
		double yper = (yoff - y) / (img.getHeight(null)*scale);
		if(scale < 20)
		{
			if(scale < 1)
				scale += 0.1*scale;
			else
				scale += 0.1;
			zx += (int) ((xper*(img.getWidth(null)*scale) - (xoff-x)));
			zy += (int) ((yper*(img.getHeight(null)*scale) - (yoff-y)));
		}
	}

	public void zoomOut(int x, int y) {
		double xper = (xoff - x) / (img.getWidth(null)*scale);
		double yper = (yoff - y) / (img.getHeight(null)*scale);
		if(scale > 0.1)
		{
			if(scale < 1)
				scale -= 0.1*scale;
			else
				scale -= 0.1;
			zx -= (int) ((xoff-x) - (xper*(img.getWidth(null)*scale)));
			zy -= (int) ((yoff-y) - (yper*(img.getHeight(null)*scale)));
		}
	}
	
	private BufferedImage getImage(String path)
	{
		InputStream img;
		img = getClass().getResourceAsStream(path);
		
		if(img != null)
		{
			BufferedImage bimg;
			try{
				bimg = ImageIO.read(img);
			}
			catch(IOException e) {
				return null;
			}
			return bimg;
		}
		else
			return null;
	}
}

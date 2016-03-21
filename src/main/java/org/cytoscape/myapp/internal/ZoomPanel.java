package org.cytoscape.myapp.internal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

class ZoomPanel extends JPanel
{
	static final long serialVersionUID = 59820525894L;

	//the image being displayed
	private Image img;
	//base image dimensions for convenience
	private int iw, ih;
	//factor to scale image by
	private double scale;
	//maximum scale with signed 16 bit ints used for offset
	private double maxScale;
	//the final offset used to draw the image
	private int xoff, yoff; 
	//click coordinates are to give an offset from drag
	private int cx, cy; 
	//drag coordinates are for moving image with mouse
	private int dx, dy; 
	//zoom cordinates for offsetting while zooming
	private int zx, zy; 

	public ZoomPanel(String path, final Component parent)
	{
		BufferedImage image = getImage(path);
		img = image;
		iw = image.getWidth(null);
		ih = image.getHeight(null);
		scale = 400.0/(double)iw;
		setPreferredSize(new Dimension(400, 400));	
		maxScale = 32768/Math.max(iw,ih);
		
		addMouseWheelListener(new MouseAdapter() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoom(e.getWheelRotation(), e.getX(), e.getY());
				parent.repaint();
			}
		});
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e) {
				cx = e.getX(); 
				cy = e.getY();
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				dx -= cx - x; 
				dy -= cy - y; 
				cx = x; 
				cy = y;
				parent.repaint();
			}
		});
	}

	public void paintComponent(Graphics g)
	{
		//calculate the actual scaled dimensions
		int width = (int) (iw*scale);
		int height = (int) (ih*scale);

		//default centeed values for offset
		int defx = (int) (getWidth() - width)/2;
		int defy = (int) (getHeight() - height)/2;
		

		if(width < getWidth())
		//image width is less than frame width
		{
			dx = defx;
			zx = 0;
		}
		else
		//check to see if we need to prevent further dragging
		{
			if(dx + zx > 0)
			{
				dx = 0;
				zx = 0;
			}
			if(dx + zx + width < getWidth())
			{
				dx = (width - getWidth()) * -1; 
				zx = 0;
			}
		}
		if (height < getHeight()) 
		//image height is less than frame height
		{
			dy = defy;
			zy = 0;
		}
		else
		{
			if(dy + zy > 0)
			{
				dy = 0;
				zy = 0;
			}			
			if(dy + zy + height < getHeight())
			{
				dy = (height - getHeight()) * -1; 
				zy = 0;
			}
		}
		xoff = dx + zx;
		yoff = dy + zy;

		Graphics2D gr = (Graphics2D) g;
		gr.drawImage(img, xoff, yoff, width, height, this);
	}

	public void zoom(int io, int x, int y)
	{
		int xdiff = xoff - x;
		int ydiff = yoff - y;
		double xper = xdiff / (iw*scale);
		double yper = ydiff / (ih*scale);
		if(io < 0)
		{
			if(scale < maxScale)
			{
				if(scale < 1)
					scale += 0.1*scale;
				else
					scale += 0.1;
			}
		}
		else
		{
			if(scale > 0.1)
			{
				if(scale < 1)
					scale -= 0.1*scale;
				else
					scale -= 0.1;

			}
		}
		zx += (int) (xper*(iw*scale) - xdiff);
		zy += (int) (yper*(ih*scale) - ydiff);
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

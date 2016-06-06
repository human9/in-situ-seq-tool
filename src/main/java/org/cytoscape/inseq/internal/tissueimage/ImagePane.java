package org.cytoscape.inseq.internal.tissueimage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqActivator;

class ImagePane extends JPanel {

	private static final long serialVersionUID = 178665L;
	final public BufferedImage image;
	public BufferedImage paintedImage;
	private double scale = 1;
	public Dimension offset = new Dimension();
	private Dimension requested;
	public ZoomPane zp;
	private boolean rescaling;
	public boolean ratioIsCurrent;
	boolean timerDone = true;
	public Point selectedOrigin = new Point();
	public Point selectedFinish = new Point();
	public Rectangle rect;
	InseqActivator ia;
	Timer imageTimer = new Timer();
	
	public ImagePane(final BufferedImage image, InseqActivator ia) {
		this.image = image;
		this.ia = ia;
		this.paintedImage = image;
		this.scale = 400d/(double)(image.getHeight());
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		setSize();
	}
			

	@Override
	public void paintComponent(Graphics g) {

		Graphics2D gr = (Graphics2D) g;
		gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		//Although this looks nice it cuts the framerate a bit
		//gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		gr.setColor(Color.BLACK);
		gr.fillRect(0, 0, getWidth(), getHeight());

		
		int size = 8;
		int scaledOffset = (int)(size/2);
		Rectangle view = zp.getView();

		if(ia.pointsToDraw != null)
		{
			if(getWidth() > 4000 || getHeight() > 4000)
			{
				System.out.println("Close zoom mode");
				gr.drawImage(image, offset.width, offset.height, requested.width, requested.height, null);
				rescaling = false;
				int i = 0;
				for(String key : ia.pointsToDraw.keySet())
				{
					ArrayList<Point2D.Double> arr = ia.pointsToDraw.get(key);
					int hue = (int)(i*(360d/ia.pointsToDraw.size())+((i++%2)*90))%360;
					//System.out.println(i + " hue: " + hue);
					gr.setColor(Color.getHSBColor(hue/360f,1,1));
					
					for(Point2D.Double p : arr)
					{
						if(p.x*scale > view.x && p.y*scale > view.y && p.x*scale < view.x+view.width && p.y*scale < view.y+view.height) 
							gr.drawOval((int)(p.x*scale) - scaledOffset,(int)(p.y*scale) - scaledOffset,size,size);
					}
					gr.drawString(key, 6, i*14);
				}
			}
			else
			{
				if(rescaling) {
					System.out.println("Rescaling far zoom");
					paintedImage = new BufferedImage(requested.width, requested.height, image.getType());
					Graphics imgG = paintedImage.getGraphics();
					Graphics2D imgG2 = (Graphics2D) imgG;
					imgG.drawImage(image, 0, 0, requested.width, requested.height, null);
					rescaling = false;
					int i = 0;
					for(String key : ia.pointsToDraw.keySet())
					{
						ArrayList<Point2D.Double> arr = ia.pointsToDraw.get(key);
						int hue = (int)(i*(360d/ia.pointsToDraw.size())+((i++%2)*90))%360;
						//System.out.println(i + " hue: " + hue);
						imgG2.setColor(Color.getHSBColor(hue/360f,1,1));
						
						for(Point2D.Double p : arr)
						{
							//if(p.x*scale > view.x && p.y*scale > view.y && p.x*scale < view.x+view.width && p.y*scale < view.y+view.height) 
								imgG2.drawOval((int)(p.x*scale) - scaledOffset,(int)(p.y*scale) - scaledOffset,size,size);
						}
						imgG2.drawString(key, 6, i*14);
					}
					//imgG.dispose();
				}
				System.out.println("Far zoom mode");
				gr.drawImage(paintedImage, offset.width, offset.height, requested.width, requested.height, null);
			}
			
		}
		else
		{
			System.out.println("Null points mode");
			gr.drawImage(image, offset.width, offset.height, requested.width, requested.height, null);
		}

		gr.setColor(Color.YELLOW);
		gr.setStroke(new BasicStroke(2));
		int lx = (int) Math.round((selectedOrigin.x) * scale);
		int ly = (int) Math.round((selectedOrigin.y) * scale);
		int rx = (int) Math.round((selectedFinish.x) * scale);
		int ry = (int) Math.round((selectedFinish.y) * scale);
		rect = new Rectangle(Math.min(lx, rx) + offset.width, Math.min(ly, ry) + offset.height, Math.abs(lx - rx),
				Math.abs(ly - ry));
		
		int alx = selectedOrigin.x;
		int aly = selectedOrigin.y;
		int arx = selectedFinish.x;
		int ary = selectedFinish.y;
		ia.rect = new Rectangle(Math.min(alx, arx), Math.min(aly, ary), Math.abs(alx - arx), Math.abs(aly - ary));
		gr.drawRect(rect.x, rect.y, rect.width, rect.height);
		

		
		Color fill = new Color(255, 0, 0, 60);
		gr.setColor(fill);
		gr.fillRect(rect.x, rect.y, rect.width, rect.height);
		gr.dispose();

	}

	public void scaleUp() {
		
		/*Graphics g = image.createGraphics();

		if (ia.pointsToDraw != null)
		{
			g.clearRect(0, 0, image.getWidth(), image.getHeight());
			int diameter = (int)(1/scale * 10);
			int i = 0;
			for (String key : ia.pointsToDraw.keySet())
			{
				ArrayList<Point2D.Double> arr = ia.pointsToDraw.get(key);
				int hue = (int)(i*(360d/ia.pointsToDraw.size())+((i++%2)*90))%360;
				//System.out.println(i + " hue: " + hue);
				g.setColor(Color.getHSBColor(hue/360f,1,1));
				
				for(Point2D.Double p : arr)
				{
					g.drawOval((int)(p.x) - diameter/2,(int)(p.y) - diameter/2, diameter, diameter);
				}
				g.drawString(key, 6, i*14);
			}
		}

		g.dispose();
		repaint();*/
		if (scale <= 100) {
			rescaling = true;
			scale *= 1.1;
			if ((int) scale == 100)
				scale = 100;
		}
	}

	public void scaleDown() {
		if (scale > 0.01)
		{
			rescaling = true;
			scale *= 0.9;
		}
	}

	public double getScale() {
		return scale;
	}

	public void setSize() {
		Dimension minimum = getMinimumSize();
		offset = new Dimension();
		requested = new Dimension((int) Math.round(image.getWidth() * scale),
				(int) Math.round(image.getHeight() * scale));

		Dimension resize = new Dimension(requested);

		if (requested.width <= minimum.width) {
			ratioIsCurrent = false;
			resize.width = minimum.width;
			offset.width = (minimum.width - requested.width) / 2;
		}
		if (requested.height <= minimum.height) {
			ratioIsCurrent = false;
			resize.height = minimum.height;
			offset.height = (minimum.height - requested.height) / 2;
		}
		setPreferredSize(resize);
	}

	static public BufferedImage getImageFile(String path) {
		File input = new File(path);
		BufferedImage bimg;
		try {
			bimg = ImageIO.read(input);
		} catch (IOException e) {
			//JOptionPane.showMessageDialog(null, "Couldn't open the selected file.", "IO Error!",
			//		JOptionPane.WARNING_MESSAGE);
			return new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
		}
		if(bimg == null)
			return new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
		return bimg;
	}
}

package org.cytoscape.inseq.internal.imageselection;

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

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqActivator;

class ImagePane extends JPanel {

	private static final long serialVersionUID = 178665L;
	final public BufferedImage image;
	private double scale = 1;
	public Dimension offset = new Dimension();
	private Dimension requested;
	public boolean ratioIsCurrent;
	public Point selectedOrigin = new Point();
	public Point selectedFinish = new Point();
	public Rectangle rect;
	InseqActivator ia;
	
	public ImagePane(final BufferedImage image, InseqActivator ia) {
		this.image = image;
		this.ia = ia;
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

		gr.drawImage(image, offset.width, offset.height, requested.width, requested.height, null);

		gr.setColor(Color.YELLOW);
		gr.setStroke(new BasicStroke(2));
		int lx = (int) Math.round((selectedOrigin.x) * scale);
		int ly = (int) Math.round((selectedOrigin.y) * scale);
		int rx = (int) Math.round((selectedFinish.x) * scale);
		int ry = (int) Math.round((selectedFinish.y) * scale);
		rect = new Rectangle(Math.min(lx, rx) + offset.width, Math.min(ly, ry) + offset.height, Math.abs(lx - rx),
				Math.abs(ly - ry));
		gr.drawRect(rect.x, rect.y, rect.width, rect.height);
		
		int size = 8;
		int scaledOffset = (int)(size/2*scale);

		if(ia.pointsToDraw!=null)
		{
			int i = 0;
			for(String key : ia.pointsToDraw.keySet())
			{
				ArrayList<Point2D.Double> arr = ia.pointsToDraw.get(key);
				int hue = (int)(i*(360d/ia.pointsToDraw.size())+((i++%2)*90))%360;
				System.out.println(i + " hue: " + hue);
				gr.setColor(Color.getHSBColor(hue/360f,1,1));
				
				for(Point2D.Double p : arr)
				{
					gr.drawOval((int)(p.x*scale) + offset.width - scaledOffset,(int)(p.y*scale) + offset.height - scaledOffset,size,size);
				}
				gr.drawString(key, 6 + offset.width, i*14 + offset.height);
			}
			//TODO: this
		}
		
		Color fill = new Color(255, 0, 0, 60);
		gr.setColor(fill);
		gr.fillRect(rect.x, rect.y, rect.width, rect.height);

	}

	public void scaleUp() {
		if (scale <= 100) {
			scale *= 1.1;
			if ((int) scale == 100)
				scale = 100;
		}
	}

	public void scaleDown() {
		if (scale > 0.01)
			scale *= 0.9;
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
			JOptionPane.showMessageDialog(null, "Couldn't open the selected file.", "IO Error!",
					JOptionPane.WARNING_MESSAGE);
			return new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
		}
		return bimg;
	}
}

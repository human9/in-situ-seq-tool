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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import edu.wlu.cs.levy.CG.KeySizeException;

public class ImagePane extends JPanel {

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
	private InseqSession session;

	InseqActivator ia;
	Timer imageTimer = new Timer();
	
	public ImagePane(final BufferedImage image, InseqActivator ia) {
		this.image = image;
		this.session = ia.getSession();
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

		if(session.edgeSelection != null)
		{
			gr.drawImage(image, offset.width, offset.height, requested.width, requested.height, null);
			List<Integer> ints = new ArrayList<Integer>();
			List<Color> colours = new ArrayList<Color>();
			for(String name : session.edgeSelection) {
				int i = (int)((session.edgeSelection.indexOf(name)+1)*(360d/session.edgeSelection.size()));
				ints.add(i);
			}
			for(Integer i : ints) {
				colours.add(Color.getHSBColor(((i - ints.get(0))%360)/360f, 1, 1));
			}
			if(getWidth() > 3000 || getHeight() > 3000)
			{
				System.out.println("Close zoom mode");
				rescaling = false;
				try {
					for(Transcript t : session.tree.range(new double[]{view.x/scale,view.y/scale}, new double[]{view.x/scale + view.width/scale, view.y/scale + view.height/scale}))
					{
						if(t.neighbours.size() < 2) continue;
						//if(Double.compare(t.distance, session.distance) != 0) continue;
					
						int index = session.edgeSelection.indexOf(t.name);
						if(index < 0) continue;
						gr.setColor(colours.get(index));

						gr.setColor(colours.get(session.edgeSelection.indexOf(t.name)));
						gr.drawOval((int)(t.pos.x*scale) - scaledOffset + offset.width,(int)(t.pos.y*scale) - scaledOffset + offset.height,size,size);

					}
				}
				catch (KeySizeException e) {
					e.printStackTrace();
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
					try {
						for(Transcript t : session.tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE}))
						{
							if(t.neighbours.size() < 2) continue;
						//	if(Double.compare(t.distance, session.distance) != 0) continue;
						
							int index = session.edgeSelection.indexOf(t.name);
							if(index < 0) continue;
							imgG2.setColor(colours.get(index));
							imgG2.drawOval((int)(t.pos.x*scale) - scaledOffset,(int)(t.pos.y*scale) - scaledOffset,size,size);
						}
					}
					catch (KeySizeException e) {
						e.printStackTrace();
					}
				}
				System.out.println("Far zoom mode");
				gr.drawImage(paintedImage, offset.width, offset.height, requested.width, requested.height, null);
			}
			for(String name : session.edgeSelection)
			{
				gr.setColor(colours.get(session.edgeSelection.indexOf(name)));
				gr.drawString(name, 6, (session.edgeSelection.indexOf(name)+1)*14);
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
		session.rectangleSelection = new Rectangle(Math.min(alx, arx), Math.min(aly, ary), Math.abs(alx - arx), Math.abs(aly - ary));
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

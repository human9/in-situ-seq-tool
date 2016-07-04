package org.cytoscape.inseq.internal.tissueimage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;

import edu.wlu.cs.levy.CG.KeySizeException;

public class ImagePane extends JPanel {

	private static final long serialVersionUID = 178665L;
	final public BufferedImage image;
	public BufferedImage paintedImage;
	private double scale = 1;
	public Dimension offset = new Dimension();
	private Dimension requested;
	public ZoomPane zp;
	public boolean ratioIsCurrent;
	public boolean zoomAltered;
	boolean timerDone = true;
	public Point selectedOrigin = new Point();
	public Point selectedFinish = new Point();
	public Rectangle rect;
	private InseqSession session;
	private boolean cacheStopped;
	private boolean cacheAvailable;
	private boolean showNodes = false;
	private double pointScale = 1;
	private Transcript pointClicked;

	public ImagePane(final BufferedImage image, InseqSession s, Dimension parent) {
		this.image = image;
		this.session = s;
		this.paintedImage = image;
		this.scale = Math.min((double)parent.width/image.getWidth(), (double)parent.height/image.getHeight());
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		setSize();
	}

	public void setPointScale(double d) {
		pointScale = d;
	}

	public void setShowNodes(boolean b) {
		showNodes = b;
	}
	
	public void stopCache() {
		cacheStopped = true;
	}

	public void smartDraw(TypeNetwork sel, Transcript t, Graphics2D g, int size, int scaledOffset, Dimension off) {
		if(showNodes && session.nodeSelection != null && session.nodeSelection.contains(t.name)) {
			g.setColor(session.getGeneColour(t.name));
			g.drawOval((int)(pointScale * t.pos.x*scale) - scaledOffset + off.width,(int)(pointScale * t.pos.y*scale) - scaledOffset + off.height,size,size);
		}

		else {
			if(t.getNeighboursForNetwork(sel) == null || t.getNeighboursForNetwork(sel).size() < 1 || t.getSelection(sel) != sel.getSelection() || (!session.edgeSelection.keySet().contains(t.name)) && (!session.nodeSelection.contains(t.name))) return;
			for(Transcript n : t.getNeighboursForNetwork(sel)) {
				if(session.edgeSelection.get(t.name) != null && session.edgeSelection.get(t.name).contains(n.name)) {
					g.setColor(session.getGeneColour(t.name));
					g.drawOval((int)(pointScale * t.pos.x*scale) - scaledOffset + off.width,(int)(pointScale * t.pos.y*scale) - scaledOffset + off.height,size,size);
				}
				else if(session.nodeSelection.contains(t.name) && n.name.equals(t.name)) {
					g.setColor(session.getGeneColour(t.name));
					g.drawOval((int)(pointScale * t.pos.x*scale) - scaledOffset + off.width,(int)(pointScale * t.pos.y*scale) - scaledOffset + off.height,size,size);
				}
			}
		}
	}

	public void cacheImage() {
		if(getWidth() < 3000 || getHeight() < 3000) {
			cacheStopped = false;
			paintedImage = new BufferedImage(requested.width, requested.height, image.getType());
			Graphics imgG = paintedImage.getGraphics();
			Graphics2D imgG2 = (Graphics2D) imgG;
			imgG.drawImage(image, 0, 0, requested.width, requested.height, null);
			cacheAvailable = false;
			TypeNetwork sel = session.getNetwork(session.getSelectedNetwork());
			int size = 8;
			int scaledOffset = (int)(size/2);
			try {
				for(Transcript t : session.tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE}))
				{
					if(cacheStopped) return;
					smartDraw(sel, t, imgG2, size, scaledOffset, new Dimension(0,0));						
				}
			}
			catch (KeySizeException e) {
				e.printStackTrace();
			}
			imgG2.dispose();
			imgG.dispose();
			cacheAvailable = true;
		}
	}

	@Override
	public void paintComponent(Graphics g) {

		zoomAltered = false;
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
			TypeNetwork sel = session.getNetwork(session.getSelectedNetwork());
			
			if(cacheAvailable) {
				gr.drawImage(paintedImage, offset.width, offset.height, requested.width, requested.height, null);
			}
			else
			{
				gr.drawImage(image, offset.width, offset.height, requested.width, requested.height, null);
				try {
					for(Transcript t : session.tree.range(new double[]{view.x/scale/pointScale,view.y/scale/pointScale}, new double[]{view.x/scale/pointScale + view.width/scale/pointScale, view.y/scale/pointScale + view.height/scale/pointScale}))
					{
						if(zoomAltered) break;
						smartDraw(sel, t, gr, size, scaledOffset, offset);
					}
				}
				catch (KeySizeException e) {
					e.printStackTrace();
				}
			}
			List<String> names = new ArrayList<String>();
			names.addAll(session.edgeSelection.keySet());
			for(String name : session.nodeSelection) {
				if(names.contains(name))
					continue;
				else
					names.add(name);
			}

			if(pointClicked != null) {
				System.out.println(pointClicked);
				size = 12;
				scaledOffset = (int)(size/2);
				gr.setStroke(new BasicStroke(2));
				gr.setColor(session.getGeneColour(pointClicked.name));
				Point drawLocation = actualPointToScaledPixel(pointClicked.pos);
				gr.drawOval(drawLocation.x - scaledOffset, drawLocation.y - scaledOffset, size, size);
				gr.drawString(pointClicked.name, drawLocation.x + size + 2, drawLocation.y+6);
			}
			for(String name : names)
			{
				gr.setColor(session.getGeneColour(name));
				gr.drawString(name, view.x+6, view.y+(names.indexOf(name)+1)*14);
			}
			
		}
		else
		{
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
		
		session.setSelection(new Rectangle(Math.min(alx, arx), Math.min(aly, ary), Math.abs(alx - arx), Math.abs(aly - ary)));
		gr.drawRect(rect.x, rect.y, rect.width, rect.height);
		
		Color fill = new Color(255, 0, 0, 60);
		gr.setColor(fill);
		gr.fillRect(rect.x, rect.y, rect.width, rect.height);

	}

	public void forceRepaint() {
		cacheAvailable = false;
		repaint();
	}

	public void scaleUp() {
		if (scale <= 100) {
			cacheAvailable = false;
			scale *= 1.1;
			if ((int) scale == 100)
				scale = 100;
		}
	}

	public void scaleDown() {
		if (scale > 0.01)
		{
			cacheAvailable = false;
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
	
	/** Translates a VIEWPORT PIXEL into an actual point in the tree.
	 *  There's no need to have a method for scaled points to tree points
	 *  as that should never be required.
	 */
	public Point2D.Double viewportPixelPointToActual(Point p) {
		Point vp = zp.getViewport().getViewPosition();
		double x = ((p.x + vp.x - offset.width) / getScale() / pointScale);
		double y = ((p.y + vp.y - offset.height) / getScale() / pointScale);
		return new Point2D.Double(x,y);
	}

	// Translates an actual tree point into a PIXEL WITHIN THE SCALED IMAGE
	public Point actualPointToScaledPixel(Point2D.Double actual) {
		int x = (int)(pointScale * actual.x*scale) + offset.width;
		int y = (int)(pointScale * actual.y*scale) + offset.height;
		return new Point(x,y);
	}
	
	// Translates an actual tree point into a VIEWPORT PIXEL
	public Point actualPointToViewportPixel(Point2D.Double actual) {
		Point vp = zp.getViewport().getViewPosition();
		int x = (int)(pointScale * actual.x*scale) + offset.width - vp.x;
		int y = (int)(pointScale * actual.y*scale) + offset.height - vp.y;
		return new Point(x,y);
	}

	public double euclideanDistance(Point a, Point b) {
		double sqrdist = Math.pow((a.x - b.x) , 2) + Math.pow((a.y - b.y), 2);
		return Math.sqrt(sqrdist);
	}

	public void clickAtPoint(Point p) {
		Point2D.Double actual = viewportPixelPointToActual(p);
		Transcript x;
		try {
			x = session.tree.nearest(new double[]{actual.x, actual.y});
		}
		catch (KeySizeException e) {
			x = null;
		}
		double dist = euclideanDistance(actualPointToViewportPixel(x.pos), p);
		if(dist < 10) {
			pointClicked = x;
		}
		else {
			pointClicked = null;
		}
		repaint();
	}

	static public BufferedImage getImageFile(String path) {
		if(path == null) return new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);

		File input = new File(path);
		BufferedImage bimg;
		BufferedImage optImage;
		try {
			bimg = ImageIO.read(input);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				
			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();

			optImage = gc.createCompatibleImage(bimg.getWidth(), bimg.getHeight());
			
			// Copy image to buffered image
			Graphics g = optImage.createGraphics();
			g.drawImage(bimg, 0, 0, null);
			g.dispose();


		} catch (IOException|NullPointerException e) {
			//JOptionPane.showMessageDialog(null, "Couldn't open the selected file.", "IO Error!",
			//		JOptionPane.WARNING_MESSAGE);
			return new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
		}
		
		return optImage;
	}
}

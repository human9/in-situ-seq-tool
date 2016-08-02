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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.SymbolFactory;

import edu.wlu.cs.levy.CG.KeySizeException;

public class ImagePane extends JPanel {

	private static final long serialVersionUID = 178665L;
	final public BufferedImage image;
	public BufferedImage paintedImage;
	private double scale = 1;
	public Dimension offset = new Dimension();
	private Dimension requested;
	public ZoomPane zp;
	public SelectionPanel sp;
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
		if(image != null) {
			session.min = new Dimension(image.getWidth(), image.getHeight());
		}
		this.scale = Math.min((double)parent.width/session.min.width, (double)parent.height/session.min.height);
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

	private void drawIfExists(Graphics2D gr, BufferedImage img, int sx, int sy, int width, int height) {
		if(img == null) return;
		gr.drawImage(img, sx, sy, width, height, null);
	}

	/** 
	 * Finds whether the given transcript should be made visible, based on the given TypeNetwork.
	 * Returns true if it should be visible, or false if it should be hidden. This method is called by SmartDraw.
	 */
	private boolean isActive(TypeNetwork sel, Transcript t) {

		if(showNodes && session.nodeSelection != null && session.nodeSelection.contains(t.name)) return true;

		if(t.getNeighboursForNetwork(sel) == null || t.getNeighboursForNetwork(sel).size() < 1	|| t.getSelection(sel) != sel.getSelection() || 
				(!session.edgeSelection.keySet().contains(t.name)) && (!session.nodeSelection.contains(t.name))) return false;
		
		for(Transcript n : t.getNeighboursForNetwork(sel)) {
			if(session.edgeSelection.get(t.name) != null && session.edgeSelection.get(t.name).contains(n.name)) return true;
			if(session.nodeSelection.contains(t.name) && n.name.equals(t.name)) return true;
		}

		return false;
	}

	/** 
	 * Given a list of transcripts, draws only those within the current selection, with correct colours.
	 */

	private void smartDraw(TypeNetwork sel, Transcript t, Graphics2D g, int size, int scaledOffset, Dimension off) {
		if(isActive(sel, t)) {
			g.setColor(session.getGeneColour(t.name));
			g.draw(SymbolFactory.makeSymbol(session.getGeneSymbol(t.name), (int)(pointScale * t.pos.x*scale) - scaledOffset + off.width,(int)(pointScale * t.pos.y*scale) - scaledOffset + off.height,size,size));
		}
	}

	// TODO: if density too high randomly drop points but display them once caching kicks in
	public void cacheImage() {
		List<Transcript> viewList;
		Rectangle view = zp.getView();
		try {
			viewList = session.tree.range(new double[]{view.x/scale/pointScale,view.y/scale/pointScale}, new double[]{view.x/scale/pointScale + view.width/scale/pointScale, view.y/scale/pointScale + view.height/scale/pointScale});
		}
		catch (KeySizeException e) {
			e.printStackTrace();
			return;
		}
		if(requested.width * requested.height < 1e8) {
            System.out.println("Caching, " + requested.width*requested.height);
			cacheStopped = false;

			paintedImage = new BufferedImage(requested.width, requested.height, (image == null) ? BufferedImage.TYPE_BYTE_INDEXED : image.getType());
			Graphics imgG = paintedImage.getGraphics();
			Graphics2D imgG2 = (Graphics2D) imgG;
			drawIfExists(imgG2, image, 0, 0, requested.width, requested.height);
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
            repaint();
		}
        else {


        }
	}

    boolean timeout = false;
    int delay = 12; // entire method must complete within ~16ms to meet 60Hz refresh rates
    
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
				drawIfExists(gr, paintedImage, offset.width, offset.height, requested.width, requested.height);
			}
			else
			{
				drawIfExists(gr, image, offset.width, offset.height, requested.width, requested.height);
				try {
                    timeout = false;
                    TimerTask timeoutTask = new TimerTask() {
                        public void run() {
                            timeout = true;
                        }
                    };

                    List<Transcript> range =  session.tree.range(new double[]{view.x/scale/pointScale,view.y/scale/pointScale}, new double[]{view.x/scale/pointScale + view.width/scale/pointScale, view.y/scale/pointScale + view.height/scale/pointScale});
                        
                    // This timer is to prevent lag during zooming/scrolling
                    // Once time is up, drawing will be stopped.
                    Timer timeoutTimer = new Timer();
                    timeoutTimer.schedule(timeoutTask, delay);

                    // For huge ranges, shuffling the entire list takes too long
                    // So instead we just draw transcripts at random until timeout
                    if(range.size() > 4e5) {
                        
                        Random r = new Random();

                        for(int i = 0; i < range.size(); i++)
                        {
                            if(timeout) {
                                Thread p = new Thread(new Runnable() {
                                    public void run()
                                    {
                                        stopCache();
                                        cacheImage();
                                    }
                                });
                                p.start();
                                break;
                            }
                            
                            int index = r.nextInt(range.size());
                            Transcript t = range.get(index);
                            smartDraw(sel, t, gr, size, scaledOffset, offset);
                        }
                    }
                    // But for average sized lists, we may as well shuffle the whole thing 
                    else if (range.size() > 2e4) {
                        Collections.shuffle(range);
                        for(Transcript t : range) {
                            if(timeout) {
                                Thread p = new Thread(new Runnable() {
                                    public void run()
                                    {
                                        stopCache();
                                        cacheImage();
                                    }
                                });
                                p.start();
                                break;
                            }

						    
                            smartDraw(sel, t, gr, size, scaledOffset, offset);
                        }
                    }
                    // If it's really small there's no point randomising anything
                    // as we'll be able to draw it all in time.
                    else {
                        for(Transcript t : range) {
						    
                            smartDraw(sel, t, gr, size, scaledOffset, offset);
                        }
                    }
                    timeoutTimer.cancel();
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
				gr.setStroke(new BasicStroke(3));
				Point drawLocation = actualPointToScaledPixel(pointClicked.pos);
				smartDraw(sel, pointClicked, gr, size, scaledOffset, offset);
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
			drawIfExists(gr, image, offset.width, offset.height, requested.width, requested.height);
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
		
		session.setSelection(new Rectangle((int)(Math.min(alx, arx)/pointScale), (int)(Math.min(aly, ary)/pointScale), (int)(Math.abs(alx - arx)/pointScale), (int)(Math.abs(aly - ary)/pointScale)));
		if(rect.width > 1 && rect.height > 1) {
			gr.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
		
		Color fill = new Color(255, 0, 0, 60);
		gr.setColor(fill);
		gr.fillRect(rect.x, rect.y, rect.width, rect.height);

	}

	public void forceRepaint() {
        System.out.println("Repaint forced");
		cacheAvailable = false;
		repaint();
	}

	public void scaleUp() {
		if (scale <= 100) {
            stopCache();
			cacheAvailable = false;
			scale *= 1.06;
			if ((int) scale == 100)
				scale = 100;
		}
	}

	public void scaleDown() {
		if (scale > 0.01)
		{
            stopCache();
			cacheAvailable = false;
			scale *= 0.94;
		}
	}

	public double getScale() {
		return scale;
	}

	public void setSize() {
		Dimension minimum = getMinimumSize();
		offset = new Dimension();
		requested = new Dimension((int) Math.round(session.min.width * scale),
				(int) Math.round(session.min.height * scale));

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
	
	public double euclideanDistance(Point2D.Double a, Point2D.Double b) {
		double sqrdist = Math.pow((a.x - b.x) , 2) + Math.pow((a.y - b.y), 2);
		return Math.sqrt(sqrdist);
	}

	public void clickAtPoint(Point p) {

		int viewportDist = 10;
		double trueDist = euclideanDistance(
			viewportPixelPointToActual(new Point(0,0)),
			viewportPixelPointToActual(new Point(viewportDist,0))
		);
		Point2D.Double actual = viewportPixelPointToActual(p);
		List<Transcript> list;
		try {
			list = session.tree.nearestEuclidean(new double[]{actual.x, actual.y}, Math.pow(trueDist, 2));
			Collections.reverse(list);
		}
		catch (KeySizeException e) {
			list = null;
		}
		
		if(list == null || list.size() < 1) {
			pointClicked = null;
			repaint();
		}

		for (Transcript x : list) {
			if(isActive(session.getNetwork(session.getSelectedNetwork()), x)) {
				pointClicked = x;
				sp.setSelected(pointClicked);
				repaint();
				return;
			}
		}

		pointClicked = null;
		sp.setSelected(pointClicked);
		repaint();
	}


}

package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** 
 * A component to display, zoom/drag and make selections on an image.
 * ZoomPanel overrides JPanel's painComponent method to accomplish this.
 */

class ZoomPanel extends JPanel
{
	static final long serialVersionUID = 403245914L;
	//the image being displayed
	private Image img;
	
	//base image dimensions for convenience
	private Dimension imgDimension; 

	//factor to scale image by
	private double scale;
	
	//maximum scale with signed 16 bit ints used for offset
	private double maxScale;
	
	//the final offset used to draw the image
	private IterPoint imgOffset;
	
	//click coordinates are to give an offset from drag
	private IterPoint mouseClick = new IterPoint();
 
	//drag coordinates are for moving image with mouse
	private IterPoint mouseDrag = new IterPoint();
 
	//zoom cordinates for offsetting while zooming
	private IterPoint zoom = new IterPoint(); 
 
	//toggle for displaying information
	private boolean info;
	private boolean button1;
	private boolean button3;

	private IterPoint select = new IterPoint();
	private IterPoint small = new IterPoint();
	private IterPoint origin = new IterPoint();
	private IterPoint selectDrag = new IterPoint();
	private IterPoint selectedDims = new IterPoint();
	private IterPoint selectedOrigin = new IterPoint();
	
	public final Component parent;
	
	private String path;

	public ZoomPanel(String path, final Component parent, boolean isResource)
	{
		this.parent = parent;
		this.path = path;
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		BufferedImage image;
		if(isResource)
			 image = getImage(path);
		else
			image = getImageFile(path);
		if (image == null)
			return;

		this.img = image;
		imgDimension = new Dimension(image.getWidth(null), image.getHeight(null));
		Dimension pd;
		if(parent.getBounds().getSize().width > 0)
			pd = parent.getBounds().getSize();
		else
			pd = new Dimension(400, 400);

		if(imgDimension.width > imgDimension.height)
			scale = pd.width/(double)imgDimension.width;
		else
			scale = pd.height/(double)imgDimension.height;
		setPreferredSize(pd);	
		maxScale = 32768/Math.max(imgDimension.width,imgDimension.height);
		
		if(parent.getBounds().getSize().width > 0)
			pd = parent.getBounds().getSize();
		else
			pd = new Dimension(400, 400);

		if(imgDimension.width > imgDimension.height)
			scale = pd.width/(double)imgDimension.width;
		else
			scale = pd.height/(double)imgDimension.height;
		setPreferredSize(pd);	
		maxScale = 32768/Math.max(imgDimension.width,imgDimension.height);
		
		addMouseWheelListener(new MouseAdapter() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoom(e.getWheelRotation(), e.getX(), e.getY());
				parent.repaint();
			}
		});
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					button1 = true;
					mouseClick = new IterPoint(e.getX(), e.getY());
				}
				if(e.getButton() == MouseEvent.BUTTON3) {
					button3 = true;
					selectDrag.move(0,0);
					selectedOrigin.move(0,0);
					selectedDims.move(0,0);
					select.setLocation(e.getPoint());
					parent.repaint();
				}
			}
			public void mouseReleased(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					button1 = false;
				}
				if(e.getButton() == MouseEvent.BUTTON3) {
					button3 = false;
				}
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent e) {
				if(button1) {
					mouseDrag.translate(-(mouseClick.x - e.getX()), -(mouseClick.y - e.getY()));
					mouseClick.setLocation(e.getPoint());
					parent.repaint();
				}
				if(button3) {
					selectDrag.move(select.x - e.getX(), select.y - e.getY());
					origin.setLocation(select);
					IterPoint eventPoint = new IterPoint(e.getPoint());
					for(IterPoint.Coord c : IterPoint.Coord.values())
					{
						if(selectDrag.getPoint(c) > 0)
							origin.setPoint(c, eventPoint.getPoint(c));
						else
							selectDrag.setPoint(c, Math.abs(selectDrag.getPoint(c)));
						selectedDims.setPoint(c, (int)Math.round(selectDrag.getPoint(c)/scale));
						selectedOrigin.setPoint(c, (int)Math.round((imgOffset.getPoint(c)-origin.getPoint(c))/scale));
					}
					parent.repaint();
				}
			}
		});
	}

	IterPoint getOffset(IterPoint scaled, IterPoint panel)
	{
		IterPoint offset = new IterPoint();
		for(IterPoint.Coord c : IterPoint.Coord.values())
		{
			if(scaled.getPoint(c) < panel.getPoint(c))
			//image width is less than frame width
			{
				mouseDrag.setPoint(c, (int) (panel.getPoint(c) - scaled.getPoint(c))/2);
				zoom.setPoint(c, 0);
				small.setPoint(c, 1);
			}
			else
			//check to see if we need to prevent further dragging
			{
				small.setPoint(c, 0);
				if(mouseDrag.getPoint(c) + zoom.getPoint(c) > 0)
				{
					mouseDrag.setPoint(c, 0);
					zoom.setPoint(c, 0);
				}
				if(mouseDrag.getPoint(c) + zoom.getPoint(c) + scaled.getPoint(c) < panel.getPoint(c))
				{
					mouseDrag.setPoint(c, (scaled.getPoint(c) - panel.getPoint(c)) * -1); 
					zoom.setPoint(c, 0);
				}
			}
			offset.setPoint(c, zoom.getPoint(c)+mouseDrag.getPoint(c));
		}
		return offset;
	}

	@Override
	public void paintComponent(Graphics g)
	{
		// get up to date scaled dimensions
		IterPoint scaled = new IterPoint(imgDimension.width*scale, imgDimension.height*scale);

		// get up to date panel dimensions
		IterPoint panel = new IterPoint(getWidth(), getHeight());

		imgOffset = getOffset(scaled, panel);

		Graphics2D gr = (Graphics2D) g;
		gr.setColor(Color.BLACK);
		gr.fillRect(0, 0, getWidth(), getHeight());
		gr.drawImage(img, imgOffset.x, imgOffset.y, scaled.x, scaled.y, this);
		
		gr.setColor(Color.YELLOW);
		if(selectedDims.x != 0 && selectedDims.y != 0)
		{
			if(button3)
				gr.drawRect(origin.x, origin.y, selectDrag.x, selectDrag.y);
			else
			{
				gr.drawRect((int)Math.round(imgOffset.x - (selectedOrigin.x * scale)), (int) Math.round(imgOffset.y - (selectedOrigin.y * scale)), (int)Math.round(selectedDims.x * scale), (int)Math.round(selectedDims.y * scale));
			}
		}

		
		if(info)
		{
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					        RenderingHints.VALUE_ANTIALIAS_ON);
			gr.drawString("File: \"" +path+"\"" + " Dimensions: " + imgDimension.width + "x" + imgDimension.height, 2, 12);
			gr.drawString("Zoom: " + (int)(scale*100) + "%", 2, 26);
			IterPoint left = new IterPoint();
			IterPoint right = new IterPoint();
			IterPoint dims = new IterPoint(imgDimension.width, imgDimension.height);
			for(IterPoint.Coord c : IterPoint.Coord.values())
			{
				if(small.getPoint(c) == 1)
				{
					left.setPoint(c, 0);
					right.setPoint(c, dims.getPoint(c));
				}
				else
				{
					left.setPoint(c, (int)(imgOffset.getPoint(c)/scale)*-1);
					right.setPoint(c, (int)(((imgOffset.getPoint(c)-panel.getPoint(c))/scale)*-1));
				}
			}
			gr.drawString("Visible: " + "("+left.x+","+left.y+") -> ("+right.x+","+right.y+")", 2, 40);
			gr.drawString("Selected: " + "("+-selectedOrigin.x+","+-selectedOrigin.y+") -> ("+((-selectedOrigin.x)+selectedDims.x)+","+((-selectedOrigin.y)+selectedDims.y)+")", 2, 54);
		}
	}

	public void toggleInfo()
	{
		info = !info;
	}

	private void zoom(int io, int xzoom, int yzoom)
	{
		IterPoint diff = new IterPoint(imgOffset.x-xzoom, imgOffset.y-yzoom);
		double ratioX = diff.x / (imgDimension.width*scale);
		double ratioY = diff.y / (imgDimension.height*scale);
		if(io < 0)
		{
			if(scale < maxScale && scale <= 3)
			{
				if(scale < 1)
					scale += 0.1*scale;
				else
					scale += 0.1;
				if((int)scale == 3)
					scale = 3;
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
		zoom.translate((int) (ratioX*(imgDimension.width*scale) - diff.x), (int) (ratioY*(imgDimension.height*scale) - diff.y));
		
		// in case we don't draw before zoom is called again
		imgOffset.move(mouseDrag.x+zoom.x, mouseDrag.y+zoom.y);
	}
		
	private BufferedImage getImage(String path) {
		InputStream img;
		img = getClass().getResourceAsStream(path);

		if (img != null) {
			BufferedImage bimg;
			try {
				bimg = ImageIO.read(img);
			} catch (IOException e) {
				return null;
			}
			return bimg;
		} else
			return null;
	}

	private BufferedImage getImageFile(String path)
	{
		File input = new File(path);
		BufferedImage bimg;
		try{
			bimg = ImageIO.read(input);
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null,"Couldn't open the selected file.","IO Error!",JOptionPane.WARNING_MESSAGE);

			return null;
		}
		return bimg;
	}
}

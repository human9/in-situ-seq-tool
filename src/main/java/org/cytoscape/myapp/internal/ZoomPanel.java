package org.cytoscape.myapp.internal;

import java.awt.Color;
import java.awt.Component;
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


class ZoomPanel extends JPanel
{
	int x = 0; int y = 1;
	//the image being displayed
	private Image img;
	
	//base image dimensions for convenience
	private int[] imageDim = new int[2]; 

	//factor to scale image by
	private double scale;
	
	//maximum scale with signed 16 bit ints used for offset
	private double maxScale;
	
	//the final offset used to draw the image
	private int[] offset = new int[2]; 
	
	//click coordinates are to give an offset from drag
	private int[] click = new int[2]; 
 
	//drag coordinates are for moving image with mouse
	private int[] drag = new int[2]; 
 
	//zoom cordinates for offsetting while zooming
	private int[] zoom = new int[2]; 
 
	//toggle for displaying information
	private boolean info=false;
	private boolean button1 = false;
	private boolean button3 = false;
	private int[] select = new int[2];
	private int[] origin = new int[2];
	private int[] selectDrag = new int[2];
	int[] selectedDims = new int[2];
	int[] selectedOrigin = new int[2];
	public final Component parent;

	
	private String path;

	public ZoomPanel(String path, final Component parent, boolean isResource)
	{
		this.parent = parent;
		this.path = path;
		BufferedImage image;
		if(isResource)
			 image = getImage(path);
		else
			image = getImageFile(path);
		if (image == null)
			return;

		img = image;
		imageDim[x] = image.getWidth(null);
		imageDim[y] = image.getHeight(null);
		Dimension pd;
		if(parent.getBounds().getSize().width > 0)
			pd = parent.getBounds().getSize();
		else
			pd = new Dimension(400, 400);

		if(imageDim[x] > imageDim[y])
			scale = pd.width/(double)imageDim[0];
		else
			scale = pd.height/(double)imageDim[1];
		setPreferredSize(pd);	
		maxScale = 32768/Math.max(imageDim[x],imageDim[y]);
		
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
					click[x] = e.getX();
					click[y] = e.getY();
				}
				if(e.getButton() == MouseEvent.BUTTON3) {
					button3 = true;
					for(int i = 0; i < 2; i++)
					{
						selectDrag[i] = 0;
						selectedOrigin[i] = 0;
						selectedDims[i] = 0;
					}
					select[x] = e.getX();
					select[y] = e.getY();
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
					int xdrag = e.getX();
					int ydrag = e.getY();
					drag[x] -= click[x] - xdrag; 
					drag[y] -= click[y] - ydrag; 
					click[x] = xdrag; 
					click[y] = ydrag;
					parent.repaint();
				}
				if(button3) {
					int[] current = new int[2];
					current[x] = e.getX();
					current[y] = e.getY();
					selectDrag[x] = select[x] - current[x]; 
					selectDrag[y] = select[y] - current[y]; 
					for(int i = 0; i < 2; i++)
					{
						if(selectDrag[i] > 0)
							origin[i] = current[i];
						else
						{
							origin[i] = select[i];
							selectDrag[i] = Math.abs(selectDrag[i]);
						}
						selectedDims[i] = (int) Math.round(selectDrag[i] / scale);
						selectedOrigin[i] = (int) Math.round((offset[i] - origin[i]) / scale);
					}
					parent.repaint();
				}
			}
		});
	}

	int[] getOffset(boolean[] small, int[] scaled, int[] panel)
	{
		int[] offset = new int[2];
		for(int i = 0; i < 2; i ++)
		{
			if(scaled[i] < panel[i])
			//image width is less than frame width
			{
				drag[i] = (int) (panel[i] - scaled[i])/2;
				zoom[i] = 0;
				small[i] = true;
			}
			else
			//check to see if we need to prevent further dragging
			{
				if(drag[i] + zoom[i] > 0)
				{
					drag[i] = 0;
					zoom[i] = 0;
				}
				if(drag[i] + zoom[i] + scaled[i] < panel[i])
				{
					drag[i] = (scaled[i] - panel[i]) * -1; 
					zoom[i] = 0;
				}
			}
			offset[i] = zoom[i]+drag[i];
		}
		return offset;
	}

	public void paintComponent(Graphics g)
	{
		// get up to date scaled dimensions
		int[] scaled = new int[2];
		scaled[x] = (int) (imageDim[x]*scale);
		scaled[y] = (int) (imageDim[y]*scale);

		// get up to date panel dimensions
		int[] panel = new int[2];
		panel[x] = getWidth();
		panel[y] = getHeight();

		boolean[] small = new boolean[2];

		offset = getOffset(small, scaled, panel);
		
		Graphics2D gr = (Graphics2D) g;
		
		gr.setColor(Color.BLACK);
		gr.fillRect(0, 0, getWidth(), getHeight());

		gr.drawImage(img, offset[x], offset[y], scaled[x], scaled[y], this);
		
		gr.setColor(Color.YELLOW);
		if(selectedDims[x] != 0 && selectedDims[y] != 0)
		{
			if(button3)
				gr.drawRect(origin[x], origin[y], selectDrag[x], selectDrag[y]);
			else
			{
				gr.drawRect((int)Math.round(offset[x] - (selectedOrigin[x] * scale)), (int) Math.round(offset[y] - (selectedOrigin[y] * scale)), (int)Math.round(selectedDims[x] * scale), (int)Math.round(selectedDims[y] * scale));
			}
		}

		
		if(info)
		{
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					        RenderingHints.VALUE_ANTIALIAS_ON);
			gr.drawString("File: \"" +path+"\"" + " Dimensions: " + imageDim[x] + "x" + imageDim[y], 2, 12);
			gr.drawString("Zoom: " + (int)(scale*100) + "%", 2, 26);
			int[] left = new int[2];
			int[] right = new int[2];
			for(int i = 0; i < 2; i++)
			{
				if(small[i])
				{
					left[i] = 0;
					right[i] = imageDim[x];
				}
				else
				{
					left[i] = (int)(offset[i]/scale)*-1;
					right[i] = (int)((offset[i]-panel[i])/scale)*-1;
				}
			}
			gr.drawString("Visible: " + "("+left[x]+","+left[y]+") -> ("+right[x]+","+right[y]+")", 2, 40);
			gr.drawString("Selected: " + "("+-selectedOrigin[x]+","+-selectedOrigin[y]+") -> ("+((-selectedOrigin[x])+selectedDims[x])+","+((-selectedOrigin[y])+selectedDims[y])+")", 2, 54);
		}
	}

	public void toggleInfo()
	{
		info = !info;
	}

	private void zoom(int io, int xzoom, int yzoom)
	{
		int xdiff = offset[x] - xzoom;
		int ydiff = offset[y] - yzoom;
		double ratioX = xdiff / (imageDim[x]*scale);
		double ratioY = ydiff / (imageDim[y]*scale);
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
		zoom[x] += (int) (ratioX*(imageDim[x]*scale) - xdiff);
		zoom[y] += (int) (ratioY*(imageDim[y]*scale) - ydiff);
		
		// in case we don't draw before zoom is called again
		offset[x] = drag[x] + zoom[x];
		offset[y] = drag[y] + zoom[y];
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

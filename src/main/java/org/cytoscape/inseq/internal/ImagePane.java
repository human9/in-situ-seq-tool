package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

class ImagePane extends JPanel
{
	final public BufferedImage image;
	private double scale = 1;
	private Dimension offset, requested;
	public boolean ratioIsCurrent;

	public ImagePane(final BufferedImage image)
	{
		this.image = image;
		setSize();
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		Graphics2D gr = (Graphics2D) g;
		gr.setColor(Color.BLACK);
		gr.fillRect(0, 0, getWidth(), getHeight());
		
		gr.drawImage(image, offset.width, offset.height, requested.width, requested.height, null); 
	}

	public void scaleUp()
	{
		if(scale <= 100)
		{
			scale *= 1.1;
			if((int)scale == 100)
				scale = 100;
		}
	}
	
	public void scaleDown()
	{
		if(scale > 0.01)
			scale *= 0.9;
	}

	public double getScale()
	{
		return scale;
	}

	public void setSize()
	{
		Dimension minimum = getMinimumSize();
		offset = new Dimension();
		requested = new Dimension((int) Math.round(image.getWidth()*scale), (int) Math.round(image.getHeight()*scale));
		
		Dimension resize = new Dimension(requested);

		if(requested.width <= minimum.width)
		{
			ratioIsCurrent = false;
			resize.width = minimum.width;
			offset.width = (minimum.width - requested.width)/2;
		}
		if(requested.height <= minimum.height)
		{
			ratioIsCurrent = false;
			resize.height = minimum.height;
			offset.height = (minimum.height - requested.height)/2;
		}
		setPreferredSize(resize);
	}

	static public BufferedImage getImageFile(String path)
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

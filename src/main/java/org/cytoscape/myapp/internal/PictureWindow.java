package org.cytoscape.myapp.internal;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

public class PictureWindow extends JInternalFrame
{

	static final long serialVersionUID = 55391;
	private JDesktopPane desktop;
	public ZoomPanel zp;

	PictureWindow(JDesktopPane jdp)
	{
		super("CoolApp", true, true, true, true);
		this.desktop = jdp;
		this.setVisible(true);
		this.setSize(400, 400);
		
		BufferedImage data = getImage("/data.jpg");
		if (data != null)
		{
			zp = new ZoomPanel(data, data.getWidth(), this);
			this.getContentPane().add(zp);
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
			catch(IOException ioe) {
				return null;
			}
			return bimg;
		}
		else
			return null;
	}

	public void add()
	{
		for(Component comp : desktop.getComponents())
		{
			if (comp.getClass() == PictureWindow.class)
				return;
		}
		desktop.add(this);
		this.setVisible(true);
		desktop.repaint();
	}
	public void remove()
	{
		desktop.remove(this);
		this.setVisible(false);
		desktop.repaint();
	}
}

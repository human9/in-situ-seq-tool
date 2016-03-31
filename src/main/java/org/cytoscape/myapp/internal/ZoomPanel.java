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

class ZoomPanel extends JPanel {
	static final long serialVersionUID = 59820525894L;

	// the image being displayed
	private Image img;
	// base image dimensions for convenience
	private int iw, ih;
	// factor to scale image by
	private double scale;
	// maximum scale with signed 16 bit ints used for offset
	private double maxScale;
	// the final offset used to draw the image
	private int xoff, yoff;
	// click coordinates are to give an offset from drag
	private int cx, cy;
	// drag coordinates are for moving image with mouse
	private int dx, dy;
	// zoom cordinates for offsetting while zooming
	private int zx, zy;
	// whether to display info
	private boolean info;

	final Component parent;

	public ZoomPanel(String path, final Component parent, boolean isResource) {
		this.parent = parent;
		BufferedImage image;
		if(isResource)
			 image = getImage(path);
		else
			image = getImageFile(path);
		if (image == null)
			return;
		img = image;
		iw = image.getWidth(null);
		ih = image.getHeight(null);
		Dimension pd;
		if (parent.getBounds().getSize().width > 0)
			pd = parent.getBounds().getSize();
		else
			pd = new Dimension(400, 400);
		if (iw > ih)
			scale = pd.width / (double) iw;
		else
			scale = pd.height / (double) ih;
		setPreferredSize(pd);
		setPreferredSize(new Dimension(400, 400));
		maxScale = 32768 / Math.max(iw, ih);

		addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				zoom(e.getWheelRotation(), e.getX(), e.getY());
				parent.repaint();
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				cx = e.getX();
				cy = e.getY();
			}
		});
		addMouseMotionListener(new MouseAdapter() {
			@Override
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

	@Override
	public void paintComponent(Graphics g) {
		// calculate the actual scaled dimensions
		int width = (int) (iw * scale);
		int height = (int) (ih * scale);

		// default centeed values for offset
		int defx = (getWidth() - width) / 2;
		int defy = (getHeight() - height) / 2;

		boolean sw = false;
		boolean sh = false;
		if (width < getWidth())
		// image width is less than frame width
		{
			dx = defx;
			zx = 0;
			sw = true;
		} else
		// check to see if we need to prevent further dragging
		{
			if (dx + zx > 0) {
				dx = 0;
				zx = 0;
			}
			if (dx + zx + width < getWidth()) {
				dx = (width - getWidth()) * -1;
				zx = 0;
			}
		}
		if (height < getHeight())
		// image height is less than frame height
		{
			dy = defy;
			zy = 0;
			sh = true;
		} else {
			if (dy + zy > 0) {
				dy = 0;
				zy = 0;
			}
			if (dy + zy + height < getHeight()) {
				dy = (height - getHeight()) * -1;
				zy = 0;
			}
		}

		xoff = dx + zx;
		yoff = dy + zy;
		Graphics2D gr = (Graphics2D) g;
		gr.setColor(Color.BLACK);
		gr.fillRect(0, 0, getWidth(), getHeight());
		gr.drawImage(img, xoff, yoff, width, height, this);
		if (info) {
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gr.setColor(Color.YELLOW);
			gr.drawString("Dimensions: " + iw + "x" + ih, 2, 12);
			gr.drawString("Zoom: " + (int) (scale * 100) + "%", 2, 26);
			int lx, ly, rx, ry;
			if (sw) {
				lx = 0;
				rx = iw;
			} else {
				lx = (int) (xoff / scale) * -1;
				rx = (int) ((xoff - getWidth()) / scale) * -1;
			}
			if (sh) {
				ly = 0;
				ry = ih;

			} else {
				ly = (int) (yoff / scale) * -1;
				ry = (int) ((yoff - getHeight()) / scale) * -1;
			}

			gr.drawString("Visible: " + "(" + lx + "," + ly + ") -> (" + rx + "," + ry + ")", 2, 40);
		}

	}

	public void toggleInfo() {
		info = !info;
	}

	private void zoom(int io, int x, int y) {
		int xdiff = xoff - x;
		int ydiff = yoff - y;
		double xper = xdiff / (iw * scale);
		double yper = ydiff / (ih * scale);
		if (io < 0) {
			if (scale < maxScale) {
				if (scale < 1)
					scale += 0.1 * scale;
				else
					scale += 0.1;
			}
		} else {
			if (scale > 0.1) {
				if (scale < 1)
					scale -= 0.1 * scale;
				else
					scale -= 0.1;

			}
		}
		zx += (int) (xper * (iw * scale) - xdiff);
		zy += (int) (yper * (ih * scale) - ydiff);

		// in case we don't draw before zoom is called again
		xoff = dx + zx;
		yoff = dy + zy;
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

package org.cytoscape.myapp.internal;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

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
		
		zp = new ZoomPanel("/data.jpg", this);
		this.getContentPane().add(zp);
		
		this.addComponentListener(new ComponentAdapter(){
			public void componentMoved(ComponentEvent e){
				repaint();
			}
		});
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

package org.cytoscape.myapp.internal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;

import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;

public class PictureWindow extends JInternalFrame {
	static final long serialVersionUID = 5539194954L;
	private JDesktopPane desktop;
	public ZoomPanel zp;
	private PictureWindow pw;
	private KeyEventDispatcher ked;

	PictureWindow(JDesktopPane jdp) {
		super("CoolApp", true, true, true, true);
		this.pw = this;
		this.desktop = jdp;
		this.setVisible(true);
		this.setSize(400, 400);

		zp = new ZoomPanel("/data.jpg", this, true);
		this.getContentPane().add(zp);

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				repaint();
			}
		});
		this.toFront();
		try {
			this.setSelected(true);
		} catch (PropertyVetoException e) {
			// couldn't make active
		}
		ked = new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent ke) {
				switch (ke.getID()) {
				case KeyEvent.KEY_PRESSED:
					if (ke.getKeyCode() == KeyEvent.VK_I) {
						if(pw.isSelected) {
							zp.toggleInfo();
							zp.parent.repaint();
						}
					}
					if (ke.getKeyCode() == KeyEvent.VK_O) {
						final JFileChooser fc = new JFileChooser();

						int returnVal = fc.showOpenDialog(pw);
						
						if(returnVal == JFileChooser.APPROVE_OPTION) {
							pw.getContentPane().remove(zp);
							zp = new ZoomPanel(fc.getSelectedFile().getAbsolutePath(), pw, false);
							pw.getContentPane().add(zp);
							zp.setVisible(true);
							pw.repaint();
						}
					}
					break;
				}
				
				return false;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ked);

		jdp.repaint();
	}

	public void add() {
		for (Component comp : desktop.getComponents()) {
			if (comp.getClass() == PictureWindow.class)
				return;
		}
		desktop.add(this);
		this.setVisible(true);
		desktop.repaint();
	}

	public void remove() {
		desktop.remove(this);
		this.setVisible(false);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(ked);
		desktop.repaint();
	}
}

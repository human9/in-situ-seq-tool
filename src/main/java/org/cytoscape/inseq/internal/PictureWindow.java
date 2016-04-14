package org.cytoscape.inseq.internal;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

public class PictureWindow extends JPanel {
	static final long serialVersionUID = 5539194954L;
	public ZoomPanel zp;
	private PictureWindow pw;
	private KeyEventDispatcher ked;

	PictureWindow() {
		this.pw = this;
		this.setVisible(true);

		zp = new ZoomPanel("/data.jpg", this, true);
		this.add(zp);

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				repaint();
			}
		});
		ked = new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent ke) {
				switch (ke.getID()) {
				case KeyEvent.KEY_PRESSED:
					if (ke.getKeyCode() == KeyEvent.VK_I) {
							zp.toggleInfo();
							zp.parent.repaint();
					}
					if (ke.getKeyCode() == KeyEvent.VK_O) {
						final JFileChooser fc = new JFileChooser();

						int returnVal = fc.showOpenDialog(pw);
						
						if(returnVal == JFileChooser.APPROVE_OPTION) {
							pw.remove(zp);
							zp = new ZoomPanel(fc.getSelectedFile().getAbsolutePath(), pw, false);
							pw.add(zp);
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
	}

	public void add() {
		this.setVisible(true);
	}

	public void remove() {
		this.setVisible(false);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(ked);
	}
}

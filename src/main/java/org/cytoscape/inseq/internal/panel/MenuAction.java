package org.cytoscape.inseq.internal.panel;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cytoscape.util.swing.DropDownMenuButton;

public class MenuAction extends AbstractAction {

    JPopupMenu popup;

    public MenuAction(String text) {
        super(text);
        popup = new JPopupMenu();
        popup.add(new JMenuItem("Import new..."));
        popup.add(new JMenuItem("Rename"));
        popup.add(new JMenuItem("Delete"));

    }

    public void actionPerformed(ActionEvent e) {
        DropDownMenuButton b = (DropDownMenuButton) e.getSource();
        popup.show(b, 0, b.getHeight());
    }

}

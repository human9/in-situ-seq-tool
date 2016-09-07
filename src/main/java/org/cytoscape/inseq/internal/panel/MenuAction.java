package org.cytoscape.inseq.internal.panel;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cytoscape.util.swing.DropDownMenuButton;

public class MenuAction extends AbstractAction {

    JPopupMenu popup = new JPopupMenu();
    JMenuItem delete = new JMenuItem("Delete");

    private MainPanel main;

    public MenuAction(String text, MainPanel main) {
        super(text);
        this.main = main;
        delete.addActionListener(this);
        popup.add(delete);

    }

    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == delete) {
            main.deletePanel(); 
        }
        else {
            DropDownMenuButton b = (DropDownMenuButton) e.getSource();
            popup.show(b, 0, b.getHeight());
        }

    }

}

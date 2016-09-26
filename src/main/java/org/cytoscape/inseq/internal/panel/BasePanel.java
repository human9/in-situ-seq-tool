package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class BasePanel extends JPanel implements ContainerListener, ActionListener {
    
    final SelectionPanel sp;

    final JButton recallButton = new JButton("Recall Imageplot");
    final JPanel centralPanel = new JPanel();
        
    public BasePanel(SelectionPanel s) {
        sp = s;
        setLayout(new BorderLayout());
        centralPanel.setLayout(new GridBagLayout());
        centralPanel.add(recallButton);
        add(sp);
        addContainerListener(this);
        recallButton.addActionListener(this);
    }

    public void componentAdded(ContainerEvent e) {
        if(e.getChild() == sp) {
            remove(centralPanel);
        }
    }
    public void componentRemoved(ContainerEvent e) {
        if(e.getChild() == sp) {
            add(centralPanel);
        }
    
    }
    
    public void actionPerformed(ActionEvent e) {
        add(sp, BorderLayout.CENTER);
        sp.revalidate();
        sp.dispatchCloseEvent();
    }
}

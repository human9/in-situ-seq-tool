package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.util.swing.DropDownMenuButton;

public class MainPanel extends JPanel implements CytoPanelComponent, ItemListener {

    static final long serialVersionUID = 692;

    private DefaultComboBoxModel<SessionPanel> sessionPanels;
    private JComboBox<SessionPanel> sessionSelectionBox;
    private SessionPanel currentPanel;
    private InseqActivator ia;
    private CardLayout deck;
    private JPanel table;

    public MainPanel(InseqActivator ia) {
        this.ia = ia;
        setPreferredSize(new Dimension(400,400));
        setLayout(new BorderLayout());

        sessionPanels = new DefaultComboBoxModel<SessionPanel>();
        sessionSelectionBox = new JComboBox<SessionPanel>(sessionPanels);
        sessionSelectionBox.addItemListener(this);
        DropDownMenuButton newNet = new DropDownMenuButton(new MenuAction("Menu", this));

        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());
        
        GridBagConstraints boxCons
            = new GridBagConstraints(0, 0, 1, 1, 1, 0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(4, 4, 4, 0), 1, 1);
        GridBagConstraints newNetCons
            = new GridBagConstraints(1, 0, 1, 1, 0.1, 0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(4, 0, 4, 4), 1, 1);


        header.add(sessionSelectionBox, boxCons);
        header.add(newNet, newNetCons);
        add(header, BorderLayout.NORTH);

        deck = new CardLayout();
        table = new JPanel(deck);
        add(table, BorderLayout.CENTER);
    }
    
    public void itemStateChanged(ItemEvent e) {
        if(sessionSelectionBox.getSelectedItem() != null) {
            currentPanel = (SessionPanel) sessionSelectionBox.getSelectedItem();

            deck.show(table, currentPanel.name);

        }

        revalidate();
        repaint();
    }

    protected void deletePanel() {
        table.remove(currentPanel);
        currentPanel.shutDown();
        sessionPanels.removeElement(currentPanel);
    }

    public void addSession(String name, InseqSession session) {
        SessionPanel card = new SessionPanel(name, ia, session);

        table.add(card);
        deck.addLayoutComponent(card, card.name);
        sessionPanels.addElement(card);
        sessionSelectionBox.setSelectedItem(card);
    }

    public void updateSelectionPanel() {
        currentPanel.updateSelectionPanel();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getTitle() {
        return "Inseq";
    }

    public boolean exists(String filename) {
        for(int i = 0; i < sessionPanels.getSize(); i++) {
            SessionPanel sp = (SessionPanel) sessionPanels.getElementAt(i);
            if(sp.name.equals(filename)) return true;
        }
        return false;
    }

    public void shutDown() {
        for(int i = 0; i < sessionPanels.getSize(); i++) {
            SessionPanel sp = (SessionPanel) sessionPanels.getElementAt(i);
            sp.shutDown();
        }
    }
}

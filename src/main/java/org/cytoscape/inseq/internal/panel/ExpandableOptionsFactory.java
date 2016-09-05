package org.cytoscape.inseq.internal.panel;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import org.cytoscape.util.swing.BasicCollapsiblePanel;

public class ExpandableOptionsFactory {

    public static BasicCollapsiblePanel makeOptionsPanel(String title, Component... components) {
        BasicCollapsiblePanel bcp = new BasicCollapsiblePanel(title);
        bcp.getContentPane().setLayout(new GridBagLayout());

        int i = 0;
        for(Component c : components) {
            GridBagConstraints cons = new GridBagConstraints();
            cons.gridy = i++;
            cons.weightx = 1;
            cons.anchor = GridBagConstraints.NORTHWEST;
            bcp.add(c, cons);
        }

        bcp.setCollapsed(false); // Start fully expanded

        return bcp;
    }
}

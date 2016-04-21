package org.cytoscape.inseq.internal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.imageselection.SelectionWindow;

public class InseqControlPanel extends JPanel implements CytoPanelComponent {

	static final long serialVersionUID = 692;

	public InseqControlPanel(final InseqActivator ia)
	{
		this.setLayout(new GridBagLayout()); 
		this.setPreferredSize(new Dimension(400,400));

		GridBagConstraints cons= new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,4,4), 1,1);
		JButton openSelector = new JButton("Open selection window");
		openSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SelectionWindow(ia.swingAppAdapter.getCySwingApplication().getJFrame(), ia.gridSize);
			}
		});
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, openSelector, null);
		this.add(splitPane, cons);
		this.repaint();
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
}

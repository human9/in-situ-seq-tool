package org.cytoscape.inseq.internal;

import java.awt.Color;
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
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class InseqControlPanel extends JPanel implements CytoPanelComponent {

	static final long serialVersionUID = 692;

	InseqActivator ia;
	public InseqControlPanel(final InseqActivator iac)
	{
		this.ia = iac;
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout()); 
		this.setPreferredSize(new Dimension(400,400));

		GridBagConstraints cons= new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,4,4), 1,1);
		JButton openSelector = new JButton("Open selection window");
		openSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SelectionWindow(ia);
			}
		});
		
		GridBagConstraints cons2= new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,4,4), 1,1);
		JButton sne = new JButton("Layout SNE");
		sne.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				layoutSNE();
			}
		});
		
		GridBagConstraints cons3 = new GridBagConstraints(0,2,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(4,4,4,4), 1,1);
		JButton grid = new JButton("Layout Grid");
		grid.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				layoutGrid();
			}
		});

		panel.add(openSelector, cons);
		panel.add(sne, cons2);
		panel.add(grid, cons3);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, null);
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
	
	void layoutGrid(){
		
		CyTable CSVTable = ia.inseqTable;
		for (CyNode node : ia.inseqNetwork.getNodeList())
		{
			View<CyNode> nv = ia.inseqView.getNodeView(node);
			CyRow gridRow = CSVTable.getRow(node.getSUID());
			nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, gridRow.get("grid_center_X", Double.class));
			nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, gridRow.get("grid_center_Y", Double.class));
		}
		ia.inseqView.updateView();

	}
	void layoutSNE(){
		
		CyTable CSVTable = ia.inseqTable;
		for (CyNode node : ia.inseqNetwork.getNodeList())
		{
			View<CyNode> nv = ia.inseqView.getNodeView(node);
			CyRow gridRow = CSVTable.getRow(node.getSUID());
			nv.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.GREEN); 
			nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, gridRow.get("SNEx", Double.class)*100);
			nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, gridRow.get("SNEy", Double.class)*100);
		}
		ia.inseqView.updateView();

	}
}

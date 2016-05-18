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
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

	public InseqControlPanel(final InseqActivator iac) {
		this.ia = iac;
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		this.setPreferredSize(new Dimension(400, 400));

		GridBagConstraints cons = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton openSelector = new JButton("Open selection window");
		openSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SelectionWindow(ia);
			}
		});

		GridBagConstraints cons2 = new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton sne = new JButton("Layout SNE");
		sne.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				layoutSNE();
			}
		});

		GridBagConstraints cons3 = new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton grid = new JButton("Layout Grid");
		grid.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				layoutGrid();
			}
		});

		GridBagConstraints cons4 = new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton ratio = new JButton("Generate Ratio Net");
		ratio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new RatioNetwork(ia);
			}
		});

		GridBagConstraints cons5 = new GridBagConstraints(0, 4, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton distance = new JButton("Generate Distance Net");
		distance.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new DistanceNetwork(ia);
			}
		});
		
		GridBagConstraints cons6 = new GridBagConstraints(0, 6, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton types = new JButton("Find distances");
		types.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ia.tn.calculateDistances();
			}
		});
		
		GridBagConstraints cons7 = new GridBagConstraints(0, 8, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton gentype = new JButton("Generate Cell Types Net");
		gentype.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ia.tn.makeNetwork();
			}
		});
		
		GridBagConstraints cons8 = new GridBagConstraints(0, 5, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		final JSpinner distanceCutoff = new JSpinner(new SpinnerNumberModel(4d, 0d, 100d, 0.1d));
		distanceCutoff.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				ia.tn.distanceCutoff = Math.pow((Double)(distanceCutoff.getValue()), 2);
			}
		});
		
		GridBagConstraints cons9 = new GridBagConstraints(0, 7, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		final JSpinner requiredNum = new JSpinner(new SpinnerNumberModel(0.1d, 0d, 100d, 0.01d));
		requiredNum.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				ia.tn.requiredNum = (Double)requiredNum.getValue();
			}
		});

		panel.add(openSelector, cons);
		panel.add(sne, cons2);
		panel.add(grid, cons3);
		panel.add(ratio, cons4);
		panel.add(distance, cons5);
		panel.add(types, cons6);
		panel.add(gentype, cons7);
		panel.add(distanceCutoff, cons8);
		panel.add(requiredNum, cons9);

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

	void layoutGrid() {

		CyTable genTable = ia.inseqTable;
		double xmax = 0;
		double ymax = 0;
		for (CyNode node : ia.inseqNetwork.getNodeList()) {
			View<CyNode> nv = ia.inseqView.getNodeView(node);
			CyRow gridRow = genTable.getRow(node.getSUID());
			double x = gridRow.get("grid_center_X", Double.class);
			double y = gridRow.get("grid_center_Y", Double.class);
			nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
			nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			if (x > xmax)
				xmax = x;
			if (y > ymax)
				ymax = y;
		}
		ia.inseqView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, xmax / 2);
		ia.inseqView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, ymax / 2);
		ia.inseqView.updateView();

	}

	void layoutSNE() {

		CyTable genTable = ia.inseqTable;
		double xmax = 0;
		double ymax = 0;
		for (CyNode node : ia.inseqNetwork.getNodeList()) {
			View<CyNode> nv = ia.inseqView.getNodeView(node);
			CyRow gridRow = genTable.getRow(node.getSUID());
			double x = gridRow.get("SNEx", Double.class) * 100;
			double y = gridRow.get("SNEy", Double.class) * 100;
			nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
			nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			nv.setVisualProperty(BasicVisualLexicon.NODE_LABEL, gridRow.get("name", String.class).substring(1));
			if (x > xmax)
				xmax = x;
			if (y > ymax)
				ymax = y;
		}
		ia.inseqView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, xmax / 2);
		ia.inseqView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, ymax / 2);
		ia.inseqView.updateView();

	}
}

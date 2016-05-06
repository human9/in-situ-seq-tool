package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.imageselection.SelectionWindow;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.TaskIterator;

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
				genRatio();
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

		panel.add(openSelector, cons);
		panel.add(sne, cons2);
		panel.add(grid, cons3);
		panel.add(ratio, cons4);
		panel.add(distance, cons5);

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

	void genRatio() {

		// generate gene nodes
		CyNetwork ratioNet = ia.networkFactory.createNetwork();
		ratioNet.getRow(ratioNet).set(CyNetwork.NAME, "ratio network");
		CyTable ratioTable = ratioNet.getDefaultNodeTable();

		Map<Integer, CyNode> populations = new HashMap<Integer, CyNode>();
		ia.selectedNodes.size();
		for (String name : ia.geneNames) {
			ratioTable.createColumn(name.substring(1), Double.class, false);
		}
		ratioTable.createColumn("grids_in_pop", Integer.class, false);
		ratioTable.createColumn("size", Double.class, false);
		ArrayList<CyNode> populationNodes = new ArrayList<CyNode>();
		for (CyNode node : ia.selectedNodes) {
			CyRow gridRow = ia.inseqTable.getRow(node.getSUID());
			Integer population = gridRow.get("population", Integer.class);
			if (!populations.containsKey(population)) {
				CyNode popNode = ratioNet.addNode();
				populationNodes.add(popNode);
				CyRow popRow = ratioTable.getRow(popNode.getSUID());
				popRow.set("grids_in_pop", 1);
				popRow.set("size", 1d);
				popRow.set(CyNetwork.NAME, population.toString());
				populations.put(population, popNode);
				for (String name : ia.geneNames) {
					popRow.set(name.substring(1), (double) gridRow.get(name.substring(1), Integer.class));
				}
			} else {
				CyNode pop = populations.get(population);
				CyRow popRow = ratioTable.getRow(pop.getSUID());
				for (String name : ia.geneNames) {
					popRow.set(name.substring(1), popRow.get(name.substring(1), Double.class)
							+ gridRow.get(name.substring(1), Integer.class));
				}
				popRow.set("grids_in_pop", popRow.get("grids_in_pop", Integer.class) + 1);
				int size = popRow.get("grids_in_pop", Integer.class);
				popRow.set("size", (double)size);
			}
		}
		for (String name : ia.geneNames) {
			Double average = 0d;
			for (double value : ratioTable.getColumn(name.substring(1)).getValues(Double.class)) {
				average += value;
			}
			average /= ratioTable.getColumn(name.substring(1)).getValues(Double.class).size();
			for (CyNode node : ratioNet.getNodeList()) {
				CyRow row = ratioTable.getRow(node.getSUID());
				row.set(name.substring(1), row.get(name.substring(1), Double.class)
						/ (double) row.get("grids_in_pop", Integer.class) / average);
			}
		}
		CyTable edges = ratioNet.getDefaultEdgeTable();
		edges.createColumn("strength", Double.class, false);

		for (String name : ia.geneNames) {
			CyNode node = ratioNet.addNode();
			CyRow nodeRow = ratioTable.getRow(node.getSUID());
			nodeRow.set(CyNetwork.NAME, name.substring(1));
			for (CyNode popNode : populationNodes) {
				if (ratioTable.getRow(popNode.getSUID()).get(name.substring(1), Double.class) > 0.01)
				{
					CyEdge edge = ratioNet.addEdge(node, popNode, false);
					CyRow row = edges.getRow(edge.getSUID());
					Double strength = ratioTable.getRow(popNode.getSUID()).get(name.substring(1), Double.class)*100;
					if(strength <= 40)
						row.set("strength", strength);
					else
						row.set("strength", 40d);
			}

			}
		}

		CyNetworkView view = ia.networkViewFactory.createNetworkView(ratioNet);

		VisualStyle vs = ia.visualFactory.createVisualStyle("Ratio Style");
		vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.BLUE);
		for (VisualStyle style : ia.visualManager.getAllVisualStyles()) {
			if (style.getTitle() == "Ratio Style") {
				ia.visualManager.removeVisualStyle(style);
				break;
			}
		}
		VisualMappingFunction<String,String> pMap = ia.passthroughMappingFactory.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(pMap);
		VisualMappingFunction<Double,Double> sizeMap = ia.continuousMappingFactory.createVisualMappingFunction("size", Double.class, BasicVisualLexicon.NODE_SIZE);
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(1d,new  BoundaryRangeValues<Double>(20d,25d,30d));
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(30d,new  BoundaryRangeValues<Double>(40d,50d,60d));
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(100d,new  BoundaryRangeValues<Double>(80d,90d,100d));
		vs.addVisualMappingFunction(sizeMap);
		VisualMappingFunction<Double,Double> edgeMap = ia.continuousMappingFactory.createVisualMappingFunction("strength", Double.class, BasicVisualLexicon.EDGE_WIDTH);
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(2d,new  BoundaryRangeValues<Double>(1d,2d,3d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(12d,new  BoundaryRangeValues<Double>(6d,7d,8d));
		vs.addVisualMappingFunction(edgeMap);
		ia.visualManager.addVisualStyle(vs);
		vs.apply(view);
		for (CyNode node : populationNodes) {
			View<CyNode> gv = view.getNodeView(node);
			gv.setVisualProperty(BasicVisualLexicon.NODE_SIZE,
					(double) ratioTable.getRow(node.getSUID()).get("grids_in_pop", Integer.class));
			gv.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
		}
		
		final CyLayoutAlgorithmManager algm = ia.appAdapter.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		ia.appAdapter.getTaskManager().execute(itr);

		ia.networkManager.addNetwork(ratioNet);
		ia.networkViewManager.addNetworkView(view);
		view.updateView();

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

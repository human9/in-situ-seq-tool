package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.imageselection.SelectionWindow;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualStyle;

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
				genDistance();
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

	String distanceKeygen(CyNode node1, CyNode node2) {
		long a = Math.min(node1.getSUID(), node2.getSUID());
		long b = Math.max(node1.getSUID(), node2.getSUID());
		return a+"-"+b;
	}

	void genRatio() {

		// generate gene nodes
		CyNetwork ratioNet = ia.networkFactory.createNetwork();
		ratioNet.getRow(ratioNet).set(CyNetwork.NAME, "ratio network");
		CyTable ratioTable = ratioNet.getDefaultNodeTable();

		Map<Double, CyNode> populations = new HashMap<Double, CyNode>();
		ia.selectedNodes.size();
		for (String name : ia.geneNames) {
			ratioTable.createColumn(name.substring(1), Double.class, false);
		}
		ratioTable.createColumn("grids_in_pop", Integer.class, false);
		ArrayList<CyNode> populationNodes = new ArrayList<CyNode>();
		for (CyNode node : ia.selectedNodes) {
			CyRow gridRow = ia.inseqTable.getRow(node.getSUID());
			Double population = gridRow.get("population", Double.class);
			if (!populations.containsKey(population)) {
				CyNode popNode = ratioNet.addNode();
				populationNodes.add(popNode);
				CyRow popRow = ratioTable.getRow(popNode.getSUID());
				popRow.set("grids_in_pop", 1);
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

		for (String name : ia.geneNames) {
			CyNode node = ratioNet.addNode();
			CyRow nodeRow = ratioTable.getRow(node.getSUID());
			nodeRow.set(CyNetwork.NAME, name.substring(1));
			for (CyNode popNode : populationNodes) {
				if (ratioTable.getRow(popNode.getSUID()).get(name.substring(1), Double.class) > 0.01)
					ratioNet.addEdge(node, popNode, false);

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
		ia.visualManager.addVisualStyle(vs);
		vs.apply(view);
		for (CyNode node : populationNodes) {
			View<CyNode> gv = view.getNodeView(node);
			gv.setVisualProperty(BasicVisualLexicon.NODE_SIZE,
					(double) ratioTable.getRow(node.getSUID()).get("grids_in_pop", Integer.class));
			gv.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
		}

		ia.networkManager.addNetwork(ratioNet);
		ia.networkViewManager.addNetworkView(view);
		view.updateView();

	}

	Point gridToPoint(int grid)
	{
		int x = (int)Math.ceil(grid / ia.gridSize.height);
		int y = grid % ia.gridSize.height;
		return new Point(x,y);
	}

	int pointToGrid(Point point)
	{
		return ((point.x-1) * ia.gridSize.height) + point.y;
	}
	
	String primaryKeyColname;
	Map<Integer, CyNode> selectedNodes;
	Map<String, Double> totalDistance; 
	Map<String, Integer> numDistances;
	Map<String, Double> distances;

	private CyNode getNodeWithID(final Integer ID, String name, CyNode current)
	{
		if(selectedNodes.containsKey(ID))
		{
			//add to the map here
			CyNode node = selectedNodes.get(ID);
			CyRow row = ia.inseqTable.getRow(node.getSUID());
			if(row.get(name.substring(1), Integer.class) > 0)
			{
				CyRow currentRow = ia.inseqTable.getRow(current.getSUID());
				int a1 = currentRow.get("grid_ID", Integer.class) % ia.gridSize.height;
				int b1 = currentRow.get("grid_ID", Integer.class) / ia.gridSize.width;
				int a2 = row.get("grid_ID", Integer.class) % ia.gridSize.height;
				int b2 = row.get("grid_ID", Integer.class) / ia.gridSize.width;
				
				String key = distanceKeygen(current, node);
				if (!distances.containsKey(key)) 
					distances.put(key, Math.hypot(a1-a2, b1-b2));

				if(!totalDistance.containsKey(name))
					totalDistance.put(name, distances.get(key));
				else
					totalDistance.put(name, totalDistance.get(name) + distances.get(key));
				
				if(!numDistances.containsKey(name))
					numDistances.put(name, 1);
				else
					numDistances.put(name, 1 + numDistances.get(name));

				return node;
			}
			else return null;
					
		}
		else
			return null;
	}
	
	private Map<String, CyNode> getNodeWithTranscript(final Set<CyNode> selection, final CyNode current)
	{
		ArrayList<String> genes = new ArrayList<String>(ia.geneNames);
		Map<String, CyNode> nodes = new HashMap<String, CyNode>();
		CyRow currentRow = ia.inseqTable.getRow(current.getSUID());
		for(Iterator<String> itr = genes.iterator(); itr.hasNext();)
		{
			String name = itr.next();
			if(currentRow.get(name.substring(1), Integer.class) > 0)
			{
				nodes.put(name, current);
				itr.remove();
			}
		}
		if(genes.size() == 0)
			return nodes;
		
		Point center = gridToPoint((int)Math.round(currentRow.get("grid_ID", Integer.class)));
		for(int i = 1; i < ia.gridSize.height/2; i++)
		{
			System.out.println(i);
			Point iteration = new Point(center.x - i, center.y + i);
			for(int a = 0; a < i*2; a++)
			{
				for(Iterator<String> itr = genes.iterator(); itr.hasNext();)
				{
					String name = itr.next();
					CyNode node = getNodeWithID(pointToGrid(iteration), name, current);
					if(node != null)
					{
						nodes.put(name, node);
						itr.remove();
					}
					if(genes.size() == 0)
						return nodes;
					iteration.translate(a,0);
				}
			}
			for(int a = 0; a < i*2; a++)
			{
				for(Iterator<String> itr = genes.iterator(); itr.hasNext();)
				{
					String name = itr.next();
					CyNode node = getNodeWithID(pointToGrid(iteration), name, current);
					if(node != null)
					{
						nodes.put(name, node);
						itr.remove();
					}
					if(genes.size() == 0)
						return nodes;
					iteration.translate(0,-a);
				}
			}
			for(int a = 0; a < i*2; a++)
			{
				for(Iterator<String> itr = genes.iterator(); itr.hasNext();)
				{
					String name = itr.next();
					CyNode node = getNodeWithID(pointToGrid(iteration), name, current);
					if(node != null)
					{
						nodes.put(name, node);
						itr.remove();
					}
					if(genes.size() == 0)
						return nodes;
					iteration.translate(-a,0);
				}
			}
			for(int a = 0; a < i*2; a++)
			{
				for(Iterator<String> itr = genes.iterator(); itr.hasNext();)
				{
					String name = itr.next();
					CyNode node = getNodeWithID(pointToGrid(iteration), name, current);
					if(node != null)
					{
						nodes.put(name, node);
						itr.remove();
					}
					if(genes.size() == 0)
						return nodes;
					iteration.translate(0,a);
				}
			}
			if(genes.size() == 0)
				return nodes;
		}
		return nodes;

	}


	void genDistance() {
		
		/* Stores every single distance.. perhaps this is not the best way */ 

		primaryKeyColname = ia.inseqTable.getPrimaryKey().getName();
		selectedNodes = new HashMap<Integer, CyNode>();
		for(CyNode node : ia.selectedNodes)
		{
			CyRow row = ia.inseqTable.getRow(node.getSUID());
			selectedNodes.put(row.get("grid_ID", Integer.class), node);
		}

		distances = new HashMap<String, Double>();
		totalDistance = new HashMap<String, Double>();
		numDistances = new HashMap<String, Integer>();
		for(CyNode node1 : ia.selectedNodes)
		{
			CyRow gridRow = ia.inseqTable.getRow(node1.getSUID());
			
			for(String name : ia.geneNames)
			{
				System.out.println(name + gridRow.get("grid_ID", Integer.class));
				if(gridRow.get(name.substring(1), Integer.class) > 0)
				{
					Map<String, CyNode> nodes = getNodeWithTranscript(ia.selectedNodes, node1);
				}
			}
		}
		
		// that was a lot more difficult than I had anticipated...

		/*
		// generate gene nodes
		CyNetwork distanceNet = ia.networkFactory.createNetwork();
		distanceNet.getRow(distanceNet).set(CyNetwork.NAME, "distance network");
		CyTable distanceTable = distanceNet.getDefaultNodeTable();
		

		for(CyNode node : ia.selectedNodes)
		{
			CyRow gridRow = ia.inseqTable.getRow(node.getSUID());
			for (String name : ia.geneNames) {
				int value = gridRow.get(name.substring(1), Integer.class);
				if(value > 0)
				{
					double dist = getDistance(node, name.substring(1));
				}
			}
		}

		CyNetworkView view = ia.networkViewFactory.createNetworkView(distanceNet);
		ia.networkManager.addNetwork(distanceNet);
		ia.networkViewManager.addNetworkView(view);
		view.updateView();
		*/
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

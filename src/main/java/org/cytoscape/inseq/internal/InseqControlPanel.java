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
import java.util.Arrays;
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

		Map<Integer, CyNode> populations = new HashMap<Integer, CyNode>();
		ia.selectedNodes.size();
		for (String name : ia.geneNames) {
			ratioTable.createColumn(name.substring(1), Double.class, false);
		}
		ratioTable.createColumn("grids_in_pop", Integer.class, false);
		ArrayList<CyNode> populationNodes = new ArrayList<CyNode>();
		for (CyNode node : ia.selectedNodes) {
			CyRow gridRow = ia.inseqTable.getRow(node.getSUID());
			Integer population = gridRow.get("population", Integer.class);
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
		return ((point.x) * ia.gridSize.height) + point.y;
	}
	
	String primaryKeyColname;
	Map<Integer, CyNode> selectedNodes;
	Map<String, ArrayList<Double>> totalDistance; 
	Map<String, Integer> numDistances;
	Map<String, Double> distances;

	ArrayList<String>comparisonGenes;
	ArrayList<String> genes;


	private String transcriptKeygen(String first, String second)
	{
		List<String> names = Arrays.asList(first, second);
		java.util.Collections.sort(names);
		return names.toString();
	}

	private void findGenesInGridID(final Integer ID, String name, CyNode current)
	{
		// check that the grid ID represents a node within our selection
		if(selectedNodes.containsKey(ID))
		{
			// get the node and row of the grid (each grid is represented by a node)
			CyNode node = selectedNodes.get(ID);
			CyRow row = ia.inseqTable.getRow(node.getSUID());

			//System.out.println(node.toString() + current.toString());

			// check whether any gene of interest is within this grid
			// comparisonGenes are the genes that we are looking for in the second grid
			for(Iterator<String> itr = comparisonGenes.iterator(); itr.hasNext();)
			{
				String comparisonName = itr.next();
				if(row.get(comparisonName.substring(1), Integer.class) > 0)
				{
					// the gene is in this grid! first let's generate some keys
					String tKey = transcriptKeygen(comparisonName, name);
					String dKey = distanceKeygen(current, node);
					
					// now let's see we need to find the distance between these grids
					if(!distances.containsKey(dKey))
					{
						// the key isn't in there so we need to get the locations and calculate it
						CyRow currentRow = ia.inseqTable.getRow(current.getSUID());
						int grid1X = gridToPoint(currentRow.get("grid_ID", Integer.class)).x;
						int grid1Y = gridToPoint(currentRow.get("grid_ID", Integer.class)).y;
						int grid2X = gridToPoint(ID).x; 
						int grid2Y = gridToPoint(ID).y; 

						// now find the hypotenuse and add it to the distances map
						distances.put(dKey, Math.hypot(grid1X-grid2X, grid1Y-grid2Y));
					}

					if(!totalDistance.containsKey(tKey))
					{
						// create the arraylist first because it hasn't been made yet
						totalDistance.put(tKey, new ArrayList<Double>());
					}
					
					// now we can add this distance to the transcript distance map
					totalDistance.get(tKey).add(distances.get(dKey));

					// remove this gene from the internal iterations
					itr.remove();
				}
			}
		}
	}
	

	private void searchGrid(Point point, CyNode current)
	{
		int grid = pointToGrid(point);
		if(grid > 0 && grid <= ia.gridSize.width * ia.gridSize.height)
		{
			// first we iterate over the genes which are present in the current node
			for(Iterator<String> itr = genes.iterator(); itr.hasNext();)
			{
				String name = itr.next();
				// now, search the selected grid for the current gene, passing the list of genes to compare
				findGenesInGridID(grid, name, current);
				if(comparisonGenes.size() == 0) {
					itr.remove();
				}
				if(genes.size() == 0) return;
			}
		}
	}
	
	private void beginSearchPattern(final Set<CyNode> selection, final CyNode current)
	{
		genes = new ArrayList<String>(ia.geneNames);
		comparisonGenes = new ArrayList<String>(ia.geneNames);
		CyRow currentRow = ia.inseqTable.getRow(current.getSUID());
		for(Iterator<String> itr = genes.iterator(); itr.hasNext();)
		{
			String name = itr.next();
			if(currentRow.get(name.substring(1), Integer.class) <= 0)
			{
				itr.remove();
			}
		}
		
		Point center = gridToPoint(currentRow.get("grid_ID", Integer.class));
		searchGrid(center, current);
		if(genes.size() == 0)
			return;
		
		for(int i = 1; i < ia.gridSize.height; i++)
		{
			Point gridPoint = new Point(center.x - i, center.y + i);
			for(int a = 0; a < i*2; a++)
			{
				searchGrid(gridPoint, current);
				gridPoint.translate(1,0);
				if(genes.size() == 0)
					return;
			}
			for(int a = 0; a < i*2; a++)
			{
				searchGrid(gridPoint, current);
				gridPoint.translate(0,-1);
				if(genes.size() == 0)
					return;
			}
			for(int a = 0; a < i*2; a++)
			{
				searchGrid(gridPoint, current);
				gridPoint.translate(-1,0);
				if(genes.size() == 0)
					return;
			}
			for(int a = 0; a < i*2; a++)
			{
				searchGrid(gridPoint, current);
				gridPoint.translate(0,1);
				if(genes.size() == 0)
					return;
			}
		}
	}


	public static Double arrayListAverage(ArrayList<Double> arrayList)
	{
		Double total = 0d;
		for(Double value : arrayList)
		{
			total += value;
		}
		return total/arrayList.size();
	}

	void genDistance() {
		
		primaryKeyColname = ia.inseqTable.getPrimaryKey().getName();
		selectedNodes = new HashMap<Integer, CyNode>();
		for(CyNode node : ia.selectedNodes)
		{
			CyRow row = ia.inseqTable.getRow(node.getSUID());
			selectedNodes.put(row.get("grid_ID", Integer.class), node);
		}

		distances = new HashMap<String, Double>();
		totalDistance = new HashMap<String, ArrayList<Double>>();
		numDistances = new HashMap<String, Integer>();
		for(CyNode node : ia.selectedNodes)
		{
			beginSearchPattern(ia.selectedNodes, node);
		}
		
		// generate gene nodes
		CyNetwork distanceNet = ia.networkFactory.createNetwork();
		distanceNet.getRow(distanceNet).set(CyNetwork.NAME, "distance network");
		CyTable distanceTable = distanceNet.getDefaultNodeTable();
		
		
		for(String name : ia.geneNames)
		{
			distanceTable.createColumn(name, Double.class, false);
		}
		
		for(String name1 : ia.geneNames)
		{
			CyNode node = distanceNet.addNode();
			CyRow row = distanceTable.getRow(node.getSUID());
			row.set(CyNetwork.NAME, name1);
		
			for(String name2 : ia.geneNames)
			{
				String key = transcriptKeygen(name1, name2);
				if(totalDistance.get(key) != null)
					row.set(name2, arrayListAverage(totalDistance.get(key)));
				/*
				else
					System.out.println("NO DATA FOR " + name1 + " to " + name2);
					*/
			}
		}

		CyNetworkView view = ia.networkViewFactory.createNetworkView(distanceNet);
		ia.networkManager.addNetwork(distanceNet);
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

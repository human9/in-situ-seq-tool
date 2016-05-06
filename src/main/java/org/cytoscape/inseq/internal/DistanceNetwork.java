package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;

public class DistanceNetwork {

	InseqActivator ia;

	public DistanceNetwork(final InseqActivator iac) {
		this.ia = iac;
		genDistance();
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
		//java.util.Collections.sort(names);
		return names.toString();
	}
	
	String distanceKeygen(CyNode node1, CyNode node2) {
		long a = Math.min(node1.getSUID(), node2.getSUID());
		long b = Math.max(node1.getSUID(), node2.getSUID());
		return a+"-"+b;
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
		// TODO: once doing this properly, go 42% bigger to complete the circle
		// also instead of removing the first gene from iterator, just find all secondary genes first
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
		for (CyNode node1 : distanceNet.getNodeList())
		{
			CyRow row1 = distanceTable.getRow(node1.getSUID());
			for (CyNode node2 : distanceNet.getNodeList())
			{
				CyRow row2 = distanceTable.getRow(node2.getSUID());
				String gene = row2.get(CyNetwork.NAME, String.class);
				if (row1.get(gene, Double.class) != null && row1.get(gene, Double.class) < 3 && row1.get(CyNetwork.NAME, String.class) != gene)
					distanceNet.addEdge(node1, node2, false);
			}
		}


		CyNetworkView view = ia.networkViewFactory.createNetworkView(distanceNet);
		ia.networkManager.addNetwork(distanceNet);
		
		VisualStyle vs = ia.visualFactory.createVisualStyle("Inseq Style");

		ia.visualManager.addVisualStyle(vs);
		vs.apply(view);
			
		for(CyNode node : distanceNet.getNodeList())
		{
			View<CyNode> nv = view.getNodeView(node);
			CyRow gridRow = distanceTable.getRow(node.getSUID());
			nv.setVisualProperty(BasicVisualLexicon.NODE_LABEL, gridRow.get("name", String.class).substring(1));
		}

		ia.networkViewManager.addNetworkView(view);
		
		final CyLayoutAlgorithmManager algm = ia.appAdapter.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		ia.appAdapter.getTaskManager().execute(itr);

		view.updateView();
		
	}
}

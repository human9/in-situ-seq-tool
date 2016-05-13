package org.cytoscape.inseq.internal;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;

class DualPoint
{
	Point2D.Double p1;
	Point2D.Double p2;

	public DualPoint(Point2D.Double p1, Point2D.Double p2)
	{
		this.p1 = p1;
		this.p2 = p2;
	}
}

public class TypeNetwork {

	InseqActivator ia;
	Double distanceCutoff = 16d;
	Integer requiredNum = 4;

	Map<DualPoint, Double> distances;

	public TypeNetwork(final InseqActivator iac) {
		this.ia = iac;
	}

	// gets the first node with the specified name if it exists, else returns null
	private CyNode getNodeWithName(CyNetwork net, CyTable table, String name)
	{
		for(CyNode node : net.getNodeList())
		{
			CyRow row  = table.getRow(node.getSUID());
			String nodeName = row.get(CyNetwork.NAME, String.class);
			if (nodeName.equals(name)) return node;
		}
		return null;
	}

	public void calculateDistances()
	{
		Point2D.Double[] points = new Point2D.Double[ia.transcripts.size()];
		int i = 0;
		for(Point2D.Double point : ia.transcripts.keySet())
		{
			points[i] = new Point2D.Double(point.x, point.y);
			i++;
		}
		distances = new HashMap<DualPoint, Double>();
		for(int a = 0; a < points.length; a++)
		{
			Point2D.Double p1 = points[a];
			if(a%1000 == 0)System.out.println(a);
			for(int b = a+1; b < points.length; b++)
			{
				Point2D.Double p2 = points[b];
				Double distance = ((p1.x - p2.x) * (p1.x - p2.x)) + ((p1.y - p2.y) * (p1.y - p2.y));
				if(distance < distanceCutoff)
				{
					distances.put(new DualPoint(p1,p2), Math.sqrt(distance));
					System.out.println(ia.transcripts.get(p1) + " - " + ia.transcripts.get(p2) + " = " + Math.sqrt(distance));
				}
			}
		}
	}

	public void makeNetwork()
	{
		CyNetwork typeNet = ia.networkFactory.createNetwork();
		typeNet.getRow(typeNet).set(CyNetwork.NAME, "type network");
		CyTable typeTable = typeNet.getDefaultNodeTable();

		ArrayList<String> genes = new ArrayList<String>();

		//get a list of all genes that have low distances
		for(DualPoint dp : distances.keySet())
		{

			String name1 = ia.transcripts.get(dp.p1);
			if(!genes.contains(name1))
				genes.add(name1);
			String name2 = ia.transcripts.get(dp.p2);
			if(!genes.contains(name2))
				genes.add(name2);
		}

		// add the nodes, and name them
		for(String gene : genes)
		{
			System.out.println("ADDING NODE FOR " + gene);
			CyNode node = typeNet.addNode();
			CyRow row = typeTable.getRow(node.getSUID());
			
			row.set(CyNetwork.NAME, gene);
		}

		// add edges, and give them weight
		CyTable edgeTable = typeNet.getDefaultEdgeTable();	
		edgeTable.createColumn("weight", Double.class, false);
		edgeTable.createColumn("num", Integer.class, false);
		for(DualPoint dp : distances.keySet())
		{
			String name1 = ia.transcripts.get(dp.p1);
			String name2 = ia.transcripts.get(dp.p2);
			if (name1.equals(name2)) continue;

			CyNode n1 = getNodeWithName(typeNet, typeTable, name1); 
			CyNode n2 = getNodeWithName(typeNet, typeTable, name2); 

			Double distance = distances.get(dp);

			if(!typeNet.containsEdge(n1,n2))
			{
				CyEdge edge = typeNet.addEdge(n1, n2, false);

				CyRow edgeRow = edgeTable.getRow(edge.getSUID());
				edgeRow.set(CyNetwork.NAME, name1 + " - " + name2);
				edgeRow.set("weight", distance); 
				edgeRow.set("num", 1);
			}
			else
			{
				CyEdge edge = typeNet.getConnectingEdgeList(n1,n2, CyEdge.Type.ANY).get(0);
				CyRow edgeRow = edgeTable.getRow(edge.getSUID());
				edgeRow.set("weight", distance + edgeRow.get("weight", Double.class)); 
				edgeRow.set("num", edgeRow.get("num", Integer.class) + 1);
			}

		}

		ArrayList<CyEdge> poorEdges = new ArrayList<CyEdge>();
		for(CyEdge edge : typeNet.getEdgeList())
		{
			CyRow row = edgeTable.getRow(edge.getSUID());
			row.set("weight", row.get("weight", Double.class) / (double)row.get("num", Integer.class));
			if(row.get("num", Integer.class) < requiredNum)
				poorEdges.add(edge);
		}
		typeNet.removeEdges(poorEdges);

		CyNetworkView view = ia.networkViewFactory.createNetworkView(typeNet);

		VisualStyle vs = ia.visualFactory.createVisualStyle("Inseq Style");
		
		
		VisualMappingFunction<String,String> pMap = ia.passthroughMappingFactory.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(pMap);
		ia.visualManager.addVisualStyle(vs);
		ia.visualManager.setCurrentVisualStyle(vs);
		vs.apply(view);

		final CyLayoutAlgorithmManager algm = ia.appAdapter.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		ia.appAdapter.getTaskManager().execute(itr);

		ia.networkManager.addNetwork(typeNet);
		ia.networkViewManager.addNetworkView(view);
		view.updateView();


	}
}


package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
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
	Double requiredNum = 0.1;
	public CyNetwork tn;
	public CyTable tt;
	public CyTable et;

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
					//System.out.println(ia.transcripts.get(p1) + " - " + ia.transcripts.get(p2) + " = " + Math.sqrt(distance));
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
		typeTable.createColumn("num", Integer.class, false);
		for(String gene : genes)
		{
			System.out.println("ADDING NODE FOR " + gene);
			CyNode node = typeNet.addNode();
			CyRow row = typeTable.getRow(node.getSUID());
			
			row.set(CyNetwork.NAME, gene);
			row.set("num", 0);
		}

		// add edges, and give them weight
		CyTable edgeTable = typeNet.getDefaultEdgeTable();	
		edgeTable.createColumn("weight", Double.class, false);
		edgeTable.createColumn("num", Integer.class, false);
		edgeTable.createColumn("node1", String.class, false);
		edgeTable.createColumn("node2", String.class, false);
		edgeTable.createColumn("normal", Double.class, false);

		ia.mps = new HashMap<String, ArrayList<Point2D.Double>>();
		
		try	{
			PrintWriter out = new PrintWriter("/home/jrs/Desktop/output.csv", "UTF-8");
			out.println("name1,name2,x1,y1,x2,y2");
			for(DualPoint dp : distances.keySet())
			{
				String name1 = ia.transcripts.get(dp.p1);
				String name2 = ia.transcripts.get(dp.p2);
				
				//if (name1.equals(name2)) continue;

				if(!ia.mps.containsKey(name1))
				{
					ArrayList<Point2D.Double> al = new ArrayList<Point2D.Double>(); 
					al.add(dp.p1);
					ia.mps.put(name1, al);
				}
				else
					ia.mps.get(name1).add(dp.p1);
				
				if(!ia.mps.containsKey(name2))
				{
					ArrayList<Point2D.Double> al = new ArrayList<Point2D.Double>(); 
					al.add(dp.p2);
					ia.mps.put(name2, al);
				}
				else
					ia.mps.get(name2).add(dp.p2);

				CyNode n1 = getNodeWithName(typeNet, typeTable, name1); 
				CyNode n2 = getNodeWithName(typeNet, typeTable, name2); 

				CyRow n1Row = typeTable.getRow(n1.getSUID());
				n1Row.set("num", n1Row.get("num", Integer.class) + 1);
				CyRow n2Row = typeTable.getRow(n2.getSUID());
				n2Row.set("num", n2Row.get("num", Integer.class) + 1);

				Double distance = distances.get(dp);
			
				out.println(name1 + "," + name2 + "," + dp.p1.x + "," + dp.p1.y + "," + dp.p2.x + "," + dp.p2.y);

				if(!typeNet.containsEdge(n1,n2))
				{
					CyEdge edge = typeNet.addEdge(n1, n2, false);

					CyRow edgeRow = edgeTable.getRow(edge.getSUID());
					edgeRow.set(CyNetwork.NAME, name1 + " - " + name2);
					edgeRow.set("weight", distance); 
					edgeRow.set("node1", name1); 
					edgeRow.set("node2", name2); 
					edgeRow.set("num", 1);
				}
				else
				{
					CyEdge edge = typeNet.getConnectingEdgeList(n1,n2, CyEdge.Type.ANY).get(0);
					CyRow edgeRow = edgeTable.getRow(edge.getSUID());
					edgeRow.set("weight", distance + edgeRow.get("weight", Double.class)); 
					edgeRow.set("node1", name1); 
					edgeRow.set("node2", name2); 
					edgeRow.set("num", edgeRow.get("num", Integer.class) + 1);
				}

			}
		
			
			out.close();
		}
		catch(FileNotFoundException e) {
			System.out.println("whoops");
		}
		catch(UnsupportedEncodingException e) {
			System.out.println("whoops");
		}

		ArrayList<CyEdge> poorEdges = new ArrayList<CyEdge>();
		for(CyEdge edge : typeNet.getEdgeList())
		{
			CyRow row = edgeTable.getRow(edge.getSUID());
			int startNum = row.get("num", Integer.class);
			row.set("weight", row.get("weight", Double.class) / (double)startNum);
			
			int node1Num = ia.mps.get(row.get("node1", String.class)).size();
			int node2Num = ia.mps.get(row.get("node2", String.class)).size();
			row.set("normal", (double)startNum / (double)(node1Num+node2Num));
			if(row.get("normal", Double.class) < requiredNum)
				poorEdges.add(edge);
		}
		typeNet.removeEdges(poorEdges);

		CyNetworkView view = ia.networkViewFactory.createNetworkView(typeNet);

		VisualStyle vs = ia.visualFactory.createVisualStyle("Inseq Style");
		
		
		VisualMappingFunction<String,String> ntool = ia.passthroughMappingFactory.createVisualMappingFunction("num", String.class, BasicVisualLexicon.NODE_TOOLTIP);
		vs.addVisualMappingFunction(ntool);
		VisualMappingFunction<String,String> etool = ia.passthroughMappingFactory.createVisualMappingFunction("num", String.class, BasicVisualLexicon.EDGE_TOOLTIP);
		vs.addVisualMappingFunction(etool);
		VisualMappingFunction<String,String> pMap = ia.passthroughMappingFactory.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(pMap);
		VisualMappingFunction<Integer,Double> sizeMap = ia.continuousMappingFactory.createVisualMappingFunction("num", Integer.class, BasicVisualLexicon.NODE_SIZE);
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(1,new  BoundaryRangeValues<Double>(20d,25d,30d));
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(100,new  BoundaryRangeValues<Double>(40d,50d,60d));
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(10000,new  BoundaryRangeValues<Double>(80d,90d,100d));
		vs.addVisualMappingFunction(sizeMap);
		VisualMappingFunction<Double,Double> edgeMap = ia.continuousMappingFactory.createVisualMappingFunction("normal", Double.class, BasicVisualLexicon.EDGE_WIDTH);
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(0.01d,new  BoundaryRangeValues<Double>(0d,0d,3d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(0.1d,new  BoundaryRangeValues<Double>(6d,8d,10d));
		vs.addVisualMappingFunction(edgeMap);
		vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.LIGHT_GRAY);
		for (VisualStyle style : ia.visualManager.getAllVisualStyles()) {
			if (style.getTitle() == "Ratio Style") {
				ia.visualManager.removeVisualStyle(style);
				break;
			}
		}
		ia.visualManager.addVisualStyle(vs);
		ia.visualManager.setCurrentVisualStyle(vs);
		vs.apply(view);

		final CyLayoutAlgorithmManager algm = ia.appAdapter.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		ia.appAdapter.getTaskManager().execute(itr);

		// have to find it first, destroy it after already adding the new one, otherwise things get messed up
		CyNetwork dnet = null;
		for (CyNetwork net : ia.networkManager.getNetworkSet()) {
			if(net.getRow(net).get(CyNetwork.NAME, String.class) == "type network") {
				dnet = net;
				break;
			}
		}
		ia.networkManager.addNetwork(typeNet);
		tn = typeNet;
		tt = typeTable;
		et = edgeTable;
		ia.networkViewManager.addNetworkView(view);
		view.updateView();

		ia.networkManager.destroyNetwork(dnet);

	}
}


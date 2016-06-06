package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.inseq.internal.types.DualPoint;
import org.cytoscape.inseq.internal.types.Transcript;
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
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;


public class TypeNetwork extends AbstractTask {

	InseqActivator ia;
	public Double distanceCutoff = 16d;
	public Double requiredNum = 0.1;
	public CyNetwork tn;
	public CyTable tt;
	public CyTable et;

	Map<DualPoint, Double> distances;

	public TypeNetwork(final InseqActivator ia) {
		this.ia = ia;
	}

	// gets the first node with the specified name if it exists, else returns null
	public static CyNode getNodeWithName(CyNetwork net, CyTable table, String name)
	{
		for(CyNode node : net.getNodeList())
		{
			CyRow row  = table.getRow(node.getSUID());
			String nodeName = row.get(CyNetwork.NAME, String.class);
			if (nodeName.equals(name)) return node;
		}
		return null;
	}



	public void makeNetwork()
	{
		CyNetwork typeNet = ia.appAdapter.getCyNetworkFactory().createNetwork();
		typeNet.getRow(typeNet).set(CyNetwork.NAME, "type network");
		CyTable typeTable = typeNet.getDefaultNodeTable();

		ArrayList<String> genes = new ArrayList<String>();

		//get a list of all genes that have low distances
		for(DualPoint dp : distances.keySet())
		{

			String name1 = ia.selTranscripts.get(dp.p1);
			if(!genes.contains(name1))
				genes.add(name1);
			String name2 = ia.selTranscripts.get(dp.p2);
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
		ia.edgePoints = new HashMap<CyEdge, ArrayList<DualPoint>>();

			for(DualPoint dp : distances.keySet())
			{
				String name1 = ia.selTranscripts.get(dp.p1);
				String name2 = ia.selTranscripts.get(dp.p2);
				
				if (name1.equals(name2)) continue;

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

				if(!typeNet.containsEdge(n1,n2))
				{
					CyEdge edge = typeNet.addEdge(n1, n2, false);
					ia.edgePoints.put(edge, new ArrayList<DualPoint>());	
					ia.edgePoints.get(edge).add(dp);	

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
					ia.edgePoints.get(edge).add(dp);	
					
					CyRow edgeRow = edgeTable.getRow(edge.getSUID());
					edgeRow.set("weight", distance + edgeRow.get("weight", Double.class)); 
					edgeRow.set("node1", name1); 
					edgeRow.set("node2", name2); 
					edgeRow.set("num", edgeRow.get("num", Integer.class) + 1);
				}

			}
		
			

		ArrayList<CyEdge> poorEdges = new ArrayList<CyEdge>();
		for(CyEdge edge : typeNet.getEdgeList())
		{
			CyRow row = edgeTable.getRow(edge.getSUID());
			int startNum = row.get("num", Integer.class);
			row.set("weight", row.get("weight", Double.class) / (double)startNum);
			
			int node1Num = ia.mps.get(row.get("node1", String.class)).size();
			int node2Num = ia.mps.get(row.get("node2", String.class)).size();
			row.set("normal", Math.sqrt((double)startNum / (double)(node1Num) / (double)node2Num) );
			if(row.get("normal", Double.class) < requiredNum)
				poorEdges.add(edge);
		}
		typeNet.removeEdges(poorEdges);

		CyNetworkView view = ia.appAdapter.getCyNetworkViewFactory().createNetworkView(typeNet);

		VisualStyle vs = ia.appAdapter.getVisualStyleFactory().createVisualStyle("Inseq Style");
		
		VisualMappingFunctionFactory cvmf = ia.appAdapter.getVisualMappingFunctionContinuousFactory();
		VisualMappingFunctionFactory pvmf = ia.appAdapter.getVisualMappingFunctionPassthroughFactory();
		
		VisualMappingFunction<String,String> ntool = pvmf.createVisualMappingFunction("num", String.class, BasicVisualLexicon.NODE_TOOLTIP);
		vs.addVisualMappingFunction(ntool);
		VisualMappingFunction<String,String> etool = pvmf.createVisualMappingFunction("num", String.class, BasicVisualLexicon.EDGE_TOOLTIP);
		vs.addVisualMappingFunction(etool);
		VisualMappingFunction<String,String> pMap = pvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(pMap);
		VisualMappingFunction<Integer,Double> sizeMap = cvmf.createVisualMappingFunction("num", Integer.class, BasicVisualLexicon.NODE_SIZE);
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(1,new  BoundaryRangeValues<Double>(20d,25d,30d));
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(100,new  BoundaryRangeValues<Double>(40d,50d,60d));
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(10000,new  BoundaryRangeValues<Double>(80d,90d,100d));
		vs.addVisualMappingFunction(sizeMap);
		VisualMappingFunction<Double,Double> edgeMap = cvmf.createVisualMappingFunction("normal", Double.class, BasicVisualLexicon.EDGE_WIDTH);
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(0.01d,new  BoundaryRangeValues<Double>(0d,0d,3d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(0.1d,new  BoundaryRangeValues<Double>(6d,8d,10d));
		vs.addVisualMappingFunction(edgeMap);
		vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.LIGHT_GRAY);
		
		for (VisualStyle style : ia.appAdapter.getVisualMappingManager().getAllVisualStyles()) {
			if (style.getTitle() == "Inseq Style") {
				ia.appAdapter.getVisualMappingManager().removeVisualStyle(style);
				break;
			}
		}
		
		ia.appAdapter.getVisualMappingManager().addVisualStyle(vs);
		ia.appAdapter.getVisualMappingManager().setCurrentVisualStyle(vs);
		vs.apply(view);

		final CyLayoutAlgorithmManager algm = ia.appAdapter.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		ia.appAdapter.getTaskManager().execute(itr);

		// have to find it first, destroy it after already adding the new one, otherwise things get messed up
		CyNetwork dnet = null;
		for (CyNetwork net : ia.appAdapter.getCyNetworkManager().getNetworkSet()) {
			if(net.getRow(net).get(CyNetwork.NAME, String.class) == "type network") {
				dnet = net;
				break;
			}
		}
		ia.appAdapter.getCyNetworkManager().addNetwork(typeNet);
		tn = typeNet;
		tt = typeTable;
		et = edgeTable;
		ia.appAdapter.getCyNetworkViewManager().addNetworkView(view);
		view.updateView();

		ia.appAdapter.getCyNetworkManager().destroyNetwork(dnet);

	}
}


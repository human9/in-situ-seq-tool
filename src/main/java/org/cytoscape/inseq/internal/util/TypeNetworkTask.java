package org.cytoscape.inseq.internal.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.inseq.internal.InseqActivator;
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
import edu.wlu.cs.levy.CG.KeySizeException;


public class TypeNetworkTask extends AbstractTask {

	private InseqActivator ia;
	private KDTree<Transcript> tree;
	private double cutoff;

	public TypeNetworkTask(final InseqActivator ia, KDTree<Transcript> t, double c) {
		this.ia = ia;
		this.tree = t;
		this.cutoff = c;
	}

	/** A temporary node class for constructing the network. 
	 *  This is used because getting and setting values on actual CyNodes is slower and more complicated.
	 */
	private class Node {
		String name;
		Integer num = 0;
		Map<String, Integer> coNodes;

		Node(String name) {
			this.name = name;
			coNodes = new HashMap<String, Integer>();
		}
	}


	/** Creates a network based on the nearest neighbours of transcripts.
	 *  
	 */
	public void run(TaskMonitor taskMonitor) {

		Map<String, Node> nodes = new HashMap<String, Node>();

		// Iterate through all our transcripts
		try {
			for (Transcript t : tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE}))
			{
				// If no neighbours were found for this transcript, go to next.
				if(t.neighbours == null) continue;

				// If we haven't made a node for this transcript name, make one
				if(!nodes.containsKey(t.name)) {
					nodes.put(t.name, new Node(t.name));
				}

				Node node = nodes.get(t.name);
				node.num++;

				// Iterate through neighbours of this transcript
				for(Transcript n : t.neighbours) {

					// Register this transcript as a neighbour of this node if we haven't already
					if(!node.coNodes.containsKey(n.name)) {
						node.coNodes.put(n.name, 0);
					}

					// Increment the count for this relationship
					node.coNodes.put(n.name, node.coNodes.get(n.name) + 1);
				}
			}
		}
		catch (KeySizeException e) {
			e.printStackTrace();
			return;
		}

		// Create the network and name it
		CyNetwork network = ia.getCAA().getCyNetworkFactory().createNetwork();
		network.getRow(network).set(CyNetwork.NAME, "Type Network");

		// Get the node table and add columns
		CyTable nodeTable = network.getDefaultNodeTable();
		nodeTable.createColumn("num", Integer.class, false);
		
		// Get the edge table and add columns
		CyTable edgeTable = network.getDefaultEdgeTable();	
		edgeTable.createColumn("num", Integer.class, false);
		edgeTable.createColumn("normal", Double.class, false);

		// Add nodes into actual network
		for (Node n : nodes.values())
		{
			CyNode node = network.addNode();
			CyRow row = nodeTable.getRow(node.getSUID());
			row.set(CyNetwork.NAME, n.name);
			row.set("num", n.num);
		}

		// Add edges into actual network
		for (Node n : nodes.values()) 
		{
			for(String s : n.coNodes.keySet()) {
				
				int nonNormalScore = n.coNodes.get(s);
				int thisNodeNumber = n.num;
				int otherNodeNumber = nodes.get(s).num;
				double normal = (double)nonNormalScore / thisNodeNumber / otherNodeNumber;

				// Skip adding the edge if it doesn't qualify
				if(normal < cutoff) continue;

				CyNode thisNode = NetworkUtil.getNodeWithName(network, nodeTable, n.name);
				CyNode otherNode = NetworkUtil.getNodeWithName(network, nodeTable, s);
					
				if(!network.containsEdge(thisNode, otherNode)) {
					CyEdge edge = network.addEdge(thisNode, otherNode, false);

					CyRow row = edgeTable.getRow(edge.getSUID());
					row.set(CyNetwork.NAME, "Co-occurence");
					row.set("num", nonNormalScore); 
					row.set("normal", normal); 
				}
			}
		}
		
		CyNetworkView view = ia.getCAA().getCyNetworkViewFactory().createNetworkView(network);

		VisualStyle vs = ia.getCAA().getVisualStyleFactory().createVisualStyle("Inseq Style");
		
		VisualMappingFunctionFactory cvmf = ia.getCAA().getVisualMappingFunctionContinuousFactory();
		VisualMappingFunctionFactory pvmf = ia.getCAA().getVisualMappingFunctionPassthroughFactory();
		
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
		
		for (VisualStyle style : ia.getCAA().getVisualMappingManager().getAllVisualStyles()) {
			if (style.getTitle() == "Inseq Style") {
				ia.getCAA().getVisualMappingManager().removeVisualStyle(style);
				break;
			}
		}
		
		ia.getCAA().getVisualMappingManager().addVisualStyle(vs);
		ia.getCAA().getVisualMappingManager().setCurrentVisualStyle(vs);
		vs.apply(view);

		final CyLayoutAlgorithmManager algm = ia.getCAA().getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		ia.getCAA().getTaskManager().execute(itr);

		// have to find it first, destroy it after already adding the new one, otherwise things get messed up
		CyNetwork dnet = null;
		for (CyNetwork net : ia.getCAA().getCyNetworkManager().getNetworkSet()) {
			if(net.getRow(net).get(CyNetwork.NAME, String.class) == "type network") {
				dnet = net;
				break;
			}
		}
		ia.getCAA().getCyNetworkManager().addNetwork(network);
		ia.getCAA().getCyNetworkViewManager().addNetworkView(view);
		view.updateView();

		ia.getCAA().getCyNetworkManager().destroyNetwork(dnet);


		ia.getSession().network = network;
		ia.getSession().nodeTable = nodeTable;

	}
}

package org.cytoscape.inseq.internal.typenetwork;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;


public class TypeNetworkTask extends AbstractTask {

	private TypeNetwork net;
	private KDTree<Transcript> tree;
	private CyAppAdapter a;

	public TypeNetworkTask(TypeNetwork n, InseqSession s, CyAppAdapter a) {
		this.net = n;
		this.tree = s.tree;
		this.a = a;
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

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Creating network view");
		Map<String, Node> nodes = new HashMap<String, Node>();

		// Iterate through all our transcripts
		try {
		
			for (Transcript t : tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE}))
			{

				// If no neighbours were found for this transcript, go to next.
				if(t.getNeighboursForNetwork(net) == null) continue;
				if(t.getNeighboursForNetwork(net).size() < 1) continue;

				// If we haven't made a node for this transcript name, make one
				if(!nodes.containsKey(t.name)) {
					nodes.put(t.name, new Node(t.name));
				}

				Node node = nodes.get(t.name);
				node.num++;

				// Iterate through neighbours of this transcript
				for(Transcript n : t.getNeighboursForNetwork(net)) {

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

		CyNetwork network = net.getNetwork();

		// Name the network
		network.getRow(network).set(CyNetwork.NAME, String.format("%.2f-unit TypeNetwork", net.getDistance()));

		// Get the node table and add columns
		CyTable nodeTable = net.getNodeTable();
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
		a.getCyEventHelper().flushPayloadEvents();

		// Add edges into actual network
		for (Node n : nodes.values()) 
		{
			for(String s : n.coNodes.keySet()) {
				
				int nonNormalScore = n.coNodes.get(s);
				int thisNodeNumber = n.num;
				int otherNodeNumber = nodes.get(s).num;
				double normal = (double)nonNormalScore / thisNodeNumber / otherNodeNumber;

				// Skip adding the edge if it doesn't qualify
				if(normal < net.getCutoff()) continue;

				CyNode thisNode = NetworkUtil.getNodeWithName(network, nodeTable, n.name);
				CyNode otherNode = NetworkUtil.getNodeWithName(network, nodeTable, s);
					
				if(!network.containsEdge(thisNode, otherNode)) {
					CyEdge edge = network.addEdge(thisNode, otherNode, false);

					CyRow row = edgeTable.getRow(edge.getSUID());
					String edgeName;

					if(n.name.compareTo(s) > 0)
						edgeName = n.name + "-" + s;
					else
						edgeName = s + "-" + n.name;

					row.set(CyNetwork.NAME, edgeName);
					row.set(CyEdge.INTERACTION, "Co-occurence");
					row.set("num", nonNormalScore); 
					row.set("normal", normal); 
				}
			}
		}
	}
}

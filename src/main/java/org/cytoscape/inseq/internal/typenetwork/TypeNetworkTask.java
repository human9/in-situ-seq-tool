package org.cytoscape.inseq.internal.typenetwork;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
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
		int num = 0;
		int totalNum = 0;
		Map<String, Integer> coNodes;
		Map<String, Integer> baseCoNodes;

		Node(String name) {
			this.name = name;
			coNodes = new HashMap<String, Integer>();
			baseCoNodes = new HashMap<String, Integer>();
		}
	}


	/** Creates a network based on the nearest neighbours of transcripts.
	 *  
	 */
	public void run(TaskMonitor taskMonitor) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Creating network view");
		Map<String, Node> nodes = new HashMap<String, Node>();

		int N = 0;
		int K = 0;
		// Iterate through all our transcripts
		try {
		
			for (Transcript t : tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE}))
			{

				N++;
				// If we haven't made a node for this transcript name, make one
				if(!nodes.containsKey(t.name)) {
					nodes.put(t.name, new Node(t.name));
				}

				Node node = nodes.get(t.name);

				node.totalNum++;

				// If no neighbours were found for this transcript, go to next.
				if(t.getNeighboursForNetwork(net) == null) continue;
				if(t.getNeighboursForNetwork(net).size() < 1) continue;

				// If the neighbours aren't within the selection, go to next.
				if(t.getSelection(net) != net.getSelection()) continue;

				node.num++;
				//K++;

				Map<String, Boolean> hasAdded = new HashMap<String, Boolean>();

				// Iterate through neighbours of this transcript
				for(Transcript n : t.getNeighboursForNetwork(net)) {

					// Register this transcript as a neighbour of this node if we haven't already
					if(!node.coNodes.containsKey(n.name)) {
						node.coNodes.put(n.name, 0);
					}

					// Increment the count for this relationship
					node.coNodes.put(n.name, node.coNodes.get(n.name) + 1);
					
					if(hasAdded.get(n.name) == null) {
						if(!node.baseCoNodes.containsKey(n.name)) {
							node.baseCoNodes.put(n.name, 0);
						}
						node.baseCoNodes.put(n.name, node.baseCoNodes.get(n.name) + 1);
						hasAdded.put(n.name, true);
					}
				}
			}
		}
		catch (KeySizeException e) {
			e.printStackTrace();
			return;
		}


	
		CyNetwork network = net.getNetwork();

		// Name the network
		String name = network.getRow(network).get(CyNetwork.NAME, String.class);
		if(name != null)
		{
			network.getRow(network).set(CyNetwork.NAME, name);
		}
		else {
			network.getRow(network).set(CyNetwork.NAME, String.format("%.2f-unit TypeNetwork", net.getDistance()));
		}

		// Get the node table and add columns
		CyTable nodeTable = net.getNodeTable();
		nodeTable.createColumn("num", Integer.class, false);
		nodeTable.createColumn("proportion", Double.class, false);
		nodeTable.createColumn("selfnorm", Double.class, false);
		
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
			row.set("num", n.totalNum);
			row.set("proportion", (double)n.totalNum/N);
		}
		a.getCyEventHelper().flushPayloadEvents();

		// Add edges into actual network
		for (Node node : nodes.values()) 
		{
			for(String s : node.coNodes.keySet()) {
				
				CyNode thisNode = NetworkUtil.getNodeWithName(network, nodeTable, node.name);
				CyNode otherNode = NetworkUtil.getNodeWithName(network, nodeTable, s);
				
				int rawScore = node.coNodes.get(s);
				int n1 = node.totalNum;
				int n2 = nodes.get(s).totalNum;
				int n;
				int k;

				int thisCoNum = NetworkUtil.getNonNullInt(node.baseCoNodes.get(s));
				int thatCoNum = NetworkUtil.getNonNullInt(nodes.get(s).baseCoNodes.get(node.name));
				if(thisNode == otherNode)
				{
					n = n1;
					K = node.num;
					k = thisCoNum; 
				}
				else
				{
					n = n1 + n2;
					K = node.num + nodes.get(s).num;
					k = thisCoNum + thatCoNum; 
				}

				HypergeometricDistribution hgd = new HypergeometricDistribution(N,K,n);

				double cpf = hgd.cumulativeProbability(k);
				System.out.println();
				System.out.print(String.format("N:%d K:%d n:%d k:%d\n", N, K, n, k));
				System.out.println("SIG:"+node.name + "-" + s +": "+ cpf);

				// The normalised score is the raw co-occurence count divided by
				// the geometric mean of the total counts of both genes
				double normal = (double)rawScore / Math.sqrt((double)node.num * (double)nodes.get(s).num);

				// Skip adding the edge if it doesn't qualify
				//if(normal < net.getCutoff()) continue;
				
				/* THE HYPERGEOMETRIC DISTRIBUTION
				 *
				 *            (K)(N-K)
				 *            (k)(n-k)
				 * P(X = k) = --------
				 *              (N)
				 *              (n)
				 *
				 * N = total number of transcripts
				 * K = number of co-occurences for these two transcripts
				 * n = total no. of this + other transcript
				 * k = no. times these transcripts co-occur with each other
				 */

				// if 95% of random occurence likelihood is below us, this edge is significant
				if(cpf < 0.95) continue;

				// If this would be a self-link, give the normal to selfnorm property instead
				if(thisNode == otherNode) {
					nodeTable.getRow(thisNode.getSUID()).set("selfnorm", normal);
					continue;
				}
					
				if(!network.containsEdge(thisNode, otherNode)) {
					CyEdge edge = network.addEdge(thisNode, otherNode, false);

					CyRow row = edgeTable.getRow(edge.getSUID());
					String edgeName;

					if(node.name.compareTo(s) > 0)
						edgeName = node.name + "-" + s;
					else
						edgeName = s + "-" + node.name;

					row.set(CyNetwork.NAME, edgeName);
					row.set(CyEdge.INTERACTION, "Co-occurence");
					row.set("num", rawScore); 
					row.set("normal", normal); 
				}
			}
		}
	}
}

package org.cytoscape.inseq.internal.typenetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.inseq.internal.util.ParseUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

/**
 * TODO: Look into spatial autocorrelation...
 */
public class ShuffleTask extends AbstractTask {

	private TypeNetwork net;
	private InseqSession session;

	public ShuffleTask(TypeNetwork n, InseqSession s, CyAppAdapter a) {
		this.net = n;
		this.session = s;
	}

	/** Shuffles gene names in order to generate a random distribution.
	 *  
	 */
	public void run(TaskMonitor taskMonitor) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Finding distribution by shuffling names");

        // A list of every single transcript
		List <Transcript> range = ParseUtil.getRange(session.tree, 0, 0, 
                Double.MAX_VALUE, Double.MAX_VALUE);
		
        // A list into which only transcripts which both have neighbours
        // and are within the selection will be put
        List<Transcript> filtered = new ArrayList<Transcript>();

        // Stores the number of each transcript name that are found
		Map<String, Integer> nodes = new HashMap<String, Integer>();
		
		int N = 0;

		for (Transcript t : range)
		{
			N++;

			// If the neighbours aren't within the selection, go to next.
			if(t.getSelection(net) != net.getSelection()) continue;
			
			if(!nodes.keySet().contains(t.name)) {
				nodes.put(t.name, 0);
			}
			else {
				nodes.put(t.name, nodes.get(t.name) + 1);
			}
			// If no neighbours were found for this transcript, go to next.
			if(t.getNeighboursForNetwork(net) == null) continue;
			if(t.getNeighboursForNetwork(net).size() < 1) continue;
			
			filtered.add(t);
		}
		
        // Store a unique list of transcripts
		List<Transcript[]> edges = new ArrayList<Transcript[]>();

        // For every neighbour of the given transcript, add an edge
		for (Transcript t: filtered) {
			for(Transcript n : t.getNeighboursForNetwork(net)) {
				edges.add(new Transcript[]{t,n});
			}
		}

        // Make into proper array cos fast
		Transcript[][] uniqueCombos 
            = edges.toArray(new Transcript[edges.size()][]);

        // How many times to randomly shuffle
		int reps = 100;
		
        // 2D grid array for every gene interaction
        // The intersection of index1 * index2 will contain an array of length
        // <reps>, and stores the results of how many times this interaction
        // was found.
		int[][] edgecount = new int[nodes.size()*nodes.size()][reps+1];

		for(int x = 0; x < reps; x++)
		{
			if(cancelled) return;

			taskMonitor.setProgress((double)x/reps);

			session.shuffleNames();	

			for(int i = 0; i < uniqueCombos.length; i++) {
				int index = (uniqueCombos[i][0].type + 1)
                    * (uniqueCombos[i][1].type + 1);
				edgecount[index-1][x]++;
			}
		}
		
		session.restoreNames();
		for(int i = 0; i < uniqueCombos.length; i++) {
			int index = (uniqueCombos[i][0].type + 1)
                * (uniqueCombos[i][1].type + 1);
			edgecount[index-1][reps]++;
		}

		CyNetwork network = net.getNetwork();

		// Name the network
		String name
            = network.getRow(network).get(CyNetwork.NAME, String.class);
		if(name != null)
		{
			network.getRow(network).set(CyNetwork.NAME, name);
		}
		else {
			network.getRow(network).set(CyNetwork.NAME,
                    String.format("%.2f-unit TypeNetwork", net.getDistance()));
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
		for (String n : nodes.keySet())
		{
			CyNode node = network.addNode();
			CyRow row = nodeTable.getRow(node.getSUID());
			row.set(CyNetwork.NAME, n);
			row.set("num", nodes.get(n));
			row.set("proportion", (double)nodes.get(n)/N);
		}


		List<Integer> ints = new ArrayList<Integer>();
		int sigcount= 0;
		for(int i = 0; i < uniqueCombos.length; i++) {
			int index = (uniqueCombos[i][0].type + 1)
                * (uniqueCombos[i][1].type + 1);
			if(!ints.contains(index))
			{
				ints.add(index);

				StandardDeviation std = new StandardDeviation();
                Mean m = new Mean();
				
                double[] values = new double[reps];
				
                for(int z = 0; z < reps; z++) {
                    double num = edgecount[index-1][z] / 2;
					values[z] = num;
				}
				double stdev = std.evaluate(values);
                double mean = m.evaluate(values, 0, values.length);

				String edgeName = session.getTypeName(uniqueCombos[i][0].type)
                    + "-" + session.getTypeName(uniqueCombos[i][1].type);
				double actual = edgecount[index-1][reps]/2d;
				double Z = (actual - mean) / stdev;
				
				if(Math.abs(Z) > 1.96)
				{
					if(Z > 1.96) {
						CyNode thisNode = NetworkUtil.getNodeWithName(network,
                                nodeTable, uniqueCombos[i][0].name);
						CyNode otherNode = NetworkUtil.getNodeWithName(network,
                                nodeTable, uniqueCombos[i][1].name);
						
						if(thisNode == otherNode) {
							nodeTable.getRow(thisNode.getSUID())
                                .set("selfnorm", Z);
							continue;
						}

						CyEdge edge
                            = network.addEdge(thisNode, otherNode, false);

						CyRow row = edgeTable.getRow(edge.getSUID());

						row.set(CyNetwork.NAME, edgeName);
						row.set(CyEdge.INTERACTION, "Co-occurence");
						row.set("num", (int)actual); 
						row.set("normal", Z); 
					}
					sigcount++;
				}
			}
		}
		System.out.println(sigcount + " significant interactions found.\n"
                + edges.size()/2 + " total interactions.");
	}
}

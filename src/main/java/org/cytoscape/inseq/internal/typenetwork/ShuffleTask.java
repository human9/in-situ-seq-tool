package org.cytoscape.inseq.internal.typenetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * 
 */
class Colocation {

    private Transcript first;
    private Transcript second;

    public int actualCount = 0;
    public double expectedCount = 0;

    public Colocation(Transcript t1, Transcript t2) {
        Transcript[] ordered = ParseUtil.orderTranscripts(t1, t2);
        first = ordered[0];
        second = ordered[1];
    }

    public Transcript getFirst() {
        return first;
    }
    
    public Transcript getSecond() {
        return second;
    }
}

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
        List <Transcript> allTranscripts = ParseUtil.getRange(session.tree, 0, 0, 
                Double.MAX_VALUE, Double.MAX_VALUE);
        
        // A map of all colocations within the selection
        Map<String, Colocation> colocations = new HashMap<String, Colocation>();

        // Stores the number of each transcript name that are found
        Map<String, Integer> numTranscriptsForGene = new HashMap<String, Integer>();
        
        // Number of transcripts in the selection
        int N = 0;

        // Number of colocations
        int k = 0;

        for (Transcript t : allTranscripts)
        {

            // If t isn't inside the selection, go to next.
            if(t.getSelection(net) != net.getSelection()) continue;
            
            N++;
            
            // Increment n for this gene
            if(!numTranscriptsForGene.keySet().contains(t.name)) {
                numTranscriptsForGene.put(t.name, 0);
            }
            numTranscriptsForGene.put(t.name,
                        numTranscriptsForGene.get(t.name) + 1);

            // If t isn't colocated, go to next.
            if(t.getNeighboursForNetwork(net) == null) continue;
            if(t.getNeighboursForNetwork(net).size() < 1) continue;

            //
            for(Transcript n : t.getNeighboursForNetwork(net)) {
                
                String key = ParseUtil.generateName(t, n);
                if(!colocations.containsKey(key)) {
                    colocations.put(key, new Colocation(t, n));
                }
                colocations.get(key).actualCount++;
                k++;
            }
        }


        // Because we count every colocation twice
        k /= 2;
        for(Colocation c : colocations.values()) {
            c.actualCount /= 2;
            int na = numTranscriptsForGene.get(c.getFirst().name);
            int nb = numTranscriptsForGene.get(c.getSecond().name);
            c.expectedCount = (2d*k*na*nb / ((double)N*N));
            System.out.println(N + ", " + k + ", " + na + ", " + nb);
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
        for (String n : numTranscriptsForGene.keySet())
        {
            CyNode node = network.addNode();
            CyRow row = nodeTable.getRow(node.getSUID());
            row.set(CyNetwork.NAME, n);
            row.set("num", numTranscriptsForGene.get(n));
            row.set("proportion", (double)numTranscriptsForGene.get(n)/N);
        }

        for(String key : colocations.keySet()) {
            
            Colocation colocation = colocations.get(key);

            double Z = (colocation.actualCount - colocation.expectedCount) / Math.sqrt(colocation.expectedCount);

            System.out.println(key + " expected: " + colocation.expectedCount
                    + " actual: " + colocation.actualCount + " Z: " + Z);

            if(Math.abs(Z) > 1.96)
            {
                if(Z > 1.96) {
                    CyNode thisNode = NetworkUtil.getNodeWithName(network,
                            nodeTable, colocation.getFirst().name);
                    CyNode otherNode = NetworkUtil.getNodeWithName(network,
                            nodeTable, colocation.getSecond().name);
                    
                    if(thisNode == otherNode) {
                        nodeTable.getRow(thisNode.getSUID())
                            .set("selfnorm", Z);
                        continue;
                    }

                    CyEdge edge
                        = network.addEdge(thisNode, otherNode, false);

                    CyRow row = edgeTable.getRow(edge.getSUID());

                    row.set(CyNetwork.NAME, key);
                    row.set(CyEdge.INTERACTION, "Co-occurence");
                    row.set("num", (int) colocation.actualCount); 
                    row.set("normal", Z); 
                }
            }
        }
    }
}

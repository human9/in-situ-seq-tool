package org.cytoscape.inseq.internal.typenetwork;

import java.util.HashMap;
import java.util.List;
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

/**
 * 
 */

public class ShuffleTask extends AbstractTask {

    private TypeNetwork net;
    private InseqSession session;
    private boolean interaction;
    private CyNetwork network;
    private CyTable nodeTable;
    private CyTable edgeTable;

    public ShuffleTask(TypeNetwork n, boolean interaction, InseqSession s, CyAppAdapter a) {
        this.net = n;
        this.session = s;
        this.interaction = interaction;
    }

    /** Shuffles gene names in order to generate a random distribution.
     *  
     */
    public void run(TaskMonitor taskMonitor) {

        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Finding distribution by shuffling names");

        // A list of every single transcript
        List <Transcript> allTranscripts = session.tree.range(new double[]{
            0, 0,},
            new double[]{Double.MAX_VALUE, Double.MAX_VALUE});
        
        // A map of all colocations within the selection
        Map<String, Colocation> colocations = new HashMap<String, Colocation>();

        // Stores the number of each transcript name that are found
        int[] numTranscriptsForGene = new int[session.getGenes().size()];
        
        // Number of transcripts in the selection
        int N = 0;

        // Number of colocations
        int k = 0;

        System.out.println("\ninteraction, expected, actual, Z");
        for (Transcript t : allTranscripts)
        {

            // If t isn't inside the selection, go to next.
            if(t.getSelection(net) != net.getSelection()) continue;
            
            N++;

            // Increment n for this gene
            numTranscriptsForGene[t.type]++;

            // If t isn't colocated, go to next.
            if(t.getNeighboursForNetwork(net) == null) continue;
            if(t.getNeighboursForNetwork(net).size() < 1) continue;

            //
            for(Transcript n : t.getNeighboursForNetwork(net)) {
                
                String key = session.generateName(t, n);
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
            int na = numTranscriptsForGene[c.getFirst().type];
            int nb = numTranscriptsForGene[c.getSecond().type];
            if(c.getFirst().type == c.getSecond().type) {
                c.expectedCount = ((double)k*(na*(double)na-na)) / ((double)N*N - N);
                System.out.println("\n\n" + session.name(c.getFirst().type) + k + "," + na);
            } else {
                c.expectedCount = (2d*k*na*nb) / ((double)N*N - N);
            }
        }
        

        network = net.getNetwork();

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
        nodeTable = net.getNodeTable();
        nodeTable.createColumn("num", Integer.class, false);
        nodeTable.createColumn("proportion", Double.class, false);
        nodeTable.createColumn("selfnorm", Double.class, false);
        
        // Get the edge table and add columns
        edgeTable = network.getDefaultEdgeTable();  
        edgeTable.createColumn("num", Integer.class, false);
        edgeTable.createColumn("normal", Double.class, false);

        // Add nodes into actual network
        for (int i = 0; i < numTranscriptsForGene.length; i++)
        {
            CyNode node = network.addNode();
            CyRow row = nodeTable.getRow(node.getSUID());
            row.set(CyNetwork.NAME, session.name(i));
            row.set("num", numTranscriptsForGene[i]);
            row.set("proportion", (double)numTranscriptsForGene[i]/N);
        }

        for(String key : colocations.keySet()) {
            
            Colocation colocation = colocations.get(key);

            double Z = (colocation.actualCount - colocation.expectedCount) / Math.sqrt(colocation.expectedCount);

            System.out.println(key + ", " + colocation.expectedCount
                    + ", " + colocation.actualCount + ", " + Z);

            if(interaction) {
                if(Z > 1.96) {
                    addEdge(colocation, Z, key);
                }
            }
            else {
                if(Z < -1.96) {
                    addEdge(colocation, Z, key);
                }

            }

        }
    }
    private void addEdge(Colocation colocation, double Z, String key) {

            CyNode thisNode = NetworkUtil.getNodeWithName(network,
                    nodeTable, session.name(colocation.getFirst().type));
            CyNode otherNode = NetworkUtil.getNodeWithName(network,
                    nodeTable, session.name(colocation.getSecond().type));
            
            if(thisNode == otherNode) {
                nodeTable.getRow(thisNode.getSUID())
                    .set("selfnorm", Math.abs(Z));
                return;
            }

            CyEdge edge
                = network.addEdge(thisNode, otherNode, false);

            CyRow row = edgeTable.getRow(edge.getSUID());

            row.set(CyNetwork.NAME, key);
            row.set(CyEdge.INTERACTION, "Co-occurence");
            row.set("num", (int) colocation.actualCount); 
            row.set("normal", Math.abs(Z)); 
    }
    
    class Colocation {

        private Transcript first;
        private Transcript second;

        public int actualCount = 0;
        public double expectedCount = 0;

        public Colocation(Transcript t1, Transcript t2) {
            Transcript[] ordered = session.orderTranscripts(t1, t2);
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
}

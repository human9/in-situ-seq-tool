package org.cytoscape.inseq.internal.typenetwork;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
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
    private String genName;
    private double sigLevel;
    private boolean bonferroni;

    public ShuffleTask(TypeNetwork n, boolean interaction, InseqSession s, String genName,
            double sigLevel, boolean bonferroni) {
        this.net = n;
        this.sigLevel = sigLevel;
        this.bonferroni = bonferroni;
        this.session = s;
        this.interaction = interaction;
        this.genName = genName;
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

        int[] numColocationsForGene = new int[session.getGenes().size()];

        // Stores the number of each transcript name that are found
        int[] numTranscriptsForGene = new int[session.getGenes().size()];
        
        // Number of transcripts in the selection
        int N = 0;

        // Number of colocations
        int k = 0;

        //System.out.println("\ninteraction, expected, actual, Z");
        for (Transcript t : allTranscripts)
        {

            // If t isn't inside the selection, go to next.
            if(t.getSelection(net) != net.getSelection()) continue;
            
            N++;


            // If t isn't colocated, go to next.
            if(t.getNeighboursForNetwork(net) == null) continue;
            if(t.getNeighboursForNetwork(net).size() < 1) continue;
            
            // Increment n for this gene
            numTranscriptsForGene[t.type]++;

            //
            for(Transcript n : t.getNeighboursForNetwork(net)) {
                
                String key = session.generateName(t, n);
                if(!colocations.containsKey(key)) {
                    colocations.put(key, new Colocation(t, n));
                }
                colocations.get(key).actualCount++;
                k++;

                if(n.type == t.type) {
                    numColocationsForGene[t.type]++;
                }
                else {
                    numColocationsForGene[n.type]++;
                    numColocationsForGene[t.type]++;
                }
            }
        }


        // Because we count every colocation twice
        k /= 2;
        for(Colocation c : colocations.values()) {
            c.actualCount /= 2;
            int na = numColocationsForGene[c.getFirst().type] / 2;
            int nb = numColocationsForGene[c.getSecond().type] / 2;
            System.out.println(session.name(c.getFirst().type) + session.name(c.getSecond().type) + k + ", " + na + ", " + nb);
            if(c.getFirst().type == c.getSecond().type) {
                c.expectedCount = 0;
                //c.expectedCount = ((double)k*(na*(double)na-na)) / ((double)N*N - N);
                //System.out.println("\n\n" + session.name(c.getFirst().type) + k + "," + na);
            } else {
                //c.expectedCount = (2d*k*na*nb) / ((double)N*N - N);
                HypergeometricDistribution hd = new HypergeometricDistribution(k, na, nb);
                double p = hd.upperCumulativeProbability(c.actualCount);
                System.out.println(session.name(c.getFirst().type) + session.name(c.getSecond().type) + p);
                c.expectedCount = p;
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
            //System.out.println(genName);
            network.getRow(network).set(CyNetwork.NAME, genName);
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
            net.addNode(i, node);
            CyRow row = nodeTable.getRow(node.getSUID());
            row.set(CyNetwork.NAME, session.name(i));
            row.set("num", numTranscriptsForGene[i]);
            row.set("proportion", (double)numTranscriptsForGene[i]/N);
        }
/*
        double a = 100 - sigLevel;
        
        if(bonferroni) {
            double m = colocations.size();
            a = 1 - a/(m);
        }

        double decimal = a / 100;

        double div = decimal / 2;

        double level = 1 - div;

        System.out.println(level);

        NormalDistribution d = new NormalDistribution();
        double q = d.inverseCumulativeProbability(level);*/


        Comparator<Colocation> comparator = new Comparator<Colocation>() {
            public int compare(Colocation c1, Colocation c2) {
                return c2.rank - c1.getId(); // use your logic
            }
        };
        
        for(String key : colocations.keySet()) {
            
            Colocation colocation = colocations.get(key);

            double Z = (colocation.actualCount - colocation.expectedCount) / Math.sqrt(colocation.expectedCount);

            Z = colocation.expectedCount;


            //System.out.println(key + ", " + colocation.expectedCount
              //      + ", " + colocation.actualCount + ", " + Z);

            if(interaction) {
                if(Z < 0.05d) {
                    addEdge(colocation, Z, key);
                }
            }
        /*    else {
                if(Z < -q) {
                    addEdge(colocation, Z, key);
                }

            }
*/
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

        public int rank;
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

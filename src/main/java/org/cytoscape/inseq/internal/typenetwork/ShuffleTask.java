package org.cytoscape.inseq.internal.typenetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.TaskMonitor;

/**
 * Provide network generation using the label shuffling method. 
 */

public class ShuffleTask extends SpatialNetworkTask {

    private boolean interaction;
    private double sigLevel;
    private boolean bonferroni;

    public ShuffleTask(TypeNetwork n, boolean interaction, InseqSession s, String genName,
            double sigLevel, boolean bonferroni) {
        super(n, s, genName);
        this.sigLevel = sigLevel;
        this.bonferroni = bonferroni;
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

        //System.out.println("\ninteraction, expected, actual, Z");
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
            int na = getNumTranscript(c.getFirst());
            int nb = getNumTranscript(c.getSecond());
            if(c.getFirst() == c.getSecond()) {
                c.expectedCount = ((double)k*(na*(double)na-na)) / ((double)N*N - N);
                //System.out.println("\n\n" + session.name(c.getFirst().type) + k + "," + na);
            } else {
                c.expectedCount = (2d*k*na*nb) / ((double)N*N - N);
            }
        }
        

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
        double q = d.inverseCumulativeProbability(level);
        System.out.println(q);
        for(String key : colocations.keySet()) {
            
            Colocation colocation = colocations.get(key);

            double Z = (colocation.actualCount - colocation.expectedCount) / Math.sqrt(colocation.expectedCount);

            //System.out.println(key + ", " + colocation.expectedCount
              //      + ", " + colocation.actualCount + ", " + Z);

            if(interaction) {
                if(Z > q) {
                    addEdge(colocation, key);
                }
            }
            else {
                if(Z < -q) {
                    addEdge(colocation, key);
                }

            }

        }
    }
}

package org.cytoscape.inseq.internal.typenetwork;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.cytoscape.inseq.internal.InseqSession;
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
        

        // Number of colocations
        int k = 0;

        //System.out.println("\ninteraction, expected, actual, Z");
        for (Transcript t : session.getRaw())
        {

            // If t isn't inside the selection, go to next.
            if(t.getSelection(net) != net.getSelection()) continue;
            
            incrTotal();

            // Increment n for this gene
            incrTranscript(t.type);

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


        int N = getTotal();

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


        for(Colocation c : colocations.values()) {
            
            NormalDistribution d = new NormalDistribution(c.expectedCount, Math.sqrt(c.expectedCount));

            c.pvalue = 1d - d.cumulativeProbability(c.actualCount);
            
        }

        // Do ranking
        List<Colocation> colocationList = new ArrayList<Colocation>(colocations.values());
        rankEdges(colocationList, rankComparator);

        // Adds in all nodes, labels, etc
        initNetwork();

        for(String key : colocations.keySet()) {
            
            Colocation c = colocations.get(key);
        
            if(interaction) {
                if(c.actualCount >= c.expectedCount
                        && c.pvalue < 0.05d) {
                    addEdge(c, key);
                }
            }
            else {
                if(c.actualCount <= c.expectedCount
                        && c.pvalue < 0.05d) {
                    addEdge(c, key);
                }
            }

        }
    }
}

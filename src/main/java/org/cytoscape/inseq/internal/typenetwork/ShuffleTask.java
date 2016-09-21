package org.cytoscape.inseq.internal.typenetwork;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.work.TaskMonitor;

/**
 * Provide network generation using the label shuffling method. 
 */

public class ShuffleTask extends SpatialNetworkTask {

    public ShuffleTask(TypeNetwork n, InseqSession s, String genName,
            boolean interaction, double sigLevel) {
        super(n, s, genName, interaction, sigLevel);
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
                    colocations.put(key, new Colocation(session.orderTranscripts(t, n)));
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
                c.distributionMean = ((double)k*(na*(double)na-na)) / ((double)N*N - N);
                //System.out.println("\n\n" + session.name(c.getFirst().type) + k + "," + na);
            } else {
                c.distributionMean = (2d*k*na*nb) / ((double)N*N - N);
            }
        }
        
        for(Colocation c : colocations.values()) {
            
            NormalDistribution d = new NormalDistribution(c.distributionMean, Math.sqrt(c.distributionMean));

            c.probability = d.probability(c.actualCount);
            c.probabilityCumulative = d.cumulativeProbability(c.actualCount);
        }

        super.run(taskMonitor);

    }
}

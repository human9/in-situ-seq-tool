package org.cytoscape.inseq.internal.typenetwork;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.work.TaskMonitor;

/**
 * 
 */
public class HypergeometricTask extends SpatialNetworkTask {

    public HypergeometricTask(TypeNetwork n, InseqSession s, String genName,
            int interaction, double sigLevel) {
        super(n, s, genName, interaction, sigLevel);
    }

    /** Shuffles gene names in order to generate a random distribution.
     *  
     */
    public void run(TaskMonitor taskMonitor) {

        taskMonitor.showMessage(TaskMonitor.Level.INFO,
                "Finding distribution by shuffling names");

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
            

            for(Transcript n : t.getNeighboursForNetwork(net)) {
                
                String key = session.generateName(t, n);
                if(!colocations.containsKey(key)) {
                    colocations.put(key, new Colocation(session.orderTranscripts(t, n)));
                }
                colocations.get(key).add(n); // we should come to the other soon enough...
                colocations.get(key).actualCount++;

                if(n.type == t.type) {
                    incrColocalisation(t.type);
                }
                else {
                    incrColocalisation(n.type);
                    incrColocalisation(t.type);
                }
            }
        }

        for(Colocation c : colocations.values()) {

            HypergeometricDistribution hd = new HypergeometricDistribution(getTotal(), getNumTranscript(c.getFirst()), getNumTranscript(c.getSecond()));
            c.distributionMean = hd.getNumericalMean();
            
            c.probability = hd.probability(c.totalNum());
            c.probabilityCumulative = hd.cumulativeProbability(c.totalNum());
        }

        super.run(taskMonitor);
    }
}

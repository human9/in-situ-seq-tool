package org.cytoscape.inseq.internal.typenetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
 * Base class for spatial network construction.
 * Can extend this for a custom network creation algorithm. Exists because while
 * there are many ways to assign significance to edges, the basic routine of making
 * the network is usually the same.
 */
public class SpatialNetworkTask extends AbstractTask {

    final protected TypeNetwork net;
    final protected InseqSession session;
    
    private CyNetwork network;
    private CyTable nodeTable;
    private CyTable edgeTable;

    final String name;

    // Stores the number of each transcript name that are found
    private int[] numTranscriptsForGene;
    private int[] numColocationsForGene;
    protected HashMap<String, Colocation> colocations = new HashMap<String, Colocation>();

    private int interaction;
    private double sigLevel;

    // Total number of transcripts
    private int N = 0;

    public SpatialNetworkTask(TypeNetwork n, InseqSession s, String genName,
            int interaction, double sigLevel) {
        this.net = n;
        this.session = s;
        this.name = genName;
        this.interaction = interaction;
        this.sigLevel = sigLevel;

        network = net.getNetwork();
        nodeTable = net.getNodeTable();
        edgeTable = network.getDefaultEdgeTable();

        numTranscriptsForGene = new int[session.getGenes().size()];
        numColocationsForGene = new int[session.getGenes().size()];

    }

    public void run(TaskMonitor taskMonitor) {
        
        // Assign a sensible pvalue for ranking, and remove interactions as required
        List<Colocation> colocationList = new ArrayList<Colocation>(colocations.values());
        Iterator<Colocation> itr = colocationList.iterator();
        while(itr.hasNext()) {
            Colocation c = itr.next();
            
            switch(interaction) {
                case 0:
                    // Looking for more than expected, need to search the upper side
                    c.pvalue = 1d - (c.probabilityCumulative - c.probability); // invert probability
                    c.interaction = 0;

                    if(c.actualCount < c.distributionMean
                            || c.pvalue >= sigLevel) {
                        itr.remove();
                    }
                    break;
                case 1:
                    // Looking for less than expected, need to search the lower side
                    c.pvalue = c.probabilityCumulative; // no need to invert
                    c.interaction = 1;

                    if(c.actualCount > c.distributionMean
                            || c.pvalue >= sigLevel) {
                        itr.remove();
                    }
                    break;
                case 2:
                    // Look both ways
                    if(1d - (c.probabilityCumulative - c.probability) < sigLevel) {
                        c.pvalue = 1d - (c.probabilityCumulative - c.probability);
                        c.interaction = 0;
                    }
                    else if(c.probabilityCumulative < sigLevel) {
                        c.pvalue = c.probabilityCumulative;
                        c.interaction = 1;
                    }
                    else
                        itr.remove();
                    break;
            }
            
        }
        // Assign rank
        rankEdges(colocationList, Colocation.rankComparator);
        
        // Adds in all nodes, labels, etc
        initNetwork();

        for(String key : colocations.keySet()) {
            
            Colocation c = colocations.get(key);
        
            // Anything unranked was discarded earlier
            if(c.rank != 0) {
                addEdge(c, key);
            }

        }
    }

    protected void incrTotal() {
        N++;
    }

    protected int getTotal() {
        return N;
    }

    protected void incrTranscript(int type) {
        numTranscriptsForGene[type]++;
    }

    protected int getNumTranscript(int type) {
        return numTranscriptsForGene[type];
    }

    protected void incrColocalisation(int type) {
        numColocationsForGene[type]++;
    }

    protected int getNumColocalisation(int type) {
        return numColocationsForGene[type];
    }

    protected void initNetwork() {
        // Name the network
            
        network.getRow(network).set(CyNetwork.NAME, name);

        // Get the node table and add columns
        nodeTable.createColumn("num", Integer.class, false);
        nodeTable.createColumn("proportion", Double.class, false);
        nodeTable.createColumn("selfnorm", Double.class, false);
        
        // Get the edge table and add columns
        edgeTable.createColumn("num", Integer.class, false);
        edgeTable.createColumn("normal", Double.class, false);
        edgeTable.createColumn("rank", Integer.class, false);

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
        
    }

    protected void addEdge(Colocation colocation, String key) {

            CyNode thisNode = NetworkUtil.getNodeWithName(network,
                    nodeTable, session.name(colocation.getFirst()));
            CyNode otherNode = NetworkUtil.getNodeWithName(network,
                    nodeTable, session.name(colocation.getSecond()));
            
            if(thisNode == otherNode) {
                nodeTable.getRow(thisNode.getSUID())
                    .set("selfnorm", Math.abs(colocation.pvalue));
                return;
            }

            CyEdge edge
                = network.addEdge(thisNode, otherNode, false);

            CyRow row = edgeTable.getRow(edge.getSUID());

            row.set(CyNetwork.NAME, key);
            String interType = "Colocation ";
            switch(colocation.interaction) {
                case 0:
                    interType += "(Positive)";
                    break;
                case 1:
                    interType += "(Negative)";
                    break;
            }
            row.set(CyEdge.INTERACTION, interType);
            row.set("num", (int) colocation.actualCount); 
            row.set("rank", (int) colocation.rank);
            row.set("normal", Math.abs(colocation.pvalue)); 
    }


    protected void rankEdges(List<Colocation> colocations, Comparator<Colocation> comparator) {

        Collections.sort(colocations, comparator);

        // Give ranks, excluding self colocations
        int rank = 1;
        Iterator<Colocation> itr = colocations.iterator();
        while(itr.hasNext()) {
            Colocation c = itr.next();
            
            if(c.getFirst() == c.getSecond()) {
                itr.remove();
            }
            else {
                c.rank = rank++;
            }
        }
         
    }
}

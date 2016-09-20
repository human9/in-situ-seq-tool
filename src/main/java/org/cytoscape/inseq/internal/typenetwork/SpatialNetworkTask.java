package org.cytoscape.inseq.internal.typenetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    Comparator<Colocation> rankComparator;

    // Stores the number of each transcript name that are found
    private int[] numTranscriptsForGene;
    private int[] numColocationsForGene;
    protected HashMap<String, Colocation> colocations = new HashMap<String, Colocation>();

    private boolean interaction;
    private double sigLevel;

    // Total number of transcripts
    private int N = 0;

    public SpatialNetworkTask(TypeNetwork n, InseqSession s, String genName,
            boolean interaction, double sigLevel) {
        this.net = n;
        this.session = s;
        this.name = genName;
        this.interaction = interaction;
        this.sigLevel = sigLevel;

        network = net.getNetwork();
        nodeTable = net.getNodeTable();
        edgeTable = network.getDefaultEdgeTable();
    
        rankComparator = new Comparator<Colocation>() {
            public int compare(Colocation c1, Colocation c2) {
                if(c1.pvalue == c2.pvalue) return 0;
                else {
                    return c1.pvalue < c2.pvalue ? -1 : 1;
                }
            }
        };

        numTranscriptsForGene = new int[session.getGenes().size()];
        numColocationsForGene = new int[session.getGenes().size()];

    }

    public void run(TaskMonitor taskMonitor) {
        
        // Do ranking
        List<Colocation> colocationList = new ArrayList<Colocation>(colocations.values());

        Iterator<Colocation> itr = colocationList.iterator();
        while(itr.hasNext()) {
            Colocation c = itr.next();
            
            if(interaction) {
                if(c.actualCount < c.expectedCount
                        || c.pvalue >= sigLevel) {
                    itr.remove();
                }
            }
            else {
                if(c.actualCount > c.expectedCount
                        || c.pvalue >= sigLevel) {
                    itr.remove();
                }
            }
            
        }
        rankEdges(colocationList, rankComparator);
        
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
            row.set(CyEdge.INTERACTION, "Co-occurence");
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
    
    protected class Colocation {

        // First transcript type
        private int first;
        // Second transcript type
        private int second;

        public int rank;

        public int actualCount = 0;
        public double expectedCount = 0;
        
        // A p value of some sort, will be used for comparisons by default
        public double pvalue;

        // A set containing all transcripts used in this colocation
        private HashSet<Transcript> transcripts = new HashSet<Transcript>();

        public Colocation(Transcript t1, Transcript t2) {
            Transcript[] ordered = session.orderTranscripts(t1, t2);
            first = ordered[0].type;
            second = ordered[1].type;
        }

        public void add(Transcript t) {
            transcripts.add(t);
        }

        public int getFirst() {
            return first;
        }
        
        public int getSecond() {
            return second;
        }

        public int numFirst() {
            int i = 0;
            for(Transcript t : transcripts) {
                if(t.type == first) {
                    i++;
                }
            }
            return i;
        }

        public int numSecond() {
            int i = 0;
            for(Transcript t : transcripts) {
                if(t.type == second) {
                    i++;
                }
            }
            return i;

        }

        public int totalNum() {
            return transcripts.size();
        }
    }

}

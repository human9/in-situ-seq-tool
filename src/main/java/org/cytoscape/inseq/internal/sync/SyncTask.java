package org.cytoscape.inseq.internal.sync;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.Control.Type;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class SyncTask extends AbstractTask {

    final List<TypeNetwork> networkList;
    final CyAppAdapter adapter;
    final InseqSession session;

    public SyncTask(List<TypeNetwork> list, CyAppAdapter a, InseqSession s) {
        networkList = list;
        adapter = a;
        session = s;
    }

    public void run(TaskMonitor monitor) {
        monitor.setTitle("Synchronising network layouts");
        //monitor.setStatusMessage("message");

        /* Overview of algorithm
         * 1. Create new network, add in a node for each gene
         * 2. For each network in list, copy all edges, without duplication
         * 3. Layout union network with desired algorithm
         * 4. Obtain node positions and apply to all networks in list
         * 5. Delete union
         *
         * It only works with TypeNetworks within the same session.
         * If more comparison is needed just use DyNet, it's too complicated
         * to reimplement here.
         */

        // Create network, add gene nodes
        CyNetwork union = adapter.getCyNetworkFactory().createNetwork();
        List<CyNode> unionNodes = new ArrayList<CyNode>();
        for(int i = 0; i < session.getNumGenes(); i++) {
            CyNode node = union.addNode();
            unionNodes.add(node);
        }

        // Copy all edges from networks without duplication
        for(TypeNetwork tn : networkList) {
            List<CyNode> otherNodes = tn.getNodeList(); 
            List<CyEdge> otherEdges = tn.getNetwork().getEdgeList();
            for(CyEdge edge : otherEdges) {
                int source = otherNodes.indexOf(edge.getSource()); 
                int target = otherNodes.indexOf(edge.getTarget()); 

                try {
                    CyNode unionSource = unionNodes.get(source);
                    CyNode unionTarget = unionNodes.get(target);
                
                    if(!union.containsEdge(unionSource, unionTarget)) {
                        union.addEdge(unionSource, unionTarget, false);
                    }
                }
                catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    // This should only happen if a node has been manually added,
                    // and should be ignored
                }
            }
        }
        
        // Apply layout

        // Apply union node locations to other networks

        // Delete union


    }




}

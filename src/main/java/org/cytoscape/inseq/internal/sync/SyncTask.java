package org.cytoscape.inseq.internal.sync;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class SyncTask extends AbstractTask {

    final List<TypeNetwork> networkList;
    final CyAppAdapter adapter;
    final InseqSession session;
    final CyLayoutAlgorithm algorithm;

    public SyncTask(List<TypeNetwork> list, CyLayoutAlgorithm algorithm, CyAppAdapter a, InseqSession s) {
        networkList = list;
        this.algorithm = algorithm;
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

            if(tn.emptyFlag) continue;
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
		CyNetworkView view = adapter.getCyNetworkViewFactory().createNetworkView(union);
		TaskIterator itr = algorithm.createTaskIterator(view, algorithm.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		adapter.getTaskManager().execute(itr);
		adapter.getCyNetworkManager().addNetwork(union);
		adapter.getCyNetworkViewManager().addNetworkView(view);
        

        // Find union node locations
        double[][] locations = new double[unionNodes.size()][2];
        for(int i = 0; i < unionNodes.size(); i++) {
            CyNode unionNode = unionNodes.get(i);
            locations[i][0] = view.getNodeView(unionNode).getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
            locations[i][1] = view.getNodeView(unionNode).getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
        }
        double xloc = view.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION);
        double yloc = view.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION);
        double scale = view.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);
		
        // we don't actually want the union view
        adapter.getCyNetworkViewManager().destroyNetworkView(view);
		adapter.getCyNetworkManager().destroyNetwork(union);

        for(TypeNetwork tn : networkList) {

            if(tn.emptyFlag) continue;

            // Apply union zoom and centering to other views
            tn.getView().setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, xloc);
            tn.getView().setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, yloc);
            tn.getView().setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scale);
            List<CyNode> nodes = tn.getNodeList();
            // Apply union node locations to other nodes
            for(int i = 0; i < nodes.size(); i++) {
                tn.getView().getNodeView(nodes.get(i)).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, locations[i][0]);
                tn.getView().getNodeView(nodes.get(i)).setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, locations[i][1]);
                //tn.view.updateView();
            }
        }

    }




}

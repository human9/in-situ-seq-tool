package org.cytoscape.inseq.internal.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
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

        Iterator<TypeNetwork> it = networkList.iterator();
        while(it.hasNext()) {
            TypeNetwork tn = it.next();

			if(tn.isEmpty) {
				it.remove();
			}
		}


        // Create network, add gene nodes
        CyNetwork union = session.getUnion();
        // destroy anything there
        union.removeNodes(union.getNodeList());
        union.removeEdges(union.getEdgeList());
        List<CyNode> unionNodes = new ArrayList<>();
        for(int i = 0; i < session.getNumGenes(); i++) {
            CyNode node = union.addNode();
            unionNodes.add(node);
        }

        Map<CyEdge, List<CyRow>> uniqueEdges = new HashMap<>();

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
                        CyEdge key = union.addEdge(unionSource, unionTarget, false);
                        List<CyRow> edges = new ArrayList<>();
                        CyRow row = tn.getNetwork().getDefaultEdgeTable().getRow(edge.getSUID());
                        row.set("unique", 0);
                        edges.add(row);
                        uniqueEdges.put(key, edges);
                    }
                    else {
                        CyEdge key = union.getConnectingEdgeList(unionSource, unionTarget, CyEdge.Type.ANY).get(0);
                        CyRow row = tn.getNetwork().getDefaultEdgeTable().getRow(edge.getSUID());
                        row.set("unique", 0);
                        uniqueEdges.get(key).add(row);
                    }
                }
                catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    // This should only happen if a node has been manually added,
                    // and should be ignored
                }
            }
        }

        for(List<CyRow> list : uniqueEdges.values()) {
            for(CyRow row : list) {
                row.set("unique", list.size());
            }
        }
        
        // Apply layout
        CyNetworkView view = adapter.getCyNetworkViewFactory().createNetworkView(union);
		TaskIterator itr = algorithm.createTaskIterator(view, algorithm.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		adapter.getTaskManager().execute(itr);
        try {
            Thread.sleep(100); // terrible hackyness.. TODO: Write a synchronous taskmanager implementation
        } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
        }
        view.updateView();
		adapter.getCyEventHelper().flushPayloadEvents();
		//adapter.getCyNetworkViewManager().addNetworkView(view);
        
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
		//adapter.getCyEventHelper().flushPayloadEvents();
        //adapter.getCyNetworkViewManager().destroyNetworkView(view);
		//adapter.getCyNetworkManager().destroyNetwork(union);

        for(TypeNetwork tn : networkList) {
			
			try {
				Thread.sleep(100); // Prevent lockups
			} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
			}

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

            adapter.getCyNetworkManager().addNetwork(tn.getNetwork());
            adapter.getCyEventHelper().flushPayloadEvents();
            if(networkList.indexOf(tn) == networkList.size() - 1) {
                adapter.getCyNetworkViewManager().addNetworkView(tn.getView());
            }
            //tn.getView().updateView();
		    //adapter.getCyEventHelper().flushPayloadEvents();
            //adapter.getCyApplicationManager().setCurrentNetworkView(tn.getView());
        }


    }




}

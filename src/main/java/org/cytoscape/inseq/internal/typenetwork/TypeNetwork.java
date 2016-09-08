package org.cytoscape.inseq.internal.typenetwork;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

/** A wrapper class for CyNetworks to retain information about how they were constructed.
 *  @author John Salamon
 */
public class TypeNetwork {

	private CyNetwork network;
	private CyTable nodeTable;
	double distance;
	double cutoff;
	private Shape selection;
	public CyNetworkView view;

    // An ordered list of CyNodes, so that nodes of type (i) can be looked up
    // without jumping through too many hoops
    private List<CyNode> nodes = new ArrayList<CyNode>();

	public TypeNetwork(CyNetwork n, double d, double c)
	{
		network = n;
		nodeTable = n.getDefaultNodeTable();
		distance = d;
		cutoff = c;
	}

    public void addNode(int index, CyNode node) {
        nodes.add(index, node); 
    }

    public List<CyNode> getNodeList() {
        return nodes;
    }

	public CyNetwork getNetwork() {
		return network;
	}

	public void setNetwork(CyNetwork n) {
		network = n;
		nodeTable = n.getDefaultNodeTable();
	}
	
	
	public CyTable getNodeTable() {
		return nodeTable;
	}

	public double getDistance() {
		return distance;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getCutoff() {
		return cutoff;
	}
	
	public void setCutoff(Double c) {
		cutoff = c;
	}
	
	public void setSelection(Shape shape) {
		this.selection = shape;
	}

	public Shape getSelection() {
		return selection;
	}
	
	/**
	 *  This is what is displayed in the SelectionPanel's combobox.
	 *  It returns the name of the network, which is user modifiable.
	 */
	@Override
	public String toString() {
		if(network.getDefaultNetworkTable() != null)
			return network.getRow(network).get(CyNetwork.NAME, String.class);
		else
			return "Empty selection";
	}
}

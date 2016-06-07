package org.cytoscape.inseq.internal.typenetwork;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

/** A wrapper class for CyNetworks to retain information about how they were constructed.
 *  @author John Salamon
 */
public class TypeNetwork {

	CyNetwork network;
	CyTable nodeTable;
	double distance;
	double cutoff;

	public TypeNetwork(CyNetwork n, double d, double c)
	{
		network = n;
		nodeTable = n.getDefaultNodeTable();
		distance = d;
		cutoff = c;
	}

	public CyNetwork getNetwork() {
		return network;
	}
	
	public CyTable getNodeTable() {
		return nodeTable;
	}

	public double getDistance() {
		return distance;
	}

	public double getCutoff() {
		return cutoff;
	}
}

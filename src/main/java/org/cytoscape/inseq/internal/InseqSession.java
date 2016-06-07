package org.cytoscape.inseq.internal;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.vizmap.VisualStyle;

import edu.wlu.cs.levy.CG.KDTree;

/** Encapsulates the components generated during a session.
 *  It should be possible to serialize this class for restoring sessions.
 *  @author John Salamon
 */
public class InseqSession {
	
	public KDTree<Transcript> tree;
	public CyNetwork network;
	public List<String> edgeSelection;
	public Rectangle rectangleSelection;
	public VisualStyle style;

	private List<TypeNetwork> networks;

	

	/** Components of a session are initialised as required.
	 *  A session is created when the user imports data.
	 */
	public InseqSession(KDTree<Transcript> t) {
		this.tree = t;
		networks = new ArrayList<TypeNetwork>();
	}

	public void addNetwork(TypeNetwork net) {
		networks.add(net);
	}

	public TypeNetwork getNetwork(int index) {
		return networks.get(index);
	}

	public List<TypeNetwork> getNetworkList() {
		return networks;
	}
}

package org.cytoscape.inseq.internal;

import java.awt.Rectangle;
import java.util.List;

import org.cytoscape.inseq.internal.types.Transcript;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

import edu.wlu.cs.levy.CG.KDTree;

/** Encapsulates the components generated during a session.
 *  It should be possible to serialize this class for restoring sessions.
 *  @author John Salamon
 */
public class InseqSession {
	
	public KDTree<Transcript> tree;
	public CyNetwork network;
	public CyTable nodeTable;
	public List<String> edgeSelection;
	public double distance;
	public Rectangle rectangleSelection;
	

	/** Components of a session are initialised as required.
	 *  A session is created when the user imports data.
	 */
	public InseqSession(KDTree<Transcript> t) {
		this.tree = t;
	}
}

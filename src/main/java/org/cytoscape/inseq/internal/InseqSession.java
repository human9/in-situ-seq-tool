package org.cytoscape.inseq.internal;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.inseq.internal.types.DualPoint;
import org.cytoscape.inseq.internal.types.Transcript;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

import edu.wlu.cs.levy.CG.KDTree;

/** Encapsulates the components generated during a session.
 *  It should be possible to serialize this class for restoring sessions.
 *  @author John Salamon
 */
public class InseqSession {

	public CyTable inseqTable;
	public CyNetwork inseqNetwork;
	public CyNetworkView inseqView;
	
	public Dimension gridSize;
	public Set<CyNode> selectedNodes;
	public List<String> geneNames;
	public Map<Point2D.Double, String> transcripts;
	public TypeNetwork tn;
	public Map<String, ArrayList<Point2D.Double>> mps;
	public Map<String, ArrayList<Point2D.Double>> pointsToDraw;
	public Map<CyEdge, ArrayList<DualPoint>> edgePoints;
	public Rectangle rect;
	public Map<Point2D.Double, String> selTranscripts;
	public KDTree<Transcript> kd;

	/** Components of a session are initialised as required.
	 *  A session is created when the user imports data.
	 */
	public InseqSession(KDTree<Transcript> tree) {

	}
}

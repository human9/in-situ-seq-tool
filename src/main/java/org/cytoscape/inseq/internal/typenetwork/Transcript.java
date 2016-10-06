package org.cytoscape.inseq.internal.typenetwork;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Defines a single in situ transcript detection.
 *  In addition, it contains a list of nearby Transcripts.
 *  @author John Salamon
 */
public class Transcript {

	public final Point2D.Double pos;
    public final Integer index;
    public final Integer type;
	
    // The shape of the selection area, null if entire dataset
	private Map<TypeNetwork, Shape> selection = new HashMap<>();

	private Map<TypeNetwork, List<Transcript>> neighbours = new HashMap<>();
	
	public Transcript(Point2D.Double pos, int type, int index) {
		this.pos = pos;
		this.type = type;
        this.index = index;
	}

	public List<Transcript> getNeighboursForNetwork(TypeNetwork n) {
		return neighbours.get(n);
	}
	
	public void setNeighboursForNetwork(TypeNetwork n, List<Transcript> l) {
		// The neighbour search returns the node that is used for the search also,
		// so we should remove this from the list.
		l.remove(l.size()-1);
		neighbours.put(n, l);
	}

	public void setSelection(TypeNetwork network, Shape shape) {
		this.selection.put(network, shape);
	}

	public Shape getSelection(TypeNetwork network) {
		return selection.get(network);
	}

	@Override
	public String toString() {
		return "X: " + pos.x + " Y: " + pos.y;
	}
}

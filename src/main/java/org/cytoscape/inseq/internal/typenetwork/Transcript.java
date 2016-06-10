package org.cytoscape.inseq.internal.typenetwork;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Defines a single in situ transcript detection.
 *  In addition, it contains a list of nearby Transcripts.
 *  @author John Salamon
 */
public class Transcript {

	public Point2D.Double pos;
	public String name;

	private Map<TypeNetwork, List<Transcript>> neighbours;
	
	public Transcript(Point2D.Double pos, String name) {
		this.pos = pos;
		this.name = name;
		neighbours = new HashMap<TypeNetwork, List<Transcript>>();
	}

	public List<Transcript> getNeighboursForNetwork(TypeNetwork n) {
		return neighbours.get(n);
	}
	
	public void setNeighboursForNetwork(TypeNetwork n, List<Transcript> l) {
		neighbours.put(n, l);
	}

	

	

	@Override
	public String toString() {
		return "Transcript: " + name + " X: " + pos.x + " Y: " + pos.y;
	}
}

package org.cytoscape.inseq.internal.types;

import java.awt.geom.Point2D;
import java.util.List;

/** Defines a single in situ transcript detection.
 *  In addition, it contains a list of nearby Transcripts.
 *  @author John Salamon
 */
public class Transcript {

	public Point2D.Double pos;
	public String name;
	public List<Transcript> neighbours;
	public Double distance = 0d;
	
	public Transcript(Point2D.Double pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	@Override
	public String toString() {
		return "Transcript: " + name + " X: " + pos.x + " Y: " + pos.y;
	}
}

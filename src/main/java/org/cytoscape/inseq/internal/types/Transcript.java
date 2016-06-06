package org.cytoscape.inseq.internal.types;

import java.awt.geom.Point2D;

/** Defines a single in situ transcript detection.
 *  @author John Salamon
 */
public class Transcript {

	public Point2D.Double pos;
	public String name;
	
	public Transcript(Point2D.Double pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	@Override
	public String toString() {
		return "Transcript: " + name + " X: " + pos.x + " Y: " + pos.y;
	}
}

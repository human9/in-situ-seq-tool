package org.cytoscape.myapp.internal;

import java.awt.Point;

/**
 * An extension of the Point class for which coordinates can be iterated over
 * more easily.
 */
class IterPoint extends Point {
	static final long serialVersionUID = 4343434L;

	IterPoint() {
		super();
	}

	IterPoint(Point point) {
		super(point);
	}

	IterPoint(int x, int y) {
		super(x, y);
	}

	IterPoint(double x, double y) {
		super((int) x, (int) y);
	}

	public enum Coord {
		X, Y
	}

	int getPoint(Coord coord) {
		switch (coord) {
		case X:
			return this.x;
		case Y:
			return this.y;
		default:
			return 0;
		}
	}

	void setPoint(Coord coord, int value) {
		switch (coord) {
		case X:
			this.x = value;
			break;
		case Y:
			this.y = value;
			break;
		default:
			break;
		}
	}
}

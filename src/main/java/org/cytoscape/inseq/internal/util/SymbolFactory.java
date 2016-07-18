package org.cytoscape.inseq.internal.util;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
/**
 * Creates the different symbols that can be used...
 */
public class SymbolFactory {

	public enum Symbol {
		DIAMOND, SQUARE, CIRCLE, CROSS, DIAMOND_CROSS,
		ASTERISK, STAR, DOT, UP_TRIANGLE, DOWN_TRIANGLE,
		LEFT_TRIANGLE, RIGHT_TRIANGLE
	}

	public static Shape makeSymbol(Symbol symbol, int x, int y, int w, int h) {
		switch (symbol) {
			case DIAMOND:
				return createDiamond(x, y, w, h);
			case SQUARE:
				return new Rectangle(x, y, w, h);
			default:
				return null;
		}
	}

	private static Shape createDiamond(int x, int y, int w, int h) {
		int xpts[] = {x, x+w/2, x+w, x+w/2};
		int ypts[] = {y+h/2, y+h, y+h/2, y};
		return shapeMaker(xpts, ypts);
	}

	private static Shape shapeMaker(int[] xpts, int[] ypts) {
		GeneralPath shape = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xpts.length);
		shape.moveTo(xpts[0], ypts[0]);
		for (int i = 1; i < xpts.length; i++) {
			shape.lineTo(xpts[i], ypts[i]);
		};
		shape.closePath();
		return shape;

	}


}

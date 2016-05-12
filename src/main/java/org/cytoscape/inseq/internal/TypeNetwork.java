package org.cytoscape.inseq.internal;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TypeNetwork {

	InseqActivator ia;

	Map<Integer, Double> distances = new HashMap<Integer, Double>();
	public TypeNetwork(final InseqActivator iac) {
		this.ia = iac;
		generateTypeNetwork();
	}
	

	private void generateTypeNetwork()
	{
		Point2D.Double[] points = new Point2D.Double[ia.transcripts.size()];
		int i = 0;
		for(Point2D.Double point : ia.transcripts.keySet())
		{
			points[i] = new Point2D.Double(point.x, point.y);
			i++;
		}
		for(int a = 0; a < points.length; a++)
		{
			Point2D.Double p1 = points[a];
			if(a%1000 == 0)System.out.println(a);
			for(int b = a+1; b < points.length; b++)
			{
				Point2D.Double p2 = points[b];
				Double distance = ((p1.x - p2.x) * (p1.x - p2.x)) + ((p1.y - p2.y) * (p1.y - p2.y));
				if(distance < 16) System.out.println(ia.transcripts.get(p1) + " - " + ia.transcripts.get(p2) + " = " + Math.sqrt(distance));
				//int alpha = p1.x - p2.x;	
				//genPairKey(a, i);
				
			}
			//ints.remove(a);
			/*
			if(a%1000 == 0)System.out.println(a);
			for(Integer b : ints)
			{
				//String key = genPairKey(a,b);
				//if(distances.get(key) == null)
				//{
					//Double dist = Math.pow(points.get(a).x - points.get(b).x,2) + Math.pow(points.get(a).y-points.get(b).y,2);
					//distances.put(a+b, dist);
					//distances.put(a, 2d + b);
				//}
			}
			//ints.remove(a);
*/
		}
	}
	
}

package org.cytoscape.inseq.internal.typenetwork;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;

public class FindNeighboursTask extends AbstractTask {

	KDTree<Transcript> tree;
	Double distance;
	
	public FindNeighboursTask(KDTree<Transcript> t, Double d) {

		tree = t;
		distance = d;
	}


	/** For every transcript, finds all points within the given Euclidean distance.
	 *  The neighbouring transcripts are added to a list within Transcript.
	 */
	public void run(final TaskMonitor taskMonitor)
	{
		
		taskMonitor.setTitle("Finding co-occurring neighbours");
		taskMonitor.setStatusMessage("Searching within a Euclidean distance of " + Math.sqrt(distance));

		int z = 0;

		try {
			System.out.println(tree.size());
			for(Transcript t : tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE}))
			{
				if(cancelled) break;
				
				if (z % 1000 == 0) {
					taskMonitor.setProgress((double)z/tree.size());
				}

				// don't compare again if we've already searched at this distance
				if(Double.compare(t.distance, distance) == 0) continue;

				t.neighbours = tree.nearestEuclidean(new double[]{t.pos.x,t.pos.y}, distance);
				t.distance = distance;

				z++;
			}
		}
		catch (KeySizeException e) {
			e.printStackTrace();
		};

	}
}

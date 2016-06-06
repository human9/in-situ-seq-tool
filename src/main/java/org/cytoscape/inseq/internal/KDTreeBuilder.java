package org.cytoscape.inseq.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cytoscape.inseq.internal.types.Transcript;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class KDTreeBuilder extends AbstractTask {

	List<Transcript> raw;
	KDTree<Transcript> output;

	public KDTreeBuilder(List<Transcript> rawImport) {
		raw = rawImport;
	}

	public KDTree<Transcript> getTree() {
		return output;
	}

	/** Constructs a KDTree from the list given during initialization.
	 *  Running this as a Task allows us to display progress.
	 */
	public void run(final TaskMonitor taskMonitor)
	{
		
		taskMonitor.setTitle("Constructing KD-Tree");
		taskMonitor.setStatusMessage(raw.size() + " unique transcripts found.");

		// Getting sorted lists of the transcripts makes finding the median easy.
		List<Transcript> xsorted = sortByAxis(raw, 0);
		List<Transcript> ysorted = sortByAxis(raw, 1);
		
		int xmed = raw.size()/2;
		int ymed = xmed;

		KDTree<Transcript> kdTree = new KDTree<Transcript>(2);
	
		for(int i = 0, x = 0, y = 0; i < raw.size(); i++)
		{
			if (i % 1000 == 0) {
				taskMonitor.setProgress((double)i/raw.size());
			}
			boolean axis = (i % 2) == 0;
			if(axis) {
				do {
					xmed += x * (((x++ % 2) == 0) ? -1 : 1);
				} while(!insertTranscript(xsorted.get(xmed), kdTree));
			}
			else {
				do {
					ymed += y * (((y++ % 2) == 0) ? -1 : 1);
				} while(!insertTranscript(ysorted.get(ymed), kdTree));
			}
		}

		output = kdTree;

	}

	/*
		taskMonitor.setStatusMessage("Finding Euclidean distances");
		int z = 0;

		for(Transcript t : raw)
		{
			if(cancelled) break;
			try { 
				List<Transcript> list = kdTree.nearestEuclidean(new double[]{t.pos.x,t.pos.y}, distanceCutoff);
				if (z % 1000 == 0) {
					taskMonitor.setProgress((double)z/ia.selTranscripts.size());
				}
				for(Transcript t : list) {
					distances.put(new DualPoint(point, t.pos), 1d);

				}
				//System.out.println(list.size());
			}
			catch (KeySizeException e) {};
			z++;
		}

		ia.kd = kdTree;
	*/
		
	
	/** Sorts a Transcript List in ascending order of the coordinates of a given axis.
	 */
	List<Transcript> sortByAxis(List<Transcript> list, int axis) {
		Collections.sort(list, new Comparator<Transcript>() {
			@Override
			public int compare(final Transcript t1, final Transcript t2) {
				return Double.compare(axis == 0 ? t1.pos.x : t1.pos.y, axis == 0 ? t2.pos.x : t2.pos.y);
			}
		});
		return list;
	}
				
	/** Convenience function for inserting a new Transcript into a KDTree.
	 *  Returns true on success, or false on error.
	 */
	boolean insertTranscript(Transcript t, KDTree<Transcript> tree)
	{
		try {
			tree.insert(new double[]{t.pos.x, t.pos.y}, t);			
			return true;
		}
		catch (KeyDuplicateException e) {
			return false;
		}
		catch (KeySizeException e) {
			System.out.println("Array is wrong size: Programmer error");
			return false;
		}
	}
}

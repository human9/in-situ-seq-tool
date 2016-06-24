package org.cytoscape.inseq.internal.dataimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class ConstructTreeTask extends AbstractTask {

	List<Transcript> raw;
	KDTree<Transcript> output;
	
	InseqActivator ia;

	public ConstructTreeTask(List<Transcript> rawImport, InseqActivator ia) {

		raw = rawImport;
		this.ia = ia;
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
		sortByAxis(raw, 0);
		ArrayList<Transcript> xsorted = new ArrayList<Transcript>(raw);
		sortByAxis(raw, 1);
		ArrayList<Transcript> ysorted = new ArrayList<Transcript>(raw);

		
		int xmed = (int)Math.ceil(raw.size()/2d) - 1;
		int ymed = xmed;

		KDTree<Transcript> kdTree = new KDTree<Transcript>(2);
	

		// Probably overengineered, but quickly and reliably gets the next median
		// based on x or y and inserts it into the tree
		for(int i = 0, x = 0, y = 0; ; i++)
		{
			if(cancelled) {
				break;
			}
			if (i % 1000 == 0) {
				taskMonitor.setProgress((double)i/raw.size());
			}
			boolean axis = (i % 2) == 0;
			if(axis) {
				do {
					if(x == raw.size()) break;
					xmed += x * (((x++ % 2) == 0) ? -1 : 1);
				} while(!insertTranscript(xsorted.get(xmed), kdTree));
			}
			else {
				do {
					if(y == raw.size()) break;
					ymed += y * (((y++ % 2) == 0) ? -1 : 1);
				} while(!insertTranscript(ysorted.get(ymed), kdTree));
			}
			if (x == raw.size() && y == raw.size()) break;
		}

		output = kdTree;
		if(ia != null)
		{
			ia.initSession(output, raw);
		}
	}

		
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

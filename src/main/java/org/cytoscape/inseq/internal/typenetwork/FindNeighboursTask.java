package org.cytoscape.inseq.internal.typenetwork;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;

public class FindNeighboursTask extends AbstractTask implements ObservableTask {

	KDTree<Transcript> tree;
	Double distance;
	boolean subset;
	TypeNetwork network;
    Shape selection;
	
	public FindNeighboursTask(Shape selection, KDTree<Transcript> tree, TypeNetwork net, double d, boolean s) {

		this.tree = tree;
		this.selection = selection;
		distance = d; 
		network = net;
		subset = s;
	}


	/** For every transcript, finds all points within the given Euclidean distance.
	 *  The neighbouring transcripts are added to a list within Transcript.
	 */
	public void run(final TaskMonitor taskMonitor)
	{
		
		taskMonitor.setTitle("Finding co-occurring neighbours");
		taskMonitor.setStatusMessage(String.format("Searching within a Euclidean distance of %.2f", distance));

		int z = 0;

		List<Transcript> searchArea;
		try {
			if(subset && selection != null) {
				Rectangle rect = selection.getBounds();
				searchArea = tree.range(new double[]{rect.x, rect.y}, new double[]{rect.x+rect.width, rect.y+rect.height});
			}
			else {
				searchArea = tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE});
			}

			network.setSelection(selection);
			
			for(Transcript t : searchArea) 
			{
				if(cancelled) break;
				
				if (z % 1000 == 0) {
					taskMonitor.setProgress((double)z/tree.size());
				}

                if(subset && selection != null) {
                    if(!selection.contains(t.pos.x, t.pos.y)) continue;
                }

				t.setSelection(network, selection);
				
				// don't compare again if we've already searched at this distance
				if(t.getNeighboursForNetwork(network) != null)
				{
					if(Double.compare(network.distance, distance) == 0) {
						continue;
					}
				}

				t.setNeighboursForNetwork(network, tree.nearestEuclidean(new double[]{t.pos.x,t.pos.y}, Math.pow(distance,2)));
				z++;
			}
		}
		catch (KeySizeException e) {
			e.printStackTrace();
			return;
		};

	}
    public <R> R getResults(Class<? extends R> type) {
        return null;
    }

    public TypeNetwork getNetwork() {
        return network;
    }
}

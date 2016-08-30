package org.cytoscape.inseq.internal.dataimport;

import java.util.ArrayList;
import java.util.Arrays;
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

    List<String> names;
    List<Transcript> transcripts;
    KDTree<Transcript> output;
    
    InseqActivator ia;

    public ConstructTreeTask(List<String> names, List<Transcript> transcripts, InseqActivator ia) {
    
        this.names = names;
        this.transcripts = transcripts;
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
        taskMonitor.setStatusMessage(transcripts.size() + " unique transcripts found.");

        // Getting sorted lists of the transcripts makes finding the median easy.
        boolean[] added = new boolean[transcripts.size()]; 
        Transcript[] xsort = transcripts.toArray(new Transcript[transcripts.size()]);
        Transcript[] ysort = Arrays.copyOf(xsort, xsort.length);
        sortByAxis(xsort, 0);
        sortByAxis(ysort, 1);

        int xmed = (int)Math.ceil(transcripts.size()/2d) - 1;
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
                taskMonitor.setProgress((double)i/transcripts.size());
            }
            boolean axis = (i % 2) == 0;
            if(axis) {
outer:
                while(x != transcripts.size()) {
                    xmed += x * (((x++ % 2) == 0) ? -1 : 1);
                    if(!added[xsort[xmed].index]) {
                        insertTranscript(xsort[xmed], kdTree);
                        added[xsort[xmed].index] = true;
                        break outer;
                    }
                }
            }
            else {
outer:
                while(y != transcripts.size()) {
                    ymed += y * (((y++ % 2) == 0) ? -1 : 1);
                    if(!added[ysort[ymed].index]) {
                        insertTranscript(ysort[ymed], kdTree);
                        added[ysort[ymed].index] = true;
                        break outer;
                    }
                }
            }
            if (x == transcripts.size() && y == transcripts.size()) break;
        }

        output = kdTree;
        if(ia != null)
        {
            ia.initSession(names, transcripts, output);
        }
    }

        
    /** Sorts a Transcript List in ascending order of the coordinates of a given axis.
     */
    Transcript[] sortByAxis(Transcript[] list, int axis) {
        Arrays.sort(list, new Comparator<Transcript>() {
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
            System.err.println("Invalid array size");
            return false;
        }
    }
}

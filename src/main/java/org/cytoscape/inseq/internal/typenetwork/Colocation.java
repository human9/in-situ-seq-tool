package org.cytoscape.inseq.internal.typenetwork;

import java.util.Comparator;
import java.util.HashSet;

public class Colocation {

    // First transcript type
    private int first;
    // Second transcript type
    private int second;

    public int rank;

    public int interaction;

    // Total colocation count
    public int actualCount = 0;

    // Mean of whatever distribution is being used
    public double distributionMean = 0;
    
    public double pvalue;

    // p value for P(X = x)
    public double probability;

    // p value for P(X <= x)
    public double probabilityCumulative;

    // A set containing all transcripts used in this colocation
    private HashSet<Transcript> transcripts = new HashSet<Transcript>();

    public Colocation(Transcript[] ordered) {
        first = ordered[0].type;
        second = ordered[1].type;
    }

    public static Comparator<Colocation> rankComparator = new Comparator<Colocation>() {
        public int compare(Colocation c1, Colocation c2) {
            if(c1.pvalue == c2.pvalue) return 0;
            else {
                return c1.pvalue < c2.pvalue ? -1 : 1;
            }
        }
    };

    public void add(Transcript t) {
        transcripts.add(t);
    }

    public int getFirst() {
        return first;
    }
    
    public int getSecond() {
        return second;
    }

    public int numFirst() {
        int i = 0;
        for(Transcript t : transcripts) {
            if(t.type == first) {
                i++;
            }
        }
        return i;
    }

    public int numSecond() {
        int i = 0;
        for(Transcript t : transcripts) {
            if(t.type == second) {
                i++;
            }
        }
        return i;

    }

    public int totalNum() {
        return transcripts.size();
    }
}

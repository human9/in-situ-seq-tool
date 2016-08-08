package org.cytoscape.inseq.internal.util;

import java.awt.geom.Point2D;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 *  Utility methods for parsing input.
 *  @author John Salamon
 */
public class ParseUtil {


	/** 
	 *  Parses XY coordinates and values from a csv file.
	 *  Returns a map using the coordinates as keys. 
	 */
	public static List<Transcript> parseXYFile(FileReader csv)
	{
		CSVParser inseqParser;
		try {
			inseqParser = CSVFormat.EXCEL.withHeader().parse(csv);
		} catch (IOException e) {
			return null;
		}

		List<Transcript> transcripts = new ArrayList<Transcript>(); 
		for(CSVRecord record : inseqParser)
		{
			String name = record.get("name");

			// Discard unknown reads
			if(name.equals("NNNN"))
				continue;
			
			Double x = Double.parseDouble(record.get("global_X_pos"));
			Double y = Double.parseDouble(record.get("global_Y_pos"));
			Point2D.Double coordinate = new Point2D.Double(x,y);
			transcripts.add(new Transcript(coordinate, name));

		}
		return transcripts;
	}

	/**
	 *  Returns a map of all genes in the tree and how often they occur.
	 */
	public static Map<String, Integer> getGenes(KDTree<Transcript> tree) {

		Map<String, Integer> genes = new HashMap<String, Integer>();

		// Iterate through all our transcripts
        for (Transcript t : tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE}))
        {
            if(!genes.containsKey(t.name)) {
                genes.put(t.name, 0);
            }
            else {
                genes.put(t.name, genes.get(t.name) + 1);
            }
        }

		return genes;
	}

    /**
     *  Generate a name with consistent ordering.
     *  Useful for map key generation
     */
    public static String generateName(Transcript t1, Transcript t2) {
        Transcript[] ordered = orderTranscripts(t1, t2);
        return ordered[0].name + "-" + ordered[1].name;
    }


    /**
     * Get consistant order for two transcripts.
     */
    public static Transcript[] orderTranscripts(Transcript t1, Transcript t2) {

        String n1 = t1.name;
        String n2 = t2.name;

        if(n1.compareTo(n2) < 0) {
            return new Transcript[] {t1, t2};
        } else {
            return new Transcript[] {t2, t1};
        }
    }

}

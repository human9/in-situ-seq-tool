package org.cytoscape.inseq.internal.util;

import java.awt.geom.Point2D;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

/** A collection of utilities not included in the Cytoscape API 
 *  @author various
 */
public class ParseUtil {


	/** Parses XY coordinates and values from a CSV.
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

}

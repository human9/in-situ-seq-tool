package org.cytoscape.inseq.internal.util;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

/**
 *  Utility methods for parsing input.
 *  @author John Salamon
 */
public class ParseUtil {


	/** 
	 *  Parses XY coordinates and values from a csv file.
	 *  Returns a map using the coordinates as keys. 
	 */
	public static boolean parseXYFile(FileReader csv, List<String> names, List<Transcript> transcripts)
	{
		CSVParser inseqParser;
		try {
			inseqParser = CSVFormat.EXCEL.withHeader().parse(csv);
		} catch (IOException e) {
			return false;
		}

        int index = 0;
		for(CSVRecord record : inseqParser)
		{

            String name = record.get("name");
			
            // Discard unknown reads
			if(name.equals("NNNN"))
				continue;
			
            if(!names.contains(name)) names.add(name);
			
			Double x = Double.parseDouble(record.get("global_X_pos"));
			Double y = Double.parseDouble(record.get("global_Y_pos"));
			Point2D.Double coordinate = new Point2D.Double(x,y);
			transcripts.add(new Transcript(coordinate, names.indexOf(name), index++));

		}
		return true;
	}
    
    public static BufferedImage getImageResource(String path) {

        BufferedImage bimg;
        try {
            bimg = ImageIO.read(ParseUtil.class.getResourceAsStream(path));
        } catch (IOException|NullPointerException e) {
            JOptionPane.showMessageDialog(null, 
                    "The image at: " + path + " could not be loaded", "Warning!",
            JOptionPane.WARNING_MESSAGE);
            return null;
        }
        
        return bimg;
    }

    public static BufferedImage getImageFile(String path) {

        File input = new File(path);
        BufferedImage bimg;
        try {
            bimg = ImageIO.read(input);
        } catch (IOException|NullPointerException e) {
            JOptionPane.showMessageDialog(null, 
                    "The image at: " + path + " could not be loaded", "Warning!",
            JOptionPane.WARNING_MESSAGE);
            return null;
        }
        
        System.out.println("File load success.");
        return bimg;
    }

}

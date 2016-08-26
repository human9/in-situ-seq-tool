package org.cytoscape.inseq.internal.dataimport;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.util.ParseUtil;

/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class ImportAction extends AbstractCyAction {

	static final long serialVersionUID = 69;

	private final InseqActivator ia;

	public ImportAction(InseqActivator ia) {
		super("Import Inseq data", ia.getCAA().getCyApplicationManager(), null, null);
		this.ia = ia;
		setPreferredMenu("Apps");
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		JFileChooser fc = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("csv files", "csv");
		fc.addChoosableFileFilter(filter);
		fc.setFileFilter(filter);
		
		int returnVal = fc.showOpenDialog(ia.getCSAA().getCySwingApplication().getJFrame());
		if (!(returnVal == JFileChooser.APPROVE_OPTION)) return;

		File raw = new File(fc.getSelectedFile().getAbsolutePath());

        List<String> names = new ArrayList<String>();
        List<Transcript> transcripts = new ArrayList<Transcript>();
		FileReader in;
		try {
			in = new FileReader(raw);
			if(!ParseUtil.parseXYFile(in, names, transcripts)) return;
			in.close();
            //Collections.sort(names);
			ia.constructTree(names, transcripts, ia);
		} 
		catch (FileNotFoundException x) {
			JOptionPane.showMessageDialog(null, "File not found", "Error", JOptionPane.WARNING_MESSAGE);
			return;
		}
		catch (IOException x) {
			JOptionPane.showMessageDialog(null, "IO error, please retry", "Error", JOptionPane.WARNING_MESSAGE);
			x.printStackTrace();	
			return;
		}
	}
}

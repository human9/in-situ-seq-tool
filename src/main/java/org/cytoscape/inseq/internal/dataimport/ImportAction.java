package org.cytoscape.inseq.internal.dataimport;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.util.ParseUtil;
import org.cytoscape.work.TaskIterator;

/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class ImportAction extends AbstractCyAction {

	static final long serialVersionUID = 69;

	private final InseqActivator ia;

	public ImportAction(InseqActivator ia) {
		super("Import InsituNet data", ia.getCAA().getCyApplicationManager(), null, null);
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

        String filename = fc.getSelectedFile().getName();
        if(ia.doesImportExist(filename)) {
			JOptionPane.showMessageDialog(ia.getCSAA().getCySwingApplication().getJFrame(),"An imported file named " + filename + " already exists!", "Error", JOptionPane.WARNING_MESSAGE);
            return;
    
        }
		File raw = new File(fc.getSelectedFile().getAbsolutePath());

        List<String> names = new ArrayList<String>();
        List<Transcript> transcripts = new ArrayList<Transcript>();
		FileReader in;
		try {
			in = new FileReader(raw);
			if(!ParseUtil.parseXYFile(in, names, transcripts)) return;
			in.close();
            //Collections.sort(names);
		} 
		catch (FileNotFoundException x) {
			JOptionPane.showMessageDialog(ia.getCSAA().getCySwingApplication().getJFrame(), "File not found", "Error", JOptionPane.WARNING_MESSAGE);
			return;
		}
		catch (IOException x) {
			JOptionPane.showMessageDialog(ia.getCSAA().getCySwingApplication().getJFrame(), "IO error, please retry", "Error", JOptionPane.WARNING_MESSAGE);
			x.printStackTrace();	
			return;
		}

        ConstructTreeTask ctt = new ConstructTreeTask(names, transcripts);
        TaskIterator itr = new TaskIterator(ctt);     
        ia.getCSAA().getDialogTaskManager().execute(itr);
        
        ia.initSession(filename, names, transcripts, ctt.getTree());
	}
}

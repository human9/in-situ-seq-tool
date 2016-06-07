package org.cytoscape.inseq.internal.dataimport;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

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
		FileDialog dialog = new FileDialog(ia.getCSAA().getCySwingApplication().getJFrame(), "Choose csv file", FileDialog.LOAD);
		// This works on Windowsy things 
		dialog.setFile("*.csv");
		// This works on Unixy things
		dialog.setFilenameFilter(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".csv");
			}
		});
		dialog.setVisible(true);
		String filename = dialog.getFile();
		if (filename == null) return;

		File raw = new File(dialog.getDirectory() + filename);

		FileReader in;
		try {
			in = new FileReader(raw);
			List<Transcript> rawImport = ParseUtil.parseXYFile(in);
			in.close();
			ia.constructTree(rawImport, ia);
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

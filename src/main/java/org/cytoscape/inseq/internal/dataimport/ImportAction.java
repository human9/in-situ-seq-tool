package org.cytoscape.inseq.internal.dataimport;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.inseq.internal.InseqActivator;

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
		new ImportDialog(ia);

	}
}

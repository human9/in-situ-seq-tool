package org.cytoscape.myapp.internal;
import java.awt.event.ActionEvent;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.CyApplicationManager;

/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class MenuAction extends AbstractCyAction {

	static final long serialVersionUID = 69;

	private final CyApplicationManager applicationManager;

	public MenuAction(final CyApplicationManager applicationManager, final String menuTitle) {
		
		super(menuTitle, applicationManager, null, null);
		this.applicationManager = applicationManager;
		setPreferredMenu("Apps");
	}
	
 	
	public void actionPerformed(ActionEvent e) {
		InvertAction.invertSelected(applicationManager);
	}
}


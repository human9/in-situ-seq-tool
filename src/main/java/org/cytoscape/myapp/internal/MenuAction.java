package org.cytoscape.myapp.internal;
import java.awt.event.ActionEvent;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;

/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class MenuAction extends AbstractCyAction {

	static final long serialVersionUID = 69;

	private final CyApplicationManager applicationManager;
	private final CyNetworkViewFactory networkViewFactory;
	private final CyNetworkViewManager networkViewManager;

	public MenuAction(final CyApplicationManager applicationManager, final CyNetworkViewFactory nvFactory, final CyNetworkViewManager nvManager) {
		
		super("CoolApp", applicationManager, null, null);
		this.applicationManager = applicationManager;
		this.networkViewFactory = nvFactory;
		this.networkViewManager = nvManager;
		setPreferredMenu("Apps");
	}
	
 	
	public void actionPerformed(ActionEvent e) {
		//InvertAction.invertSelected(applicationManager);
	    final CyNetworkView currentNetworkView = applicationManager.getCurrentNetworkView();
		final CyNetwork network = currentNetworkView.getModel();
		CyNetworkView testView = networkViewFactory.createNetworkView(network);

		networkViewManager.addNetworkView(testView);
	}
}


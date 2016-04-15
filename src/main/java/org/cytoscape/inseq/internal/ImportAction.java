package org.cytoscape.inseq.internal;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;

/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class ImportAction extends AbstractCyAction {

	static final long serialVersionUID = 69;

	private final CyApplicationManager applicationManager;
	private final CyNetworkViewFactory networkViewFactory;
	private final CyNetworkViewManager networkViewManager;
	private final CySwingAppAdapter swingAdapter;

	public ImportAction(final CySwingAppAdapter adapter, final CyApplicationManager applicationManager, final CyNetworkViewFactory nvFactory,
			final CyNetworkViewManager nvManager) {

		super("Import Inseq data", applicationManager, null, null);
		this.swingAdapter = adapter;
		this.applicationManager = applicationManager;
		this.networkViewFactory = nvFactory;
		this.networkViewManager = nvManager;
		setPreferredMenu("Apps");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		SelectionWindow dialog = new SelectionWindow(swingAdapter.getCySwingApplication().getJFrame());

		// InvertAction.invertSelected(applicationManager);
		/*final CyNetworkView currentNetworkView = applicationManager.getCurrentNetworkView();
		final CyNetwork network = currentNetworkView.getModel();
		CyNetworkView testView = networkViewFactory.createNetworkView(network);

		networkViewManager.addNetworkView(testView);
		*/
		
	}
}

package org.cytoscape.myapp.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class InvertAction {

	public static void invertSelected(final CyApplicationManager applicationManager) {

		final CyNetworkView currentNetworkView = applicationManager.getCurrentNetworkView();

		if (currentNetworkView == null)
			return;

		final CyNetwork network = currentNetworkView.getModel();

		for (CyNode node : CyTableUtil.getNodesInState(network, "selected", true)) {

			View<CyNode> nodeView = currentNetworkView.getNodeView(node);
			Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
			Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);

			nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x * -1);
			nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y * -1);
		}
		currentNetworkView.updateView();

	}
}

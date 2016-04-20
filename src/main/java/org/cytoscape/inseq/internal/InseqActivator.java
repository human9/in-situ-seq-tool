package org.cytoscape.inseq.internal;

import java.util.Properties;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;

import org.cytoscape.inseq.internal.dataimport.*;

public class InseqActivator extends AbstractCyActivator {

	private CyApplicationManager cyApplicationManager;
	private CyNetworkViewManager nvManager;
	private InseqControlPanel controlPanel;
	private Properties properties;
	private CyNetworkViewFactory nvFactory;
	private CySwingAppAdapter swingApp;

	@Override
	public void start(BundleContext context) throws Exception {

		cyApplicationManager = getService(context, CyApplicationManager.class);
		properties = new Properties();
		nvFactory = getService(context, CyNetworkViewFactory.class);

		swingApp = getService(context, CySwingAppAdapter.class);

		nvManager = getService(context, CyNetworkViewManager.class);
		ImportAction menuAction = new ImportAction(swingApp, cyApplicationManager, nvFactory, nvManager);
		registerAllServices(context, menuAction, properties);

		controlPanel = new InseqControlPanel(swingApp, cyApplicationManager);
		registerAllServices(context, controlPanel, properties);
	}

	@Override
	public void shutDown() {
		super.shutDown();
	}
}

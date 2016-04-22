package org.cytoscape.inseq.internal;

import java.awt.Dimension;
import java.util.Properties;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.inseq.internal.dataimport.ImportAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;

public class InseqActivator extends AbstractCyActivator {

	public CyApplicationManager applicationManager;
	public CyNetworkViewManager networkViewManager;
	public CyNetworkFactory networkFactory; 
	public CyNetworkManager networkManager; 
	public CyTableFactory tableFactory; 
	public CyTableManager tableManager; 
	public CyTable inseqTable;
	public CyNetwork inseqNetwork; 
	public CyNetworkView inseqView; 
	public CyNetworkTableManager networkTableManager; 
	public CyNetworkViewFactory networkViewFactory; 
	public CySwingAppAdapter swingAppAdapter;
	public Dimension gridSize;
	
	private InseqControlPanel controlPanel;
	private Properties properties;

	@Override
	public void start(BundleContext context) throws Exception {

		applicationManager = getService(context, CyApplicationManager.class);
		properties = new Properties();
		networkFactory = getService(context, CyNetworkFactory.class);
		tableFactory = getService(context, CyTableFactory.class);
		tableManager = getService(context, CyTableManager.class);
		networkViewManager = getService(context, CyNetworkViewManager.class);
		networkManager = getService(context, CyNetworkManager.class);
		networkTableManager = getService(context, CyNetworkTableManager.class);
		networkViewFactory = getService(context, CyNetworkViewFactory.class);
		swingAppAdapter = getService(context, CySwingAppAdapter.class);

		ImportAction menuAction = new ImportAction(this);
		registerAllServices(context, menuAction, properties);

		controlPanel = new InseqControlPanel(this);
		registerAllServices(context, controlPanel, properties);
	}

	@Override
	public void shutDown() {
		super.shutDown();
	}
}

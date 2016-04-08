package org.cytoscape.myapp.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	private CyApplicationManager cyApplicationManager;
	private CyNetworkViewManager nvManager;
	private EmptyPanel controlPanel;
	private Properties properties;
	private CyNetworkViewFactory nvFactory;
	private PictureWindow pictureWindow;

	@Override
	public void start(BundleContext context) throws Exception {

		pictureWindow = new PictureWindow();

		cyApplicationManager = getService(context, CyApplicationManager.class);
		properties = new Properties();
		nvFactory = getService(context, CyNetworkViewFactory.class);

		nvManager = getService(context, CyNetworkViewManager.class);
		MenuAction menuAction = new MenuAction(cyApplicationManager, nvFactory, nvManager);
		registerAllServices(context, menuAction, properties);

		controlPanel = new EmptyPanel(cyApplicationManager, pictureWindow);
		registerAllServices(context, controlPanel, properties);
	}

	@Override
	public void shutDown() {
		pictureWindow.remove();
		super.shutDown();
	}
}

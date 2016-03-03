package org.cytoscape.myapp.internal;

import java.awt.Component;
import java.util.Properties;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	private CySwingAppAdapter adapter;

	@Override
	public void start(BundleContext context) throws Exception {
		
		CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
		
		MenuAction action = new MenuAction(cyApplicationManager, "COOL APP");
		
		Properties properties = new Properties();

		adapter = getService(context, CySwingAppAdapter.class);
		CySwingApplication application = adapter.getCySwingApplication();
		CytoPanel control = application.getCytoPanel(CytoPanelName.WEST);

		for (int i = 0; i < control.getCytoPanelComponentCount(); i++) {
			control.getComponentAt(i);

		}

		MyCytoPanel myPanel = new MyCytoPanel(cyApplicationManager);

		registerAllServices(context, myPanel, properties);	
		registerAllServices(context, action, properties);
	}

}

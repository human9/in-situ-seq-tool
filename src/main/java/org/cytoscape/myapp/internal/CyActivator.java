package org.cytoscape.myapp.internal;

import java.awt.Component;
import java.awt.Container;
import java.util.Properties;

import javax.swing.JDesktopPane;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

	private CyApplicationManager cyApplicationManager;
	private CySwingApplication cySwingApplication;
	private CyNetworkViewManager nvManager;
	private EmptyPanel controlPanel;
	private Properties properties;
	private CyNetworkViewFactory nvFactory;
	private JDesktopPane desktopPane;
	private PictureWindow pwi;

	@Override
	public void start(BundleContext context) throws Exception {

		cySwingApplication = getService(context, CySwingApplication.class);
		desktopPane = findDesktop(cySwingApplication.getJFrame().getComponents());
		pwi = new PictureWindow(desktopPane);

		cyApplicationManager = getService(context, CyApplicationManager.class);
		properties = new Properties();
		nvFactory = getService(context, CyNetworkViewFactory.class);

		nvManager = getService(context, CyNetworkViewManager.class);
		MenuAction menuAction = new MenuAction(cyApplicationManager, nvFactory, nvManager);
		registerAllServices(context, menuAction, properties);

		controlPanel = new EmptyPanel(cyApplicationManager, pwi);
		registerAllServices(context, controlPanel, properties);

	}

	private JDesktopPane findDesktop(Component[] components) {
		for (Component component : components) {
			if (component.getClass() == JDesktopPane.class)
				return (JDesktopPane) component;
			else if (component instanceof Container) {
				Container container = (Container) component;
				JDesktopPane pane = findDesktop(container.getComponents());
				if (pane != null)
					return pane;
			}
		}
		return null;
	}

	@Override
	public void shutDown() {
		pwi.remove();
		pwi.dispose();
		super.shutDown();
	}
}

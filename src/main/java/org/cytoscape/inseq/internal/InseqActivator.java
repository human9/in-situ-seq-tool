package org.cytoscape.inseq.internal;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.inseq.internal.dataimport.ImportAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
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
	public VisualMappingManager visualManager;
	public CyAppAdapter appAdapter;
	public VisualStyleFactory visualFactory;
	public VisualMappingFunctionFactory discreteMappingFactory;
	public VisualMappingFunctionFactory continuousMappingFactory;
	public VisualMappingFunctionFactory passthroughMappingFactory;
	public Set<CyNode> selectedNodes;
	public List<String> geneNames;
	public Map<Point2D.Double, String> transcripts;
	public TypeNetwork tn;
	public Map<String, ArrayList<Point2D.Double>> mps;
	public Map<String, ArrayList<Point2D.Double>> pointsToDraw;

	private InseqControlPanel controlPanel;
	private Properties properties;

	@Override
	public void start(BundleContext context) throws Exception {

		// todo: just pass the context instead of doing all this crap
		applicationManager = getService(context, CyApplicationManager.class);
		properties = new Properties();
		networkFactory = getService(context, CyNetworkFactory.class);
		tableFactory = getService(context, CyTableFactory.class);
		tableManager = getService(context, CyTableManager.class);
		appAdapter = getService(context, CyAppAdapter.class);
		networkViewManager = getService(context, CyNetworkViewManager.class);
		networkManager = getService(context, CyNetworkManager.class);
		networkTableManager = getService(context, CyNetworkTableManager.class);
		networkViewFactory = getService(context, CyNetworkViewFactory.class);
		swingAppAdapter = getService(context, CySwingAppAdapter.class);
		visualManager = getService(context, VisualMappingManager.class);
		visualFactory = getService(context, VisualStyleFactory.class);
		discreteMappingFactory = getService(context, VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
		continuousMappingFactory = getService(context, VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
		passthroughMappingFactory = getService(context, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");

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

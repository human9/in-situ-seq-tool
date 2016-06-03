package org.cytoscape.inseq.internal;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.inseq.internal.dataimport.ImportAction;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkView;
import org.osgi.framework.BundleContext;

import edu.wlu.cs.levy.CG.KDTree;

public class InseqActivator extends AbstractCyActivator {

	public CyTable inseqTable;
	public CyNetwork inseqNetwork;
	public CyNetworkView inseqView;
	
	public CyAppAdapter appAdapter;
	public CySwingAppAdapter swingAppAdapter;
	
	public Dimension gridSize;
	public Set<CyNode> selectedNodes;
	public List<String> geneNames;
	public Map<Point2D.Double, String> transcripts;
	public TypeNetwork tn;
	public Map<String, ArrayList<Point2D.Double>> mps;
	public Map<String, ArrayList<Point2D.Double>> pointsToDraw;
	public Map<CyEdge, ArrayList<DualPoint>> edgePoints;
	public Rectangle rect;
	public Map<Point2D.Double, String> selTranscripts;
	public KDTree<Transcript> kd;

	private InseqControlPanel controlPanel;
	private Properties properties;

	@Override
	public void start(BundleContext context) throws Exception {

		properties = new Properties();
		appAdapter = getService(context, CyAppAdapter.class);
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

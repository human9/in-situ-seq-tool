package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Paint;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class ViewStyler extends AbstractTask {

	private CyAppAdapter a;
	private CyNetwork network;
	private VisualStyle style;

	public ViewStyler(CyNetwork n, VisualStyle s, CyAppAdapter a) {
		this.a = a;
		this.network = n;
		this.style = s;
	}

	public static VisualStyle initStyle(Map<String, Color> geneColours, CyAppAdapter a) {

		VisualStyle vs = a.getVisualStyleFactory().createVisualStyle("Inseq Style");
		
		VisualMappingFunctionFactory cvmf = a.getVisualMappingFunctionContinuousFactory();
		VisualMappingFunctionFactory pvmf = a.getVisualMappingFunctionPassthroughFactory();
		VisualMappingFunctionFactory dvmf = a.getVisualMappingFunctionDiscreteFactory();

		vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.LIGHT_GRAY);
		
		VisualMappingFunction<String,Paint> nodeColour = dvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_FILL_COLOR);
		for(String name : geneColours.keySet())
		{
			((DiscreteMapping<String,Paint>)nodeColour).putMapValue(name, geneColours.get(name));
		}
		vs.addVisualMappingFunction(nodeColour);
		
		VisualMappingFunction<String,String> ntool = pvmf.createVisualMappingFunction("num", String.class, BasicVisualLexicon.NODE_TOOLTIP);
		vs.addVisualMappingFunction(ntool);
		VisualMappingFunction<String,String> etool = pvmf.createVisualMappingFunction("num", String.class, BasicVisualLexicon.EDGE_TOOLTIP);
		vs.addVisualMappingFunction(etool);
		VisualMappingFunction<String,String> pMap = pvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(pMap);
		VisualMappingFunction<Integer,Double> sizeMap = cvmf.createVisualMappingFunction("num", Integer.class, BasicVisualLexicon.NODE_SIZE);
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(1,new  BoundaryRangeValues<Double>(20d,25d,30d));
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(100,new  BoundaryRangeValues<Double>(40d,50d,60d));
		((ContinuousMapping<Integer,Double>)sizeMap).addPoint(10000,new  BoundaryRangeValues<Double>(80d,90d,100d));
		vs.addVisualMappingFunction(sizeMap);
		VisualMappingFunction<Double,Double> edgeMap = cvmf.createVisualMappingFunction("normal", Double.class, BasicVisualLexicon.EDGE_WIDTH);
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(0.01d,new  BoundaryRangeValues<Double>(0d,0d,3d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(0.1d,new  BoundaryRangeValues<Double>(6d,8d,10d));
		vs.addVisualMappingFunction(edgeMap);
		
		a.getVisualMappingManager().addVisualStyle(vs);

		return(vs);

	}

	public void run(TaskMonitor monitor) {
		
		CyNetworkView view = a.getCyNetworkViewFactory().createNetworkView(network);
		style.apply(view);

		final CyLayoutAlgorithmManager algm = a.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		a.getTaskManager().execute(itr);

		a.getCyNetworkManager().addNetwork(network);
		a.getCyNetworkViewManager().addNetworkView(view);
		view.updateView();
	}


}


package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Paint;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class ViewStyler extends AbstractTask {

	private CyAppAdapter a;
	private TypeNetwork network;
	private VisualStyle style;

	public ViewStyler(TypeNetwork n, VisualStyle s, CyAppAdapter a) {
		this.a = a;
		this.network = n;
		this.style = s;
	}

	public static VisualStyle initStyle(Map<String, Color> geneColours, CyAppAdapter a) {

		VisualStyle vs = a.getVisualStyleFactory().createVisualStyle("Inseq Style");
		
		VisualMappingFunctionFactory cvmf = a.getVisualMappingFunctionContinuousFactory();
		VisualMappingFunctionFactory pvmf = a.getVisualMappingFunctionPassthroughFactory();
		VisualMappingFunctionFactory dvmf = a.getVisualMappingFunctionDiscreteFactory();

		vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.DARK_GRAY);
		vs.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, new Color(5,20,2));
		vs.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, new Color(204,255,204));
		
		VisualMappingFunction<String,Paint> nodeColour = dvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_BORDER_PAINT);
		for(String name : geneColours.keySet())
		{
			((DiscreteMapping<String,Paint>)nodeColour).putMapValue(name, geneColours.get(name));
		}
		vs.addVisualMappingFunction(nodeColour);
		
		VisualMappingFunction<String,Paint> labelColor = dvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL_COLOR);
		for(String name : geneColours.keySet())
		{
			((DiscreteMapping<String,Paint>)labelColor).putMapValue(name, geneColours.get(name));
		}
		vs.addVisualMappingFunction(labelColor);
		
		VisualMappingFunction<String,String> ntool = pvmf.createVisualMappingFunction("num", String.class, BasicVisualLexicon.NODE_TOOLTIP);
		vs.addVisualMappingFunction(ntool);
		VisualMappingFunction<String,String> etool = pvmf.createVisualMappingFunction("num", String.class, BasicVisualLexicon.EDGE_TOOLTIP);
		vs.addVisualMappingFunction(etool);
		VisualMappingFunction<String,String> pMap = pvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(pMap);
		VisualMappingFunction<Double,Double> sizeMap = cvmf.createVisualMappingFunction("proportion", Double.class, BasicVisualLexicon.NODE_SIZE);
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(0d,new  BoundaryRangeValues<Double>(10d,15d,20d));
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(0.001d,new  BoundaryRangeValues<Double>(20d,25d,30d));
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(0.01d,new  BoundaryRangeValues<Double>(30d,40d,50d));
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(0.1d,new  BoundaryRangeValues<Double>(60d,70d,80d));
		((ContinuousMapping<Double,Double>)sizeMap).addPoint(1d,new  BoundaryRangeValues<Double>(80d,100d,110d));
		vs.addVisualMappingFunction(sizeMap);

		VisualMappingFunction<Double,Double> edges = cvmf.createVisualMappingFunction("normal", Double.class, BasicVisualLexicon.EDGE_WIDTH);
		vs.addVisualMappingFunction(edges);
		//VisualMappingFunction<Double,Double> borders = pvmf.createVisualMappingFunction("selfnorm", Double.class, BasicVisualLexicon.NODE_BORDER_WIDTH);
		//vs.addVisualMappingFunction(borders);
		
		((ContinuousMapping<Double,Double>)edges).addPoint(1.96d,new  BoundaryRangeValues<Double>(0d,2d,4d));
		((ContinuousMapping<Double,Double>)edges).addPoint(3d,new  BoundaryRangeValues<Double>(4d,5d,6d));
		((ContinuousMapping<Double,Double>)edges).addPoint(6d,new  BoundaryRangeValues<Double>(6d,8d,10d));
		((ContinuousMapping<Double,Double>)edges).addPoint(10d,new  BoundaryRangeValues<Double>(10d,15d,20d));
		vs.addVisualMappingFunction(edges);
/*
		VisualMappingFunction<Double,Double> edgeMap = cvmf.createVisualMappingFunction("normal", Double.class, BasicVisualLexicon.EDGE_WIDTH);
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(1.96d,new  BoundaryRangeValues<Double>(4d,8d,12d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(3d,new  BoundaryRangeValues<Double>(12d,14d,16d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(6d,new  BoundaryRangeValues<Double>(16d,20d,25d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(10d,new  BoundaryRangeValues<Double>(25d,30d,40d));
		vs.addVisualMappingFunction(edgeMap);
*/		
		VisualMappingFunction<Double,Double> selfMap = cvmf.createVisualMappingFunction("selfnorm", Double.class, BasicVisualLexicon.NODE_BORDER_WIDTH);

		((ContinuousMapping<Double,Double>)selfMap).addPoint(1.96d,new  BoundaryRangeValues<Double>(0d,2d,4d));
		((ContinuousMapping<Double,Double>)selfMap).addPoint(3d,new  BoundaryRangeValues<Double>(4d,5d,6d));
		((ContinuousMapping<Double,Double>)selfMap).addPoint(6d,new  BoundaryRangeValues<Double>(6d,8d,10d));
		((ContinuousMapping<Double,Double>)selfMap).addPoint(10d,new  BoundaryRangeValues<Double>(10d,12d,14d));
		vs.addVisualMappingFunction(selfMap);
		
		a.getVisualMappingManager().addVisualStyle(vs);

		return(vs);

	}

	public void run(TaskMonitor monitor) {
		
		CyNetworkView view = a.getCyNetworkViewFactory().createNetworkView(network.getNetwork());
		style.apply(view);

		final CyLayoutAlgorithmManager algm = a.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		a.getTaskManager().execute(itr);

		a.getCyEventHelper().flushPayloadEvents();
		a.getCyNetworkManager().addNetwork(network.getNetwork());
		a.getCyNetworkViewManager().addNetworkView(view);
		a.getVisualMappingManager().setVisualStyle(style, view);
		a.getVisualMappingManager().setCurrentVisualStyle(style);
		view.updateView();
	}


}


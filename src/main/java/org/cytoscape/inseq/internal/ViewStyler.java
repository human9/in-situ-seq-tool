package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Paint;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
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
	private TypeNetwork network;
	private VisualStyle style;

	public ViewStyler(TypeNetwork n, int testType, VisualStyle s, CyAppAdapter a) {
		this.a = a;
		this.network = n;
		this.style = s;
	}

	public static VisualStyle initStyle(InseqSession s, CyAppAdapter a) {

		VisualStyle vs = a.getVisualStyleFactory().createVisualStyle("Inseq Style");
		
		VisualMappingFunctionFactory cvmf = a.getVisualMappingFunctionContinuousFactory();
		VisualMappingFunctionFactory pvmf = a.getVisualMappingFunctionPassthroughFactory();
		VisualMappingFunctionFactory dvmf = a.getVisualMappingFunctionDiscreteFactory();

		vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.DARK_GRAY);
		vs.setDefaultValue(BasicVisualLexicon.NODE_SELECTED_PAINT, Color.RED);
		vs.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, Color.YELLOW);
		vs.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.BLACK);
		vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.WHITE);
		vs.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, new Color(204,255,204));
		vs.setDefaultValue(BasicVisualLexicon.EDGE_TRANSPARENCY, 150);
		vs.setDefaultValue(BasicVisualLexicon.NODE_TRANSPARENCY, 255);
		
		VisualMappingFunction<String,Paint> nodeColour = dvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_BORDER_PAINT);
		for(InseqSession.Gene g : s.getGenes())
		{
			((DiscreteMapping<String,Paint>)nodeColour).putMapValue(g.name, g.color);
		}
		vs.addVisualMappingFunction(nodeColour);
	/*	
		VisualMappingFunction<String,Paint> labelColor = dvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL_COLOR);
		for(InseqSession.Gene g : s.getGenes())
		{
			((DiscreteMapping<String,Paint>)labelColor).putMapValue(g.name, g.color);
		}
		vs.addVisualMappingFunction(labelColor);
	*/	
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

		//VisualMappingFunction<Double,Double> borders = pvmf.createVisualMappingFunction("selfnorm", Double.class, BasicVisualLexicon.NODE_BORDER_WIDTH);
		//vs.addVisualMappingFunction(borders);
		
        /*
		((ContinuousMapping<Double,Double>)edges).addPoint(1.96d,new  BoundaryRangeValues<Double>(0d,2d,4d));
		((ContinuousMapping<Double,Double>)edges).addPoint(3d,new  BoundaryRangeValues<Double>(4d,5d,6d));
		((ContinuousMapping<Double,Double>)edges).addPoint(6d,new  BoundaryRangeValues<Double>(6d,8d,10d));
		((ContinuousMapping<Double,Double>)edges).addPoint(10d,new  BoundaryRangeValues<Double>(10d,15d,20d));
		vs.addVisualMappingFunction(edges);
        */
		VisualMappingFunction<Integer,Double> edges = cvmf.createVisualMappingFunction("rank", Integer.class, BasicVisualLexicon.EDGE_WIDTH);

		((ContinuousMapping<Integer,Double>)edges).addPoint(1,new  BoundaryRangeValues<Double>(20d,20d,20d));
		((ContinuousMapping<Integer,Double>)edges).addPoint(10,new  BoundaryRangeValues<Double>(10d,10d,10d));
		((ContinuousMapping<Integer,Double>)edges).addPoint(50,new  BoundaryRangeValues<Double>(3d,3d,3d));
		vs.addVisualMappingFunction(edges);
		
        VisualMappingFunction<Integer,Paint> edgeColour = cvmf.createVisualMappingFunction("rank", Integer.class, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);

		((ContinuousMapping<Integer,Paint>)edgeColour).addPoint(1,new  BoundaryRangeValues<Paint>(Color.RED, Color.RED, Color.RED));
		((ContinuousMapping<Integer,Paint>)edgeColour).addPoint(50,new  BoundaryRangeValues<Paint>(Color.GRAY, Color.GRAY, Color.GRAY));
		vs.addVisualMappingFunction(edgeColour);


/*
		VisualMappingFunction<Double,Double> edgeMap = cvmf.createVisualMappingFunction("normal", Double.class, BasicVisualLexicon.EDGE_WIDTH);
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(1.96d,new  BoundaryRangeValues<Double>(4d,8d,12d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(3d,new  BoundaryRangeValues<Double>(12d,14d,16d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(6d,new  BoundaryRangeValues<Double>(16d,20d,25d));
		((ContinuousMapping<Double,Double>)edgeMap).addPoint(10d,new  BoundaryRangeValues<Double>(25d,30d,40d));
		vs.addVisualMappingFunction(edgeMap);
*/		
		VisualMappingFunction<Double,Double> selfMap = cvmf.createVisualMappingFunction("selfnorm", Double.class, BasicVisualLexicon.NODE_BORDER_WIDTH);

		((ContinuousMapping<Double,Double>)selfMap).addPoint(0d,new  BoundaryRangeValues<Double>(5d,5d,5d));
		((ContinuousMapping<Double,Double>)selfMap).addPoint(1d,new  BoundaryRangeValues<Double>(1d,1d,1d));
		vs.addVisualMappingFunction(selfMap);
		
		a.getVisualMappingManager().addVisualStyle(vs);

		return(vs);

	}

    public static void updateColours(VisualStyle style, InseqSession s, CyAppAdapter a) {
		VisualMappingFunctionFactory dvmf = a.getVisualMappingFunctionDiscreteFactory();
        VisualMappingFunction<String,Paint> nodeColour = dvmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_BORDER_PAINT);
		for(InseqSession.Gene g : s.getGenes()) {
			((DiscreteMapping<String,Paint>)nodeColour).putMapValue(g.name, g.color);
		}
        style.addVisualMappingFunction(nodeColour);

        try {
            style.apply(s.getSelectedNetwork().getView());
        } catch (NullPointerException e) {
            // No network created yet
        }
    }

	public void run(TaskMonitor monitor) {

        if(network.emptyFlag) return;
		
		CyNetworkView view = a.getCyNetworkViewFactory().createNetworkView(network.getNetwork());
		network.setView(view);
		style.apply(view);

		final CyLayoutAlgorithmManager algm = a.getCyLayoutAlgorithmManager();
		CyLayoutAlgorithm algor = algm.getDefaultLayout();
		TaskIterator itr = algor.createTaskIterator(view, algor.createLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
		a.getTaskManager().execute(itr);

		//a.getCyEventHelper().flushPayloadEvents();
		a.getCyNetworkManager().addNetwork(network.getNetwork());
		//a.getVisualMappingManager().setVisualStyle(style, view);
		//a.getVisualMappingManager().setCurrentVisualStyle(style);
	}


}


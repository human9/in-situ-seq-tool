package org.cytoscape.inseq.internal.typenetwork;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.function.Function2D;
import org.jfree.data.function.NormalDistributionFunction2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeySizeException;


public class ShuffleTask extends AbstractTask {

	private TypeNetwork net;
	private KDTree<Transcript> tree;
	private CyAppAdapter a;
	private InseqSession session;

	public ShuffleTask(TypeNetwork n, InseqSession s, CyAppAdapter a) {
		this.net = n;
		this.tree = s.tree;
		this.session = s;
		this.a = a;
	}

	/** Shuffles gene names in order to generate a random distribution.
	 *  
	 */
	public void run(TaskMonitor taskMonitor) {

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Finding distribution by shuffling names");

		// Iterate through all our transcripts
		List<Transcript> range;
		List<Transcript> filtered = new ArrayList<Transcript>();
		Map<String, Integer> nodes = new HashMap<String, Integer>();
		
		// Store a unique list of transcripts
		List<Transcript[]> edges = new ArrayList<Transcript[]>();

		try {
			range = tree.range(new double[]{0d,0d}, new double[]{Double.MAX_VALUE, Double.MAX_VALUE});
		}
		catch (KeySizeException e) {
			e.printStackTrace();
			return;
		}
		
		int N = 0;
		for (Transcript t : range)
		{
			N++;
			
			if(!nodes.keySet().contains(t.name)) {
				nodes.put(t.name, 0);
			}
			else {
				nodes.put(t.name, nodes.get(t.name) + 1);
			}
			// If no neighbours were found for this transcript, go to next.
			if(t.getNeighboursForNetwork(net) == null) continue;
			if(t.getNeighboursForNetwork(net).size() < 1) continue;
			// If the neighbours aren't within the selection, go to next.
			if(t.getSelection(net) != net.getSelection()) continue;
			
			filtered.add(t);
		}

		for (Transcript t: filtered) {
			for(Transcript n : t.getNeighboursForNetwork(net)) {
				edges.add(new Transcript[]{t,n});
			}
		}

		Transcript[][] uniqueCombos = edges.toArray(new Transcript[edges.size()][]);

		int reps = 100;
		
		int[][] edgecount = new int[nodes.size()*nodes.size()][reps+1];

		for(int x = 0; x < reps; x++)
		{
			
			if(cancelled) return;

			taskMonitor.setProgress((double)x/reps);

			session.shuffleNames();	

			for(int i = 0; i < uniqueCombos.length; i++) {
				int index = (uniqueCombos[i][0].type + 1) * (uniqueCombos[i][1].type + 1);
				edgecount[index-1][x]++;
			}
		}
		
		session.restoreNames();
		for(int i = 0; i < uniqueCombos.length; i++) {
			int index = (uniqueCombos[i][0].type + 1) * (uniqueCombos[i][1].type + 1);
			edgecount[index-1][reps]++;
		}


		CyNetwork network = net.getNetwork();

		// Name the network
		String name = network.getRow(network).get(CyNetwork.NAME, String.class);
		if(name != null)
		{
			network.getRow(network).set(CyNetwork.NAME, name);
		}
		else {
			network.getRow(network).set(CyNetwork.NAME, String.format("%.2f-unit TypeNetwork", net.getDistance()));
		}

		// Get the node table and add columns
		CyTable nodeTable = net.getNodeTable();
		nodeTable.createColumn("num", Integer.class, false);
		nodeTable.createColumn("proportion", Double.class, false);
		nodeTable.createColumn("selfnorm", Double.class, false);
		
		// Get the edge table and add columns
		CyTable edgeTable = network.getDefaultEdgeTable();	
		edgeTable.createColumn("num", Integer.class, false);
		edgeTable.createColumn("normal", Double.class, false);

		// Add nodes into actual network
		for (String n : nodes.keySet())
		{
			CyNode node = network.addNode();
			CyRow row = nodeTable.getRow(node.getSUID());
			row.set(CyNetwork.NAME, n);
			row.set("num", nodes.get(n));
			row.set("proportion", (double)nodes.get(n)/N);
		}


		List<Integer> ints = new ArrayList<Integer>();
		Function2D normal = new NormalDistributionFunction2D(0.0, 1.0);
		XYDataset line = DatasetUtilities.sampleFunction2D(normal, -10, 10, 1000, "f(x)");
		JFreeChart chart = ChartFactory.createXYLineChart("Normal distribution", "Z-Score", "Frequency", line,
				PlotOrientation.VERTICAL, false, false, false);
		XYPlot plot = (XYPlot) chart.getPlot();


		HistogramDataset dataset = new HistogramDataset();
		JFreeChart chart2 = ChartFactory.createHistogram("raw", "num co-occurences", "frequency", dataset, PlotOrientation.VERTICAL, false, false, false);
		XYPlot plot2 = (XYPlot) chart2.getPlot();

		//String daName = "Nrn1-Cck";
		//String daName = "Npy-Reln";
		String daName = "Ndnf-Lhx6";

		int sigcount= 0;
		for(int i = 0; i < uniqueCombos.length; i++) {
			int index = (uniqueCombos[i][0].type + 1) * (uniqueCombos[i][1].type + 1);
			if(!ints.contains(index))
			{
				ints.add(index);
				int sum = 0;
				for(int c = 0; c < reps; c++) {
					sum += edgecount[index-1][c];
				}
				StandardDeviation std = new StandardDeviation();
				double[] values = new double[reps];
				HashSet<Double> dedupe = new HashSet<Double>();
				for(int z = 0; z < reps; z++) {
					values[z] = (double)edgecount[index-1][z]/2d; 
					dedupe.add((double)edgecount[index-1][z]/2d); 
				}
				double mean = (double)sum/2d/reps;
				double stdev = std.evaluate(values);
				String edgeName = session.getTypeName(uniqueCombos[i][0].type) + "-" + session.getTypeName(uniqueCombos[i][1].type);
				double actual = edgecount[index-1][reps]/2d;
				double Z = (actual - mean) / stdev;
				
				if(edgeName.equals(daName)) {
					dataset.addSeries(edgeName, values, 100);
					System.out.println(edgeName + " mean: " + mean + " stdev: " + stdev + " actual: " + actual);
				}

				
				ValueMarker marker2 = new ValueMarker(actual);
				marker2.setPaint(Color.black);
				ValueMarker marker = new ValueMarker(Z);
				if(Math.abs(Z) > 1.96)
				{
					if(Z > 1.96) {
						CyNode thisNode = NetworkUtil.getNodeWithName(network, nodeTable, uniqueCombos[i][0].name);
						CyNode otherNode = NetworkUtil.getNodeWithName(network, nodeTable, uniqueCombos[i][1].name);
						
						if(thisNode == otherNode) {
							nodeTable.getRow(thisNode.getSUID()).set("selfnorm", Z);
							continue;
						}

						CyEdge edge = network.addEdge(thisNode, otherNode, false);

						CyRow row = edgeTable.getRow(edge.getSUID());

						row.set(CyNetwork.NAME, edgeName);
						row.set(CyEdge.INTERACTION, "Co-occurence");
						row.set("num", (int)actual); 
						row.set("normal", Z); 
					}
					marker.setPaint(Color.red);
					sigcount++;
				}
				else
					marker.setPaint(Color.black);
				
				XYTextAnnotation label = new XYTextAnnotation(edgeName, Z, 0.1);
				label.setFont(new Font("Sans", Font.BOLD, 8));
				label.setRotationAnchor(TextAnchor.BASELINE_CENTER);
				label.setTextAnchor(TextAnchor.BASELINE_CENTER);
				label.setRotationAngle(-3.14 / 2);
				label.setPaint(Color.black);
				plot.addAnnotation(label);
				plot.addDomainMarker(marker);
				if(edgeName.equals(daName)) {
					XYTextAnnotation label2 = new XYTextAnnotation(edgeName, actual, 10);
					label2.setFont(new Font("Sans", Font.BOLD, 8));
					label2.setRotationAnchor(TextAnchor.BASELINE_CENTER);
					label2.setTextAnchor(TextAnchor.BASELINE_CENTER);
					label2.setRotationAngle(-3.14 / 2);
					label2.setPaint(Color.black);
					plot2.addAnnotation(label);
					plot2.addDomainMarker(marker2);
				}
			}
		}
		System.out.println(sigcount + " significant interactions found.");
		ValueMarker upper = new ValueMarker(1.96, Color.green, new BasicStroke(2));
		ValueMarker lower = new ValueMarker(-1.96, Color.green, new BasicStroke(2));
		upper.setLabel("1.96");
		lower.setLabel("-1.96");
		upper.setLabelAnchor(RectangleAnchor.CENTER);
		lower.setLabelAnchor(RectangleAnchor.CENTER);
		plot.addDomainMarker(upper);
		plot.addDomainMarker(lower);
		plot.getRenderer().setSeriesPaint(0, Color.BLUE);
		plot.getRenderer().setSeriesStroke(0, new BasicStroke(2));
		JFrame frame = new JFrame();
		frame.add(new ChartPanel(chart));
		frame.setMinimumSize(new Dimension(600,300));
		frame.setVisible(true);

		JFrame frame2 = new JFrame();
		frame2.add(new ChartPanel(chart2));
		frame2.setMinimumSize(new Dimension(600,300));
		frame2.setVisible(true);




	}
}

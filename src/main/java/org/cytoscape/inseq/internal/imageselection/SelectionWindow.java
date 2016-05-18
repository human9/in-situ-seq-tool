package org.cytoscape.inseq.internal.imageselection;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class SelectionWindow extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3656880368971065116L;
	private ZoomPane zp;
	private GridBagConstraints consPanel;
	private JFrame parent;
	InseqActivator ia;

	public SelectionWindow(final InseqActivator ia) {
		super(ia.swingAppAdapter.getCySwingApplication().getJFrame(), "Select Region", false);
		this.parent = ia.swingAppAdapter.getCySwingApplication().getJFrame();
		this.setPreferredSize(new Dimension(400, 400));
		this.ia = ia;

		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		consPanel = new GridBagConstraints(0, 0, 2, 1, 0.1, 1, GridBagConstraints.SOUTH, 1, new Insets(0, 0, 0, 0), 1,
				1);
		final ImagePane ip = new ImagePane(ImagePane.getImageFile("/home/jrs/Pictures/inseq.png"), ia);
		zp = new ZoomPane(ip);
		zp.setVisible(true);
		add(zp, consPanel);

		GridBagConstraints consBrowse = new GridBagConstraints(0, 1, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton browse = new JButton("Change Image");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();

				int returnVal = fc.showOpenDialog(parent);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					changeImage(fc.getSelectedFile().getAbsolutePath());
				}
			}
		});
		add(browse, consBrowse);

		GridBagConstraints consInfo = new GridBagConstraints(2, 1, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton info = new JButton("Get Selection");
		info.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<Integer> gridNums = zp.getSelectedGridNumbers(ia.gridSize);

				for (CyNode node : ia.inseqNetwork.getNodeList()) {
					View<CyNode> nv = ia.inseqView.getNodeView(node);
					nv.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
					nv.setVisualProperty(BasicVisualLexicon.NODE_VISIBLE, true);
				}

				ia.selectedNodes = getNodesWithValue(ia.inseqNetwork, ia.inseqNetwork.getDefaultNodeTable(), "name",
						gridNums);

				for (CyNode node : ia.selectedNodes) {
					View<CyNode> nv = ia.inseqView.getNodeView(node);
					nv.setVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR, Color.GREEN);
				}
				ia.inseqView.updateView();

			}
		});
		add(info, consInfo);

		GridBagConstraints consShow = new GridBagConstraints(1, 1, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton showSelection = new JButton("See Selected");
		showSelection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<CyEdge> edges = CyTableUtil.getEdgesInState(ia.tn.tn,"selected",true);
				if(edges == null)
					System.out.println("EDGES NULL");
				else
				{
					ia.pointsToDraw = new HashMap<String, ArrayList<Point2D.Double>>();

					System.out.println("Viewing points from " + edges.size() + " edges.");
					// TODO: this
					for (CyEdge edge : edges) {
						CyRow row = ia.tn.et.getRow(edge.getSUID());
						String name1 = row.get("node1", String.class);
						ArrayList<Point2D.Double> points1 = ia.mps.get(name1);
						ia.pointsToDraw.put(name1, points1);
						
						String name2 = row.get("node2", String.class);
						ArrayList<Point2D.Double> points2 = ia.mps.get(name2);
						ia.pointsToDraw.put(name2, points2);
							
					}
				}
			}
		});
		add(showSelection, consShow);
		
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	private void changeImage(String path) {
		final ImagePane ip = new ImagePane(ImagePane.getImageFile(path), ia);
		zp.updateViewport(ip);
		repaint();
	}

	private static Set<CyNode> getNodesWithValue(final CyNetwork net, final CyTable table, final String colname,
			final ArrayList<Integer> values) {
		final Set<CyNode> nodes = new HashSet<CyNode>();
		for (Integer value : values) {
			final Collection<CyRow> matchingRows = table.getMatchingRows(colname, value.toString());
			final String primaryKeyColname = table.getPrimaryKey().getName();
			for (final CyRow row : matchingRows) {
				final Long nodeId = row.get(primaryKeyColname, Long.class);
				if (nodeId == null)
					continue;
				final CyNode node = net.getNode(nodeId);
				if (node == null)
					continue;
				nodes.add(node);
			}
		}
		return nodes;
	}
}

package org.cytoscape.inseq.internal.tissueimage;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.cytoscape.inseq.internal.DualPoint;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;

/** A window for displaying and selecting points plotted on an image
 *  @author John Salamon
 */
public class SelectionWindow extends JDialog {

	private static final long serialVersionUID = -3656880368971065116L;
	private ZoomPane zp;
	private GridBagConstraints consPanel;
	private JFrame parent;
	InseqActivator ia;
	public ImagePane imagePane;

	public SelectionWindow(final InseqActivator ia) {
		super(ia.swingAppAdapter.getCySwingApplication().getJFrame(), "Select Region", false);
		this.parent = ia.swingAppAdapter.getCySwingApplication().getJFrame();
		this.setPreferredSize(new Dimension(400, 400));
		this.ia = ia;

		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		consPanel = new GridBagConstraints(0, 0, 3, 1, 0.1, 1, GridBagConstraints.SOUTH, 1, new Insets(0, 0, 0, 0), 1,
				1);
		final ImagePane ip = new ImagePane(ImagePane.getImageFile("/home/jrs/Pictures/inseq.png"), ia);
		imagePane = ip;
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

				ia.selTranscripts = new HashMap<Point2D.Double, String>();
				//System.out.println(ia.rect.x + "-" + ia.rect.y + "-" + (ia.rect.x+ia.rect.width) + "-" + (ia.rect.y+ia.rect.height));
				for(Point2D.Double point : ia.transcripts.keySet())
				{
					if(point.x > ia.rect.x && point.x < (ia.rect.x+ia.rect.width) && point.y > ia.rect.y && point.y < (ia.rect.y+ia.rect.height))
					{
						//System.out.println(point);
						ia.selTranscripts.put(point, ia.transcripts.get(point));
					}
				}

			}
		});
		add(info, consInfo);

		GridBagConstraints consShow = new GridBagConstraints(1, 1, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton showSelection = new JButton("Show points");
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
						String name2 = row.get("node2", String.class);
						
						if(!ia.pointsToDraw.containsKey(name1))
							ia.pointsToDraw.put(name1, new ArrayList<Point2D.Double>());
						if(!ia.pointsToDraw.containsKey(name2))
							ia.pointsToDraw.put(name2, new ArrayList<Point2D.Double>());
						
						for(DualPoint p : ia.edgePoints.get(edge))
						{
							ia.pointsToDraw.get(name1).add(p.p1);
							ia.pointsToDraw.get(name2).add(p.p2);
						}
					}
				}
				zp.repaint();
				imagePane.repaint();
			}
		});
		add(showSelection, consShow);
		
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	private void changeImage(String path) {
		final ImagePane ip = new ImagePane(ImagePane.getImageFile(path), ia);
		imagePane = ip;
		zp.updateViewport(ip);
		repaint();
	}
}

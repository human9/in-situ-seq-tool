package org.cytoscape.inseq.internal.tissueimage;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTableUtil;

/** 
 *  A panel containing the zoomable imageplot as well as controls.
 *  It will start embedded in the MainPanel, but can become a seperate window.
 *  @author John Salamon
 */
public class SelectionPanel extends JPanel {

	private static final long serialVersionUID = -3656880368971065116L;
	private ZoomPane zp;
	private GridBagConstraints consPanel;
	private JFrame parent;
	InseqActivator ia;
	public ImagePane imagePane;

	public void setParent(JFrame parent) {
		this.parent = parent;
	}

	public SelectionPanel(final InseqActivator ia) {
		this.parent = ia.getCSAA().getCySwingApplication().getJFrame();
		this.setLayout(new BorderLayout());
		this.ia = ia;

		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);

		consPanel = new GridBagConstraints(0, 1, 3, 1, 0.1, 1, GridBagConstraints.SOUTH, 1, new Insets(0, 0, 0, 0), 0, 0);
		final ImagePane ip = new ImagePane(ImagePane.getImageFile("/home/jrs/Pictures/inseq.png"), ia.getSession());
		imagePane = ip;
		zp = new ZoomPane(ip);
		zp.setVisible(true);
		add(zp, consPanel);

		GridBagConstraints consBrowse = new GridBagConstraints(0, 2, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton browse = new JButton("Change Image");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FileDialog dialog = new FileDialog(parent, "Choose image", FileDialog.LOAD);
				// This works on Windowsy things 
				dialog.setFile("*.png;*.jpg;*.jpeg");
				// This works on Unixy things
				dialog.setFilenameFilter(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
					}
				});
				dialog.setVisible(true);
				String filename = dialog.getFile();
				if (filename == null) return;

				changeImage(dialog.getDirectory() + filename);
			}
		});
		add(browse, consBrowse);

		GridBagConstraints consInfo = new GridBagConstraints(2, 2, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton info = new JButton("Get Selection");
/*		info.addActionListener(new ActionListener() {
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

		});
*/
		add(info, consInfo);

		GridBagConstraints consShow = new GridBagConstraints(1, 2, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton showSelection = new JButton("Show points");
		showSelection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<CyEdge> edges = CyTableUtil.getEdgesInState(ia.getSession().network,"selected",true);
				if(edges == null || edges.size() < 1) ia.getSession().edgeSelection = null;
				else
				{
					ia.getSession().edgeSelection = new ArrayList<String>();
				/*	for(CyEdge edge : edges)
					{

						String source = ia.getSession().nodeTable.getRow(edge.getSource().getSUID()).get(CyNetwork.NAME, String.class);
						String target = ia.getSession().nodeTable.getRow(edge.getTarget().getSUID()).get(CyNetwork.NAME, String.class);
						if(!(ia.getSession().edgeSelection.contains(source)))
							ia.getSession().edgeSelection.add(source);
						if(!(ia.getSession().edgeSelection.contains(target)))
							ia.getSession().edgeSelection.add(target);
					}
*/
					System.out.println("Viewing points from " + edges.size() + " edges.");
					
				}
				zp.repaint();
				imagePane.repaint();
			}
		});
		add(showSelection, consShow);
		
		setVisible(true);
	}

	private void changeImage(String path) {
		final ImagePane ip = new ImagePane(ImagePane.getImageFile(path), ia.getSession());
		imagePane = ip;
		zp.updateViewport(ip);
		repaint();
	}
}

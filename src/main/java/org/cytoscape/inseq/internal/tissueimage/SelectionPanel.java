package org.cytoscape.inseq.internal.tissueimage;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;

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

				JFileChooser fc = new JFileChooser();
				
				// Force TIF support into ImageIO
				IIORegistry reg = IIORegistry.getDefaultInstance();
				reg.registerServiceProvider(new TIFFImageReaderSpi());

				FileFilter filter = new FileNameExtensionFilter("Supported image formats", ImageIO.getReaderFileSuffixes());
				fc.addChoosableFileFilter(filter);
				fc.setFileFilter(filter);
				
				int returnVal = fc.showOpenDialog(ia.getCSAA().getCySwingApplication().getJFrame());
				if (!(returnVal == JFileChooser.APPROVE_OPTION)) return;

				changeImage(fc.getSelectedFile().getAbsolutePath());
			}

		});
		add(browse, consBrowse);

		GridBagConstraints consInfo = new GridBagConstraints(2, 2, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton info = new JButton("this does nothing");
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
				List<CyEdge> edges = CyTableUtil.getEdgesInState(ia.getSession().getNetwork(ia.getSession().getSelectedNetwork()).getNetwork(),"selected",true);
				if(edges == null || edges.size() < 1) ia.getSession().edgeSelection = null;
				else
				{
					ia.getSession().edgeSelection = new HashMap<String, List<String>>();
					Map<String, List<String>> edgeSelection = ia.getSession().edgeSelection;
					for(CyEdge edge : edges)
					{

						String source = ia.getSession().getNetwork(ia.getSession().getSelectedNetwork()).getNodeTable().getRow(edge.getSource().getSUID()).get(CyNetwork.NAME, String.class);
						String target = ia.getSession().getNetwork(ia.getSession().getSelectedNetwork()).getNodeTable().getRow(edge.getTarget().getSUID()).get(CyNetwork.NAME, String.class);
						if(!(edgeSelection.keySet().contains(source)))
						{
							List<String> n = new ArrayList<String>();
							n.add(target);
							edgeSelection.put(source, n);
						}
						else {
							edgeSelection.get(source).add(target);
						}
						if(!(edgeSelection.keySet().contains(target)))
						{
							List<String> n = new ArrayList<String>();
							n.add(source);
							ia.getSession().edgeSelection.put(target, n);
						}
						else {
							edgeSelection.get(target).add(source);
						}
					}
					System.out.println("Viewing points from " + edges.size() + " edges.");
					
				}
				zp.repaint();
				imagePane.forceRepaint();
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

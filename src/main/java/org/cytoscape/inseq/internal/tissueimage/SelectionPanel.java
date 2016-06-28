package org.cytoscape.inseq.internal.tissueimage;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;

/** 
 *  A panel containing the zoomable imageplot as well as controls.
 *  It will start embedded in the MainPanel, but can become a seperate window.
 *  @author John Salamon
 */
public class SelectionPanel extends JLayeredPane {

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
		this.ia = ia;

		//GridBagLayout gbl = new GridBagLayout();
		//setLayout(gbl);

		consPanel = new GridBagConstraints(0, 1, 4, 1, 0.1, 1, GridBagConstraints.SOUTH, 1, new Insets(0, 0, 0, 0), 0, 0);
		final ImagePane ip = new ImagePane(ImagePane.getImageFile("/home/jrs/Pictures/inseq.png"), ia.getSession());
		imagePane = ip;
		zp = new ZoomPane(ip);
		zp.setVisible(true);
		//add(zp, consPanel);
		add(zp, JLayeredPane.DEFAULT_LAYER);
		zp.setBounds(0, 0, getWidth(), getHeight());

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				revalidate();
				zp.setBounds(0, 0, getWidth(), getHeight());
				zp.revalidate();
				zp.repaint();
				zp.updateViewport();
			}
		});

		GridBagConstraints consBrowse = new GridBagConstraints(0, 1, 1, 1, 0.1, 0, GridBagConstraints.SOUTH, 0,
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
		//add(browse, consBrowse);
		add(browse, JLayeredPane.PALETTE_LAYER);
		browse.setBounds(0,0,100,50);

		//node selection shows all points checkbox
		JCheckBox nodeBox = new JCheckBox("Show all points from selected nodes");
		GridBagConstraints consCheckBox = new GridBagConstraints(0, 3, 4, 1, 1, 0, GridBagConstraints.CENTER, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		nodeBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imagePane.setShowNodes(nodeBox.isSelected());
				zp.restartTimer();
			}
		});
		//add(nodeBox, consCheckBox);

		GridBagConstraints consPointScale = new GridBagConstraints(3, 2, 1, 1, 1, 0, GridBagConstraints.CENTER, 0, new Insets(4, 4, 4, 4), 1, 1);
		JLabel label = new JLabel("Point scaling:");
		GridBagConstraints consLabel = new GridBagConstraints(2,2,1,1,0.1,0,GridBagConstraints.CENTER,0,new Insets(4,4,4,4),1,1);
		//add(label, consLabel);
		JSpinner pointScale = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, 0.01d));
		pointScale.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				imagePane.setPointScale((Double)pointScale.getValue());	
			}
		});
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
		//add(pointScale, consPointScale);

		GridBagConstraints consShow = new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.CENTER, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton showSelection = new JButton("Show points");
		showSelection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CyNetwork network = ia.getSession().getNetwork(ia.getSession().getSelectedNetwork()).getNetwork();
				List<CyNode> nodes = CyTableUtil.getNodesInState(network, "selected", true);
				List<CyEdge> edges = CyTableUtil.getEdgesInState(network, "selected",true);
	
				ia.getSession().nodeSelection = new ArrayList<String>();
				for(CyNode node : nodes) {
					String name = network.getDefaultNodeTable().getRow(node.getSUID()).get(CyNetwork.NAME, String.class);
					ia.getSession().nodeSelection.add(name);
				}
				ia.getSession().edgeSelection = new HashMap<String, List<String>>();
				Map<String, List<String>> edgeSelection = ia.getSession().edgeSelection;
				for(CyEdge edge : edges)
				{

					String source = network.getDefaultNodeTable().getRow(edge.getSource().getSUID()).get(CyNetwork.NAME, String.class);
					String target = network.getDefaultNodeTable().getRow(edge.getTarget().getSUID()).get(CyNetwork.NAME, String.class);
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
				zp.repaint();
				zp.restartTimer();
				imagePane.forceRepaint();
			}
		});
		//add(showSelection, consShow);
		
		setVisible(true);
	}

	private void changeImage(String path) {
		final ImagePane ip = new ImagePane(ImagePane.getImageFile(path), ia.getSession());
		imagePane = ip;
		zp.updateViewport(ip);
		repaint();
	}
}

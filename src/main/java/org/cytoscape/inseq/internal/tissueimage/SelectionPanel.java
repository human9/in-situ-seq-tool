package org.cytoscape.inseq.internal.tissueimage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.inseq.internal.util.WrapLayout;
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
public class SelectionPanel extends JPanel {

	private static final long serialVersionUID = -3656880368971065116L;
	private ZoomPane zp;
	private JFrame parent;
	public JPanel plotControls;
	InseqActivator ia;
	public ImagePane imagePane;
	boolean showAllSelected = false;

	public void setParent(JFrame parent) {
		this.parent = parent;
	}

	public SelectionPanel(final InseqActivator ia) {
		this.parent = ia.getCSAA().getCySwingApplication().getJFrame();
		this.ia = ia;

		setLayout(new BorderLayout());
		plotControls = new JPanel();
		plotControls.setLayout(new WrapLayout(WrapLayout.LEADING));
		add(plotControls, BorderLayout.PAGE_START);

		final ImagePane ip = new ImagePane(ImagePane.getImageFile(null), ia.getSession(), new Dimension(300,300));
		imagePane = ip;
		zp = new ZoomPane(ip);
		zp.setVisible(true);
		add(zp, BorderLayout.CENTER);

		JButton browse = new JButton(UIManager.getIcon("FileView.directoryIcon"));
		plotControls.add(browse);
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

		//node selection shows all points checkbox
		
		JButton showAll = new JButton(NetworkUtil.iconFromResource("/notshowall.png"));
		plotControls.add(showAll);
		showAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showAllSelected = !showAllSelected;
				if(showAllSelected) {
					showAll.setIcon(NetworkUtil.iconFromResource("/showall.png"));
				}
				else {
					showAll.setIcon(NetworkUtil.iconFromResource("/notshowall.png"));
				}
				imagePane.setShowNodes(showAllSelected);
				zp.restartTimer();
			}
		});
		
		JSpinner pointScale = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, 0.01d));
		plotControls.add(pointScale);
		pointScale.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				imagePane.setPointScale((Double)pointScale.getValue());	
			}
		});

		JButton showSelection = new JButton(NetworkUtil.iconFromResource("/refresh.png"));
		plotControls.add(showSelection);
		showSelection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateSelection();
			}
		});
		
		JLabel label = new JLabel("Point scaling:");
		add(label, BorderLayout.PAGE_END);

		setVisible(true);
	}

	private void changeImage(String path) {
		final ImagePane ip = new ImagePane(ImagePane.getImageFile(path), ia.getSession(), zp.getViewport().getExtentSize());
		imagePane = ip;
		zp.updateViewport(ip);
		repaint();
	}

	public void updateSelection() {

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
}

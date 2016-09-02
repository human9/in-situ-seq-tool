package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
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
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.gl.JqadvPanel;
import org.cytoscape.inseq.internal.panel.VisualPicker;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.inseq.internal.util.ParseUtil;
import org.cytoscape.inseq.internal.util.WrapLayout;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;

/** 
 *  A panel containing the zoomable imageplot as well as controls.
 *  It will start embedded in MainPanel, but can become a seperate window.
 *  @author John Salamon
 */
public class SelectionPanel extends JPanel {

    private static final long serialVersionUID = -3656880368971065116L;
    //private ZoomPane zp;
    public JPanel plotControls;
    public JPanel externalControls;
    InseqActivator ia;
    boolean showAllSelected = false;
    private JButton colourPicker;
    private JFrame parent;
    private InseqSession session;
    private JqadvPanel jqadvpanel;
    private Transcript selected;

    public void setWindow(JFrame parent) {
        this.parent = parent;
    }

    public JqadvPanel getJqadvPanel() {
        return jqadvpanel;
    }
    
    public SelectionPanel(final InseqActivator ia) {
        this.ia = ia;
        this.session = ia.getSession();
        this.parent = ia.getCSAA().getCySwingApplication().getJFrame();

        setLayout(new BorderLayout());
        plotControls = new JPanel();

        WrapLayout wl = new WrapLayout(WrapLayout.LEADING);
        wl.setVgap(1);
        plotControls.setLayout(wl);
        
        add(plotControls, BorderLayout.PAGE_START);

        jqadvpanel = new JqadvPanel(ia.getSession(), this);
        add(jqadvpanel, BorderLayout.CENTER);

        JButton browse 
            = new JButton(UIManager.getIcon("FileView.directoryIcon"));
        plotControls.add(browse);
        browse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser fc = new JFileChooser();
                
                // Force TIF support into ImageIO
                IIORegistry reg = IIORegistry.getDefaultInstance();
                reg.registerServiceProvider(new TIFFImageReaderSpi());

                FileFilter filter 
                    = new FileNameExtensionFilter("Supported image formats", 
                            ImageIO.getReaderFileSuffixes());
                fc.addChoosableFileFilter(filter);
                fc.setFileFilter(filter);
                
                int returnVal 
                    = fc.showOpenDialog(parent);
                if (!(returnVal == JFileChooser.APPROVE_OPTION)) return;

                jqadvpanel.getUpdater().changeImage(
                        ParseUtil.getImageFile(fc.getSelectedFile().getAbsolutePath()));
            }

        });

        //node selection shows all points checkbox
        
        JButton showAll 
            = new JButton(NetworkUtil.iconFromResource("/texture/notshowall.png"));
        plotControls.add(showAll);
        showAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAllSelected = !showAllSelected;
                if(showAllSelected) {
                    showAll.setIcon(NetworkUtil
                            .iconFromResource("/texture/showall.png"));
                }
                else {
                    showAll.setIcon(NetworkUtil
                            .iconFromResource("/texture/notshowall.png"));
                }
                session.setShowAll(showAllSelected);
                updateSelection();
            }
        });
        
        
        JButton showSelection 
            = new JButton("RECT");
        plotControls.add(showSelection);
        showSelection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jqadvpanel.getListener().enableRect();
            }
        });
        
        JButton polygonSelect 
            = new JButton("POLY");
        plotControls.add(polygonSelect);
        polygonSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jqadvpanel.getListener().enablePoly();
            }
        });
        
        JButton center 
            = new JButton("RESET");
        plotControls.add(center);
        center.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jqadvpanel.center();
            }
        });

        JCheckBox bigSymbols = new JCheckBox("Big symbols");
        plotControls.add(bigSymbols);
        bigSymbols.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jqadvpanel.getUpdater().largePoints(bigSymbols.isSelected());
            }
        });

        colourPicker = new JButton("C/S");
        plotControls.add(colourPicker);
        colourPicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new VisualPicker(parent, session, jqadvpanel, selected.type);
            }
        });
        colourPicker.setEnabled(false);
        
        JSpinner pointScale 
            = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, 0.01d));
        plotControls.add(new JLabel("Scaling: "), BorderLayout.LINE_START);
        plotControls.add(pointScale);
        pointScale.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                jqadvpanel.getUpdater().setImageScale(((Double)pointScale.getValue()).floatValue());   
            }
        });
    }

    public void setSelected(Transcript t) {
        if(t == null) {
            colourPicker.setEnabled(false);
        }
        else {
            selected = t;
            colourPicker.setEnabled(true);
        }

    }


    public void updateSelection() {

        CyNetwork network 
            = ia.getSession().getSelectedNetwork().getNetwork();
        List<CyNode> nodes 
            = CyTableUtil.getNodesInState(network, "selected", true);
        List<CyEdge> edges 
            = CyTableUtil.getEdgesInState(network, "selected",true);

        // Nothing is selected, don't bother doing anything more
        if(nodes.size() < 1 && edges.size() < 1) return;

        ia.getSession().nodeSelection = new ArrayList<Integer>();
        for(CyNode node : nodes) {
            Integer type 
                = session.names.indexOf(network.getDefaultNodeTable().getRow(node.getSUID())
                .get(CyNetwork.NAME, String.class));
            ia.getSession().nodeSelection.add(type);
        }
        ia.getSession().edgeSelection = new HashMap<Integer, List<Integer>>();
        Map<Integer, List<Integer>> edgeSelection 
            = ia.getSession().edgeSelection;
        for(CyEdge edge : edges)
        {

            Integer source 
                = session.names.indexOf(network.getDefaultNodeTable().getRow(edge.getSource()
                        .getSUID()).get(CyNetwork.NAME, String.class));
            Integer target 
                = session.names.indexOf(network.getDefaultNodeTable().getRow(edge.getTarget()
                        .getSUID()).get(CyNetwork.NAME, String.class));
            if(!(edgeSelection.keySet().contains(source)))
            {
                List<Integer> n = new ArrayList<Integer>();
                n.add(target);
                edgeSelection.put(source, n);
            }
            else {
                edgeSelection.get(source).add(target);
            }
            if(!(edgeSelection.keySet().contains(target)))
            {
                List<Integer> n = new ArrayList<Integer>();
                n.add(source);
                ia.getSession().edgeSelection.put(target, n);
            }
            else {
                edgeSelection.get(target).add(source);
            }
        }

        jqadvpanel.getUpdater().changeNetworkComponents();

    }

}

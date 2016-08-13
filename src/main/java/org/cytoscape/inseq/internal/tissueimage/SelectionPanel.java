package org.cytoscape.inseq.internal.tissueimage;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.gl.JqadvPanel;
import org.cytoscape.inseq.internal.panel.VisualPicker;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.util.NetworkUtil;
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
    public ImagePane imagePane;
    boolean showAllSelected = false;
    public StatusBar statusBar;
    private JButton colourPicker;
    private JFrame parent;
        
    VisualPicker visualPicker;

    public void setWindow(JFrame parent) {
        this.parent = parent;
    }

    public BufferedImage getImageFile(String path) {

        File input = new File(path);
        BufferedImage bimg;
        BufferedImage optImage;
        try {
            bimg = ImageIO.read(input);
            GraphicsEnvironment ge 
                = GraphicsEnvironment.getLocalGraphicsEnvironment();
                
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();

            optImage = gc.createCompatibleImage(bimg.getWidth(),
                    bimg.getHeight());
            
            // Copy image to buffered image
            Graphics g = optImage.createGraphics();
            g.drawImage(bimg, 0, 0, null);
            g.dispose();


        } catch (IOException|NullPointerException e) {
            JOptionPane.showMessageDialog(null, 
                    "The selected file could not be opened.", "Warning!",
            JOptionPane.WARNING_MESSAGE);
            return null;
        }
        
        return optImage;
    }
    
    public SelectionPanel(final InseqActivator ia) {
        this.ia = ia;
        this.parent = ia.getCSAA().getCySwingApplication().getJFrame();

        setLayout(new BorderLayout());
        plotControls = new JPanel();
        plotControls.setLayout(new WrapLayout(WrapLayout.LEADING));
        
        add(plotControls, BorderLayout.PAGE_START);

        JqadvPanel jqadvpanel = new JqadvPanel(ia.getSession());
        add(jqadvpanel, BorderLayout.CENTER);
        //final ImagePane ip = new ImagePane(null, ia.getSession(), 
          //      new Dimension(300,300));
        //imagePane = ip;
        //visualPicker = new VisualPicker(imagePane, parent, ia.getSession());
        statusBar = new StatusBar();
        updateZoom();
        //zp = new ZoomPane(this, ia.getSession());
        //add(zp, BorderLayout.CENTER);

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
                    = fc.showOpenDialog(ia.getCSAA().getCySwingApplication()
                            .getJFrame());
                if (!(returnVal == JFileChooser.APPROVE_OPTION)) return;

                changeImage(fc.getSelectedFile().getAbsolutePath());
            }

        });

        //node selection shows all points checkbox
        
        JButton showAll 
            = new JButton(NetworkUtil.iconFromResource("/notshowall.png"));
        plotControls.add(showAll);
        showAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAllSelected = !showAllSelected;
                if(showAllSelected) {
                    showAll.setIcon(NetworkUtil
                            .iconFromResource("/showall.png"));
                }
                else {
                    showAll.setIcon(NetworkUtil
                            .iconFromResource("/notshowall.png"));
                }
                //imagePane.setShowNodes(showAllSelected);
                //imagePane.forceRepaint();
            }
        });
        
        
        JButton showSelection 
            = new JButton("RECT");
        plotControls.add(showSelection);
        showSelection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //zp.enableRect();
            }
        });
        
        JButton polygonSelect 
            = new JButton("POLY");
        plotControls.add(polygonSelect);
        polygonSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //zp.enablePoly();
            }
        });

        colourPicker = new JButton("C/S");
        plotControls.add(colourPicker);
        colourPicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                visualPicker.setVisible(true);
                visualPicker.toFront();
                visualPicker.repaint();
            }
        });
        colourPicker.setEnabled(false);
        
        JPanel statusPanel = new JPanel();
        BorderLayout layout = new BorderLayout();
        layout.setVgap(0);
        statusPanel.setLayout(layout);
        add(statusPanel, BorderLayout.PAGE_END);

        statusPanel.add(statusBar, BorderLayout.CENTER);
    
        JSpinner pointScale 
            = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, 0.01d));
        JPanel scalePanel = new JPanel();
        BorderLayout scaleLayout = new BorderLayout();
        scaleLayout.setVgap(0);
        scalePanel.setLayout(scaleLayout);
        scalePanel.add(new JLabel("Scaling: "), BorderLayout.LINE_START);
        scalePanel.add(pointScale);
        statusPanel.add(scalePanel, BorderLayout.LINE_END);
        pointScale.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                //imagePane.setPointScale((Double)pointScale.getValue());   
                //imagePane.forceRepaint();
            }
        });
    }

    public void updateZoom() {
        //DecimalFormat df = new DecimalFormat("#.##");
        //statusBar.setZoom(df.format(imagePane.getScale()*100)+"%");
    }

    private void changeImage(String path) {
        //final ImagePane ip = new ImagePane(getImageFile(path), 
          //      ia.getSession(), zp.getViewport().getExtentSize());
        //ip.sp = this;
        //imagePane = ip;
        //zp.updateViewport(ip);
        repaint();
    }

    public void setSelected(Transcript t) {
        if(t == null) {
            statusBar.setTranscript(""); 
            colourPicker.setEnabled(false);
        }
        else {
            DecimalFormat df = new DecimalFormat("#.##");
            statusBar.setTranscript(t.name + " ("+df.format(t.pos.x)+", "
                    + df.format(t.pos.y) + ")");
            colourPicker.setEnabled(true);
            visualPicker.setSelected(t.name);
            visualPicker.toFront();
        }

    }


    public void updateSelection() {

        CyNetwork network 
            = ia.getSession().getNetwork(ia.getSession()
                    .getSelectedNetwork()).getNetwork();
        List<CyNode> nodes 
            = CyTableUtil.getNodesInState(network, "selected", true);
        List<CyEdge> edges 
            = CyTableUtil.getEdgesInState(network, "selected",true);

        ia.getSession().nodeSelection = new ArrayList<String>();
        for(CyNode node : nodes) {
            String name 
                = network.getDefaultNodeTable().getRow(node.getSUID())
                .get(CyNetwork.NAME, String.class);
            ia.getSession().nodeSelection.add(name);
        }
        ia.getSession().edgeSelection = new HashMap<String, List<String>>();
        Map<String, List<String>> edgeSelection 
            = ia.getSession().edgeSelection;
        for(CyEdge edge : edges)
        {

            String source 
                = network.getDefaultNodeTable().getRow(edge.getSource()
                        .getSUID()).get(CyNetwork.NAME, String.class);
            String target 
                = network.getDefaultNodeTable().getRow(edge.getTarget()
                        .getSUID()).get(CyNetwork.NAME, String.class);
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

        //zp.repaint();
        //imagePane.forceRepaint();
    }

}

class StatusBar extends JLabel {

    private static final long serialVersionUID = 43852L;
    private String transcript = "";
    private String zoom;

    StatusBar() {
        super();
        updateText();
    }

    void setTranscript(String str) {
        this.transcript = str;
        updateText();
    }
    
    void setZoom(String str) {
        this.zoom = str;
        updateText();
    }

    void updateText() {
        this.setText("Z: " + zoom + " " + transcript);
    }

}

package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.ViewStyler;
import org.cytoscape.inseq.internal.sync.SyncTask;
import org.cytoscape.inseq.internal.typenetwork.FindNeighboursTask;
import org.cytoscape.inseq.internal.typenetwork.ShuffleTask;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class SessionPanel extends JPanel {
    
    static final long serialVersionUID = 45353l;

    InseqActivator ia;

    private double distance = 30;
    private double cutoff = 0;
    private SeparateFrame frame;
    private SelectionPanel selectionPanel;
    private BasePanel basePanel;
    private JButton pop;
    JList<TypeNetwork> networkList;
    DefaultListModel<TypeNetwork> model;
    private InseqSession session;
    private JCheckBox autoSlider;
    public final String name;
    private CyLayoutAlgorithm layoutAlgorithm;
    double mag = 20;
    double sig = 95;
    JCheckBox bonferroni;
    boolean bonferroniCorrection = true;
    private boolean useSubset = true;
    private boolean interaction = true;

    private TaskIterator itr; 

    class SeparateFrame extends JFrame {
        static final long serialVersionUID = 4324324l;
        SeparateFrame(Component parent, String title, Dimension size) {
            super(title);
            this.setMinimumSize(new Dimension(100,100));
            setPreferredSize(new Dimension((int)Math.max(size.getWidth(), 400), (int)Math.max(size.getHeight(), 400)));
            setLayout(new BorderLayout());
            add(selectionPanel);
            pack();
            setLocationRelativeTo(parent);
            setVisible(true);
        }

    }
    
    private void closeWindow() {
        pop.setVisible(true);
        selectionPanel.setWindow(ia.getCSAA().getCySwingApplication().getJFrame());
        basePanel.add(selectionPanel);
        basePanel.revalidate();
        repaint();
    }

    int pos = 0;
    private GridBagConstraints makeCons() {
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridy = pos++;
        cons.weightx = 1;
        cons.anchor = GridBagConstraints.NORTHWEST;
        cons.fill = GridBagConstraints.HORIZONTAL;
        cons.insets = new Insets(4,8,4,8);
        return cons;
    }

    public void updateListSelection() {
        TypeNetwork selected = (TypeNetwork) networkList.getSelectedValue();
        session.setSelectedNetwork(selected);
        if(selected == null) {
            session.setSelection(null);
        }
        else { 
            session.setSelection(selected.getSelection());
        }
        selectionPanel.getJqadvPanel().getGL().selectionChanged(true);
    }

    public SessionPanel(String name, final InseqActivator ia, InseqSession session) {
        this.ia = ia;
        this.name = name;
        this.session = session;
        this.setLayout(new GridBagLayout());

        ImageIcon icon = NetworkUtil.iconFromResource("/texture/pop.png");
        pop = new JButton(icon);
        pop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                pop.setVisible(false);
                frame = new SeparateFrame(ia.getCSAA().getCySwingApplication().getJFrame(), "Imageplot", selectionPanel.getSize());
                selectionPanel.setWindow(frame);
                frame.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        closeWindow();
                    }
                });
                revalidate();
                repaint();
                } catch (java.lang.IllegalArgumentException i) {
                    pop.setVisible(true);
                    JOptionPane.showMessageDialog(SessionPanel.this, "Couldn't open new window", "Error", JOptionPane.WARNING_MESSAGE);
                }
                
            }
        });

        model = new DefaultListModel<TypeNetwork>();
        networkList = new JList<TypeNetwork>(model);
        networkList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        networkList.setLayoutOrientation(JList.VERTICAL);

        networkList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateListSelection();
            }
        });
        networkList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                updateListSelection(); 
            }
        });

        JScrollPane listScroller = new JScrollPane(networkList);
        listScroller.setMinimumSize(networkList.getPreferredScrollableViewportSize());

        JComboBox<CyLayoutAlgorithm> layoutComboBox = new JComboBox<CyLayoutAlgorithm>(ia.getCAA().getCyLayoutAlgorithmManager().getAllLayouts().toArray(new CyLayoutAlgorithm[0]));
        layoutAlgorithm = ia.getCAA().getCyLayoutAlgorithmManager().getLayout("force-directed");
        layoutComboBox.setSelectedItem(layoutAlgorithm);
        layoutComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED){
                    layoutAlgorithm = (CyLayoutAlgorithm) layoutComboBox.getSelectedItem();
                }
            }
        });

        JButton layoutButton = new JButton("Layout selected");
        layoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SyncTask t = new SyncTask(networkList.getSelectedValuesList(), layoutAlgorithm, ia.getCAA(), session);
                ia.getCSAA().getDialogTaskManager().execute(new TaskIterator(t));
            }
        });
        add(ExpandableOptionsFactory.makeOptionsPanel("Network list", listScroller, layoutComboBox, layoutButton), makeCons());

        JPanel magPanel = new JPanel();
        magPanel.setLayout(new BoxLayout(magPanel, BoxLayout.LINE_AXIS));
        JLabel mlabel = new JLabel("Magnification: ");
        magPanel.add(mlabel);
        JSpinner magnification = new JSpinner(new SpinnerNumberModel(mag, 0d, 100d, 1d));
        magPanel.add(magnification);
        magnification.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                mag = (Double)(magnification.getValue());
            }
        });
        JLabel xlabel = new JLabel("x");
        magPanel.add(xlabel);

        JPanel distancePanel = new JPanel();
        distancePanel.setLayout(new BoxLayout(distancePanel, BoxLayout.LINE_AXIS));
        JLabel dlabel = new JLabel("Search distance: ");
        distancePanel.add(dlabel);
        JSpinner distanceCutoff = new JSpinner(new SpinnerNumberModel(distance, 0d, 100d, 0.1d));
        distancePanel.add(distanceCutoff);
        distanceCutoff.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                distance = (Double)(distanceCutoff.getValue());
            }
        });
        JLabel ulabel = new JLabel("μm");
        distancePanel.add(ulabel);
        
        add(ExpandableOptionsFactory.makeOptionsPanel("Distance control", magPanel, distancePanel), makeCons());

        JRadioButton entire = new JRadioButton("Entire dataset");

        JRadioButton subset = new JRadioButton("Current selection");
        subset.setSelected(true);

        ButtonGroup selectionGroup = new ButtonGroup();
        selectionGroup.add(entire);
        selectionGroup.add(subset);
        
        autoSlider = new JCheckBox("Use automatic slider (multiple networks)");
        autoSlider.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    useSubset = true;
                    entire.setEnabled(false);
                    subset.setEnabled(false);
                }
                else {
                    entire.setEnabled(true);
                    subset.setEnabled(true);
                }
            }
        });

        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.LINE_AXIS));
        subPanel.add(entire);
        subPanel.add(subset);
        add(ExpandableOptionsFactory.makeOptionsPanel("Region Selection", autoSlider, subPanel), makeCons());


        JRadioButton negative = new JRadioButton("Negative");
        JRadioButton positive = new JRadioButton("Positive");
        positive.setSelected(true);

        ButtonGroup significanceGroup = new ButtonGroup();
        significanceGroup.add(negative);
        significanceGroup.add(positive);

        JPanel sigPanel = new JPanel();
        sigPanel.setLayout(new BoxLayout(sigPanel, BoxLayout.LINE_AXIS));
        sigPanel.add(new JLabel("Find"));
        sigPanel.add(positive);
        sigPanel.add(negative);
        sigPanel.add(new JLabel("interactions."));

        bonferroni = new JCheckBox("Use Bonferroni Correction", true);
        bonferroni.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                bonferroniCorrection = bonferroni.isSelected();
            }
        });
        JSpinner sigLevel = new JSpinner(new SpinnerNumberModel(sig, 0d, 100d, 1d));
        sigLevel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                sig = (Double)(magnification.getValue());
            }
        });
        JPanel bonPanel = new JPanel();
        bonPanel.setLayout(new BoxLayout(bonPanel, BoxLayout.LINE_AXIS));
        bonPanel.add(new JLabel("Use a "));
        bonPanel.add(sigLevel);
        bonPanel.add(new JLabel("% confidence interval."));


        add(ExpandableOptionsFactory.makeOptionsPanel("Significance", sigPanel, bonferroni, bonPanel), makeCons());

        entire.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                useSubset = false;  
            }
        });
        
        subset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                useSubset = true;   
            }
        });

        positive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                interaction = true;  
            }
        });
        
        negative.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                interaction = false;
            }
        });

        JButton types = new JButton("Generate co-occurence network(s)");
        itr = new TaskIterator();
        types.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Note: Make sure to initialise tasks before starting the iterator to avoid race conditions
                //
                itr = new TaskIterator();
                if(autoSlider.isSelected()) {
                   
                    // Get user input
                    AutoselectorSetupDialog dialog = new AutoselectorSetupDialog(ia.getCSAA().getCySwingApplication().getJFrame());
                    Dimension d = dialog.getInput();
                    Dimension total = session.min;

                    int numHorizontal = (int) Math.ceil(total.width / d.width);
                    int numVertical = (int) Math.ceil(total.height / d.height);

                    int width = (int) Math.ceil(total.width / numHorizontal);
                    int height = (int) Math.ceil(total.height / numVertical);

                    for(int h = 0; h < numVertical; h++) {
                        for(int w = 0; w < numHorizontal; w++) {
                            Rectangle r = new Rectangle(w*width, h*height, width, height);
                            session.setSelection(r);
                            // Makes new network be created
                            makeNetwork(getTN());
                        }
                    }
                }
                else {
                
                    if(itr.hasNext()) return;
                    makeNetwork(getTN());
                }
        
                ia.getCSAA().getDialogTaskManager().execute(itr);

            }
        });
        add(types, makeCons());
        

        selectionPanel = new SelectionPanel(ia, session);
        selectionPanel.plotControls.add(pop);
        selectionPanel.setWindow(ia.getCSAA().getCySwingApplication().getJFrame());
        
        GridBagConstraints selectionCons = new GridBagConstraints();
        selectionCons.fill = GridBagConstraints.BOTH;
        selectionCons.gridy = pos++;
        selectionCons.weighty = 1;
        selectionCons.anchor = GridBagConstraints.NORTHWEST;
        basePanel = new BasePanel(selectionPanel);
        add(basePanel, selectionCons);

    }   

    double CONV = 0.15;
    public TypeNetwork getTN() {

        // Create a new TypeNetwork
        TypeNetwork network
            = new TypeNetwork(ia.getCAA().getCyNetworkFactory().createNetwork(),
                              distance / (mag*CONV), cutoff);
        return network;
    }

    public void makeNetwork(TypeNetwork network) {
        
        // Create and execute a task to find distances.
        Shape selection = session.getSelection();
        char type;
        int x = 0; int y = 0; int w = 0; int h = 0;
        if(useSubset && selection != null) {
            Rectangle2D bounds = selection.getBounds2D();
            type = selection.contains(bounds) ? 'R' : 'P';
            x = (int)bounds.getX();
            y = (int)bounds.getY();
            w = (int)bounds.getWidth();
            h = (int)bounds.getHeight();
        }
        else {
            type = 'E';
        }

        double px = distance / (mag*CONV);
        String networkName 
            = String.format("%c%.2f X%d Y%d W%d H%d", type, network.getDistance(),x,y,w,h);
        Task neighboursTask = new FindNeighboursTask(selection, session.tree, network, px, useSubset);
        
        itr.append(neighboursTask);

        
        // Register the network
        Task registerTask = new AbstractTask() {
            public void run (TaskMonitor monitor) {
                session.addNetwork(network, px, cutoff);
            }
        };

        // Construct and display the new network.
        Task networkTask = new ShuffleTask(network, interaction, session, networkName, sig, bonferroniCorrection);

        Task styleTask = new ViewStyler(network, session.getStyle(), ia.getCAA());

        Task refreshTask = new AbstractTask() {
            public void run(TaskMonitor monitor) {
                refreshNetworks(network);
                updateListSelection();
            }
        };
        
        itr.insertTasksAfter(neighboursTask, registerTask, networkTask, styleTask, refreshTask);
    }
    
    public void refreshNetworks(TypeNetwork selected) {
        if(!model.contains(selected)) {
            model.addElement(selected);
        }
        networkList.setSelectedValue(selected, true);
    }

    public void updateSelectionPanel() {
        selectionPanel.updateSelection();
    }

    class AutoselectorSetupDialog extends JDialog
                                  implements PropertyChangeListener {

        static final long serialVersionUID = 453l;

        JOptionPane op;
        JSpinner x, y;
        Dimension d;

        public AutoselectorSetupDialog(Frame parent) {
            super(parent, "Autoselector Setup", true);

            JPanel spinnerPanel = new JPanel();
            spinnerPanel.setLayout(new FlowLayout());
            x = new JSpinner(new SpinnerNumberModel((int)((session.min.width/4) / (mag*CONV)), 0, (int)(session.min.width / (mag*CONV)), 1));
            y = new JSpinner(new SpinnerNumberModel((int)((session.min.height/4) / (mag*CONV)), 0, (int)(session.min.height / (mag*CONV)), 1));
            spinnerPanel.add(x);
            spinnerPanel.add(new JLabel("x"));
            spinnerPanel.add(y);
            Object[] array = {"Select sliding region dimensions:", spinnerPanel};
            Object[] options = {"OK", "Cancel"};
            op = new JOptionPane(array, JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION, null, options,
                    options[0]);
            setContentPane(op);
            op.addPropertyChangeListener(this);
            pack();
            setLocationRelativeTo(parent);
        }

        public Dimension getInput() {
            setVisible(true);
            d.width *= (mag*CONV);
            d.height *= (mag*CONV);
            return d;
        }

        public void propertyChange(PropertyChangeEvent e) {
            Object value = op.getValue();
            if(value.equals("OK")) {
                System.out.println(x.getValue());
                d = new Dimension((int)x.getValue(), (int)y.getValue());
                setVisible(false);
            }
            else {
                d = null;
                setVisible(false);
            }

        }


    }

    @Override
    public String toString() {
        return name;
    }

    public void shutDown() {
        if(frame != null) {
            frame.dispose();
        }
        selectionPanel.getJqadvPanel().destroy();
    }



}

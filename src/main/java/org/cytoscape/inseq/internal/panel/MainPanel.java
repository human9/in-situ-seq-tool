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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.ViewStyler;
import org.cytoscape.inseq.internal.tissueimage.SelectionPanel;
import org.cytoscape.inseq.internal.typenetwork.FindNeighboursTask;
import org.cytoscape.inseq.internal.typenetwork.ShuffleTask;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class MainPanel extends JPanel implements CytoPanelComponent {

    static final long serialVersionUID = 692;

    InseqActivator ia;

    private double distance = 8;
    private double cutoff = 0;
    private SeparateFrame frame;
    private SelectionPanel selectionPanel;
    private JSplitPane splitPane;
    private JButton pop;
    private boolean useSubset;
    private boolean interaction = true;
    private Vector<TypeNetwork> networkVector;
    private JComboBox<TypeNetwork> netBox; 
    private InseqSession session;

    private TaskIterator itr; 

    class SeparateFrame extends JFrame {
        static final long serialVersionUID = 4324324l;
        SeparateFrame(JFrame parent, String title, Dimension size) {
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
        splitPane.setRightComponent(selectionPanel);
        pop.setVisible(true);
        selectionPanel.setWindow(ia.getCSAA().getCySwingApplication().getJFrame());
        revalidate();
        repaint();
    }


    public MainPanel(final InseqActivator ia, InseqSession session) {
        this.ia = ia;
        this.session = session;
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        JRadioButton entire = new JRadioButton("Entire dataset");
        entire.setSelected(true);

        JRadioButton subset = new JRadioButton("Current selection");

        ButtonGroup selectionGroup = new ButtonGroup();
        selectionGroup.add(entire);
        selectionGroup.add(subset);
        
        GridBagConstraints regionCons = new GridBagConstraints(0, 4, 2, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);

        JPanel regionPanel = new JPanel();
        regionPanel.setLayout(new BoxLayout(regionPanel, BoxLayout.LINE_AXIS));
        regionPanel.add(entire);
        regionPanel.add(subset);
        panel.add(regionPanel, regionCons);

        JRadioButton negative = new JRadioButton("Negative");
        JRadioButton positive = new JRadioButton("Positive");
        positive.setSelected(true);

        ButtonGroup significanceGroup = new ButtonGroup();
        significanceGroup.add(negative);
        significanceGroup.add(positive);

        GridBagConstraints sigCons = new GridBagConstraints(0, 5, 2, 1, 0 ,0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4,4,4,4), 1, 1);
        JPanel sigPanel = new JPanel();
        sigPanel.setLayout(new BoxLayout(sigPanel, BoxLayout.LINE_AXIS));
        sigPanel.add(positive);
        sigPanel.add(negative);
        panel.add(sigPanel, sigCons);

        Border etch = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        TitledBorder significanceBorder = BorderFactory.createTitledBorder(etch,
                "Interaction type");
        significanceBorder.setTitleJustification(TitledBorder.LEFT);
        sigPanel.setBorder(significanceBorder);
        
        TitledBorder regionBorder = BorderFactory.createTitledBorder(etch,
                "Region selection");
        regionBorder.setTitleJustification(TitledBorder.LEFT);
        regionPanel.setBorder(regionBorder);

        JCheckBox autoSlider = new JCheckBox("Use automatic slider");
        GridBagConstraints autoCons = new GridBagConstraints(0, 3, 2, 1, 1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        panel.add(autoSlider, autoCons);
        autoSlider.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    useSubset = true;
                    regionPanel.setEnabled(false);
                    subset.setSelected(true);
                    entire.setEnabled(false);
                    subset.setEnabled(false);
                }
                else {
                    regionPanel.setEnabled(true);
                    entire.setEnabled(true);
                    subset.setEnabled(true);
                }
            }
        });

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
                
        GridBagConstraints dlabelCons = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        JLabel dlabel = new JLabel("Distance cutoff:");
        panel.add(dlabel, dlabelCons);

        GridBagConstraints distanceCons = new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        JSpinner distanceCutoff = new JSpinner(new SpinnerNumberModel(distance, 0d, 100d, 0.1d));
        panel.add(distanceCutoff, distanceCons);
        distanceCutoff.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                distance = (Double)(distanceCutoff.getValue());
            }
        });
    /*  
        GridBagConstraints nlabelCons = new GridBagConstraints(0, 2, 2, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        JButton distButton = new JButton("Find distribution");
        panel.add(distButton, nlabelCons);
        distButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TaskIterator ti = new TaskIterator(new ShuffleTask(session.getNetwork(session.getSelectedNetwork()), session, ia.getCAA()));
                ia.getCSAA().getDialogTaskManager().execute(ti);
            }
        });
*/
        GridBagConstraints consTypes = new GridBagConstraints(0, 6, 2, 1, 1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        JButton types = new JButton("Generate co-occurence network");
        panel.add(types, consTypes);
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
                            netBox.setSelectedItem(null);
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


        GridBagConstraints sep = new GridBagConstraints(0, 5, 2, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        panel.add(Box.createHorizontalStrut(0), sep);
        

        GridBagConstraints smallCons = new GridBagConstraints(0, 0, 2, 1, 1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        JPanel small = new JPanel();
        small.setLayout(new GridBagLayout());
        GridBagConstraints netBoxCons = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        networkVector = new Vector<TypeNetwork>(ia.getSession().getNetworkList());
        netBox = new JComboBox<TypeNetwork>(networkVector);

        netBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                TypeNetwork selected = (TypeNetwork) netBox.getSelectedItem();
                session.setSelectedNetwork(selected);
                if(selected == null) {
                    session.setSelection(null);
                }
                else { 
                    session.setSelection(selected.getSelection());
                }
                selectionPanel.getJqadvPanel().getUpdater().selectionChanged(true);
            }
        });
        small.add(netBox, netBoxCons);

        GridBagConstraints newNetCons = new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
        JButton newNet = new JButton("New");
        newNet.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                netBox.setSelectedItem(null);
            }
        });
        small.add(newNet, newNetCons);

        panel.add(small, smallCons);
        
        
        ImageIcon icon = NetworkUtil.iconFromResource("/pop.png");
        pop = new JButton(icon);
        pop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pop.setVisible(false);
                frame = new SeparateFrame(ia.getCSAA().getCySwingApplication().getJFrame(), "Imageplot", selectionPanel.getSize());
                selectionPanel.setWindow(frame);
                frame.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        selectionPanel.setWindow(ia.getCSAA().getCySwingApplication().getJFrame());
                        closeWindow();
                    }
                });
                revalidate();
                repaint();
                
            }
        });
        this.setLayout(new BorderLayout());

        selectionPanel = new SelectionPanel(ia);
        selectionPanel.plotControls.add(pop);
        selectionPanel.setWindow(ia.getCSAA().getCySwingApplication().getJFrame());
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, selectionPanel);
        this.add(splitPane, BorderLayout.CENTER);
        this.repaint();
    }   

    public TypeNetwork getTN() {

        // Create a new TypeNetwork
        TypeNetwork network;
        if((TypeNetwork)(netBox.getSelectedItem()) == null)
            network = new TypeNetwork(ia.getCAA().getCyNetworkFactory().createNetwork(), distance, cutoff);
        else
            network = (TypeNetwork)(netBox.getSelectedItem());
        return network;
    }

    public void makeNetwork(TypeNetwork network) {
        
        // Create and execute a task to find distances.
        Shape selection = null;
        if(useSubset) selection = session.getSelection();
        Task neighboursTask = new FindNeighboursTask(selection, session.tree, network, distance, useSubset);
        
        itr.append(neighboursTask);

        
        // Register the network
        Task registerTask = new AbstractTask() {
            public void run (TaskMonitor monitor) {
                session.addNetwork(network, distance, cutoff);
            }
        };

        // Construct and display the new network.
        Task networkTask = new ShuffleTask(network, interaction, session, ia.getCAA());

        Task styleTask = new ViewStyler(network, session.getStyle(), ia.getCAA());

        Task refreshTask = new AbstractTask() {
            public void run(TaskMonitor monitor) {
                refreshNetworks(network);
            }
        };
        
        itr.insertTasksAfter(neighboursTask, registerTask, networkTask, styleTask, refreshTask);
    }
    
    public void refreshNetworks(TypeNetwork selected) {
        networkVector.clear();
        networkVector.addAll(ia.getSession().getNetworkList());
        netBox.setSelectedItem(selected);
        netBox.repaint();
    }

    public void updateSelectionPanel() {
        selectionPanel.updateSelection();
    }


    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getTitle() {
        return "Inseq";
    }

    public void shutDown() {
        if(frame != null) {
            frame.dispose();
        }
    }

    class AutoselectorSetupDialog extends JDialog
                                  implements PropertyChangeListener {

        JOptionPane op;
        JSpinner x, y;
        Dimension d;

        public AutoselectorSetupDialog(Frame parent) {
            super(parent, "Autoselector Setup", true);

            JPanel spinnerPanel = new JPanel();
            spinnerPanel.setLayout(new FlowLayout());
            x = new JSpinner(new SpinnerNumberModel(session.min.width/4, 0, session.min.width, 1));
            y = new JSpinner(new SpinnerNumberModel(session.min.height/4, 0, session.min.height, 1));
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
            return d;
        }

        public void propertyChange(PropertyChangeEvent e) {
            Object value = op.getValue();
            if(value.equals("OK")) {
                d = new Dimension((int)x.getValue(), (int)y.getValue());
                setVisible(false);
            }
            else {
                d = null;
                setVisible(false);
            }

        }


    }
}

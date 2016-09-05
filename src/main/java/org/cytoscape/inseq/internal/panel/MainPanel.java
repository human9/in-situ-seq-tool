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
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
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

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.ViewStyler;
import org.cytoscape.inseq.internal.typenetwork.FindNeighboursTask;
import org.cytoscape.inseq.internal.typenetwork.ShuffleTask;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.util.swing.DropDownMenuButton;
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
    private BasePanel basePanel;
    private JButton pop;
    private boolean useSubset;
    private boolean interaction = true;
    private JComboBox<InseqSession> netBox; 
    JList<TypeNetwork> networkList;
    DefaultListModel<TypeNetwork> model;
    private InseqSession session;
    private JCheckBox autoSlider;

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


    public MainPanel(final InseqActivator ia, InseqSession session) {
        this.ia = ia;
        this.session = session;
        this.setLayout(new GridBagLayout());

        //use JTable for TypeNetworks
        //use combobox for sessions

        JPanel header = new JPanel();
        header.setLayout(new GridBagLayout());
        
        GridBagConstraints netBoxCons = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 0), 1, 1);
        netBox = new JComboBox<InseqSession>();

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
                selectionPanel.getJqadvPanel().getGL().selectionChanged(true);
            }
        });
        header.add(netBox, netBoxCons);

        GridBagConstraints newNetCons = new GridBagConstraints(1, 0, 1, 1, 0.1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(4, 0, 4, 4), 1, 1);
        DropDownMenuButton newNet = new DropDownMenuButton(new MenuAction("Menu"));
        header.add(newNet, newNetCons);

        add(header, makeCons());
        
        
        ImageIcon icon = NetworkUtil.iconFromResource("/texture/pop.png");
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
                        closeWindow();
                    }
                });
                revalidate();
                repaint();
                
            }
        });

        model = new DefaultListModel<TypeNetwork>();
        networkList = new JList<TypeNetwork>(model);
        networkList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        networkList.setLayoutOrientation(JList.VERTICAL);
        JScrollPane listScroller = new JScrollPane(networkList);
        listScroller.setMinimumSize(networkList.getPreferredScrollableViewportSize());
        add(ExpandableOptionsFactory.makeOptionsPanel("Network list", listScroller), makeCons());

        JPanel distancePanel = new JPanel();
        distancePanel.setLayout(new BoxLayout(distancePanel, BoxLayout.LINE_AXIS));
        JLabel dlabel = new JLabel("Distance cutoff: ");
        distancePanel.add(dlabel);
        JSpinner distanceCutoff = new JSpinner(new SpinnerNumberModel(distance, 0d, 100d, 0.1d));
        distancePanel.add(distanceCutoff);
        distanceCutoff.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                distance = (Double)(distanceCutoff.getValue());
            }
        });
        add(ExpandableOptionsFactory.makeOptionsPanel("Distance control", distancePanel), makeCons());

        JRadioButton entire = new JRadioButton("Entire dataset");
        entire.setSelected(true);

        JRadioButton subset = new JRadioButton("Current selection");

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
        sigPanel.add(positive);
        sigPanel.add(negative);

        add(ExpandableOptionsFactory.makeOptionsPanel("Significance", sigPanel), makeCons());

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
        add(types, makeCons());
        

        selectionPanel = new SelectionPanel(ia);
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
        String networkName 
            = String.format("%c%.2f X%d Y%d W%d H%d", type, network.getDistance(),x,y,w,h);
        Task neighboursTask = new FindNeighboursTask(selection, session.tree, network, distance, useSubset);
        
        itr.append(neighboursTask);

        
        // Register the network
        Task registerTask = new AbstractTask() {
            public void run (TaskMonitor monitor) {
                session.addNetwork(network, distance, cutoff);
            }
        };

        // Construct and display the new network.
        Task networkTask = new ShuffleTask(network, interaction, session, networkName);

        Task styleTask = new ViewStyler(network, session.getStyle(), ia.getCAA());

        Task refreshTask = new AbstractTask() {
            public void run(TaskMonitor monitor) {
                refreshNetworks(network);
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
        selectionPanel.getJqadvPanel().destroy();
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

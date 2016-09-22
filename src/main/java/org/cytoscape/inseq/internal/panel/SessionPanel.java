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
import org.cytoscape.inseq.internal.typenetwork.HypergeometricTask;
import org.cytoscape.inseq.internal.typenetwork.ShuffleTask;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.NetworkUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

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
    double rankCutoff = 100;
    double sig = 0.05;
    private boolean useSubset = true;
    private boolean rcutoff = false;
    private int interaction = 0;
    private int testType = 0;

    private SetterUpperer su;

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
                pop.setVisible(false);
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
        magnification.setMaximumSize(new Dimension(80, magnification.getPreferredSize().height));
        magPanel.add(magnification);
        magnification.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                mag = (Double)(magnification.getValue());
            }
        });
        JLabel xlabel = new JLabel("X");
        magPanel.add(xlabel);

        JPanel distancePanel = new JPanel();
        distancePanel.setLayout(new BoxLayout(distancePanel, BoxLayout.LINE_AXIS));
        JLabel dlabel = new JLabel("Search distance: ");
        distancePanel.add(dlabel);
        JSpinner distanceCutoff = new JSpinner(new SpinnerNumberModel(distance, 0d, 100d, 0.1d));
        distanceCutoff.setMaximumSize(new Dimension(80, distanceCutoff.getPreferredSize().height));
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
        
        autoSlider = new JCheckBox("Use automatic slider (Creates multiple networks)");
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

        String[] interactions = new String[]{"More than expected", "Less than expected", "Any unexpected interaction"};
        JComboBox<String> interactionBox = new JComboBox<String>(interactions);
        interactionBox.setSelectedItem(interactions[0]);
        interactionBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED){
                    interaction = interactionBox.getSelectedIndex();
                }
            }
        });
        
        // List of all our test task classes

        String[] tests = new String[]{"Label shuffle", "Hypergeometric test"};
        JComboBox<String> testBox = new JComboBox<String>(tests);
        testBox.setSelectedItem(tests[0]);
        testBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED){
                    testType = testBox.getSelectedIndex();
                }
            }
        });

        JButton testInfo = new JButton("?");
        testInfo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String description = "<html><body><p style='width: 300px;'>";
                switch(testType) {
                    case 0:
                        description
                            += "The label shuffle method creates a distribution that"
                            + " models shuffling the labels of every transcript "
                            + "and then recounting which transcripts are colocated."
                            + "The actual colocation count for each transcript pair is"
                            + " then compared to this distribution to assess probability."
                            + "</p></body></html>";
                        JOptionPane.showMessageDialog(ia.getCSAA().getCySwingApplication().getJFrame(), description, "Label shuffle test", JOptionPane.INFORMATION_MESSAGE);
                        break;
                    case 1:
                        description
                            += "The hypergeometric test method also creates a distribution"
                            + " used to assess probability. For the interaction of transcripts A-B, it uses the "
                            + "hypergeometric distribution with the following parameters:"
                            + "<li>N = total transcript no.</li>"
                            + "<li>n = total no. of A</li>"
                            + "<li>K = total no. of B</li>"
                            + "<li>k = no. of transcripts involved in A-B colocations</li>"
                            + "</p></body></html>";

                        JOptionPane.showMessageDialog(ia.getCSAA().getCySwingApplication().getJFrame(), description, "Hypergeometric test", JOptionPane.INFORMATION_MESSAGE);
                        break;
                }
                
            }
        });

        JPanel testPanel = new JPanel();
        testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.LINE_AXIS));
        testPanel.add(new JLabel("Test type: "));
        testPanel.add(testBox);
        testPanel.add(testInfo);
        
        JPanel interactionPanel = new JPanel();
        interactionPanel.setLayout(new BoxLayout(interactionPanel, BoxLayout.LINE_AXIS));
        interactionPanel.add(new JLabel("Find interactions that occur: "));
        interactionPanel.add(interactionBox);


        JSpinner sigLevel = new JSpinner(new SpinnerNumberModel(sig, 0d, 1, 0.01d));
        sigLevel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                sig = (Double)(sigLevel.getValue());
            }
        });
        sigLevel.setMaximumSize(new Dimension(80, sigLevel.getPreferredSize().height));
        JPanel bonPanel = new JPanel();
        bonPanel.setLayout(new BoxLayout(bonPanel, BoxLayout.LINE_AXIS));
        bonPanel.add(new JLabel("Condition: p < "));
        bonPanel.add(sigLevel);

        JPanel rankPanel = new JPanel();
        rankPanel.setLayout(new BoxLayout(rankPanel, BoxLayout.LINE_AXIS));

        JCheckBox useRankCutoff = new JCheckBox("Only show edges ranked up to ");
        useRankCutoff.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    rcutoff = true;
                }
                else {
                    rcutoff = false;
                }
            }
        });
        rankPanel.add(useRankCutoff);
        JSpinner rankLimit = new JSpinner(new SpinnerNumberModel(rankCutoff, 1d, 10000d, 1d));
        rankLimit.setMaximumSize(new Dimension(80, rankLimit.getPreferredSize().height));
        rankLimit.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                rankCutoff = (Double)(rankLimit.getValue());
            }
        });
        rankPanel.add(rankLimit);


        add(ExpandableOptionsFactory.makeOptionsPanel("Significance", testPanel, interactionPanel, bonPanel, rankPanel), makeCons());

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

        JButton types = new JButton("Generate network(s)");
        types.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Note: Make sure to initialise tasks before starting the iterator to avoid race conditions
                
                su = new SetterUpperer();
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
                    makeNetwork(getTN());
                }

                su.run();

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
            if(autoSlider.isSelected()) {
                type = 'A';
            }
            else {
                type = selection.contains(bounds) ? 'R' : 'P';
            }
            x = (int)bounds.getX();
            y = (int)bounds.getY();
            w = (int)bounds.getWidth();
            h = (int)bounds.getHeight();
        }
        else {
            type = 'E';
        }

        char test = (testType == 0) ? 'S' : 'H';
        double px = distance / (mag*CONV);

        double r;
        if(rcutoff) {
            r = rankCutoff;
        }
        else {
            r = Double.POSITIVE_INFINITY;
        }

        String rankString;
        if(r == Double.POSITIVE_INFINITY) {
            rankString = "ALL";
        }
        else {
            rankString = ((Integer)((Double)r).intValue()).toString();
        }
        
        char side;
        switch(interaction) {
            case 0:
                side = 'M';
                break;
            case 1:
                side = 'L';
                break;
            case 2:
                side = 'A';
                break;
            default:
                side = 'E';
                break;
        }

        String networkName 
            = String.format("%c%.2f%c%.2f%c%s:%d:%d:%d:%d", type, network.getDistance(), test, sig, side, rankString, x,y,w,h);
        FindNeighboursTask neighboursTask = new FindNeighboursTask(selection, session.tree, network, px, useSubset);
        
        // Register the network
        Task registerTask = new AbstractTask() {
            public void run (TaskMonitor monitor) {
                session.addNetwork(network, px, cutoff);
            }
        };

        // Construct and display the new network.
        Task networkTask;

        switch(testType) {
            case 0:
                networkTask = new ShuffleTask(network, session, networkName, interaction, sig, r);
                break;
            case 1:
                networkTask = new HypergeometricTask(network, session, networkName, interaction, sig, r);
                break;
            default:
                throw new java.lang.IndexOutOfBoundsException("Selected test type invalid");
        }

        Task styleTask = new ViewStyler(network, testType, session.getStyle(), ia.getCAA());

        Task refreshTask = new AbstractTask() {
            public void run (TaskMonitor monitor) {
                refreshNetworks(network);
                //updateListSelection();
            }
        };

        su.add(neighboursTask, new TaskIterator(registerTask, networkTask, styleTask, refreshTask));
    }
    
    class SetterUpperer implements TaskObserver {

        int state = 0;
        
        TaskIterator findNeighboursIterator = new TaskIterator();
        TaskIterator laterIterator = new TaskIterator();

        public void add(FindNeighboursTask f, TaskIterator i) {
            findNeighboursIterator.append(f);
            laterIterator.append(i);
        }

        public void run() {
            ia.getCSAA().getDialogTaskManager().execute(findNeighboursIterator, this);
        }

        public void allFinished(FinishStatus s) {
            if(s.getType() == FinishStatus.Type.SUCCEEDED) {
                switch(state) {
                    case 0:
                        ia.getCSAA().getDialogTaskManager().execute(laterIterator, this);
                        state++;
                        break;
                    case 1:
                        networkList.setSelectedValue(model.lastElement(), true);
                        break;
                }
            }
        }

        public void taskFinished(ObservableTask t) {
            
        }
    }

    public void refreshNetworks(TypeNetwork selected) {
        if(!model.contains(selected)) {
            model.addElement(selected);
        }
        //networkList.setSelectedValue(selected, true);
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

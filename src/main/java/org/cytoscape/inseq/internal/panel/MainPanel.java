package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.ViewStyler;
import org.cytoscape.inseq.internal.tissueimage.SelectionPanel;
import org.cytoscape.inseq.internal.typenetwork.FindNeighboursTask;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.typenetwork.TypeNetworkTask;
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
	private Vector<TypeNetwork> networkVector;
	private JComboBox<TypeNetwork> netBox; 

	private TaskIterator itr; 

	class SeparateFrame extends JFrame {
		static final long serialVersionUID = 4324324l;
		SeparateFrame(String title, Dimension size) {
			super(title);
			this.setMinimumSize(new Dimension(100,100));
			setPreferredSize(new Dimension((int)Math.max(size.getWidth(), 400), (int)Math.max(size.getHeight(), 400)));
			setVisible(true);
			setLayout(new BorderLayout());
			add(selectionPanel);
			pack();
			
		}

	}
	
	private void closeWindow() {
		splitPane.setRightComponent(selectionPanel);
		pop.setVisible(true);
		selectionPanel.setParent(ia.getCSAA().getCySwingApplication().getJFrame());
		revalidate();
		repaint();
	}


	public MainPanel(final InseqActivator ia, InseqSession session) {
		this.ia = ia;
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		JRadioButton entire = new JRadioButton("Entire dataset");
		entire.setSelected(true);

		JRadioButton subset = new JRadioButton("Current selection");

		ButtonGroup group = new ButtonGroup();
		group.add(entire);
		group.add(subset);
		
		GridBagConstraints entireCons = new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		GridBagConstraints subsetCons = new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		panel.add(entire, entireCons);
		panel.add(subset, subsetCons);

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
		
		GridBagConstraints nlabelCons = new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		JLabel nlabel = new JLabel("Normal cutoff:");
		panel.add(nlabel, nlabelCons);

		GridBagConstraints normalCons = new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		JSpinner normalCutoff = new JSpinner(new SpinnerNumberModel(cutoff, 0d, 100d, 0.001d));
		normalCutoff.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				cutoff = (Double)normalCutoff.getValue();
			}
		});

		panel.add(normalCutoff, normalCons);

		GridBagConstraints consTypes = new GridBagConstraints(0, 4, 2, 1, 1, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		JButton types = new JButton("Generate co-occurence network");
		panel.add(types, consTypes);
		itr = new TaskIterator();
		types.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(itr.hasNext()) return;
				
				// Create a new TypeNetwork
				TypeNetwork network;
				if((TypeNetwork)(netBox.getSelectedItem()) == null)
					network = new TypeNetwork(ia.getCAA().getCyNetworkFactory().createNetwork(), distance, cutoff);
				else
					network = (TypeNetwork)(netBox.getSelectedItem());
				
				// Create and execute a task to find distances.
				Task neighboursTask = new FindNeighboursTask(session, network, distance, useSubset);
				itr = new TaskIterator(neighboursTask);
				ia.getCSAA().getDialogTaskManager().execute(itr);

				
				// Register the network
				itr.append(new AbstractTask() {
					public void run (TaskMonitor monitor) {
						session.addNetwork(network, distance);
					}
				});

				// Construct and display the new network.
				Task networkTask = new TypeNetworkTask(network, session, ia.getCAA());
				itr.append(networkTask);

				
				itr.append(new ViewStyler(network, session.getStyle(), ia.getCAA()));

				itr.append(new AbstractTask() {
					public void run(TaskMonitor monitor) {
						refreshNetworks(network);
					}
				});

			}
		});


		GridBagConstraints sep = new GridBagConstraints(0, 5, 2, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		panel.add(Box.createHorizontalStrut(0), sep);
		

		GridBagConstraints smallCons = new GridBagConstraints(0, 6, 2, 1, 1, 0, GridBagConstraints.CENTER,
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
				session.setSelectedNetwork((TypeNetwork)(netBox.getSelectedItem()));
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
		
		
		GridBagConstraints consPop = new GridBagConstraints(0, 0, 3, 1, 0.5, 0, GridBagConstraints.SOUTH,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		pop = new JButton("Move image to separate window");
		pop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pop.setVisible(false);
				frame = new SeparateFrame("Imageplot", selectionPanel.getSize());
				selectionPanel.setParent(frame);
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
		this.setLayout(new BorderLayout());

		selectionPanel = new SelectionPanel(ia);
		selectionPanel.add(pop, consPop);
		selectionPanel.setParent(ia.getCSAA().getCySwingApplication().getJFrame());
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, selectionPanel);
		this.add(splitPane, BorderLayout.CENTER);
		this.repaint();
	}	
	
	public void refreshNetworks(TypeNetwork selected) {
		networkVector.clear();
		networkVector.addAll(ia.getSession().getNetworkList());
		netBox.setSelectedItem(selected);
		netBox.repaint();
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
}

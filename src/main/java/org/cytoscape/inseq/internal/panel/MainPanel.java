package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
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
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

public class MainPanel extends JPanel implements CytoPanelComponent {

	static final long serialVersionUID = 692;

	InseqActivator ia;
	public SelectionPanel sw;

	private double distance = 4;
	private double cutoff = 0;
	private SeparateFrame frame;
	private SelectionPanel selectionPanel;
	private JSplitPane splitPane;
	private JButton pop;

	class SeparateFrame extends JFrame {
		SeparateFrame(String title, Dimension size) {
			super(title);
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

		GridBagConstraints consTypes = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton types = new JButton("Generate co-occurence network");
		panel.add(types, consTypes);
		types.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Create and execute a task to find distances.
				Task neighboursTask = new FindNeighboursTask(session.tree, distance);
				TaskIterator itr= new TaskIterator(neighboursTask);
				ia.getCSAA().getDialogTaskManager().execute(itr);

				// Create a new network and add it to our list of TypeNetworks.
				TypeNetwork network = new TypeNetwork(ia.getCAA().getCyNetworkFactory().createNetwork(), distance, cutoff);
				session.addNetwork(network);

				// Construct and display the new network.
				Task networkTask = new TypeNetworkTask(network, session.tree);
				itr.append(networkTask);

				itr.append(new ViewStyler(network.getNetwork(), session.getStyle(), ia.getCAA()));
			}
		});
		
		GridBagConstraints cons8 = new GridBagConstraints(0, 5, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		final JSpinner distanceCutoff = new JSpinner(new SpinnerNumberModel(4d, 0d, 100d, 0.1d));
		distanceCutoff.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				distance = Math.pow((Double)(distanceCutoff.getValue()), 2);
			}
		});
		
		GridBagConstraints cons9 = new GridBagConstraints(0, 7, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		final JSpinner cutoffSpinner = new JSpinner(new SpinnerNumberModel(0.1d, 0d, 100d, 0.001d));
		cutoffSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				cutoff = (Double)cutoffSpinner.getValue();
			}
		});

		panel.add(distanceCutoff, cons8);
		panel.add(cutoffSpinner, cons9);

		GridBagConstraints consPop = new GridBagConstraints(0, 8, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		pop = new JButton("Move into separate window");
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
		panel.add(pop, consPop);
		this.setLayout(new BorderLayout());

		selectionPanel = new SelectionPanel(ia);
		selectionPanel.setParent(ia.getCSAA().getCySwingApplication().getJFrame());
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, selectionPanel);
		this.add(splitPane, BorderLayout.CENTER);
		this.repaint();
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

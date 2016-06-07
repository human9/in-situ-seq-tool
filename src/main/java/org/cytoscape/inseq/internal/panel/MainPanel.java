package org.cytoscape.inseq.internal.panel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
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
import org.cytoscape.inseq.internal.tissueimage.ImagePane;
import org.cytoscape.inseq.internal.tissueimage.SelectionWindow;
import org.cytoscape.inseq.internal.tissueimage.ZoomPane;
import org.cytoscape.inseq.internal.typenetwork.FindNeighboursTask;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.typenetwork.TypeNetworkTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

public class MainPanel extends JPanel implements CytoPanelComponent {

	static final long serialVersionUID = 692;

	InseqActivator ia;
	public SelectionWindow sw;

	private double distance = 4;
	private double cutoff = 0;

	public MainPanel(final InseqActivator ia, InseqSession session) {
		this.ia = ia;
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		this.setPreferredSize(new Dimension(400, 400));


		GridBagConstraints cons6 = new GridBagConstraints(0, 6, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton types = new JButton("Generate co-occurence network");
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

				itr.append(new ViewStyler(network.getNetwork(), session.style, ia.getCAA()));
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

		panel.add(types, cons6);
		panel.add(distanceCutoff, cons8);
		panel.add(cutoffSpinner, cons9);
		
		GridBagConstraints cons = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton openSelector = new JButton("Choose image...");
		openSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sw = new SelectionWindow(ia);
			}
		});
		panel.add(openSelector, cons);

		ZoomPane zp = new ZoomPane(new ImagePane(ImagePane.getImageFile("/home/jrs/Pictures/sd.png"), ia));
		zp.setVisible(true);
		GridBagConstraints consPanel = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, 1, new Insets(0, 0, 0, 0), 1, 1);
		JPanel lowerPanel = new JPanel();
		lowerPanel.add(zp, consPanel);

		// TODO: Fix this, make it sit nicely in the panel until we pop it out
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, lowerPanel);
		lowerPanel.setVisible(true);
		splitPane.setVisible(true);
		this.add(splitPane, cons);
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

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
import org.cytoscape.inseq.internal.tissueimage.SelectionWindow;
import org.cytoscape.inseq.internal.util.FindNeighboursTask;
import org.cytoscape.inseq.internal.util.TypeNetworkTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

public class ControlPanel extends JPanel implements CytoPanelComponent {

	static final long serialVersionUID = 692;

	InseqActivator ia;
	public SelectionWindow sw;

	private double distance = 4;
	private double cutoff = 0;

	public ControlPanel(final InseqActivator ia, InseqSession session) {
		this.ia = ia;
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		this.setPreferredSize(new Dimension(400, 400));

		GridBagConstraints cons = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton openSelector = new JButton("Open selection window");
		openSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sw = new SelectionWindow(ia);
			}
		});

		GridBagConstraints cons6 = new GridBagConstraints(0, 6, 1, 1, 1, 1, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 1, 1);
		JButton types = new JButton("Generate co-occurence network");
		types.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(session.tree.size());
				Task neighboursTask = new FindNeighboursTask(session.tree, distance);
				Task networkTask = new TypeNetworkTask(ia, session.tree, cutoff);
				
				TaskIterator itr = new TaskIterator(neighboursTask, networkTask);	
				ia.getCSAA().getDialogTaskManager().execute(itr);
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

		panel.add(openSelector, cons);
		panel.add(types, cons6);
		panel.add(distanceCutoff, cons8);
		panel.add(cutoffSpinner, cons9);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, null);
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

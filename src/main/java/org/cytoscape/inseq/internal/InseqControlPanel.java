package org.cytoscape.inseq.internal;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

public class InseqControlPanel extends JPanel implements CytoPanelComponent {

	static final long serialVersionUID = 692;
	CySwingAppAdapter swingAdapter;

	public InseqControlPanel(CySwingAppAdapter adapter, CyApplicationManager applicationManager, final PictureWindow pictureWindow)
	{
		swingAdapter = adapter;
		this.setLayout(new GridBagLayout()); 
		GridBagConstraints cons = new GridBagConstraints();
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1;
		cons.weighty = 1;
		cons.gridx = 0;
		cons.gridy = 1;
		pictureWindow.setLayout(new GridLayout(0, 1));

		GridBagConstraints cons2 = new GridBagConstraints();
		cons2.fill = GridBagConstraints.HORIZONTAL;
		cons2.weightx = 1;
		cons2.gridx = 0;
		cons2.gridy = 0;

		JButton openSelector = new JButton("Open selection window");
		openSelector.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SelectionWindow dialog = new SelectionWindow(swingAdapter.getCySwingApplication().getJFrame());
			}
		});
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, openSelector, pictureWindow);
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

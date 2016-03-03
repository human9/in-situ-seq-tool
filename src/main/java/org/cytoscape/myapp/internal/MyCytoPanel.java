package org.cytoscape.myapp.internal;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

public class MyCytoPanel extends JPanel implements CytoPanelComponent {
	
	static final long serialVersionUID = 692;

	public MyCytoPanel(CyApplicationManager applicationManager) {
		this.applicationManager = applicationManager;
	}

	private final CyApplicationManager applicationManager;
	
	@Override
	public Component getComponent(){
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(500, 500));
		JLabel label = new JLabel("Invert nodes");
		Button button = new Button("DO IT");

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				InvertAction.invertSelected(applicationManager);
			}
		});

		panel.add(label, BorderLayout.CENTER);
		panel.add(button, BorderLayout.CENTER);
		return panel;
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
	public String getTitle(){
		return "TEST";
	}
}


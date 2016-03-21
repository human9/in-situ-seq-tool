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
import javax.swing.border.EmptyBorder;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

public class EmptyPanel extends JPanel implements CytoPanelComponent {
	
	static final long serialVersionUID = 692;
	private final CyApplicationManager appManager;

	public EmptyPanel(CyApplicationManager applicationManager, final PictureWindow pwi) {
		appManager = applicationManager;
		this.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.setPreferredSize(new Dimension(400, 400));
		JLabel label = new JLabel("CoolApp Control Center");
		Button showButton = new Button("Show CoolApp Window");
		Button ziButton = new Button("zoom in");
		Button zoButton = new Button("zoom out");

		showButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pwi.add();
				//InvertAction.invertSelected(appManager);
			}
		});
		ziButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//pwi.zp.zoomIn();
				pwi.repaint();
			}
		});
		zoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//pwi.zp.zoomOut();
				pwi.repaint();
				
			}
		});

		this.add(label, BorderLayout.CENTER);
		this.add(showButton, BorderLayout.SOUTH);
		this.add(ziButton, BorderLayout.SOUTH);
		this.add(zoButton, BorderLayout.SOUTH);
	}



	@Override
	public Component getComponent(){
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
	public String getTitle(){
		return "CoolApp";
	}
}


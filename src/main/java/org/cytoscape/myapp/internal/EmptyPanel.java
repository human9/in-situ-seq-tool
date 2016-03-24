package org.cytoscape.myapp.internal;

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
		Button ziButton = new Button("Invert selected XY");

		showButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pwi.add();
			}
		});
		ziButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				InvertAction.invertSelected(appManager);
				pwi.repaint();
			}
		});

		this.add(label);
		this.add(showButton);
		this.add(ziButton);
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
		return "CoolApp";
	}
}

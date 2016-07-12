package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.tissueimage.ImagePane;

public class VisualPicker extends JFrame implements ChangeListener
{

	private InseqSession session;
	private ImagePane p;
	private Color colour;
	private String name;
	private JColorChooser chooser;

	public VisualPicker(ImagePane p, InseqSession s) {

		super();
		this.session = s;
		this.p = p;

		setMinimumSize(new Dimension(100,100));
		setPreferredSize(new Dimension(600, 400));
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}
		});

		chooser = new JColorChooser();
		chooser.getSelectionModel().addChangeListener(this);

		add(chooser, BorderLayout.CENTER);

		pack();
	}

	public void setSelected(String name) {
		colour = session.getGeneColour(name);	
		this.name = name;
		
		chooser.setColor(colour);
		this.setTitle("Change " + name + " colour");
	}

	public void stateChanged(ChangeEvent e) {
		Color newColour = chooser.getColor();
		//TODO: have ok button, set panel colour, refresh plot, set network colour
		session.setGeneColour(name, newColour);
		p.forceRepaint();
		session.updateStyle();
	}

}

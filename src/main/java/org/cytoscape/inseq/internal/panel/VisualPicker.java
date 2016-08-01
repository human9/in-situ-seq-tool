package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.tissueimage.ImagePane;

public class VisualPicker extends JDialog implements ChangeListener
{

	private InseqSession session;
	private ImagePane p;
	private Color colour;
    private Color newColour;
	private String name;
	private JColorChooser chooser;
    private Color old;

	public VisualPicker(ImagePane p, InseqSession s) {

		super();
		this.session = s;
		this.p = p;

		setMinimumSize(new Dimension(100,100));
		setPreferredSize(new Dimension(600, 400));
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
                responseCancel();
			}
		});

		chooser = new JColorChooser();
		chooser.getSelectionModel().addChangeListener(this);

		add(chooser, BorderLayout.CENTER);

        JPanel responses = new JPanel();
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                responseOk();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                responseCancel();
            }
        });
        responses.add(ok);
        responses.add(cancel);

        add(responses, BorderLayout.SOUTH);

		pack();
	}

    @Override
    public void setVisible(boolean isVisible) {

        if(isVisible) chooser.setColor(colour);

        super.setVisible(isVisible);

    }

    private void responseOk() {
        setVisible(false);
    }

    private void responseCancel() {
        session.setGeneColour(name, old);
        p.forceRepaint();
        session.refreshStyle();
        setVisible(false);
    }

	public void setSelected(String name) {
		colour = session.getGeneColour(name);	
		this.name = name;
        this.old = session.getGeneColour(name);
		
		chooser.setColor(colour);
		this.setTitle("Change " + name + " colour");
	}

	public void stateChanged(ChangeEvent e) {
		newColour = chooser.getColor();
		//TODO: have ok button, set panel colour, refresh plot, set network colour
		session.setGeneColour(name, newColour);

        // don't actually need this - could do it more intelligently
		p.forceRepaint();
		session.refreshStyle();
	}

}

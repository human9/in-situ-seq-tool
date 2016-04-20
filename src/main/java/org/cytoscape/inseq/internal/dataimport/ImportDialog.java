package org.cytoscape.inseq.internal.dataimport;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class ImportDialog extends JDialog {
	
	JTextField input;
	boolean listenersActive = true;

	ImportDialog(final JFrame parent)
	{
		super(parent, "Inseq Importer", true);
		setPreferredSize(new Dimension(300,160));
		GridBagLayout gbl = new GridBagLayout();
		getContentPane().setLayout(gbl);
		
		GridBagConstraints consLabel = new GridBagConstraints(0,0,2,1,0.8,0.1,GridBagConstraints.CENTER,0,new Insets(4,4,4,4), 1,1);
		JLabel label = new JLabel("In situ sequencing data importer");
		add(label, consLabel);
	
		GridBagConstraints consInput = new GridBagConstraints(0,1,1,1,0.8,0.1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(4,4,4,4), 1,1);
		input = new JTextField("Data to import");
		input.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(listenersActive)
					input.setText("");
				listenersActive = false;
			}
		});
		input.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(listenersActive)
					input.setText("");
				listenersActive = false;
			}
		});
		add(input, consInput);

		GridBagConstraints consBrowse = new GridBagConstraints(1,1,1,1,0.1,0.1,GridBagConstraints.CENTER,0,new Insets(4,4,4,4), 1,1);
		JButton browse = new JButton("Browse...");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();

				int returnVal = fc.showOpenDialog(parent);
				
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					input.setText(fc.getSelectedFile().getAbsolutePath());
					listenersActive = false;
				}
			}
		});
		add(browse, consBrowse);
		
		GridBagConstraints consCancel = new GridBagConstraints(0,2,GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE,0.1,0.1,GridBagConstraints.SOUTHWEST,0,new Insets(4,4,4,4), 1,1);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		add(cancel, consCancel);
		
		GridBagConstraints consConfirm = new GridBagConstraints(1,2,GridBagConstraints.RELATIVE,GridBagConstraints.RELATIVE,0.1,0.1,GridBagConstraints.SOUTHEAST,0,new Insets(4,4,4,4), 1,1);
		JButton confirm = new JButton("Import");
		confirm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					File imported = new File(input.getText());
				}
				catch(NullPointerException n) {
					JOptionPane.showMessageDialog(null,"don't do that","Stahp",JOptionPane.WARNING_MESSAGE);
					return;
				}

				dispose();
			}
		});
		add(confirm, consConfirm);

		pack();
		setLocationRelativeTo(parent);
		setVisible(true);

	}
}

package org.cytoscape.inseq.internal.imageselection;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class SelectionWindow extends JDialog {
	
	private ZoomPane zp;
	private GridBagConstraints consPanel;
	
	public SelectionWindow(final JFrame parent)
	{
		super(parent, "Select Region", false);
		this.setPreferredSize(new Dimension(400,400));
		
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		
		consPanel = new GridBagConstraints(0,0,2,1,0.1,1,GridBagConstraints.SOUTH,1,new Insets(0,0,0,0), 1,1);
		ImagePane ip = new ImagePane(ImagePane.getImageFile("/home/jrs/Pictures/17.jpg"));
		zp = new ZoomPane(ip);
		zp.setVisible(true);
		add(zp, consPanel);

		
		GridBagConstraints consBrowse = new GridBagConstraints(0,1,1,1,0.1,0,GridBagConstraints.SOUTH,0,new Insets(4,4,4,4), 1,1);
		JButton browse = new JButton("Change Image");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();

				int returnVal = fc.showOpenDialog(parent);
				
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					changeImage(fc.getSelectedFile().getAbsolutePath());
				}
			}
		});
		add(browse, consBrowse);
		
		GridBagConstraints consInfo = new GridBagConstraints(1,1,1,1,0.1,0,GridBagConstraints.SOUTH,0,new Insets(4,4,4,4), 1,1);
		JButton info = new JButton("Show Info");
		info.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//zp.toggleInfo();
			}
		});
		add(info, consInfo);
		
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
	}
	private void changeImage(String path)
	{
		ImagePane ip = new ImagePane(ImagePane.getImageFile(path));
		zp.updateViewport(ip);
		repaint();
	}
}

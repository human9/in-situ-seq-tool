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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class ImportDialog extends JDialog {
	
	private static final long serialVersionUID = 262657186016113345L;
	JTextField input;
	boolean listenersActive = true;
	private InseqActivator ia;

	ImportDialog(final JFrame parent, final InseqActivator ia) {

		super(parent, "Inseq Importer", true);
		this.ia = ia;
		setPreferredSize(new Dimension(300, 160));
		GridBagLayout gbl = new GridBagLayout();
		getContentPane().setLayout(gbl);

		GridBagConstraints consLabel = new GridBagConstraints(0, 0, 2, 1, 0.8, 0.1, GridBagConstraints.CENTER, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JLabel label = new JLabel("In situ sequencing data importer");
		add(label, consLabel);

		GridBagConstraints consInput = new GridBagConstraints(0, 1, 1, 1, 0.8, 0.1, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 1, 1);
		input = new JTextField("Data to import");
		input.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (listenersActive)
					input.setText("");
				listenersActive = false;
			}
		});
		input.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (listenersActive)
					input.setText("");
				listenersActive = false;
			}
		});
		add(input, consInput);

		GridBagConstraints consBrowse = new GridBagConstraints(1, 1, 1, 1, 0.1, 0.1, GridBagConstraints.CENTER, 0,
				new Insets(4, 4, 4, 4), 1, 1);
		JButton browse = new JButton("Browse...");
		browse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();

				int returnVal = fc.showOpenDialog(parent);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					input.setText(fc.getSelectedFile().getAbsolutePath());
					listenersActive = false;
				}
			}
		});
		add(browse, consBrowse);

		GridBagConstraints consCancel = new GridBagConstraints(0, 2, GridBagConstraints.RELATIVE,
				GridBagConstraints.RELATIVE, 0.1, 0.1, GridBagConstraints.SOUTHWEST, 0, new Insets(4, 4, 4, 4), 1, 1);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		add(cancel, consCancel);

		GridBagConstraints consConfirm = new GridBagConstraints(1, 2, GridBagConstraints.RELATIVE,
				GridBagConstraints.RELATIVE, 0.1, 0.1, GridBagConstraints.SOUTHEAST, 0, new Insets(4, 4, 4, 4), 1, 1);
		JButton confirm = new JButton("Import");
		confirm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File raw = new File(input.getText());
				FileReader in;
				try {
					in = new FileReader(raw);
				} catch (FileNotFoundException x) {
					JOptionPane.showMessageDialog(null, "File not found", "Error", JOptionPane.WARNING_MESSAGE);
					return;
				}
				CyNetwork net = importFile(in);
				if (net != null)
				{
					ia.inseqNetwork = net;
					dispose();
				}
			}
		});
		add(confirm, consConfirm);

		pack();
		setLocationRelativeTo(parent);
		setVisible(true);

	}

	public CyNetwork importFile(FileReader csv) {
		CSVParser inseqParser;
		try {
			inseqParser = CSVFormat.EXCEL.withHeader().parse(csv);
		} catch (IOException e) {
			return null;
		}
		if (inseqParser.getHeaderMap().containsKey("grid_ID")) {
			// import things
			System.out.println("This is valid");
		} else {
			JOptionPane.showMessageDialog(this, "Selected file is not a valid inseq CSV.", "Import Error", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		
		// create network to store the grids in
		CyNetwork CSVNet = ia.networkFactory.createNetwork();
		CSVNet.getRow(CSVNet).set(CyNetwork.NAME, "test network");
		// create table to store gene, tumour, and other info in
		CyTable CSVTable = CSVNet.getDefaultNodeTable();
		List<String> geneNames = new ArrayList<String>();
		for (String name : inseqParser.getHeaderMap().keySet())
		{
			if(name.charAt(0) == '*')
			{
				geneNames.add(name);
				System.out.println("Found gene name: " + name.substring(1));
				CSVTable.createColumn(name.substring(1), Integer.class, false);
			}
		}

		String x = "grid_center_X";
		String y = "grid_center_Y";
		CSVTable.createColumn(x, Double.class, false);
		CSVTable.createColumn(y, Double.class, false);
		
		double maxX = 0;
		double maxY = 0;
		for (CSVRecord record : inseqParser)
		{
			CyNode node = CSVNet.addNode();
			CyRow gridRow = CSVTable.getRow(node.getSUID());
			
			for(String name : geneNames)
			{
				gridRow.set(name.substring(1), Integer.parseInt(record.get(name)));
				gridRow.set(CyNetwork.NAME, record.get("grid_ID"));
				gridRow.set(x, Double.parseDouble(record.get(x)));
				gridRow.set(y, Double.parseDouble(record.get(y)));

			}
			double tx = Double.parseDouble(record.get("grid_center_X"));
			double ty = Double.parseDouble(record.get("grid_center_Y"));
			if(tx > maxX)
				maxX = tx;
			if(ty > maxY)
				maxY = ty;
		}
		int numRow = (int)Math.ceil(maxX / 400d);
		int numCol = (int)Math.ceil(maxY / 400d);
		System.out.println("Assuming grid of " + numRow + " by " + numCol);
		ia.gridSize = new Dimension(numRow, numCol);
		
		CyNetworkView testView = ia.networkViewFactory.createNetworkView(CSVNet);
		for (CyNode node : CSVNet.getNodeList())
		{
			View<CyNode> nv = testView.getNodeView(node);
			CyRow gridRow = CSVTable.getRow(node.getSUID());
			nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, gridRow.get(x, Double.class));
			nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, gridRow.get(y, Double.class));
		}
		ia.networkManager.addNetwork(CSVNet);
		ia.networkViewManager.addNetworkView(testView);

		return CSVNet;
	}
}

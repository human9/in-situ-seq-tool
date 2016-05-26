package org.cytoscape.inseq.internal.dataimport;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.cytoscape.inseq.internal.InseqActivator;
import org.cytoscape.inseq.internal.TypeNetwork;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualStyle;

public class ImportDialog extends JDialog {

	private static final long serialVersionUID = 262657186016113345L;
	JTextField input;
	boolean listenersActive = true;
	double xyScale = 1;
	private InseqActivator ia;
	private String[] integerColumnNames = { "grid_ID", "population", "tumour" };
	private String[] doubleColumnNames = { "SNEx", "SNEy", "grid_center_X", "grid_center_Y" };
	boolean raw = false;

	ImportDialog(final InseqActivator ia) {

		super(ia.swingAppAdapter.getCySwingApplication().getJFrame(), "Inseq Importer", true);
		this.ia = ia;
		setPreferredSize(new Dimension(300, 180));
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

				int returnVal = fc.showOpenDialog(ia.swingAppAdapter.getCySwingApplication().getJFrame());

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					input.setText(fc.getSelectedFile().getAbsolutePath());
					listenersActive = false;
				}
			}
		});
		add(browse, consBrowse);

		GridBagConstraints consCancel = new GridBagConstraints(0, 4, GridBagConstraints.RELATIVE,
				GridBagConstraints.RELATIVE, 0.1, 0.1, GridBagConstraints.SOUTHWEST, 0, new Insets(4, 4, 4, 4), 1, 1);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		add(cancel, consCancel);
		
		GridBagConstraints consScaler = new GridBagConstraints(1, 3, 1,1, 0.1, 0.1, GridBagConstraints.NORTHEAST, 0, new Insets(4, 4, 4, 4), 1, 1);
		final JSpinner scaler = new JSpinner(new SpinnerNumberModel(1d, 0d, 100d, 0.1d));
		scaler.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				xyScale = (Double)scaler.getValue();
			}
		});
		add(scaler, consScaler);

		GridBagConstraints consRaw = new GridBagConstraints(1, 2, 1,1, 0.1, 0.1, GridBagConstraints.SOUTHEAST, 0, new Insets(4, 4, 4, 4), 1, 1);
		JCheckBox isRaw = new JCheckBox("raw XY");
		isRaw.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				raw = !raw;
				System.out.println(raw);

			}
		});

		GridBagConstraints consConfirm = new GridBagConstraints(1, 4, GridBagConstraints.RELATIVE,
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
				if (net != null) {
					ia.inseqNetwork = net;
					dispose();
				}
			}
		});
		add(confirm, consConfirm);
		add(isRaw, consRaw);

		pack();
		setLocationRelativeTo(ia.swingAppAdapter.getCySwingApplication().getJFrame());
		setVisible(true);

	}

	public CyNetwork importFile(FileReader csv) {
		CSVParser inseqParser;
		try {
			inseqParser = CSVFormat.EXCEL.withHeader().parse(csv);
		} catch (IOException e) {
			return null;
		}

		if(raw)
		{

			CyNetwork rawNet = ia.appAdapter.getCyNetworkFactory().createNetwork();
			rawNet.getRow(rawNet).set(CyNetwork.NAME, "raw data");
//			CyTable rawTable = rawNet.getDefaultNodeTable();
			Map<Point2D.Double, String> transcripts = new HashMap<Point2D.Double, String>();
			ArrayList<String> names = new ArrayList<String>();
//			rawTable.createColumn("x", Double.class, false);
//			rawTable.createColumn("y", Double.class, false);
			for(CSVRecord record : inseqParser)
			{
				String name = record.get("name");
				if(name.equals("NNNN"))
					continue;
				if(!names.contains(name))
					names.add(name);

				Double x = Double.parseDouble(record.get("global_X_pos"));
				Double y = Double.parseDouble(record.get("global_Y_pos"));
				transcripts.put(new Point2D.Double(x * xyScale, y * xyScale), name);
/*				
				CyNode node = rawNet.addNode();ca
				CyRow row = rawTable.getRow(node.getSUID());

				row.set(CyNetwork.NAME, name);
				row.set("x", x);
				row.set("y", y);*/
				
			}
			ia.transcripts = transcripts;		
			ia.selTranscripts = transcripts;		
/*
			CyNetworkView view = ia.networkViewFactory.createNetworkView(rawNet);
			for (CyNode node : rawNet.getNodeList()) {
				View<CyNode> nv = view.getNodeView(node);
				CyRow row = rawTable.getRow(node.getSUID());
				nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, row.get("x", Double.class));
				nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, row.get("y", Double.class));
			}

			ia.networkManager.addNetwork(rawNet);
			ia.networkViewManager.addNetworkView(view);
			view.updateView();*/
			ia.tn = new TypeNetwork(ia);
			return rawNet;
		}

		// Iterate through expected names and check that they are present in the
		// CSV file
		Map<String, Integer> hMap = inseqParser.getHeaderMap();
		for (String name : doubleColumnNames) {
			if (!hMap.containsKey(name)) {
				JOptionPane.showMessageDialog(this, "Selected file is not a valid inseq CSV: " + name + " not found!",
						"Import Error", JOptionPane.WARNING_MESSAGE);
				return null;
			}
		}

		// create network to store the grids in
		CyNetwork CSVNet = ia.appAdapter.getCyNetworkFactory().createNetwork();
		CSVNet.getRow(CSVNet).set(CyNetwork.NAME, "test network");
		// create table to store gene, tumour, and other info in
		CyTable CSVTable = CSVNet.getDefaultNodeTable();
		ia.geneNames = new ArrayList<String>();
		for (String name : inseqParser.getHeaderMap().keySet()) {
			if (name.charAt(0) == '*') {
				ia.geneNames.add(name);
				System.out.println("Found gene name: " + name.substring(1));
				CSVTable.createColumn(name.substring(1), Integer.class, false);
			}
		}

		for (String name : doubleColumnNames) {
			CSVTable.createColumn(name, Double.class, false);
		}
		for (String name : integerColumnNames) {
			CSVTable.createColumn(name, Integer.class, false);
		}

		double maxX = 0;
		double maxY = 0;
		for (CSVRecord record : inseqParser) {
			CyNode node = CSVNet.addNode();
			CyRow gridRow = CSVTable.getRow(node.getSUID());

			gridRow.set(CyNetwork.NAME, record.get("grid_ID"));
			for (String name : doubleColumnNames) {
				gridRow.set(name, Double.parseDouble(record.get(name)));
			}
			for (String name : integerColumnNames) {
				gridRow.set(name, Integer.parseInt(record.get(name)));
			}
			for (String name : ia.geneNames) {
				gridRow.set(name.substring(1), Integer.parseInt(record.get(name)));
			}

			double tx = Double.parseDouble(record.get("grid_center_X"));
			double ty = Double.parseDouble(record.get("grid_center_Y"));

			if (tx > maxX)
				maxX = tx;
			if (ty > maxY)
				maxY = ty;
		}
		int numRow = (int) Math.ceil(maxX / 400d);
		int numCol = (int) Math.ceil(maxY / 400d);
		System.out.println("Assuming grid of " + numRow + " by " + numCol);
		ia.gridSize = new Dimension(numRow, numCol);

		CyNetworkView view = ia.appAdapter.getCyNetworkViewFactory().createNetworkView(CSVNet);
		for (CyNode node : CSVNet.getNodeList()) {
			View<CyNode> nv = view.getNodeView(node);
			CyRow gridRow = CSVTable.getRow(node.getSUID());
			nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, gridRow.get("grid_center_X", Double.class));
			nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, gridRow.get("grid_center_Y", Double.class));
		}
		view.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, maxX / 2);
		view.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, maxY / 2);

		VisualStyle vs = ia.appAdapter.getVisualStyleFactory().createVisualStyle("Inseq Style");
		vs.setDefaultValue(BasicVisualLexicon.NODE_SIZE, 100d);
		vs.setDefaultValue(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT, Color.BLACK);
		for (VisualStyle style : ia.appAdapter.getVisualMappingManager().getAllVisualStyles()) {
			if (style.getTitle() == "Inseq Style") {
				ia.appAdapter.getVisualMappingManager().removeVisualStyle(style);
				break;
			}
		}
		ia.appAdapter.getVisualMappingManager().addVisualStyle(vs);
		vs.apply(view);
		ia.appAdapter.getCyNetworkManager().addNetwork(CSVNet);
		ia.appAdapter.getCyNetworkViewManager().addNetworkView(view);
		ia.inseqView = view;
		ia.inseqTable = CSVTable;
		double scale = view.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR).doubleValue();
		view.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scale/numCol);

		view.updateView();

		return CSVNet;
	}
}

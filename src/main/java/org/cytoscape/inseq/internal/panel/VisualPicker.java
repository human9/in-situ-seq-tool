package org.cytoscape.inseq.internal.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.gl.JqadvPanel;

public class VisualPicker extends JDialog implements ChangeListener
{

    private static final long serialVersionUID = 8374L;

    private InseqSession session;
    private JColorChooser chooser;
    private JqadvPanel panel;
    
    // Currently selected transcript type
    private Integer type;
    
    private int symbol;
    private List<SymbolTile> tiles;
    
    private Color colour;

    public VisualPicker(JFrame parent, InseqSession s, JqadvPanel jqv, int t) {

        super(parent, ModalityType.APPLICATION_MODAL);
        this.session = s;
        this.type = t;
        this.panel = jqv;
        
        colour = session.getGeneColour(type);   
        symbol = session.getGeneSymbol(type);
        this.setTitle("Editing " + session.name(type) + " appearance");

        setMinimumSize(new Dimension(100,100));
        setPreferredSize(new Dimension(600, 460));
        getRootPane().setBorder(new EmptyBorder(2,2,2,2));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                responseCancel();
            }
        });

        chooser = new JColorChooser();
        chooser.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Colour"));
        add(chooser, BorderLayout.PAGE_START);
        chooser.setColor(colour);
        chooser.getSelectionModel().addChangeListener(this);

        JPanel symbols = new JPanel();
        symbols.setLayout(new FlowLayout());
        
        tiles = new ArrayList<SymbolTile>();
        for(int i = 0; i < s.getSymbolList().size(); i++) {
            SymbolTile tile = new SymbolTile(i);
            symbols.add(tile);
            tiles.add(tile);
        }
        tiles.get(session.getGeneSymbol(type)).select();

        symbols.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Symbol"));
        add(symbols, BorderLayout.CENTER);

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

        add(responses, BorderLayout.PAGE_END);

        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void responseOk() {
        session.setGeneColour(type, colour);
        session.setGeneSymbol(type, symbol);
        session.refreshStyle();
        panel.getGL().changeColour(type, session.getGeneColour(type));
        dispose();
    }

    private void responseCancel() {
        panel.getGL().changeColour(type, session.getGeneColour(type));
        panel.getGL().changeSymbol(type, session.getGeneSymbol(type));
        dispose();
    }

    private void clearTiles() {
        for(SymbolTile tile : tiles) {
            tile.unselect();
        }
    }

    private void setSymbol(int index) {
        clearTiles();
        symbol = index;
        tiles.get(symbol).select();
        panel.getGL().changeSymbol(type, symbol);
    }

    public void stateChanged(ChangeEvent e) {
        colour = chooser.getColor();
        panel.getGL().changeColour(type, colour);
    }

    private class SymbolTile extends JPanel {

        private static final long serialVersionUID = -2023035L;

        private int index;

        public SymbolTile(int i) {
            setPreferredSize(new Dimension(40, 40));
            this.index = i;
            this.setBorder(new LineBorder(Color.BLACK, 2));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    setSymbol(index);
                }
            });
        }
        
        public void select() {
            setBorder(new LineBorder(Color.RED, 2));
        }

        public void unselect() {
            setBorder(new LineBorder(Color.BLACK, 2));
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D gr = (Graphics2D)g;
            gr.setColor(Color.BLACK);
            gr.fillRect(0, 0, getWidth(), getHeight());
            gr.setColor(Color.BLACK);
            gr.drawImage(session.getSymbolList().get(index), 10, 10, 20, 20, Color.BLACK, null);
        }
    }
}

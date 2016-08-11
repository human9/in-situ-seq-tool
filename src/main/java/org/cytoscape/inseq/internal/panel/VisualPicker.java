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
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.tissueimage.ImagePane;
import org.cytoscape.inseq.internal.util.SymbolFactory;
import org.cytoscape.inseq.internal.util.SymbolFactory.Symbol;

public class VisualPicker extends JDialog implements ChangeListener
{

    private InseqSession session;
    private ImagePane p;
    private Color colour;
    private Color newColour;
    private String name;
    private JColorChooser chooser;
    private Color old;
    private HashMap<Symbol, SymbolTile> tiles;
    private Symbol symbol;
    private Symbol oldSymbol;

    public VisualPicker(ImagePane p, InseqSession s) {

        super(SwingUtilities.getWindowAncestor(p), ModalityType.APPLICATION_MODAL);
        this.session = s;
        this.p = p;

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

        JPanel symbols = new JPanel();
        symbols.setLayout(new FlowLayout());
        
        tiles = new HashMap<Symbol, SymbolTile>();
        for(Symbol sym : Symbol.values()) {
            SymbolTile tile = new SymbolTile(sym);
            symbols.add(tile);
            tiles.put(sym, tile);

        }
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
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(p));
    }

    @Override
    public void setVisible(boolean isVisible) {

        if(isVisible) {
            setSelected(name);
            chooser.getSelectionModel().addChangeListener(this);
        }
        else {
            chooser.getSelectionModel().removeChangeListener(this);
        }

        super.setVisible(isVisible);

    }

    private void responseOk() {
        setVisible(false);
    }

    private void responseCancel() {
        session.setGeneColour(name, old);
        session.setGeneSymbol(name, oldSymbol);
        p.forceRepaint();
        session.refreshStyle();
        setVisible(false);
    }

    public void setSelected(String name) {
        colour = session.getGeneColour(name);   
        symbol = session.getGeneSymbol(name);
        oldSymbol = symbol;
        for(SymbolTile tile : tiles.values()) {
            tile.unselect();
        }
        tiles.get(symbol).select();
        this.name = name;
        this.old = session.getGeneColour(name);
        
        chooser.setColor(colour);
        this.setTitle("Editing " + name + " appearance");
    }

    public void setSymbol(Symbol sym) {
        tiles.get(symbol).unselect();
        symbol = sym;
        tiles.get(symbol).select();
        session.setGeneSymbol(name, symbol);
        p.forceRepaint();
    }

    public void stateChanged(ChangeEvent e) {
        newColour = chooser.getColor();
        session.setGeneColour(name, newColour);

        // don't actually need this - could do it more intelligently
        p.forceRepaint();
        session.refreshStyle();
    }

    class SymbolTile extends JPanel {

        private Symbol sym;

        public SymbolTile(Symbol sym) {
            setPreferredSize(new Dimension(40, 40));
            this.sym = sym;
            this.setBorder(new LineBorder(Color.BLACK, 2));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    setSymbol(sym);
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
            gr.setColor(Color.WHITE);
            gr.fillRect(0, 0, getWidth(), getHeight());
            gr.setColor(Color.BLACK);
            gr.draw(SymbolFactory.makeSymbol(sym, 11, 11, 16, 16));
        }
    }
}

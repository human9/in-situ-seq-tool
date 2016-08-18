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
import org.cytoscape.inseq.internal.util.SymbolFactory.Symbol;

public class VisualPicker extends JDialog implements ChangeListener
{

    private InseqSession session;
    private Color colour;
    private Color newColour;
    private JColorChooser chooser;
    private Color old;
    private List<SymbolTile> tiles;
    private int symbol;
    private int oldSymbol;
    private Integer type;
    private JqadvPanel panel;

    public VisualPicker(JqadvPanel jqv, JFrame parent, InseqSession s) {

        super(parent, ModalityType.APPLICATION_MODAL);
        this.session = s;
        this.panel = jqv;

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
        
        tiles = new ArrayList<SymbolTile>();
        for(int i = 0; i < s.getSymbolList().size(); i++) {
            SymbolTile tile = new SymbolTile(i);
            symbols.add(tile);
            tiles.add(tile);
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
        setLocationRelativeTo(parent);
    }

    @Override
    public void setVisible(boolean isVisible) {

        if(isVisible) {
            setSelected(type);
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
        session.setGeneColour(type, old);
        //session.setGeneSymbol(type, oldSymbol);
        //p.forceRepaint();
        session.refreshStyle();
        setVisible(false);
    }

    public void setSelected(Integer type) {
        colour = session.getGeneColour(type);   
        symbol = type;
        oldSymbol = symbol;
        for(SymbolTile tile : tiles) {
            tile.unselect();
        }
        tiles.get(type).select();
        this.type = type;
        this.old = session.getGeneColour(type);
        
        chooser.setColor(colour);
        this.setTitle("Editing " + session.name(type) + " appearance");
    }

    public void setSymbol(int sym) {
        tiles.get(symbol).unselect();
        symbol = sym;
        tiles.get(sym).select();
        panel.updateSymbol(type, sym);
        //session.setGeneSymbol(type, symbol);
        //p.forceRepaint();
    }

    public void stateChanged(ChangeEvent e) {
        newColour = chooser.getColor();
        session.setGeneColour(type, newColour);
        panel.updateColour(type, newColour);

        //session.refreshStyle();
    }

    class SymbolTile extends JPanel {

        private int sym;

        public SymbolTile(int sym) {
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
            gr.drawImage(session.getSymbolList().get(sym), 0, 0, 40, 40, Color.BLACK, null);
        }
    }
}

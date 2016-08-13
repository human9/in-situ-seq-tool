package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.SymbolFactory.Symbol;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualStyle;

import edu.wlu.cs.levy.CG.KDTree;

/** Encapsulates the components generated during a session.
 *  It should be possible to serialize this class for restoring sessions.
 *  @author John Salamon
 */
public class InseqSession {
    
    public KDTree<Transcript> tree;
    public Map<Integer, List<Integer>> edgeSelection;
    public List<Integer> nodeSelection;
    public Dimension min;

    private Shape selection;
    private VisualStyle style;

    private CyAppAdapter CAA;
    private List<TypeNetwork> networks;
    private List<Transcript> raw;
    public List<String> names;
    private Integer selectedNetwork;

    public class Gene {
        String name;
        Color  color;
        Symbol symbol;
    }
    private List<Gene> genes;

    /** Components of a session are initialised as required.
     *  A session is created when the user imports data.
     */
    public InseqSession(List<String>       names,
                        List<Transcript>   transcripts,
                        KDTree<Transcript> tree,
                        CyAppAdapter       CAA)
                        
    {
        this.tree = tree;
        this.CAA = CAA;
        this.names = names;
        this.raw = transcripts;
        this.min = getMinimumPlotSize();

        // Give each gene a unique colour as well spaced as possible.
        genes = new ArrayList<Gene>();
        int x = 1;
        for(String name : names) {
            Gene g = new Gene();
            int i = (int)(x++*(360d/names.size()));
            g.name = name;
            g.color = Color.getHSBColor(((i)%360)/360f, 1, 1);
            g.symbol = Symbol.DIAMOND;
            genes.add(g);
        }

        networks = new ArrayList<TypeNetwork>();

        // Initialise network style
        updateStyle();

    }

    public List<Gene> getGenes() {
        return genes;
    }
    
    /**
     *  Generate a name with consistent ordering.
     *  Useful for map key generation
     */
    public String generateName(Transcript t1, Transcript t2) {
        Transcript[] ordered = orderTranscripts(t1, t2);
        return name(ordered[0].type) + "-" + name(ordered[1].type);
    }
    
    /**
     * Get consistant order for two transcripts.
     */
    public Transcript[] orderTranscripts(Transcript t1, Transcript t2) {

        String n1 = name(t1.type);
        String n2 = name(t2.type);

        if(n1.compareTo(n2) < 0) {
            return new Transcript[] {t1, t2};
        } else {
            return new Transcript[] {t2, t1};
        }
    }

    public String name(Integer type) {
        return genes.get(type).name;
    }

    public void addNetwork(TypeNetwork n, Double distance, Double cutoff) {
        if(!(networks.contains(n))) {
            networks.add(n);
        }
        else {
            String name = n.getNetwork().getRow(n.getNetwork())
                .get(CyNetwork.NAME, String.class);
            CAA.getCyNetworkManager().destroyNetwork(n.getNetwork());
            n.setNetwork(CAA.getCyNetworkFactory().createNetwork());
            if(name != null)
            {
                n.getNetwork().getRow(n.getNetwork())
                    .set(CyNetwork.NAME, name);
            }
            n.setDistance(distance);
            n.setCutoff(cutoff);
        }
    }

    public TypeNetwork getNetwork(Integer index) {
        if (index == null) return null;
        try {
            return networks.get(index);
        }
        catch(IndexOutOfBoundsException e) {
            return null;
        }
    }

    public List<String> getNodeList() {
        return new ArrayList<String>(names);
    }

    public List<Transcript> getRaw() {
        return raw;
    }

    public List<TypeNetwork> getNetworkList() {
        return networks;
    }

    public void setSelectedNetwork(TypeNetwork n) {
        if(n != null) {
            selectedNetwork = networks.indexOf(n);
            CyNetworkViewManager CNVM = CAA.getCyNetworkViewManager();
            CNVM.addNetworkView(n.view);
            n.view.updateView();
        }
    }
    
    public Integer getSelectedNetwork() {
        return selectedNetwork;
    }

    public void setGeneColour(Integer type, Color color) {
        genes.get(type).color = color;
    }

    public Color getGeneColour(Integer type) {
        return genes.get(type).color;
    }

    public void setGeneSymbol(Integer type, Symbol symbol) {
        genes.get(type).symbol = symbol;
    }

    public Symbol getGeneSymbol(Integer type) {
        return genes.get(type).symbol;
    }



    public void refreshStyle() {
        ViewStyler.updateColours(style, this, CAA);
    }

    public void updateStyle() {
        style = ViewStyler.initStyle(this, CAA);

        CAA.getVisualMappingManager().setCurrentVisualStyle(style);

        for(TypeNetwork net : networks) {
            for(CyNetworkView view : CAA.getCyNetworkViewManager()
                    .getNetworkViews(net.getNetwork()))
            {
                style.apply(view);

                CAA.getVisualMappingManager().setVisualStyle(style, view);
                view.updateView();
            }
        }
    }

    public VisualStyle getStyle() {
        return style;
    }

    public void setSelection(Shape shape) {
        this.selection = shape;
    }

    public Shape getSelection() {
        return selection;
    }

    private Dimension getMinimumPlotSize() {
        double maxX = 0, maxY = 0;
        for(Transcript t : raw) {
            if (t.pos.x > maxX) maxX = t.pos.x;
            if (t.pos.y > maxY) maxY = t.pos.y;
        }
        return new Dimension((int)Math.ceil(maxX), (int)Math.ceil(maxY));
    }

}

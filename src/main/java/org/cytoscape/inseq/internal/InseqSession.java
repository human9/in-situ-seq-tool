package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.ParseUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;

import edu.wlu.cs.levy.CG.KDTree;

/** Encapsulates the components generated during a session.
 *  It should be possible to serialize this class for restoring sessions.
 *  @author John Salamon
 */
public class InseqSession {
    
    public KDTree<Transcript> tree;
    public Map<Integer, List<Integer>> edgeSelection = new HashMap<Integer, List<Integer>>();
    public List<Integer> nodeSelection = new ArrayList<Integer>();
    public Dimension min;

    private Shape selection;
    private VisualStyle style;

    private CyAppAdapter CAA;
    private List<TypeNetwork> networks = new ArrayList<TypeNetwork>();;
    private List<Transcript> raw;
    public List<String> names;
    private Integer selectedNetwork;
    private List<BufferedImage> symbols;

    /**
     * Defines the appearance of a certain gene.
     */
    public class Gene implements Comparable<Gene>{
        // The name of the gene
        public String name;
        // The colour to be displayed
        public Color  color;
        // The position of the symbol in the symbols array
        public Integer symbol;

        @Override
        public int compareTo(Gene g) {
            return(name.compareTo(g.name));
        }
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

        // Initialise symbols
        BufferedImage pointSprites
            = ParseUtil.getImageResource("/texture/sprite_sheet.png");
        int len = pointSprites.getWidth() / pointSprites.getHeight();
        int size = pointSprites.getHeight();
        symbols = new ArrayList<BufferedImage>();
        for(int i = 0; i < len; i++) {
            symbols.add(pointSprites.getSubimage(i*size, 0, size, size)); 
        }

        // Give each gene a unique colour as well spaced as possible.
        genes = new ArrayList<Gene>();
        List<String> alphabeticalGenes = new ArrayList<String>(names);
        Collections.sort(alphabeticalGenes);
        for(String name : names) {
            int x = alphabeticalGenes.indexOf(name);
            Gene g = new Gene();
            int i = (int)((x+1)*(360d/names.size()));
            g.name = name;
            g.color = Color.getHSBColor(((i)%360)/360f, 1, 1);
            g.symbol = x%symbols.size();
            genes.add(g);
        }

        networks = new ArrayList<TypeNetwork>();

        // Initialise network style
        updateStyle();

    }

    public void setSymbolList(List<BufferedImage> list) {
        this.symbols = list;
    }

    public List<BufferedImage> getSymbolList() {
        return this.symbols;
    }

    public List<Gene> getGenes() {

        List<Gene> genesAlphabetical = new ArrayList<Gene>(genes);
        Collections.sort(genesAlphabetical);
        return genesAlphabetical;
    }

    public int getNumGenes() {
        return genes.size();
    }

    private boolean showAllSelected;
    public void setShowAll(boolean b) {
        showAllSelected = b;
    }

    public boolean isActive(Transcript t) {
        TypeNetwork sel = getSelectedNetwork();
        if(sel == null) return true;

        if(nodeSelection != null 
                && nodeSelection.contains(t.type)
                && showAllSelected) return true;

        if(t.getNeighboursForNetwork(sel) == null 
                || t.getNeighboursForNetwork(sel).size() < 1
                || t.getSelection(sel) != sel.getSelection() 
                || (!edgeSelection.keySet().contains(t.type)) 
                && (!nodeSelection.contains(t.type))) return false;
        
        for(Transcript n : t.getNeighboursForNetwork(sel)) {
            if(edgeSelection.get(t.type) != null 
                    && edgeSelection.get(t.type).contains(n.type)) 
                    return true;
            if(nodeSelection.contains(t.type) && n.type == t.type) 
                    return true;
        }

        return false;
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
        selectedNetwork = networks.indexOf(n);
        //CyNetworkViewManager CNVM = CAA.getCyNetworkViewManager();
        n.getView().updateView();
        CAA.getCyEventHelper().flushPayloadEvents();
        CAA.getCyApplicationManager().setCurrentNetworkView(n.getView());
        //CNVM.addNetworkView(n.view);
    }
    
    public TypeNetwork getSelectedNetwork() {
        try {
            return networks.get(selectedNetwork);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void setGeneColour(Integer type, Color color) {
        genes.get(type).color = color;
    }

    public Color getGeneColour(Integer type) {
        return genes.get(type).color;
    }

    public void setGeneSymbol(Integer type, Integer symbol) {
        genes.get(type).symbol = symbol;
    }

    public Integer getGeneSymbol(Integer type) {
        return genes.get(type).symbol;
    }



    public void refreshStyle() {
        ViewStyler.updateColours(style, this, CAA);
    }

    public void updateStyle() {
        style = ViewStyler.initStyle(this, CAA);

        //CAA.getVisualMappingManager().setCurrentVisualStyle(style);

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

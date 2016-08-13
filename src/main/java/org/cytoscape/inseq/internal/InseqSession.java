package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.ParseUtil;
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
	public Map<String, List<String>> edgeSelection;
	public List<String> nodeSelection;
	public Dimension min;

	private Shape selection;
	private VisualStyle style;

	private CyAppAdapter CAA;
	private List<TypeNetwork> networks;
	private List<Transcript> raw;
	private List<Integer> originalNames;
	private Integer selectedNetwork;

	private Map<String, Integer> geneCounts;
	private Map<String, Color> geneColours;
	private Map<String, Symbol> geneSymbols;
	private List<String> nodes;

	/** Components of a session are initialised as required.
	 *  A session is created when the user imports data.
	 */
	public InseqSession(KDTree<Transcript> t,
                        List<Transcript>   raw,
                        CyAppAdapter       CAA)
    {
		this.tree = t;
		this.CAA = CAA;
		this.raw = raw;
		this.min = getMinimumPlotSize();

		originalNames = new ArrayList<Integer>();
		nodes = new ArrayList<String>();
		for(Transcript tr : raw) {
			if(!nodes.contains(tr.name)) {
				nodes.add(tr.name);
			}
			originalNames.add(nodes.indexOf(tr.name));
		}

		// List and count the genes present.
		this.geneCounts = ParseUtil.getGenes(tree);

		// Give each gene a unique colour as well spaced as possible.
		geneColours = new HashMap<String, Color>();
        geneSymbols = new HashMap<String, Symbol>();
		int x = 1;
		for(String name : geneCounts.keySet()) {
			int i = (int)(x*(360d/geneCounts.keySet().size()));
			geneColours.put(name, Color.getHSBColor(((i)%360)/360f, 1, 1));
            geneSymbols.put(name, Symbol.DIAMOND);
			x++;
		}

		networks = new ArrayList<TypeNetwork>();

		// Initialise network style
		updateStyle();

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
		return new ArrayList<String>(nodes);
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

	public void setGeneColour(String name, Color color) {
		geneColours.put(name, color);
	}

	public Color getGeneColour(String name) {
		return geneColours.get(name);
	}

    public void setGeneSymbol(String name, Symbol symbol) {
        geneSymbols.put(name, symbol);
    }

    public Symbol getGeneSymbol(String name) {
        return geneSymbols.get(name);
    }



	public void refreshStyle() {
        ViewStyler.updateColours(style, geneColours, CAA);
    }

	public void updateStyle() {
		style = ViewStyler.initStyle(geneColours, CAA);

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

	public Integer geneCount(String name) {
		return geneCounts.get(name);
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

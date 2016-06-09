package org.cytoscape.inseq.internal;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.typenetwork.TypeNetwork;
import org.cytoscape.inseq.internal.util.ParseUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.vizmap.VisualStyle;

import edu.wlu.cs.levy.CG.KDTree;

/** Encapsulates the components generated during a session.
 *  It should be possible to serialize this class for restoring sessions.
 *  @author John Salamon
 */
public class InseqSession {
	
	public KDTree<Transcript> tree;
	public CyNetwork network;
	public List<String> edgeSelection;
	public Rectangle rectangleSelection;
	private VisualStyle style;

	private CyAppAdapter CAA;
	private List<TypeNetwork> networks;

	private Map<String, Integer> geneCounts;
	private Map<String, Color> geneColours;

	/** Components of a session are initialised as required.
	 *  A session is created when the user imports data.
	 */
	public InseqSession(KDTree<Transcript> t, CyAppAdapter CAA) {
		this.tree = t;
		this.CAA = CAA;


		// List and count the genes present.
		this.geneCounts = ParseUtil.getGenes(tree);

		// Give each gene a unique colour as well spaced as possible.
		geneColours = new HashMap<String, Color>();
		int x = 1;
		for(String name : geneCounts.keySet()) {
			int i = (int)(x*(360d/geneCounts.keySet().size()));
			geneColours.put(name, Color.getHSBColor(((i)%360)/360f, 1, 1));
			x++;
		}

		// Initialise network style
		updateStyle();

		networks = new ArrayList<TypeNetwork>();
	}

	public void addNetwork(TypeNetwork staleNetwork, TypeNetwork freshNetwork) {
		if(staleNetwork == null)
		{
			networks.add(freshNetwork);
		}
		else {
			networks.set(networks.indexOf(staleNetwork), freshNetwork);
			CAA.getCyNetworkManager().destroyNetwork(staleNetwork.getNetwork());
		}
	}

	public TypeNetwork getNetwork(int index) {
		return networks.get(index);
	}

	public List<TypeNetwork> getNetworkList() {
		return networks;
	}

	public void setGeneColour(String name, Color color) {
		geneColours.put(name, color);
	}

	public Color getGeneColour(String name) {
		return geneColours.get(name);
	}

	public void updateStyle() {
		style = ViewStyler.initStyle(geneColours, CAA);
	}

	public VisualStyle getStyle() {
		return style;
	}

	public Integer geneCount(String name) {
		return geneCounts.get(name);
	}
}

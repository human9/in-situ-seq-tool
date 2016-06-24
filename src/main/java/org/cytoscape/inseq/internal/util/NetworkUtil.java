package org.cytoscape.inseq.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

/** A collection of utilities not included in the Cytoscape API.
 *  @author various
 */
public class NetworkUtil {


	/** Given a network, table, and value, returns a list of nodes that match that value.
	 *  
	 */
	public static Set<CyNode> getNodesWithValue(final CyNetwork net, final CyTable table, final String colname,
			final ArrayList<Integer> values) {
		final Set<CyNode> nodes = new HashSet<CyNode>();
		for (Integer value : values) {
			final Collection<CyRow> matchingRows = table.getMatchingRows(colname, value.toString());
			final String primaryKeyColname = table.getPrimaryKey().getName();
			for (final CyRow row : matchingRows) {
				final Long nodeId = row.get(primaryKeyColname, Long.class);
				if (nodeId == null)
					continue;
				final CyNode node = net.getNode(nodeId);
				if (node == null)
					continue;
				nodes.add(node);
			}
		}
		return nodes;
	}
	
	/** Given a network, table, and name, returns the first node that matches the name.
	 */
	public static CyNode getNodeWithName(final CyNetwork net, final CyTable table, final String name)
	{
		for(CyNode node : net.getNodeList())
		{
			CyRow row  = table.getRow(node.getSUID());
			String nodeName = row.get(CyNetwork.NAME, String.class);
			if (nodeName.equals(name)) return node;
		}
		return null;
	}

	public static int getNonNullInt(Integer i) {
		if(i == null)
			return 0;
		else
			return i.intValue();
	}
}

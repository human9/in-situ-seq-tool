package org.cytoscape.inseq.internal;

import java.util.List;
import java.util.Properties;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.dataimport.ImportAction;
import org.cytoscape.inseq.internal.panel.ControlPanel;
import org.cytoscape.inseq.internal.types.Transcript;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TaskIterator;
import org.osgi.framework.BundleContext;

import edu.wlu.cs.levy.CG.KDTree;

/** Activation and session control class.
 *  It also provides other classes with access to Cytoscape APIs.
 *  @author John Salamon
 */
public class InseqActivator extends AbstractCyActivator {

	private InseqSession session;
	private Properties properties;
	private BundleContext context;

	/** The entry point for app execution.
 	 *  The only immediate visible change should be a new menu option.
	 */
	@Override
	public void start(BundleContext c) throws Exception {

		properties = new Properties();
		context = c;

		ImportAction menuAction = new ImportAction(this);
		registerAllServices(context, menuAction, properties);
	}
	
	public KDTree<Transcript> constructTree(List<Transcript> rawImport) {

		KDTreeBuilder builder = new KDTreeBuilder(rawImport);
		TaskIterator itr = new TaskIterator(builder);     
		getCSAA().getDialogTaskManager().execute(itr);

		return builder.getTree();
	}

	/** Initializes the session.
	 *  This is called by the ImportDialog on successful import.
	 */
	public void initSession(KDTree<Transcript> tree) {
		session = new InseqSession(tree);

		ControlPanel panel = new ControlPanel(this, session);
		registerAllServices(context, panel, properties);

		// Switch to the Inseq control panel.
		CytoPanel cyPanel = getCSAA().getCySwingApplication().getCytoPanel(CytoPanelName.WEST);
		int index = cyPanel.indexOfComponent(panel);
		cyPanel.setSelectedIndex(index);
	}

	/** Returns the current session.
	 */
	public InseqSession getSession() {
		return this.session;
	}

	/** Returns the CyAppAdapter convenience interface.
	 */
	public CyAppAdapter getCAA() {
		return getService(context, CyAppAdapter.class);
	}
	
	/** Returns the CySwingAppAdapter convenience interface.
	 */
	public CySwingAppAdapter getCSAA() {
		return getService(context, CySwingAppAdapter.class);
	}

	@Override
	public void shutDown() {
		super.shutDown();
	}
}

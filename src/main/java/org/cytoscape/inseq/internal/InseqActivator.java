package org.cytoscape.inseq.internal;

import java.util.List;
import java.util.Properties;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.inseq.internal.dataimport.ImportAction;
import org.cytoscape.inseq.internal.panel.MainPanel;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

import edu.wlu.cs.levy.CG.KDTree;

/** Activation and session control class.
 *  It also provides other classes with access to Cytoscape APIs.
 *
 *  @author John Salamon
 */
public class InseqActivator extends AbstractCyActivator {

    // Every unique csv imported will be assigned its own InseqSession
    private Properties properties;
    private BundleContext context;
    private MainPanel panel;

    /** 
     *  The entry point for app execution.
     *  The only immediate visible change should be a new menu option.
     */
    @Override
    public void start(BundleContext c) throws Exception {
        
        properties = new Properties();
        context = c;
        JoglInitializer.unpackNativeLibrariesForJOGL(c);

        ImportAction menuAction = new ImportAction(this);
        registerAllServices(context, menuAction, properties);
    }

    /** 
     *  Initializes the session.
     *  This is called by ImportAction on successful import.
     */
    public void initSession(String filename,
                            List<String> names,
                            List<Transcript> transcripts,
                            KDTree<Transcript> tree) {
        InseqSession s = new InseqSession(names, transcripts, tree, getCAA());

        if(panel == null) {
            panel = new MainPanel(this);
            registerAllServices(context, panel, properties);

            RowsSetListener rsl = new RowsSetListener() {
                @Override
                public void handleEvent(RowsSetEvent e) {
                    if (e.getColumnRecords(CyNetwork.SELECTED) != null) {
                        panel.updateSelectionPanel();
                    }
                }
            };
            registerService(context, rsl, RowsSetListener.class, properties);

            panel.addSession(filename, s);
        }
        else {
            panel.addSession(filename, s);
        }

        // Switch to the Inseq control panel.
        CytoPanel cyPanel = getCSAA().getCySwingApplication()
            .getCytoPanel(CytoPanelName.WEST);
        int index = cyPanel.indexOfComponent(panel);
        cyPanel.setSelectedIndex(index);
    }

    /** 
     *  Returns the CyAppAdapter convenience interface.
     */
    public CyAppAdapter getCAA() {
        return getService(context, CyAppAdapter.class);
    }
    
    /** 
     *  Returns the CySwingAppAdapter convenience interface.
     */
    public CySwingAppAdapter getCSAA() {
        return getService(context, CySwingAppAdapter.class);
    }

    @Override
    public void shutDown() {
        if(panel != null) panel.shutDown();
        super.shutDown();
    }
}

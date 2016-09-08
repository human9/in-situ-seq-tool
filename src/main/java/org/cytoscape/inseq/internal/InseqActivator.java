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
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.ViewChangeRecord;
import org.cytoscape.view.model.events.ViewChangedEvent;
import org.cytoscape.view.model.events.ViewChangedListener;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
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

            ViewChangedListener vcl = new ViewChangedListener() {
                public void handleEvent(ViewChangedEvent<?> e) {
                   for (ViewChangeRecord<?> record : e.getPayloadCollection()) {
                        VisualProperty<?> vp = record.getVisualProperty();
                        if (vp == BasicVisualLexicon.NODE_X_LOCATION || vp == BasicVisualLexicon.NODE_Y_LOCATION) {
                            //System.out.println(vp.getDisplayName() + record.getValue());
                        }
                   }
                }
            };
            registerService(context, vcl, ViewChangedListener.class, properties);
            RowsSetListener rsl = new RowsSetListener() {
                @Override
                public void handleEvent(RowsSetEvent e) {
                    boolean willUpdate = false;
                    for (RowSetRecord record : e.getColumnRecords(CyNetwork.SELECTED)) {
                        if((Boolean)record.getValue() == true) willUpdate = true;
                    }
                    if(willUpdate) {
                        //System.out.println("Updating");
                        panel.updateSelectionPanel();
                    }
                    /*CyTable t = e.getSource();
                    for(CyColumn c : t.getColumns()) {

                          System.out.println(c.getName());
                    }*/

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

    public boolean doesImportExist(String filename) {
        if(panel == null) return false;

        return panel.exists(filename);
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

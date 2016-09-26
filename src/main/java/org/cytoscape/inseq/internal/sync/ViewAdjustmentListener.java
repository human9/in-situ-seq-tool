package org.cytoscape.inseq.internal.sync;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.ViewChangeRecord;
import org.cytoscape.view.model.events.ViewChangedEvent;
import org.cytoscape.view.model.events.ViewChangedListener;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class ViewAdjustmentListener implements ViewChangedListener {

    private InseqSession session;

    private boolean enabled;

    public ViewAdjustmentListener(InseqSession s) {
        this.session = s;
    }

    public void handleEvent(ViewChangedEvent<?> e) {
       if(enabled) {
           for (ViewChangeRecord<?> record : e.getPayloadCollection()) {
                VisualProperty<?> vp = record.getVisualProperty();
                if (vp == BasicVisualLexicon.NODE_X_LOCATION || vp == BasicVisualLexicon.NODE_Y_LOCATION) {
                    //System.out.println(vp.getDisplayName() + record.getValue());
                }
           }
       }
    }

    public void setEnabled(boolean state) {
        enabled = state;
    }
}

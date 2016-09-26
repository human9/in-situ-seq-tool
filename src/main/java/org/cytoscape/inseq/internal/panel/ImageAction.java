package org.cytoscape.inseq.internal.panel;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.cytoscape.inseq.internal.gl.JqadvGL;
import org.cytoscape.inseq.internal.panel.SelectionPanel.Parent;
import org.cytoscape.inseq.internal.util.ParseUtil;
import org.cytoscape.util.swing.DropDownMenuButton;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;

public class ImageAction extends AbstractAction {

    JPopupMenu popup = new JPopupMenu();
    JMenuItem load = new JMenuItem("Load new image...");
    JMenuItem scale = new JMenuItem("Relative scaling...");

    Parent parent;
    JqadvGL gl;

    public ImageAction(String text, Icon i, JqadvGL gl, Parent parent) {
        super(text, i);
        this.gl = gl;
        this.parent = parent;
        load.addActionListener(this);
        scale.addActionListener(this);
        popup.add(load);
        popup.add(scale);

    }

    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == load) {
            JFileChooser fc = new JFileChooser();
            
            // Force TIF support into ImageIO
            IIORegistry reg = IIORegistry.getDefaultInstance();
            reg.registerServiceProvider(new TIFFImageReaderSpi());

            FileFilter filter 
                = new FileNameExtensionFilter("Supported image formats", 
                        ImageIO.getReaderFileSuffixes());
            fc.addChoosableFileFilter(filter);
            fc.setFileFilter(filter);
            
            int returnVal 
                = fc.showOpenDialog(parent.getWindow());
            if (!(returnVal == JFileChooser.APPROVE_OPTION)) return;

            gl.changeImage(
                    ParseUtil.getImageFile(fc.getSelectedFile().getAbsolutePath()));
        }
        else if(e.getSource() == scale) {
            new ScaleDialog();
        }
        else {
            DropDownMenuButton b = (DropDownMenuButton) e.getSource();
            popup.show(b, 0, b.getHeight());
        }

    }

    class ScaleDialog extends JDialog implements PropertyChangeListener {
        
        JOptionPane op;
        JSpinner pointScale;
        double scale;

        public ScaleDialog() {
            super(parent.getWindow(), "Set relative scale", true);

            scale = gl.getImageScale();

            pointScale = new JSpinner(new SpinnerNumberModel(scale, 0d, 100d, 0.01d));
            pointScale.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    gl.setImageScale(((Double)pointScale.getValue()).floatValue());   
                }
            });
            Object[] array = {"Select image scale relative to points:", pointScale};
            Object[] options = {"OK", "Cancel"};
            op = new JOptionPane(array, JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION, null, options,
                    options[0]);
            setContentPane(op);
            op.addPropertyChangeListener(this);
            pack();
            setLocationRelativeTo(parent.getWindow());
            setVisible(true);
        }

        public void propertyChange(PropertyChangeEvent e) {
            Object value = op.getValue();

            if(value.equals("OK")) {
                gl.setImageScale(((Double)pointScale.getValue()).floatValue());   
                setVisible(false);
            }
            else {
                gl.setImageScale((float)scale);   
                setVisible(false);
            }
        }

    }

}

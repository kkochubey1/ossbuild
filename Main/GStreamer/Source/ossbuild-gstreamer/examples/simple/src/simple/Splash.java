/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * Splash.java
 *
 * Created on Feb 16, 2010, 2:08:58 AM
 */

package simple;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import ossbuild.extract.IResourceCallback;
import ossbuild.extract.IResourceProgressListener;
import ossbuild.extract.MissingResourceException;
import ossbuild.extract.ResourceCallback;
import ossbuild.extract.ResourceProgressListenerAdapter;
import ossbuild.extract.Resources;
import ossbuild.gstreamer.Native;

/**
 *
 * @author David
 */
public class Splash extends javax.swing.JDialog {

	//<editor-fold defaultstate="collapsed" desc="Boilerplate">
    /** Creates new form Splash */
    public Splash(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lbl = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Loading...");
        setMinimumSize(new java.awt.Dimension(300, 150));
        setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        setResizable(false);
        setUndecorated(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        lbl.setText("Loading...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        getContentPane().add(lbl, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 3, 2, 3);
        getContentPane().add(progress, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
		init();
	}//GEN-LAST:event_formWindowOpened

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Splash dialog = new Splash(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lbl;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables
	//</editor-fold>
	
	private void init() {
		
		try {
			Native.initialize(
				new ResourceProgressListenerAdapter() {
					@Override
					public void report(int totalNumberOfResources, int totalNumberOfPackages, long totalNumberOfBytes, long numberOfBytesCompleted, int numberOfResourcesCompleted, int numberOfPackagesCompleted, long startTime, long duration, String message) {
						lbl.setText(message);

						double percent = (((double)numberOfResourcesCompleted / (double)totalNumberOfResources) * 100.0D);
						progress.setValue((int)percent);
					}
				},

				new ResourceCallback() {
					@Override
					protected void completed(Resources rsrcs, Object t) {
						try {
							Thread.currentThread().sleep(2000);
							
							SwingUtilities.invokeAndWait(new Runnable() {
								@Override
								public void run() {
									Player dlg = new Player();
									dlg.setVisible(true);
								}
							});

							Splash.this.setVisible(false);
						} catch (InterruptedException ex) {
						} catch (InvocationTargetException ex) {
						}
					}
				}
			);
		} catch(Throwable t) {
			setVisible(false);
			dispose();
			JOptionPane.showMessageDialog(this, "Unable to extract and load GStreamer libraries for this platform or JVM.");
			System.exit(1);
		}
	}
}
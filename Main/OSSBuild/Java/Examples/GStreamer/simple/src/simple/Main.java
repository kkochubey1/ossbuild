package simple;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import ossbuild.NativeResource;
import ossbuild.Sys;

public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		//Sys.initialize(false);
		Sys.initializeRegistry();
		Sys.loadNativeResources(NativeResource.Base);
		Sys.loadNativeResources(NativeResource.XML);
		Sys.loadNativeResources(NativeResource.GLib);
		Sys.loadNativeResources(NativeResource.Images);
		Sys.loadNativeResources(NativeResource.Fonts);
		Sys.loadNativeResources(NativeResource.Graphics);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (UnsupportedLookAndFeelException ex) {
				} catch (ClassNotFoundException ex) {
				} catch (InstantiationException ex) {
				} catch (IllegalAccessException ex) {
				}
				
				Splash dlg = new Splash(null, true);
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				Dimension labelSize = dlg.getSize();
				dlg.setLocation(screenSize.width / 2 - (labelSize.width / 2), screenSize.height / 2 - (labelSize.height / 2));
				dlg.setVisible(true);
			}
		});
	}
}

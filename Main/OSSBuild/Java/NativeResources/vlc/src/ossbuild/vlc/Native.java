
package ossbuild.vlc;

import ossbuild.NativeResource;
import ossbuild.Sys;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Native {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static final Object lock = new Object();

	private static boolean initialized = false;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public static Object getLock() {
		return lock;
	}
	//</editor-fold>

	public static void initialize() {
		synchronized(lock) {
			if (initialized)
				return;

			initialized = true;

			Sys.loadNativeResources(NativeResource.VLC);

			
		}
	}
}

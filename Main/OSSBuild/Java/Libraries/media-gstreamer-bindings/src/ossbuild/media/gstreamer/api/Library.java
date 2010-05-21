
package ossbuild.media.gstreamer.api;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String[] DEFAULT_LIBRARY_NAME_FORMATS = {
		  "%s"
		, "lib%s"
		, "lib%s-0"
	};
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static AtomicBoolean initialized = new AtomicBoolean(false);
	//</editor-fold>

	static void init() {
		if (initialized.compareAndSet(true, true))
			return;
	}

	public static boolean isInitialized() {
		return initialized.get();
	}
}

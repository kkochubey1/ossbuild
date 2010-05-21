
package ossbuild.gst;

import ossbuild.gst.callbacks.IBusSyncHandler;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IBus extends INativeObject, IDisposable {
	void syncHandler(final IBusSyncHandler handler);
}

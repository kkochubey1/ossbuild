
package ossbuild.gst;

import ossbuild.gst.callbacks.IBusSyncHandler;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IPipeline extends IBin {
	IBus getBus();
	void busSyncHandler(final IBusSyncHandler handler);
}

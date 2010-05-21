
package ossbuild.media.gstreamer;

import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IPipeline extends IBin {
	IBus getBus();
	void busSyncHandler(final IBusSyncHandler handler);
}


package ossbuild.media.gstreamer.callbacks;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.Bus;
import ossbuild.media.gstreamer.BusSyncReply;
import ossbuild.media.gstreamer.Message;
import ossbuild.media.gstreamer.api.ICallback;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IBusSyncHandler extends ICallback {
	BusSyncReply handle(Bus bus, Message msg, Pointer src, Pointer data);
}

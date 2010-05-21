
package ossbuild.gst.callbacks;

import com.sun.jna.Pointer;
import ossbuild.gst.Bus;
import ossbuild.gst.BusSyncReply;
import ossbuild.gst.Message;
import ossbuild.gst.api.ICallback;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IBusSyncHandler extends ICallback {
	BusSyncReply handle(Bus bus, Message msg, Pointer src, Pointer data);
}

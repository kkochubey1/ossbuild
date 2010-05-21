
package ossbuild.media.gstreamer.signals;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.Signal;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
@Signal(name="no-more-pads")
public interface INoMorePads extends ISignal {
	void noMorePads(Pointer pElement, Pointer pUserData);
}

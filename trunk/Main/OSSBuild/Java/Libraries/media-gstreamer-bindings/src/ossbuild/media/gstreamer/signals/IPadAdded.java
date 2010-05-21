
package ossbuild.media.gstreamer.signals;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.Signal;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
@Signal(name="pad-added")
public interface IPadAdded extends ISignal {
	void padAdded(Pointer pElement, Pointer pPad);
}


package ossbuild.media.gstreamer.signals;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.Signal;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
@Signal(name="notify", detail="caps") /*notify::caps*/
public interface INotifyCaps extends ISignal {
	void notifyCaps(Pointer pPad, Pointer pUnused, Pointer pDynamic);
}

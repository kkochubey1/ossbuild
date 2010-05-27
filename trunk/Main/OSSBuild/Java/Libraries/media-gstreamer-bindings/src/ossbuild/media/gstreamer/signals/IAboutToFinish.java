
package ossbuild.media.gstreamer.signals;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.Signal;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
@Signal(name="about-to-finish")
public interface IAboutToFinish extends ISignal {
	void aboutToFinish(Pointer pPlaybin, Pointer pUserData);
}

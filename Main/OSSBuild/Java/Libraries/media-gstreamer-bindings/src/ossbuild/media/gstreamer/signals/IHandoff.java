
package ossbuild.media.gstreamer.signals;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.Signal;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
@Signal(name="handoff")
public interface IHandoff extends ISignal {
	void handoff(Pointer pElement, Pointer pBuffer, Pointer pPad);
}

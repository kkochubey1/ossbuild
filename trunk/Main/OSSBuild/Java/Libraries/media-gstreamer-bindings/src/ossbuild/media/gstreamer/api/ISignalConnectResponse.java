
package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface ISignalConnectResponse {
	NativeLong getHandlerID();
	ISignal getSignal();
	String getSignalName();
	Pointer getInstance();
}

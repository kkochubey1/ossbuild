
package ossbuild.media.gstreamer.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IGWeakNotify extends Callback {
	void notify(Pointer data, Pointer ptr);
}

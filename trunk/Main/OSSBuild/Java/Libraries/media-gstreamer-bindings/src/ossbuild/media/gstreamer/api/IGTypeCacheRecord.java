
package ossbuild.media.gstreamer.api;

import com.sun.jna.Pointer;
import java.lang.reflect.Constructor;
import ossbuild.media.gstreamer.INativeObject;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IGTypeCacheRecord {
	Class<? extends INativeObject> getJavaClass();
	INativeObject instantiateFromPointer(Pointer ptr);
}

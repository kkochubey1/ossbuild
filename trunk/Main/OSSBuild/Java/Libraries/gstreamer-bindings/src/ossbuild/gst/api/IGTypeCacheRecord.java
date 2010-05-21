
package ossbuild.gst.api;

import com.sun.jna.Pointer;
import java.lang.reflect.Constructor;
import ossbuild.gst.INativeObject;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IGTypeCacheRecord {
	Class<? extends INativeObject> getJavaClass();
	Constructor<? extends INativeObject> getJavaConstructor();
	INativeObject instantiateFromPointer(Pointer ptr);
}

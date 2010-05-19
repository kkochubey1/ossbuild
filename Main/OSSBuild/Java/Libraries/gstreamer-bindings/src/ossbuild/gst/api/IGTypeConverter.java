
package ossbuild.gst.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IGTypeConverter {
	Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue);
}

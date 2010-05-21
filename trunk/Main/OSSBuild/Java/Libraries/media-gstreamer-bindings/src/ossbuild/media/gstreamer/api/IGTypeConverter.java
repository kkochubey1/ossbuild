
package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IGTypeConverter {
	boolean convertToProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue, Object value);
	Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue);
}

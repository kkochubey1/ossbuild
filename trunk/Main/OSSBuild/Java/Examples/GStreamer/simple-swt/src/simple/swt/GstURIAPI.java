
package simple.swt;

import com.sun.jna.Pointer;
import org.gstreamer.lowlevel.GstNative;
import org.gstreamer.lowlevel.annotations.CallerOwnsReturn;

/**
 * GstURI methods.
 */
public interface GstURIAPI extends com.sun.jna.Library {
	GstURIAPI GSTURI_API = GstNative.load(GstURIAPI.class);

	boolean gst_uri_protocol_is_valid(String protocol);
	boolean gst_uri_protocol_is_supported(URIType type, String protocol);
	boolean gst_uri_is_valid(String uri);
	boolean gst_uri_has_protocol(String uri, String protocol);
	@CallerOwnsReturn Pointer gst_element_make_from_uri(URIType type, String uri, String name);
}
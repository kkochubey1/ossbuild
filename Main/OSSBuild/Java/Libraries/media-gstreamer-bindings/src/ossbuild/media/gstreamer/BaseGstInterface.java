
package ossbuild.media.gstreamer;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class BaseGstInterface extends BaseNativeObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public BaseGstInterface(IElement element, NativeLong /*GType*/ gtype) {
		if (!gst_element_implements_interface(element.getPointer(), gtype))
			throw new IllegalArgumentException("The provided element does not implement the given interface");
		this.ptr = gst_implements_interface_cast(element.getPointer(), gtype);
		this.managed = true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public Pointer getPointerAsInterface() {
		return ptr;
	}
	//</editor-fold>
}

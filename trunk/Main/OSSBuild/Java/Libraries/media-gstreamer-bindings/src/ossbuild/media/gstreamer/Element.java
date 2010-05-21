
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Element extends BaseGstObject implements IElement {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Element(String factoryName) {
		super(factoryName);
	}

	public Element(String factoryName, String elementName) {
		super(factoryName, elementName);
	}

	Element(Pointer ptr) {
		super(ptr);
	}

	@Override
	protected Pointer createNativeObject(String factoryName, String elementName) {
		Pointer p = gst_element_factory_make(factoryName, elementName);
		gst_object_ref_sink(p);
		return p;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static IElement make(String factoryName) {
		return new Element(factoryName);
	}

	public static IElement make(String factoryName, String elementName) {
		return new Element(factoryName, elementName);
	}

	public static IElement from(Pointer pElement) {
		return new Element(pElement);
	}
	//</editor-fold>
}

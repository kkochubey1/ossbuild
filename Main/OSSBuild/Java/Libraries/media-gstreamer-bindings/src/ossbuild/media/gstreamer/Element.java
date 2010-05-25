
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Element extends BaseGstElement {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Element(String factoryName) {
		super(factoryName);
	}

	public Element(String factoryName, String elementName) {
		super(factoryName, elementName);
	}

	protected Element(Pointer ptr) {
		super(ptr);
	}

	protected Element(Pointer ptr, boolean incRef) {
		super(ptr, incRef);
	}

	@Override
	protected Pointer createNativeObject(Object... arguments) {
		String elementFactoryName = (String)arguments[0];
		String elementName = (String)arguments[1];
		Pointer p = gst_element_factory_make(elementFactoryName, elementName);
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

	public static IElement from(Pointer pElement, boolean incRef) {
		return new Element(pElement, incRef);
	}
	//</editor-fold>
}

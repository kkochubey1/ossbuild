
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import ossbuild.StringUtil;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Bin extends BaseGstBin {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Bin() {
		super();
		init();
	}

	public Bin(String elementName) {
		super(elementName);
		init();
	}

	public Bin(String factoryName, String elementName) {
		super(factoryName, elementName);
		init();
	}

	Bin(Pointer ptr) {
		super(ptr);
		init();
	}

	@Override
	protected Pointer createNativeObject(Object... arguments) {
		String elementFactoryName = (String)arguments[0];
		String elementName = (String)arguments[1];
		Pointer p = (!StringUtil.isNullOrEmpty(elementFactoryName) ? gst_element_factory_make(elementFactoryName, elementName) : gst_bin_new(elementName));
		gst_object_ref_sink(p);
		return p;
	}

	private void init() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeBin() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static IBin make() {
		return new Bin();
	}

	public static IBin make(String elementName) {
		return new Bin(elementName);
	}

	public static IBin make(String factoryName, String elementName) {
		return new Bin(factoryName, elementName);
	}

	public static IBin from(Pointer pElement) {
		return new Bin(pElement);
	}
	//</editor-fold>
}

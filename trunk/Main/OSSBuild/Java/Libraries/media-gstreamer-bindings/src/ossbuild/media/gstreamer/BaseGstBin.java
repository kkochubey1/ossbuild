
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.util.concurrent.TimeUnit;
import ossbuild.StringUtil;
import ossbuild.media.gstreamer.api.IGTypeConverter;
import static ossbuild.media.gstreamer.api.GLib.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class BaseGstBin extends BaseGstObject implements IBin {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected Integer factoryRank;
	protected String factoryName, factoryClass;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public BaseGstBin() {
		super((String)null, (String)null);
	}

	public BaseGstBin(String elementName) {
		super((String)null, elementName);
	}

	public BaseGstBin(String factoryName, String elementName) {
		super(factoryName, elementName);
	}

	BaseGstBin(Pointer ptr) {
		super(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected final void disposeObject() {
		if (ptr == Pointer.NULL)
			return;

		disposeBin();

		unref();
	}

	protected void disposeBin() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public boolean add(IElement element) {
		if (gst_bin_add(ptr, element.getPointer())) {
			//gst_object_ref(ptr);
			return true;
		}
		return false;
	}

	@Override
	public boolean addMany(IElement... elements) {
		if (elements == null || elements.length <= 0)
			return true;
		for(int i = 0; i < elements.length; ++i) {
			if (elements[i] != null) {
				if (!gst_bin_add(ptr, elements[i].getPointer()))
					return false;
				//gst_object_ref(ptr);
			}
		}
		return true;
	}

	@Override
	public boolean addAndLinkMany(IElement... elements) {
		if (!addMany(elements))
			return false;
		return Element.linkMany(elements);
	}
	//</editor-fold>
}

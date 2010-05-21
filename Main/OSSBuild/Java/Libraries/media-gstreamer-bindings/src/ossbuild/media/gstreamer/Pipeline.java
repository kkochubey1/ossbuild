
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.util.concurrent.TimeUnit;
import ossbuild.StringUtil;
import ossbuild.media.gstreamer.api.IGTypeConverter;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;
import static ossbuild.media.gstreamer.api.GLib.*;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Pipeline extends Bin implements IPipeline {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected Bus bus;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Pipeline() {
		super();
		init();
	}

	public Pipeline(String elementName) {
		super(elementName);
		init();
	}

	public Pipeline(String factoryName, String elementName) {
		super(factoryName, elementName);
		init();
	}

	Pipeline(Pointer ptr) {
		super(ptr);
		init();
	}

	@Override
	protected Pointer createNativeObject(String factoryName, String elementName) {
		Pointer p = (!StringUtil.isNullOrEmpty(factoryName) ? gst_element_factory_make(factoryName, elementName) : gst_pipeline_new(elementName));
		gst_object_ref_sink(p);
		return p;
	}

	protected void init() {
		this.bus = Bus.from(gst_pipeline_get_bus(ptr));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	@Override
	public IBus getBus() {
		return bus;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeBin() {
		bus.dispose();

		//Need to unref him ourselves b/c Bus.from() will create an
		//unmanaged reference (IOW, someone else will clean him up)
		g_object_unref(bus.ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public void busSyncHandler(final IBusSyncHandler handler) {
		if (bus != null)
			bus.syncHandler(handler);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static IPipeline make() {
		return new Pipeline();
	}

	public static IPipeline make(String elementName) {
		return new Pipeline(elementName);
	}

	public static IPipeline make(String factoryName, String elementName) {
		return new Pipeline(factoryName, elementName);
	}

	public static IPipeline from(Pointer pElement) {
		return new Pipeline(pElement);
	}
	//</editor-fold>
}

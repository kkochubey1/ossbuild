
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
abstract class BaseGstMiniObject extends BaseGObject {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		  GST_MINI_OBJECT_FLAG_READONLY = (1<<0)
		, GST_MINI_OBJECT_FLAG_LAST = (1<<4)
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	protected BaseGstMiniObject() {
		super();
	}

	protected BaseGstMiniObject(Pointer ptr) {
		super(ptr);
		this.managed = false;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		unref();
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Getters">
	public boolean isWritable() {
		return gst_mini_object_is_writable(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref Counting">
	@Override
	public void ref() {
		if (ptr != Pointer.NULL)
			gst_mini_object_ref(ptr);
	}
	
	@Override
	public void unref() {
		if (ptr != Pointer.NULL)
			gst_mini_object_unref(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@SuppressWarnings("unchecked")
	public <T extends BaseGstMiniObject> T makeWritable() {
		Pointer p = gst_mini_object_make_writable(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		this.ptr = p;
		return (T)this;
	}
	//</editor-fold>
}

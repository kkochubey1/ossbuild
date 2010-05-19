
package ossbuild.gst.api;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GObject extends Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		  API_VERSION_MAJOR     = 2
		, API_VERSION_MINOR     = 0
		, API_VERSION_REVISION  = 0
	;

	public static final String
		  API_VERSION = API_VERSION_MAJOR + "." + API_VERSION_MINOR //0.10
	;

	public static final String
		  LIBRARY_NAME = "gobject-" + API_VERSION //gobject-0.10
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		GLib.init();
		initLibrary();
		init();
	}

	private static void initLibrary() {
		for (int i = 0; i < DEFAULT_LIBRARY_NAME_FORMATS.length; ++i) {
			try {
				Native.register(String.format(DEFAULT_LIBRARY_NAME_FORMATS[i], LIBRARY_NAME));
			} catch (UnsatisfiedLinkError ex) {
				continue;
			}
			return;
		}
		throw new UnsatisfiedLinkError("Could not load library " + LIBRARY_NAME);
	}

	static void init() {
		
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	public static native void g_object_weak_ref(Pointer object, Callback notify, Pointer data);
	public static native void g_object_weak_unref(Pointer object, Callback notify, Pointer data);
	public static native Pointer g_object_ref(Pointer object);
	public static native void g_object_unref(Pointer object);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Class">
	public static native Pointer /*GParamSpec*/ g_object_class_find_property(Pointer oclass, String property_name);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Signals">
	public static native NativeLong g_signal_connect_data(Pointer instance, String signal, Callback callback, Pointer data, Callback destroy_data, int connect_flags);
    public static native void g_signal_handler_disconnect(Pointer instance, NativeLong id);
	//</editor-fold>
	//</editor-fold>
}

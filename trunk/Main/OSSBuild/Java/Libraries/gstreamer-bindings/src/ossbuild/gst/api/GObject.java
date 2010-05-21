
package ossbuild.gst.api;

import com.sun.jna.Callback;
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

	//<editor-fold defaultstate="collapsed" desc="Value">
	public static native Pointer /*GValue*/ g_value_init(Pointer /*GValue*/ value, NativeLong /*GType*/ g_type);
    public static native Pointer /*GValue*/ g_value_reset(Pointer /*GValue*/ value);
    public static native void g_value_unset(Pointer /*GValue*/ value);
    public static native void g_value_set_char(Pointer /*GValue*/ value, byte v_char);
    public static native byte g_value_get_char(Pointer /*GValue*/ value);
    public static native void g_value_set_uchar(Pointer /*GValue*/ value, byte v_uchar);
    public static native byte g_value_get_uchar(Pointer /*GValue*/ value);
    public static native void g_value_set_boolean(Pointer /*GValue*/ value, boolean v_boolean);
    public static native boolean g_value_get_boolean(Pointer /*GValue*/ value);
    public static native void g_value_set_int(Pointer /*GValue*/ value, int v_int);
    public static native int g_value_get_int(Pointer /*GValue*/ value);
    public static native void g_value_set_uint(Pointer /*GValue*/ value, int v_int);
    public static native int g_value_get_uint(Pointer /*GValue*/ value);
    public static native void g_value_set_long(Pointer /*GValue*/ value, NativeLong v_long);
    public static native NativeLong g_value_get_long(Pointer /*GValue*/ value);
    public static native void g_value_set_ulong(Pointer /*GValue*/ value, NativeLong v_long);
    public static native NativeLong g_value_get_ulong(Pointer /*GValue*/ value);
    public static native void g_value_set_int64(Pointer /*GValue*/ value, long v_int64);
    public static native long g_value_get_int64(Pointer /*GValue*/ value);
    public static native void g_value_set_uint64(Pointer /*GValue*/ value, long v_uint64);
    public static native long g_value_get_uint64(Pointer /*GValue*/ value);
    public static native void g_value_set_float(Pointer /*GValue*/ value, float v_float);
    public static native float g_value_get_float(Pointer /*GValue*/ value);
    public static native void g_value_set_double(Pointer /*GValue*/ value, double v_double);
    public static native double g_value_get_double(Pointer /*GValue*/ value);
    public static native void g_value_set_enum(Pointer /*GValue*/ value, int v_enum);
    public static native int g_value_get_enum(Pointer /*GValue*/ value);
    public static native void g_value_set_string(Pointer /*GValue*/ value, String v_string);
    public static native void g_value_set_static_string (Pointer /*GValue*/ value, String v_string);
    public static native String g_value_get_string(Pointer /*GValue*/ value);
    public static native boolean g_value_type_compatible(NativeLong /*GType*/ src_type, NativeLong /*GType*/ dest_type);
    public static native boolean g_value_type_transformable(NativeLong /*GType*/ src_type, NativeLong /*GType*/ dest_type);
	public static native boolean g_value_transform(Pointer /*GValue*/ src_value, Pointer /*GValue*/ dest_value);

    public static native void g_value_set_object(Pointer /*GValue*/ value, Pointer /*GObject*/ v_object);
    public static native void g_value_take_object(Pointer /*GValue*/ value, Pointer /*GObject*/ v_object);
    public static native Pointer /*GObject*/ g_value_get_object(Pointer /*GValue*/ value);
    public static native Pointer /*GObject*/ g_value_dup_object(Pointer /*GValue*/ value);

    public static native Pointer /*GValue*/ g_value_array_get_nth(Pointer /*GValueArray*/ value_array, int index);
    public static native Pointer g_value_array_new(int n_prealloced);
    public static native void g_value_array_free (Pointer /*GValueArray*/ value_array);

    public static native Pointer g_value_array_copy(Pointer /*GValueArray*/ value_array);
    public static native Pointer g_value_array_prepend(Pointer /*GValueArray*/ value_array, Pointer /*GValue*/ value);
    public static native Pointer g_value_array_append(Pointer /*GValueArray*/ value_array, Pointer /*GValue*/ value);
    public static native Pointer g_value_array_insert(Pointer /*GValueArray*/ value_array, int index_, Pointer /*GValue*/ value);
    public static native Pointer g_value_array_remove(Pointer /*GValueArray*/ value_array, int index);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Properties">
	public static native void g_object_set_property(Pointer /*GObject*/ obj, String property, Pointer /*GValue*/ value);
	public static native void g_object_get_property(Pointer /*GObject*/ obj, String property, Pointer /*GValue*/ value);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Types">
	public static native NativeLong /*GType*/ g_type_parent(NativeLong /*GType*/ type);
	public static native int g_type_depth(NativeLong /*GType*/ type);
	//</editor-fold>
	//</editor-fold>
}

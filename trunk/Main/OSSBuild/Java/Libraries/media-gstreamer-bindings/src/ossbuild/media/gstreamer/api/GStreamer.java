
package ossbuild.media.gstreamer.api;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.concurrent.atomic.AtomicBoolean;
import ossbuild.StringUtil;
import static ossbuild.media.gstreamer.api.GLib.*;
import static ossbuild.media.gstreamer.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GStreamer extends Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		  API_VERSION_MAJOR     = 0
		, API_VERSION_MINOR     = 10
		, API_VERSION_REVISION  = 0
	;

	public static final String
		  API_VERSION = API_VERSION_MAJOR + "." + API_VERSION_MINOR //0.10
	;

	public static final String
		  LIBRARY_NAME = "gstreamer-" + API_VERSION //gstreamer-0.10
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static AtomicBoolean initialized = new AtomicBoolean(false);
	static Pointer context;
	static Pointer contextLoop;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		GObject.init();
		initLibrary();
		init();
	}

	private static void initLibrary() {
		for (int i = 0; i < DEFAULT_LIBRARY_NAME_FORMATS.length; ++i) {
			try {
				Native.register(String.format(DEFAULT_LIBRARY_NAME_FORMATS[i], LIBRARY_NAME));
			} catch(IllegalArgumentException e) {
				throw e;
			} catch (Throwable t) {
				continue;
			}
			return;
		}
		throw new UnsatisfiedLinkError("Could not load library " + LIBRARY_NAME);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public static Pointer getMainContext() {
		return context;
	}

	public static Pointer getMainContextLoop() {
		return contextLoop;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static boolean initialize() {
		return initialize(null, null);
	}

	public static boolean initialize(String programName) {
		return initialize(programName, null);
	}

	public static synchronized boolean initialize(String programName, String[] args) {
		if (initialized.compareAndSet(true, true))
			return true;

		//Add the program name to the beginning of the array
		args = Utils.joinBefore(args, !StringUtil.isNullOrEmpty(programName) ? programName : StringUtil.empty);

		//Initialize GStreamer
		IntByReference argc = new IntByReference(args.length);
		PointerByReference argv = new PointerByReference(new StringArray(args));
		if (!gst_init_check(argc, argv, null))
			return false;

		//Add GType converters
		GTypeConverters.initialize();

		//Fill GType cache
		GTypeCache.initialize();

		//Create a glib main context and loop
		context = g_main_context_new();
		contextLoop = g_main_loop_new(context, false);
		g_main_context_unref(context);
		
		//Ensure that we quit the main loop when the app shuts down
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				quit();
			}
		}));

		//Create a thread to run the main loop. Care must be taken that
		//we attach signals to this loop instead of the default one.
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				g_main_loop_run(contextLoop);
			}
		});
		t.setName("gstreamer main loop");
		t.setDaemon(true);
		t.start();
		
		return true;
	}

	public static synchronized boolean quit() {
		if (!initialized.get())
			return true;

		if (contextLoop == null || contextLoop == Pointer.NULL)
			return true;

		g_main_loop_quit(contextLoop);
		g_main_context_release(context);

		contextLoop = null;
		context = null;

		return true;
	}


	
	public static NativeLong connectSignal(Pointer ptr, String signal, ICallback callback) {
		if (contextLoop == null)
			return null;
		return g_signal_connect_data(ptr, signal, callback, Pointer.NULL, null, 0);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Gst">
	public static native boolean gst_init(IntByReference argc, PointerByReference argv);
	public static native boolean gst_init_check(IntByReference argc, PointerByReference argv, PointerByReference err);
	public static native void gst_deinit();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstObject">
	public static native void gst_object_ref(Pointer obj);
	public static native void gst_object_unref(Pointer obj);
	public static native void gst_object_sink(Pointer object);
	public static native void gst_object_ref_sink(Pointer object);

	public static native Pointer gst_object_get_name(Pointer obj);
	public static native Pointer gst_object_get_parent(Pointer obj);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstPluginFeature">
	public static native void gst_plugin_feature_set_rank(Pointer feature, int rank);
	public static native void gst_plugin_feature_set_name(Pointer feature, String name);
	public static native int gst_plugin_feature_get_rank(Pointer feature);
	public static native String gst_plugin_feature_get_name(Pointer feature);
	public static native boolean gst_plugin_feature_check_version(Pointer feature, int min_major, int min_minor, int min_micro);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstElementFactory">
	public static native Pointer gst_element_factory_make(String factoryName, String elementName);
	public static native String gst_element_factory_get_klass(Pointer factory);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstElement">
	public static native int gst_element_set_state(Pointer elem, int state);
	public static native int gst_element_get_state(Pointer elem, IntByReference state, IntByReference pending, long timeout);

	public static native boolean gst_element_link(Pointer elem1, Pointer elem2);

	public static native Pointer gst_element_get_factory(Pointer element);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstBin">
	public static native Pointer gst_bin_new(String name);
	public static native boolean gst_bin_add(Pointer bin, Pointer element);
	//Direct mapping does not support varargs
	//public static native void gst_bin_add_many(Pointer bin, Pointer... /*GstElement...*/ elements);
	public static native boolean gst_bin_remove(Pointer bin, Pointer element);
	//Direct mapping does not support varargs
	//public static native void gst_bin_remove_many(Pointer bin, Pointer /*GstElement...*/ elements);
	public static native Pointer gst_bin_get_by_name(Pointer bin, String name);
	public static native Pointer gst_bin_get_by_name_recurse_up(Pointer bin, String name);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstPipeline">
	public static native Pointer gst_pipeline_new(String name);
	public static native Pointer gst_pipeline_get_bus(Pointer pipeline);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstBus">
	public static native Pointer gst_bus_new();
	public static native NativeLong gst_bus_add_watch(Pointer bus, Callback func, Pointer user_data);
	public static native void gst_bus_set_sync_handler(Pointer bus, Callback func, Pointer user_data);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstMessage">
	//public static native Pointer gst_message_ref(Pointer msg);
	//public static native void gst_message_unref(Pointer msg);

	public static native void gst_message_parse_state_changed(Pointer msg, IntByReference old, IntByReference current, IntByReference pending);
	public static native void gst_message_parse_tag(Pointer msg, PointerByReference tagList);
	public static native void gst_message_parse_clock_provide(Pointer msg, PointerByReference clock, IntByReference ready);
	public static native void gst_message_parse_new_clock(Pointer msg, PointerByReference clock);
	public static native void gst_message_parse_error(Pointer msg, PointerByReference err, PointerByReference debug);
	public static native void gst_message_parse_warning(Pointer msg, PointerByReference err, PointerByReference debug);
	public static native void gst_message_parse_info(Pointer msg, PointerByReference err, PointerByReference debug);
	public static native void gst_message_parse_buffering(Pointer msg, IntByReference percent);
	public static native void gst_message_parse_segment_start(Pointer message, IntByReference /*Format*/ format, LongByReference position);
	public static native void gst_message_parse_segment_done(Pointer message, IntByReference /*Format*/ format, LongByReference position);
	public static native void gst_message_parse_duration(Pointer message, IntByReference /*Format*/ format, LongByReference duration);
	public static native void gst_message_parse_async_start(Pointer message, IntByReference /* boolean */ new_base_time);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstMiniObject">
	public static native void gst_mini_object_ref(Pointer ptr);
	public static native void gst_mini_object_unref(Pointer ptr);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstStructure">
	public static native boolean gst_structure_fixate_field_nearest_int(Pointer structure, String field, int target);
	public static native String gst_structure_to_string(Pointer structure);
	public static native Pointer gst_structure_from_string(String data, PointerByReference end);
	public static native Pointer gst_structure_empty_new(String name);
	//Direct mapping does not support varargs
	//public static native Pointer gst_structure_new(String name, String firstField, PointerByReference /*Object...*/ args);
	public static native Pointer gst_structure_copy(Pointer src);
	public static native void gst_structure_remove_field(Pointer structure, String fieldName);
	//Direct mapping does not support varargs
	//public static native void gst_structure_remove_fields(Pointer structure, PointerByReference /*String...*/ fieldNames);
	public static native void gst_structure_remove_all_fields(Pointer structure);

	public static native String gst_structure_get_name(Pointer structure);
	public static native void gst_structure_set_name(Pointer structure, String name);
	public static native boolean gst_structure_has_name(Pointer structure, String name);
	public static native int gst_structure_n_fields(Pointer structure);
	public static native String gst_structure_nth_field_name(Pointer structure, int index);
	public static native boolean gst_structure_has_field(Pointer structure, String fieldname);
	public static native boolean gst_structure_has_field_typed(Pointer structure, String fieldname, int /*GType*/ type);

	public static native boolean gst_structure_get_boolean(Pointer structure, String fieldname, IntByReference value);
	public static native boolean gst_structure_get_int(Pointer structure, String fieldname, IntByReference value);
	public static native boolean gst_structure_get_fourcc(Pointer structure, String fieldname, IntByReference value);
	public static native boolean gst_structure_get_double(Pointer structure, String fieldname, DoubleByReference value);
	public static native boolean gst_structure_get_date(Pointer structure, String fieldname, PointerByReference value);
	//boolean gst_structure_get_clock_time(Structure structure, String fieldname, GstClockTime *value);

	public static native String gst_structure_get_string(Pointer structure, String fieldname);
	public static native boolean gst_structure_get_enum(Pointer structure, String fieldname, int /*GType*/ gtype_enumtype, IntByReference value);
	public static native boolean gst_structure_get_fraction(Pointer structure, String fieldname, IntByReference value_numerator, IntByReference value_denominator);
	public static native Pointer gst_structure_get_value (Pointer structure, String fieldname);
	//Direct mapping does not support varargs
	//public static native void gst_structure_set(Pointer structure, String fieldname, PointerByReference /*Object...*/ args);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Types">
	public static native NativeLong gst_bin_get_type();
	public static native NativeLong gst_buffer_get_type();
	public static native NativeLong gst_bus_get_type();
	public static native NativeLong gst_caps_get_type();
	public static native NativeLong gst_child_proxy_get_type();
	public static native NativeLong gst_clock_get_type();
	public static native NativeLong gst_date_get_type();
	public static native NativeLong gst_element_get_type();
	public static native NativeLong gst_element_factory_get_type();
	public static native NativeLong gst_event_get_type();
	public static native NativeLong gst_g_error_get_type();
	public static native NativeLong gst_ghost_pad_get_type();
	public static native NativeLong gst_index_get_type();
	public static native NativeLong gst_index_entry_get_type();
	public static native NativeLong gst_index_factory_get_type();
	public static native NativeLong gst_message_get_type();
	public static native NativeLong gst_mini_object_get_type();
	public static native NativeLong gst_object_get_type();
	public static native NativeLong gst_pad_get_type();
	public static native NativeLong gst_pad_template_get_type();
	public static native NativeLong gst_pipeline_get_type();
	public static native NativeLong gst_plugin_get_type();
	public static native NativeLong gst_plugin_feature_get_type();
	public static native NativeLong gst_query_get_type();
	public static native NativeLong gst_registry_get_type();
	public static native NativeLong gst_segment_get_type();
	public static native NativeLong gst_static_pad_template_get_type();
	public static native NativeLong gst_static_caps_get_type();
	public static native NativeLong gst_system_clock_get_type();
	public static native NativeLong gst_structure_get_type();
	public static native NativeLong gst_tag_get_type(String tag);
	public static native NativeLong gst_tag_list_get_type();
	public static native NativeLong gst_tag_setter_get_type();
	public static native NativeLong gst_task_get_type();
	public static native NativeLong gst_type_find_get_type();
	public static native NativeLong gst_type_find_factory_get_type();
	public static native NativeLong gst_uri_handler_get_type();
	//</editor-fold>
	//</editor-fold>
}

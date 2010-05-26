/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2009 Levente Farkas
 * Copyright (C) 2007, 2008 Wayne Meissner
 *
 * This file contains code from the gstreamer-java project.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import ossbuild.StringUtil;
import ossbuild.Sys;
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
		if (!initialized.compareAndSet(false, true))
			return true;

		//Add the program name to the beginning of the array
		args = Utils.joinBefore(args, !StringUtil.isNullOrEmpty(programName) ? programName : StringUtil.empty);

		if (StringUtil.isNullOrEmpty(Sys.getEnvironmentVariable("GST_DEBUG_DUMP_DOT_DIR")))
			Sys.setEnvironmentVariable("GST_DEBUG_DUMP_DOT_DIR", new File(".").getAbsolutePath());

		//Initialize GStreamer
		IntByReference argc = new IntByReference(args.length);
		PointerByReference argv = new PointerByReference(new StringArray(args));
		if (!gst_init_check(argc, argv, null))
			return false;

		//Add GType converters
		GTypeConverters.initialize();

		//Fill GType cache
		GTypeCache.initialize();

		//Read signal annotations ahead of time
		SignalCache.initialize();

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

		//TODO: Fix this - it causes a crash for some reason
		//if (g_main_loop_is_running(contextLoop)) {
		//	g_main_loop_quit(contextLoop);
		//	g_main_context_release(context);
		//}

		contextLoop = null;
		context = null;

		return true;
	}


	
	public static NativeLong connectSignal(Pointer ptr, String signal, ICallback callback) {
		if (contextLoop == null)
			return null;
		return g_signal_connect_data(ptr, signal, callback, Pointer.NULL, null, 0);
	}

	public static boolean disconnectSignal(Pointer ptr, NativeLong handler_id) {
		if (ptr == null || ptr == Pointer.NULL)
			return false;
		if (handler_id == null || handler_id.intValue() == 0)
			return false;
		
		if (g_signal_handler_is_connected(ptr, handler_id))
			g_signal_handler_disconnect(ptr, handler_id);
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		  GST_SEEK_FLAG_NONE        = 0
		, GST_SEEK_FLAG_FLUSH       = (1 << 0)
		, GST_SEEK_FLAG_ACCURATE    = (1 << 1)
		, GST_SEEK_FLAG_KEY_UNIT    = (1 << 2)
		, GST_SEEK_FLAG_SEGMENT     = (1 << 3)
		, GST_SEEK_FLAG_SKIP        = (1 << 4)
	;
	//</editor-fold>

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
	public static native void gst_element_unlink(Pointer elem1, Pointer elem2);

	public static native Pointer gst_element_get_factory(Pointer element);
	public static native Pointer /*GstPad*/ gst_element_get_static_pad(Pointer /*GstElement*/ element, String name);
	public static native boolean gst_element_add_pad(Pointer /*GstElement*/ elem, Pointer /*GstPad*/ pad);
	public static native boolean gst_element_remove_pad(Pointer /*GstElement*/ elem, Pointer /*GstPad*/ pad);
	
	public static native boolean gst_element_link_pads(Pointer /*GstElement*/ src, String srcpadname, Pointer /*GstElement*/ dest, String destpadname);
	public static native void gst_element_unlink_pads(Pointer /*GstElement*/ src, String srcpadname, Pointer /*GstElement*/ dest, String destpadname);
	public static native boolean gst_element_link_pads_filtered(Pointer /*GstElement*/ src, String srcpadname, Pointer /*GstElement*/ dest, String destpadname, Pointer /*GstCaps*/ filter);

	public static native Pointer /*GstIterator*/ gst_element_iterate_pads(Pointer /*GstElement*/ element);
	public static native Pointer /*GstIterator*/ gst_element_iterate_src_pads(Pointer /*GstElement*/ element);
	public static native Pointer /*GstIterator*/ gst_element_iterate_sink_pads(Pointer /*GstElement*/ element);

	public static native boolean gst_element_post_message(Pointer /*GstElement*/ element, Pointer /*GstMessage*/ message);

	public static native NativeLong gst_element_get_base_time(Pointer /*GstElement*/ element);
	public static native NativeLong gst_element_get_start_time(Pointer /*GstElement*/ element);

	public static native boolean gst_element_query_position(Pointer /*GstElement*/ elem, IntByReference /*GstFormat**/ fmt, LongByReference /*GstClockTime*/ pos);
	public static native boolean gst_element_query_duration(Pointer /*GstElement*/ elem, IntByReference /*GstFormat**/ fmt, LongByReference /*GstClockTime*/ dur);
	public static native boolean gst_element_seek(Pointer /*GstElement*/ elem, double /*gdouble*/ rate, int /*GstFormat*/ format, int /*GstSeekFlags*/ flags, int /*GstSeekType*/ cur_type, long cur, int /*GstSeekType*/ stop_type, long stop);
	public static native boolean gst_element_seek_simple(Pointer /*GstElement*/ elem, int /*GstFormat*/ format, int /*GstSeekFlags*/ flags, long pos);
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

	public static native Pointer /*GstElement*/ gst_bin_iterate_elements(Pointer /*GstBin*/ bin);
	public static native Pointer /*GstElement*/ gst_bin_iterate_sorted(Pointer /*GstBin*/ bin);
	public static native Pointer /*GstElement*/ gst_bin_iterate_recurse(Pointer /*GstBin*/ bin);
	public static native Pointer /*GstElement*/ gst_bin_iterate_sinks(Pointer /*GstBin*/ bin);
	public static native Pointer /*GstElement*/ gst_bin_iterate_sources(Pointer /*GstBin*/ bin);
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

	public static native Pointer /*GstStructure*/ gst_message_get_structure(Pointer /*GstMessage*/ message);

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

	public static native boolean gst_mini_object_is_writable(Pointer /*GstMiniObject*/ mini_object);
	public static native Pointer /*GstMiniObject*/ gst_mini_object_make_writable(Pointer /*GstMiniObject*/ mini_object);
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
	public static native void gst_structure_free(Pointer /*GstStructure*/ structure);

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
	public static native Pointer /*GValue*/ gst_structure_get_value (Pointer structure, String fieldname);
	public static native void gst_structure_set_value(Pointer structure, String fieldname, Pointer /*GValue*/ value);
	//Direct mapping does not support varargs
	//public static native void gst_structure_set(Pointer structure, String fieldname, PointerByReference /*Object...*/ args);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstCaps">
	public static native Pointer /*GstCaps*/ gst_caps_new_empty();
	public static native Pointer /*GstCaps*/ gst_caps_new_any();

	public static native Pointer gst_caps_ref(Pointer /*GstCaps*/ caps);
	public static native void gst_caps_unref(Pointer /*GstCaps*/ caps);
	public static native Pointer /*GstCaps*/ gst_caps_copy(Pointer /*GstCaps*/ caps);
	public static native Pointer /*GstCaps*/ gst_caps_from_string(String string);
	public static native Pointer /*GstCaps*/ gst_caps_make_writable(Pointer /*GstCaps*/ caps);

	/* manipulation */
	public static native void gst_caps_append(Pointer /*GstCaps*/ caps1, Pointer /*GstCaps*/ caps2);
	public static native void gst_caps_merge(Pointer /*GstCaps*/ caps1, Pointer /*GstCaps*/ caps2);
	public static native void gst_caps_append_structure(Pointer /*GstCaps*/ caps, Pointer /*GstStructure*/ structure);
	public static native void gst_caps_remove_structure(Pointer /*GstCaps*/ caps, int idx);
	public static native void gst_caps_merge_structure(Pointer /*GstCaps*/ caps, Pointer /*GstStructure*/ structure);
	public static native int gst_caps_get_size(Pointer /*GstCaps*/ caps);
	public static native Pointer /*GstStructure*/ gst_caps_get_structure(Pointer /*GstCaps*/ caps, int index);
	public static native Pointer /*GstCaps*/ gst_caps_copy_nth(Pointer /*GstCaps*/ caps, int nth);
	public static native void gst_caps_truncate(Pointer /*GstCaps*/ caps);
	public static native void gst_caps_set_value(Pointer /*GstCaps*/ caps, String field, Pointer /*GValue*/ value);
	//public static native void gst_caps_set_simple(Pointer /*GstCaps*/ caps, String field, Object... values);

	/* operations */
	public static native Pointer /*GstCaps*/ gst_caps_intersect(Pointer /*GstCaps*/ caps1, Pointer /*GstCaps*/ caps2);
	public static native Pointer /*GstCaps*/ gst_caps_subtract(Pointer /*GstCaps*/ minuend, Pointer /*GstCaps*/ subtrahend);
	public static native Pointer /*GstCaps*/ gst_caps_union(Pointer /*GstCaps*/ caps1, Pointer /*GstCaps*/ caps2);
	public static native Pointer /*GstCaps*/ gst_caps_normalize(Pointer /*GstCaps*/ caps);
	public static native boolean gst_caps_do_simplify(Pointer /*GstCaps*/ caps);
	public static native String gst_caps_to_string(Pointer /*GstCaps*/ caps);

	/* tests */
	public static native boolean gst_caps_is_any(Pointer /*GstCaps*/ caps);
	public static native boolean gst_caps_is_empty(Pointer /*GstCaps*/ caps);
	public static native boolean gst_caps_is_fixed(Pointer /*GstCaps*/ caps);
	public static native boolean gst_caps_is_always_compatible(Pointer /*GstCaps*/ caps1,  Pointer /*GstCaps*/ caps2);
	public static native boolean gst_caps_is_subset(Pointer /*GstCaps*/ subset,  Pointer /*GstCaps*/ superset);
	public static native boolean gst_caps_is_equal(Pointer /*GstCaps*/ caps1,  Pointer /*GstCaps*/ caps2);
	public static native boolean gst_caps_is_equal_fixed(Pointer /*GstCaps*/ caps1,  Pointer /*GstCaps*/ caps2);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstValue">
	public static native void gst_value_set_fourcc(Pointer /*GValue*/ value, int fourcc);
	
	public static native void gst_value_set_int_range(Pointer /*GValue*/ value, int start, int end);
	public static native void gst_value_set_double_range(Pointer /*GValue*/ value, double start, double end);
	public static native void gst_value_set_fraction(Pointer /*GValue*/ value, int numerator, int denominator);
	public static native void gst_value_set_fraction_range_full(Pointer /*GValue*/ value, int numerator_start, int denominator_start, int numerator_end, int denominator_end);

	public static native void gst_value_set_caps(Pointer /*GValue*/ value, Pointer /*GstCaps*/ caps);
	public static native Pointer /*GstCaps*/ gst_value_get_caps(Pointer /*GValue*/ value);

	public static native Pointer /*GstMiniObject*/ gst_value_get_mini_object(Pointer /*GValue*/ value);
	public static native void gst_value_set_mini_object(Pointer /*GValue*/ value, Pointer /*GstMiniObject*/ mini_object);

	public static native int gst_value_get_fraction_numerator(Pointer /*GValue*/  value);
	public static native int gst_value_get_fraction_denominator(Pointer /*GValue*/ value);
	public static native Pointer /*GValue*/ gst_value_get_fraction_range_min(Pointer /*GValue*/ value);
	public static native Pointer /*GValue*/ gst_value_get_fraction_range_max(Pointer /*GValue*/ value);
	public static native double gst_value_get_double_range_min(Pointer /*GValue*/ value);
	public static native double gst_value_get_double_range_max(Pointer /*GValue*/ value);
	public static native int gst_value_get_int_range_min(Pointer /*GValue*/ value);
	public static native int gst_value_get_int_range_max(Pointer /*GValue*/ value);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstPadTemplate">
	public static native Pointer /*GstPadTemplate*/ gst_pad_template_new(String name_template, int /*GstPadDirection*/ direction, int /*GstPadPresence*/ presence, Pointer /*GstCaps*/ caps);
	public static native Pointer /*GstPadTemplate*/ gst_static_pad_template_get(Pointer /*GstStaticPadTemplate*/ pad_template);
	public static native Pointer /*GstCaps*/ gst_static_pad_template_get_caps(Pointer /*GstStaticPadTemplate*/ template);
	public static native Pointer /*GstCaps*/ gst_pad_template_get_caps(Pointer /*GstPadTemplate*/ template);
	public static native void gst_pad_template_pad_created(Pointer /*GstPadTemplate*/ templ, Pointer /*GstPad*/ pad);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstPad">
	public static native Pointer /*GstPad*/ gst_pad_new(String name, int /*GstPadDirection*/ direction);
	public static native Pointer /*GstPad*/ gst_pad_new_from_template(Pointer /*GstPadTemplate*/ templ, String name);

	//public static native String gst_pad_get_name(Pointer /*GstPad*/ pad);
	
	public static native int /*GstPadLinkReturn*/ gst_pad_link(Pointer /*GstPad*/ src, Pointer /*GstPad*/ sink);
	public static native boolean gst_pad_unlink(Pointer /*GstPad*/ src, Pointer /*GstPad*/ sink);
	public static native boolean gst_pad_is_linked(Pointer /*GstPad*/ pad);
	public static native Pointer /*GstPad*/ gst_pad_get_peer(Pointer /*GstPad*/ pad);
	public static native int /*GstPadDirection*/ gst_pad_get_direction(Pointer /*GstPad*/ pad);

	/* pad functions from gstutils.h */
	public static native boolean gst_pad_can_link(Pointer /*GstPad*/ srcpad, Pointer /*GstPad*/ sinkpad);

	public static native void gst_pad_use_fixed_caps(Pointer /*GstPad*/ pad);
	public static native Pointer /*GstCaps*/ gst_pad_get_fixed_caps_func(Pointer /*GstPad*/ pad);
	public static native Pointer /*GstCaps*/ gst_pad_proxy_getcaps(Pointer /*GstPad*/ pad);
	public static native boolean gst_pad_proxy_setcaps(Pointer /*GstPad*/ pad, Pointer /*GstCaps*/ caps);
	public static native Pointer /*GstElement*/ gst_pad_get_parent_element(Pointer /*GstPad*/ pad);


	public static native boolean gst_pad_set_active(Pointer /*GstPad*/ pad, boolean active);
	public static native boolean gst_pad_is_active(Pointer /*GstPad*/ pad);
	public static native boolean gst_pad_activate_pull(Pointer /*GstPad*/ pad, boolean active);
	public static native boolean gst_pad_activate_push(Pointer /*GstPad*/ pad, boolean active);
	public static native boolean gst_pad_set_blocked(Pointer /*GstPad*/ pad, boolean blocked);
	public static native boolean gst_pad_is_blocked(Pointer /*GstPad*/ pad);
	public static native boolean gst_pad_is_blocking(Pointer /*GstPad*/ pad);
	/* get_pad_template returns a non-refcounted PadTemplate */
	public static native Pointer /*GstPadTemplate*/ gst_pad_get_pad_template(Pointer /*GstPad*/ pad);
	//public static native boolean gst_pad_set_blocked_async(Pointer /*GstPad*/ pad, boolean blocked, PadBlockCallback callback, Pointer userData);

	/* capsnego function for connected/unconnected pads */
	public static native Pointer /*GstCaps*/ gst_pad_get_caps(Pointer /*GstPad*/  pad);
	public static native void gst_pad_fixate_caps(Pointer /*GstPad*/ pad, Pointer /*GstCaps*/ caps);
	public static native boolean gst_pad_accept_caps(Pointer /*GstPad*/ pad, Pointer /*GstCaps*/ caps);
	public static native boolean gst_pad_set_caps(Pointer /*GstPad*/ pad, Pointer /*GstCaps*/ caps);
	public static native Pointer /*GstCaps*/ gst_pad_peer_get_caps(Pointer /*GstPad*/ pad);
	public static native boolean gst_pad_peer_accept_caps(Pointer /*GstPad*/ pad, Pointer /*GstCaps*/ caps);

	/* capsnego for connected pads */
	public static native Pointer /*GstCaps*/ gst_pad_get_allowed_caps(Pointer /*GstPad*/ pad);
	public static native Pointer /*GstCaps*/ gst_pad_get_negotiated_caps(Pointer /*GstPad*/ pad);

	/* data passing functions to peer */
	public static native int /*GstFlowReturn*/ gst_pad_push(Pointer /*GstPad*/ pad, Pointer /*GstBuffer*/ buffer);
	public static native boolean gst_pad_check_pull_range(Pointer /*GstPad*/ pad);
	public static native int /*GstFlowReturn*/ gst_pad_pull_range(Pointer /*GstPad*/ pad, /* guint64 */ long offset, /* guint */ int size, PointerByReference buffer);
	public static native boolean gst_pad_push_event(Pointer /*GstPad*/ pad, Pointer /*GstEvent*/ event);
	public static native boolean gst_pad_event_default(Pointer /*GstPad*/ pad, Pointer /*GstEvent*/ event);
	
	/* data passing functions on pad */
	public static native int /*GstFlowReturn*/ gst_pad_chain(Pointer /*GstPad*/ pad, Pointer /*GstBuffer*/ buffer);
	public static native int /*GstFlowReturn*/ gst_pad_get_range(Pointer /*GstPad*/ pad, /* guint64 */ long offset, /* guint */ int size, PointerByReference buffer);
	public static native boolean gst_pad_send_event(Pointer /*GstPad*/ pad, Pointer /*GstEvent*/ event);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstBuffer">
	public static native Pointer /*GstBuffer*/ gst_buffer_new();
	public static native Pointer /*GstBuffer*/ gst_buffer_new_and_alloc(int size);
	public static native Pointer /*GstBuffer*/ gst_buffer_try_new_and_alloc(int size);
	public static native boolean gst_buffer_is_metadata_writable(Pointer /*GstBuffer*/ buf);
	public static native Pointer /*GstBuffer*/ gst_buffer_make_metadata_writable(Pointer /*GstBuffer*/ buf);

	/* creating a subbuffer */
	public static native Pointer /*GstBuffer*/ gst_buffer_create_sub(Pointer /*GstBuffer*/ parent, int offset, int size);

	public static native Pointer /*GstCaps*/ gst_buffer_get_caps(Pointer /*GstBuffer*/ buffer);
	public static native void gst_buffer_set_caps(Pointer /*GstBuffer*/ buffer, Pointer /*GstCaps*/ caps);

	/* span two buffers intelligently */
	public static native boolean gst_buffer_is_span_fast(Pointer /*GstBuffer*/ buf1, Pointer /*GstBuffer*/ buf2);
	public static native Pointer /*GstBuffer*/ gst_buffer_span(Pointer /*GstBuffer*/ buf1, int offset, Pointer /*GstBuffer*/ buf2, int len);

	/* buffer functions from gstutils.h */
	public static native Pointer /*GstBuffer*/ gst_buffer_merge(Pointer /*GstBuffer*/ buf1, Pointer /*GstBuffer*/ buf2);
	public static native Pointer /*GstBuffer*/ gst_buffer_join(Pointer /*GstBuffer*/ buf1, Pointer /*GstBuffer*/ buf2);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstEvent">
	public static native Pointer /*GstStructure*/ gst_event_get_structure(Pointer /*GstEvent*/ event);
	//These are inlined away -- need to use casting to do the same thing
	//public static native Pointer /*GstEvent*/ gst_event_ref(Pointer /*GstEvent*/ event);
	//public static native void gst_event_unref(Pointer /*GstEvent*/ event);

	public static native Pointer /*GstEvent*/ gst_event_new_step(int /*GstFormat*/ format, long /*guint64*/ amount, double /*gdouble*/ rate, int /*gboolean*/ flush, int /*gboolean*/ intermediate);
	public static native void gst_event_parse_step(Pointer /*GstEvent*/ event, IntByReference /*GstFormat*/ format, LongByReference /*guint64*/ amount, DoubleByReference /*gdouble*/ rate, IntByReference /*gboolean*/ flush, IntByReference /*gboolean**/ intermediate);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstIterator">
	public static native void gst_iterator_free(Pointer iter);
	public static native int /*GstIteratorResult*/ gst_iterator_next(Pointer iter, PointerByReference next);
	public static native void gst_iterator_resync(Pointer iter);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstGhostPad">
	public static native Pointer /*GstGhostPad*/ gst_ghost_pad_new_from_template(String name, Pointer /*GstPad*/ target, Pointer /*GstPadTemplate*/ templ);
	public static native Pointer /*GstGhostPad*/ gst_ghost_pad_new_no_target_from_template(String name, Pointer /*GstPadTemplate*/ templ);
	public static native Pointer /*GstGhostPad*/ gst_ghost_pad_new(String name, Pointer /*GstPad*/ target);
	public static native Pointer /*GstGhostPad*/ gst_ghost_pad_new_no_target(String name, int direction);

	public static native Pointer /*GstPad*/ gst_ghost_pad_get_target(Pointer /*GstGhostPad*/ gpad);
	public static native boolean gst_ghost_pad_set_target(Pointer /*GstGhostPad*/ gpad, Pointer /*GstPad*/ newtarget);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstDebug">
	public static native void _gst_debug_bin_to_dot_file (Pointer /*GstBin*/ bin, int details, String file_name);
	public static native void _gst_debug_bin_to_dot_file_with_ts (Pointer /*GstBin*/ bin, int details, String file_name);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="GstInterface">
	public static native NativeLong /*GType*/ gst_implements_interface_get_type();
	public static native Pointer /*gpointer*/ gst_implements_interface_cast(Pointer /*gpointer*/ from, NativeLong /*GType*/ type);
	public static native Pointer /*gpointer*/ gst_implements_interface_check(Pointer /*gpointer*/ from, NativeLong /*GType*/ type);
	public static native boolean gst_element_implements_interface(Pointer /*GstElement*/ element, NativeLong /*GType*/ iface_type);
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
	public static native NativeLong gst_fourcc_get_type();
	public static native NativeLong gst_int_range_get_type();
	public static native NativeLong gst_double_range_get_type();
	public static native NativeLong gst_fraction_range_get_type();
	public static native NativeLong gst_value_list_get_type();
	public static native NativeLong gst_fraction_get_type();
	//</editor-fold>
	//</editor-fold>
}

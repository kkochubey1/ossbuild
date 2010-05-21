
package ossbuild.gst.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import ossbuild.gst.Bin;
import ossbuild.gst.Bus;
import ossbuild.gst.Element;
import ossbuild.gst.INativeObject;
import ossbuild.gst.Message;
import ossbuild.gst.Pipeline;
import static ossbuild.gst.api.GStreamer.*;
import static ossbuild.gst.api.GStreamerBase.*;
import static ossbuild.gst.api.GStreamerInterfaces.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GTypeCache {
	private static final Map<NativeLong, IGTypeCacheRecord> cache = new HashMap<NativeLong, IGTypeCacheRecord>(20) {{
		//GObject types
		put(gst_color_balance_channel_get_type(),   null /*ColorBalanceChannel.class*/);
		put(gst_mixer_track_get_type(),             null /*MixerTrack.class*/);
		put(gst_tuner_channel_get_type(),           null /*TunerChannel.class*/);
		put(gst_tuner_norm_get_type(),              null /*TunerNorm.class*/);

		//GstObject types
		put(gst_element_get_type(),                 new DefaultGTypeCacheRecord(Element.class));
		put(gst_clock_get_type(),                   null /*Clock.class*/);
		put(gst_date_get_type(),                    null /*GDate.class*/);
		put(gst_pipeline_get_type(),                new DefaultGTypeCacheRecord(Pipeline.class));
		put(gst_bus_get_type(),                     new DefaultGTypeCacheRecord(Bus.class));
		put(gst_pad_get_type(),                     null /*Pad.class*/);
		put(gst_pad_template_get_type(),            null /*PadTemplate.class*/);
		put(gst_ghost_pad_get_type(),               null /*GhostPad.class*/);
		put(gst_plugin_get_type(),                  null /*Plugin.class*/);
		put(gst_plugin_feature_get_type(),          null /*PluginFeature.class*/);
		put(gst_registry_get_type(),                null /*Registry.class*/);

		//GstMiniObject types
		put(gst_buffer_get_type(),                  null /*Buffer.class*/);
		put(gst_event_get_type(),                   null /*Event.class*/);
		put(gst_message_get_type(),                 new DefaultGTypeCacheRecord(Message.class));
		put(gst_query_get_type(),                   null /*Query.class*/);

		//Element types
		put(gst_base_sink_get_type(),               null /*BaseSink.class*/);
		put(gst_base_src_get_type(),                null /*BaseSrc.class*/);
		put(gst_type_find_get_type(),               null /*TypeFind.class*/);
		put(gst_element_factory_get_type(),	        null /*ElementFactory.class*/);
		put(gst_bin_get_type(),                     new DefaultGTypeCacheRecord(Bin.class));
	}};

	public static NativeLong gtypeForClass(Class<? extends INativeObject> cls) {
		if (cls == null)
			return null;
		for(Map.Entry<NativeLong, IGTypeCacheRecord> entry : cache.entrySet())
			if (entry.getValue().getJavaClass().isAssignableFrom(cls))
				return entry.getKey();
		return null;
	}

	public static Class<? extends INativeObject> classForGType(NativeLong gtype) {
		if (gtype == null)
			return null;
		
		IGTypeCacheRecord record = cache.get(gtype);
		if (record == null)
			return null;
		else
			return record.getJavaClass();
	}

	public static INativeObject instantiateForGType(NativeLong gtype, Pointer ptr) {
		if (gtype == null)
			return null;
		IGTypeCacheRecord record = cache.get(gtype);
		if (record == null)
			return null;
		else
			return record.instantiateFromPointer(ptr);
	}

	public static void initialize() {
		//Do nothing -- ensures that the type map is created, however.
	}
}

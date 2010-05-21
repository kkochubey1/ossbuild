
package ossbuild.media.gstreamer.api;

import java.util.HashMap;
import java.util.Map;
import ossbuild.media.gstreamer.signals.IElementAdded;
import ossbuild.media.gstreamer.signals.IHandoff;
import ossbuild.media.gstreamer.signals.INoMorePads;
import ossbuild.media.gstreamer.signals.INotifyCaps;
import ossbuild.media.gstreamer.signals.IPadAdded;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SignalCache {
	private static final Map<Class<? extends ISignal>, ISignalCacheRecord> cache = new HashMap<Class<? extends ISignal>, ISignalCacheRecord>(20) {{
		put(IHandoff.class, SignalCacheRecord.from(IHandoff.class));
		put(IPadAdded.class, SignalCacheRecord.from(IPadAdded.class));
		put(INotifyCaps.class, SignalCacheRecord.from(INotifyCaps.class));
		put(INoMorePads.class, SignalCacheRecord.from(INoMorePads.class));
		put(IElementAdded.class, SignalCacheRecord.from(IElementAdded.class));
	}};

	public static ISignalCacheRecord cacheRecordForSignal(ISignal signal) {
		if (signal == null)
			return null;
		return cacheRecordForClass(signal.getClass());
	}
	
	public static ISignalCacheRecord cacheRecordForClass(Class<? extends ISignal> cls) {
		if (cls == null)
			return null;
		return cache.get(cls);
	}

	public static String findSignalName(ISignal signal) {
		if (signal == null)
			return null;
		return findSignalName(signal.getClass());
	}

	@SuppressWarnings("unchecked")
	public static String findSignalName(Class<? extends ISignal> cls) {
		if (cls == null)
			return null;

		//Attempt to get it from the cache
		ISignalCacheRecord record = cacheRecordForClass(cls);
		if (record != null)
			return record.getSignalName();

		//Inspect the interfaces
		for(Class icls : cls.getInterfaces()) {
			if (icls == null || !ISignal.class.isAssignableFrom(icls))
				continue;

			record = cacheRecordForClass((Class<? extends ISignal>)icls);
			if (record != null)
				return record.getSignalName();

			cache.put(icls, (record = SignalCacheRecord.from(icls)));
			return record.getSignalName();
		}

		//Not in the cache -- look up the annotation, add it to the cache
		//and return the signal name.
		cache.put(cls, (record = SignalCacheRecord.from(cls)));
		return record.getSignalName();
	}

	public static void initialize() {
		//Do nothing -- ensures that the type map is created, however.
	}
}

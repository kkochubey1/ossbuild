
package ossbuild.media.gstreamer;

import java.util.LinkedList;
import java.util.List;
import ossbuild.media.gstreamer.api.GStreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum SeekFlags {
	  None      (GStreamer.GST_SEEK_FLAG_NONE)
	, Flush     (GStreamer.GST_SEEK_FLAG_FLUSH)
	, Accurate  (GStreamer.GST_SEEK_FLAG_ACCURATE)
	, KeyUnit   (GStreamer.GST_SEEK_FLAG_KEY_UNIT)
	, Segment   (GStreamer.GST_SEEK_FLAG_SEGMENT)
	, Skip      (GStreamer.GST_SEEK_FLAG_SKIP)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	SeekFlags(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(SeekFlags... values) {
		if (values == null || values.length <= 0)
			return 0;
		int flags = 0;
		for(SeekFlags value : values)
			flags = flags | value.nativeValue;
		return flags;
	}
	
	public static SeekFlags[] fromNative(int nativeValue) {
		List<SeekFlags> lst = new LinkedList<SeekFlags>();
		for(SeekFlags value : values())
			if ((nativeValue & ~value.nativeValue) == value.nativeValue)
				lst.add(value);
		return lst.toArray(new SeekFlags[lst.size()]);
	}
	//</editor-fold>
}

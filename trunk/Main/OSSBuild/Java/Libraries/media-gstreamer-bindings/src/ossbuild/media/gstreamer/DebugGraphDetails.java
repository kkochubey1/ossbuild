
package ossbuild.media.gstreamer;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum DebugGraphDetails {
	  ShowMediaType         ( 1 << 0)
	, ShowCapsDetails       ( 1 << 1)
	, ShowNonDefaultParams  ( 1 << 2)
	, ShowStates            ( 1 << 3)
	, ShowAll               ((1 << 4) - 1)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	DebugGraphDetails(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(DebugGraphDetails... values) {
		if (values == null || values.length <= 0)
			return 0;
		int flags = 0;
		for(DebugGraphDetails value : values)
			flags = flags | value.nativeValue;
		return flags;
	}
	
	public static DebugGraphDetails[] fromNative(int nativeValue) {
		List<DebugGraphDetails> lst = new LinkedList<DebugGraphDetails>();
		for(DebugGraphDetails value : values())
			if ((nativeValue & ~value.nativeValue) == value.nativeValue)
				lst.add(value);
		return lst.toArray(new DebugGraphDetails[lst.size()]);
	}
	//</editor-fold>
}

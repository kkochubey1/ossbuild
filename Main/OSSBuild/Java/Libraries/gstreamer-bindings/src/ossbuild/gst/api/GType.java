
package ossbuild.gst.api;

import com.sun.jna.NativeLong;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum GType {
	  Invalid   ( 0)

	, None      ( 1)
	, Interface ( 2)
	, Char      ( 3)
	, UChar     ( 4)
	, Boolean   ( 5)
	, Int       ( 6)
	, UInt      ( 7)
	, Long      ( 8)
	, ULong     ( 9)
	, Int64     (10)
	, UInt64    (11)
	, Enum      (12)
	, Flags     (13)
	, Float     (14)
	, Double    (15)
	, String    (16)
	, Pointer   (17)
	, Boxed     (18)
	, Param     (19)
	, Object    (20)

	, User      (-1)
	;

	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		  G_TYPE_FUNDAMENTAL_SHIFT = 2
		, G_TYPE_RESERVED_USER_FIRST = 49
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	static Map<Long, IGTypeConverter> converters;
	final long nativeValue;
	final NativeLong nativeLong;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	GType(long nativeValue) {
		this.nativeValue = G_TYPE_MAKE_FUNDAMENTAL(nativeValue);
		this.nativeLong = new NativeLong(this.nativeValue);
	}

	GType(long nativeValue, IGTypeConverter converter) {
		this(nativeValue);
		addCustomConverter(nativeValue, converter);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public long getNativeValue() {
		return nativeValue;
	}

	public NativeLong getNativeLong() {
		return nativeLong;
	}

	public NativeLong asNativeLong() {
		return nativeLong;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helpers">
	public static long G_TYPE_MAKE_FUNDAMENTAL(long value) {
		return (value << G_TYPE_FUNDAMENTAL_SHIFT);
	}

	public static int G_TYPE_UNMAKE_FUNDAMENTAL(long value) {
		return (int)(value >> G_TYPE_FUNDAMENTAL_SHIFT);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static boolean clearCustomConverters() {
		if (converters == null || converters.isEmpty())
			return true;
		converters.clear();
		return true;
	}

	public static boolean removeCustomConverter(NativeLong nativeValue) {
		return removeCustomConverter(nativeValue.longValue());
	}

	public static boolean removeCustomConverter(long nativeValue) {
		if (converters == null || converters.isEmpty())
			return false;
		return (converters.remove(nativeValue) != null);
	}

	public static boolean addCustomConverter(NativeLong nativeValue, IGTypeConverter converter) {
		return addCustomConverter(nativeValue, converter);
	}

	public static boolean addCustomConverter(long nativeValue, IGTypeConverter converter) {
		if (converter == null)
			return false;
		if (G_TYPE_UNMAKE_FUNDAMENTAL(nativeValue) < G_TYPE_RESERVED_USER_FIRST)
			throw new IllegalArgumentException("Cannot add a custom converter for a GType that is below the reserved glib list");
		
		if (converters == null)
			converters = new HashMap<Long, IGTypeConverter>(1, 1.0f);
		converters.put(nativeValue, converter);
		return true;
	}

	public static IGTypeConverter customConverter(NativeLong nativeValue) {
		return customConverter(nativeValue.longValue());
	}

	public static IGTypeConverter customConverter(long nativeValue) {
		if (converters == null || converters.isEmpty())
			return null;
		return converters.get(nativeValue);
	}

	public static long toNative(GType item) {
		return item.nativeValue;
	}

	public static GType fromNative(NativeLong nativeValue) {
		return fromNative(nativeValue.longValue());
	}
	
	public static GType fromNative(long nativeValue) {
		//Attempt to skip directly to the GType in question
		int val = G_TYPE_UNMAKE_FUNDAMENTAL(nativeValue);
		if (val < 0 || val > values().length)
			return User;
		return values()[val];
	}
	//</editor-fold>
}


package ossbuild.gst.api;

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
	;

	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		G_TYPE_FUNDAMENTAL_SHIFT = 2
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	GType(int nativeValue) {
		this.nativeValue = G_TYPE_MAKE_FUNDAMENTAL(nativeValue);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helpers">
	private static int G_TYPE_MAKE_FUNDAMENTAL(int value) {
		return (value << G_TYPE_FUNDAMENTAL_SHIFT);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(GType item) {
		return item.nativeValue;
	}
	
	public static GType fromNative(int nativeValue) {
		for(GType item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Invalid;
	}
	//</editor-fold>
}

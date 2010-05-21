
package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum Rank {
	  None      (0)
	, Marginal  (64)
	, Secondary (128)
	, Primary   (256)

	, Other     (~0)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	Rank(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int asInt() {
		return nativeValue;
	}
	
	public int getValue() {
		return nativeValue;
	}
	
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static boolean atLeastNone(int rank) {
		return rank <= None.nativeValue;
	}

	public static boolean atLeastMarginal(int rank) {
		return rank >= Marginal.nativeValue;
	}

	public static boolean atLeastSecondary(int rank) {
		return rank >= Secondary.nativeValue;
	}

	public static boolean atLeastPrimary(int rank) {
		return rank >= Primary.nativeValue;
	}

	public static boolean isOtherValue(int rank) {
		return (fromNative(rank) == Other);
	}

	public static int toNative(Rank item) {
		return item.nativeValue;
	}
	
	public static Rank fromNative(int nativeValue) {
		for(Rank item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Other;
	}
	//</editor-fold>
}

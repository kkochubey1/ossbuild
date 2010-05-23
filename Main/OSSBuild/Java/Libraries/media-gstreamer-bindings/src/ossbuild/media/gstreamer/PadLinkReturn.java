
package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum PadLinkReturn {
	  Unknown           (~0)

	, OK                ( 0)
	, WrongHierarchy    (-1)
	, WasLinked         (-2)
	, WrongDirection	(-3)
	, NoFormat          (-4)
	, NoSched           (-5)
	, Refused           (-6)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	PadLinkReturn(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}

	public boolean isSuccessful() {
		return (this.nativeValue >= OK.nativeValue && this.nativeValue != Unknown.nativeValue);
	}

	public boolean isFailure() {
		return (this.nativeValue < OK.nativeValue || this.nativeValue == Unknown.nativeValue);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(PadLinkReturn item) {
		return item.nativeValue;
	}
	
	public static PadLinkReturn fromNative(int nativeValue) {
		for(PadLinkReturn item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Unknown;
	}
	//</editor-fold>
}

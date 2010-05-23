
package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum PadDirection {
	  Unknown   (0)
	, Src       (1)
	, Sink      (2)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	PadDirection(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(PadDirection item) {
		return item.nativeValue;
	}
	
	public static PadDirection fromNative(int nativeValue) {
		for(PadDirection item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Unknown;
	}
	//</editor-fold>
}

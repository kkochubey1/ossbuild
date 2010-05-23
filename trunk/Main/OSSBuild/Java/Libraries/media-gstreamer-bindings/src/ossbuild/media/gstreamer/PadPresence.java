
package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum PadPresence {
	  Unknown   (~0)
	
	, Always    ( 0)
	, Sometimes ( 1)
	, Request   ( 2)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	PadPresence(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(PadPresence item) {
		return item.nativeValue;
	}
	
	public static PadPresence fromNative(int nativeValue) {
		for(PadPresence item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Unknown;
	}
	//</editor-fold>
}

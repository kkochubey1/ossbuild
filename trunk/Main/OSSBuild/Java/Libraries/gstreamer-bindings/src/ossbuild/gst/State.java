
package ossbuild.gst;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum State {
	  Unknown       (~0)

	, VoidPending   (0)
	, Null          (1)
	, Ready         (2)
	, Paused        (3)
	, Playing       (4)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	State(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(State item) {
		return item.nativeValue;
	}
	
	public static State fromNative(int nativeValue) {
		for(State item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Unknown;
	}
	//</editor-fold>
}

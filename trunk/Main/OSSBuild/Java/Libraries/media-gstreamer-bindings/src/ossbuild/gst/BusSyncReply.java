
package ossbuild.gst;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum BusSyncReply {
	  Unknown(~0)

	, Drop(0)
	, Pass(1)
	, Async(2)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	BusSyncReply(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(BusSyncReply item) {
		return item.nativeValue;
	}
	
	public static BusSyncReply fromNative(int nativeValue) {
		for(BusSyncReply item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Unknown;
	}
	//</editor-fold>
}

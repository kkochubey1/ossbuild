
package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum Format {
	  Undefined (0)

	, Default   (1)
	, Bytes     (2)
	, Time      (3)
	, Buffers   (4)
	, Percent   (5)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	Format(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(Format item) {
		return item.nativeValue;
	}
	
	public static Format fromNative(int nativeValue) {
		for(Format item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Undefined;
	}
	//</editor-fold>
}

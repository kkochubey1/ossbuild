
package ossbuild.media.gstreamer.elements;

public enum VideoTestSrcPattern {
	  SMPTE(0)
	, Snow(1)
	, Black(2)
	, White(3)
	, Red(4)
	, Green(5)
	, Blue(6)
	, Checkers1(7)
	, Checkers2(8)
	, Checkers4(9)
	, Checkers8(10)
	, Circular(11)
	, Blink(12)
	, SMPTE75(13)
	, ZonePlate(14)
	, Gamut(15)
	;

	//<editor-fold defaultstate="collapsed" desc="Constants">
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	VideoTestSrcPattern(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(VideoTestSrcPattern item) {
		return item.nativeValue;
	}

	public static VideoTestSrcPattern fromNative(int nativeValue) {
		for(VideoTestSrcPattern item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return SMPTE;
	}
	//</editor-fold>
}


package simple.swt.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum ErrorType {
	  Unknown(0)
	
	, GeneralFailure(1)
	, TypeNotFound(4)
	, CodecNotFound(6)
	, DecodeFailed(7)
	, EncodeFailed(8)
	, DemuxFailed(9)
	, MuxFailed(10)
	, InvalidFormat(11)
	, DecryptFailed(12)
	, DecryptFailedWithNoKey(13)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	private ErrorType(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static ErrorType fromNativeValue(int value) {
		for(ErrorType et : ErrorType.values())
			if (et.nativeValue == value)
				return et;
		return ErrorType.Unknown;
	}
	//</editor-fold>
}

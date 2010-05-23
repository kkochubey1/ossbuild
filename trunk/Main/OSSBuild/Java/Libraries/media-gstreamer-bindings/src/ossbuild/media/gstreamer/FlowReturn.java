
package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum FlowReturn {
	  Unknown           (  ~0)

	, CustomSuccess2    ( 102)
	, CustomSuccess1    ( 101)
	, CustomSuccess     ( 100)

	, Resend            (   1)
	, OK                (   0)
	, NotLinked         (  -1)
	, WrongState        (  -2)
	, Unexpected        (  -3)
	, NotNegotiated     (  -4)
	, Error             (  -5)
	, NotSupported      (  -6)
	
	, CustomError       (-100)
	, CustomError1      (-101)
	, CustomError2      (-102)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	FlowReturn(int nativeValue) {
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
	public static int toNative(FlowReturn item) {
		return item.nativeValue;
	}
	
	public static FlowReturn fromNative(int nativeValue) {
		for(FlowReturn item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Unknown;
	}
	//</editor-fold>
}


package ossbuild.gst;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum MessageType {
	  Unknown           (0)

	, EOS               (1 <<  0)
	, Error             (1 <<  1)
	, Warning           (1 <<  2)
	, Info              (1 <<  3)
	, Tag               (1 <<  4)
	, Buffering         (1 <<  5)
	, StateChanged      (1 <<  6)
	, StateDirty        (1 <<  7)
	, StepDone          (1 <<  8)
	, ClockProvide      (1 <<  9)
	, ClockLost         (1 << 10)
	, NewClock          (1 << 11)
	, StructureChange   (1 << 12)
	, StreamStatus      (1 << 13)
	, Application       (1 << 14)
	, Element           (1 << 15)
	, SegmentStart      (1 << 16)
	, SegmentDone       (1 << 17)
	, Duration          (1 << 18)
	, Latency           (1 << 19)
	, AsyncStart        (1 << 20)
	, AsyncDone         (1 << 21)
	, RequestState      (1 << 22)
	, StepStart         (1 << 23)
	, QOS               (1 << 24)

	, Any               (~0)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	MessageType(int nativeValue) {
		this.nativeValue = nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static int toNative(MessageType item) {
		return item.nativeValue;
	}
	
	public static MessageType fromNative(int nativeValue) {
		for(MessageType item : values())
			if (item.nativeValue == nativeValue)
				return item;
		return Unknown;
	}
	//</editor-fold>
}

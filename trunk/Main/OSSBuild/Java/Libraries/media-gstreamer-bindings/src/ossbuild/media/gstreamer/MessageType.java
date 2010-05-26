
package ossbuild.media.gstreamer;

import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.signals.IBuffering;
import ossbuild.media.gstreamer.signals.IEndOfStream;
import ossbuild.media.gstreamer.signals.IError;
import ossbuild.media.gstreamer.signals.ISegmentDone;
import ossbuild.media.gstreamer.signals.IStateChanged;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public enum MessageType {
	  Unknown           (0)

	, EOS               (1 <<  0, IEndOfStream.class    )
	, Error             (1 <<  1, IError.class          )
	, Warning           (1 <<  2, null                  )
	, Info              (1 <<  3, null                  )
	, Tag               (1 <<  4, null                  )
	, Buffering         (1 <<  5, IBuffering.class      )
	, StateChanged      (1 <<  6, IStateChanged.class   )
	, StateDirty        (1 <<  7, null                  )
	, StepDone          (1 <<  8, null                  )
	, ClockProvide      (1 <<  9, null                  )
	, ClockLost         (1 << 10, null                  )
	, NewClock          (1 << 11, null                  )
	, StructureChange   (1 << 12, null                  )
	, StreamStatus      (1 << 13, null                  )
	, Application       (1 << 14, null                  )
	, Element           (1 << 15, null                  )
	, SegmentStart      (1 << 16, null                  )
	, SegmentDone       (1 << 17, ISegmentDone.class    )
	, Duration          (1 << 18, null                  )
	, Latency           (1 << 19, null                  )
	, AsyncStart        (1 << 20, null                  )
	, AsyncDone         (1 << 21, null                  )
	, RequestState      (1 << 22, null                  )
	, StepStart         (1 << 23, null                  )
	, QOS               (1 << 24, null                  )

	, Any               (~0)
	;

	//<editor-fold defaultstate="collapsed" desc="Variables">
	final int nativeValue;
	final Class<? extends ISignal> signalClass;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	MessageType(int nativeValue) {
		this(nativeValue, null);
	}

	MessageType(int nativeValue, Class<? extends ISignal> signalClass) {
		this.nativeValue = nativeValue;
		this.signalClass = signalClass;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeValue() {
		return nativeValue;
	}

	public Class<? extends ISignal> getSignalClass() {
		return signalClass;
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

	public static MessageType fromSignalClass(Class<? extends ISignal> cls) {
		if (cls == null)
			return Unknown;
		for(MessageType item : values()) {
			if (item.signalClass == null)
				continue;
			if (item.signalClass.isAssignableFrom(cls))
				return item;
		}
		return Unknown;
	}

	public static Class<? extends ISignal> toSignalClass(MessageType item) {
		return item.signalClass;
	}
	//</editor-fold>
}


package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SignalConnectResponse implements ISignalConnectResponse {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private NativeLong handlerID;
	private ISignal signal;
	private String signalName;
	private Pointer instance;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public SignalConnectResponse(ISignal signal, String signalName, Pointer instance, NativeLong handlerID) {
		this.signal = signal;
		this.instance = instance;
		this.handlerID = handlerID;
		this.signalName = signalName;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public NativeLong getHandlerID() {
		return handlerID;
	}

	@Override
	public ISignal getSignal() {
		return signal;
	}

	@Override
	public String getSignalName() {
		return signalName;
	}

	@Override
	public Pointer getInstance() {
		return instance;
	}
	//</editor-fold>
}

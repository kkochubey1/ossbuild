
package ossbuild.media.gstreamer;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.Callbacks;
import ossbuild.media.gstreamer.api.GStreamer;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;
import ossbuild.media.gstreamer.api.SignalCache;
import ossbuild.media.gstreamer.api.SignalConnectResponse;
import static ossbuild.media.gstreamer.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class BaseGObject extends BaseNativeObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	protected BaseGObject() {
		super();
	}

	protected BaseGObject(Pointer ptr) {
		super(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	@Override
	public void ref() {
		g_object_ref(ptr);
	}

	@Override
	public void unref() {
		g_object_unref(ptr);
	}

	@Override
	public int refCount() {
		//It's one pointer off from this one b/c a GObject struct looks like:
		//struct _GObject
		//{
		//    GTypeInstance  g_type_instance; <-- Contains 1 pointer to the class
		//
		//    volatile guint ref_count;
		//    GData         *qdata;
		//};
		return ptr.getInt(Pointer.SIZE);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Signals">
	public ISignalConnectResponse connect(ISignal signal) {
		if (signal == null)
			return null;
		
		String signalName = SignalCache.findSignalName(signal);
		if (signalName == null)
			return null;


		NativeLong handlerID = GStreamer.connectSignal(ptr, signalName, signal);
		if (handlerID == null || handlerID.intValue() == 0)
			return null;

		try {
			Callbacks.register(ptr, signal);
		} catch(Throwable t) {
			GStreamer.disconnectSignal(ptr, handlerID);
			return null;
		}

		return new SignalConnectResponse(signal, signalName, ptr, handlerID);
	}

	public boolean disconnect(ISignalConnectResponse response) {
		if (response == null)
			return false;

		if (!Callbacks.unregister(response.getInstance(), response.getSignal()))
			return false;

		return GStreamer.disconnectSignal(response.getInstance(), response.getHandlerID());
	}
	//</editor-fold>
}

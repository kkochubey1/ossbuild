
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
public abstract class BaseGObject implements INativeObject, IDisposable {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	boolean managed = true;
	boolean disposed = false;
	Pointer ptr = Pointer.NULL;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		GStreamer.initialize();
	}

	protected BaseGObject() {
	}

	protected BaseGObject(Pointer ptr) {
		this.ptr = ptr;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public final Pointer getPointer() {
		return ptr;
	}

	@Override
	public final boolean isManaged() {
		return managed;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	public final boolean isDisposed() {
		return disposed;
	}

	@Override
	protected final void finalize() throws Throwable {
		super.finalize();
		dispose();
	}

	@Override
	public synchronized final void dispose() {
		if (disposed)
			return;
		disposed = true;
		disposeObject();
	}

	protected void disposeObject() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Equals">
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + (this.ptr != null ? this.ptr.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return (ptr == Pointer.NULL);
		if (obj instanceof BaseGObject)
			return equals((BaseGObject)obj);
		return super.equals(obj);
	}

	@Override
	public boolean equals(INativeObject obj) {
		if (obj == null)
			return (ptr == Pointer.NULL);
		return equals(obj.getPointer());
	}

	public boolean equals(BaseGObject obj) {
		if (obj == null)
			return (ptr == Pointer.NULL);
		return equals(obj.ptr);
	}

	@Override
	public boolean equals(Pointer objPtr) {
		if (ptr != Pointer.NULL && (objPtr == null || objPtr == Pointer.NULL))
			return false;
		else if (ptr == Pointer.NULL && (objPtr == null || objPtr == Pointer.NULL))
			return true;
		else if (ptr == Pointer.NULL && (objPtr != null && objPtr != Pointer.NULL))
			return false;
		else
			return ptr.equals(objPtr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	@Override
	public void ref() {
		g_object_ref(ptr);
	}

	public void ref(int times) {
		for (int i = 0; i < times; ++i)
			g_object_ref(ptr);
	}

	@Override
	public void unref() {
		g_object_unref(ptr);
	}

	public void unref(int times) {
		for (int i = 0; i < times; ++i)
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

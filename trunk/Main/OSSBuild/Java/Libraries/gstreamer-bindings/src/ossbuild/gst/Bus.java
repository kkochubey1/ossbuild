
package ossbuild.gst;

import com.sun.jna.Pointer;
import ossbuild.gst.api.Callbacks;
import ossbuild.gst.api.ICallback;
import ossbuild.gst.callbacks.IBusSyncHandler;
import static ossbuild.gst.api.GObject.*;
import static ossbuild.gst.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Bus extends GObject implements IBus {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private ICallback syncHandler;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Bus() {
		super();
		this.ptr = gst_bus_new();
		this.managed = true;
	}

	Bus(Pointer ptr) {
		super();
		this.ptr = ptr;
		this.managed = false;
	}

	static Bus from(Pointer ptr) {
		return new Bus(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		if (ptr == Pointer.NULL)
			return;

		if (managed)
			g_object_unref(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public void syncHandler(final IBusSyncHandler handler) {
		//Make sure we hold onto this
		ICallback newSyncHandler = (handler == null ? null : new ICallback() {
			public int callback(Pointer pBus, Pointer pMsg, Pointer pData) {
				Bus bus = Bus.from(pBus);
				Message message = Message.from(pMsg);
				BusSyncReply reply = handler.handle(bus, message, message.getSource(), pData);
				if (reply == BusSyncReply.Drop)
					message.unref();
				return reply.nativeValue;
			}
		});

		if (newSyncHandler != null) {
			if (Callbacks.register(ptr, newSyncHandler))
				gst_bus_set_sync_handler(ptr, syncHandler = newSyncHandler, Pointer.NULL);
		} else {
			//Clear the sync handler
			if (syncHandler != null) {
				Callbacks.unregister(ptr, syncHandler);
				syncHandler = null;
			}
			gst_bus_set_sync_handler(ptr, null, Pointer.NULL);
		}
	}
	//</editor-fold>
}

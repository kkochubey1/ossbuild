
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.Callbacks;
import ossbuild.media.gstreamer.api.ICallback;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Bus extends BaseGObject implements IBus {
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


package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import ossbuild.media.gstreamer.api.Callbacks;
import ossbuild.media.gstreamer.api.GError;
import ossbuild.media.gstreamer.api.ICallback;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;
import ossbuild.media.gstreamer.api.SignalConnectResponse;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;
import ossbuild.media.gstreamer.signals.IBuffering;
import ossbuild.media.gstreamer.signals.IEndOfStream;
import ossbuild.media.gstreamer.signals.IError;
import ossbuild.media.gstreamer.signals.ISegmentDone;
import ossbuild.media.gstreamer.signals.IStateChanged;
import static ossbuild.media.gstreamer.api.GLib.*;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Bus extends BaseGObject implements IBus {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private SyncHandler syncHandler = null;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Bus() {
		super();
		this.ptr = gst_bus_new();
		this.managed = true;
		init();
	}

	protected Bus(Pointer ptr) {
		super();
		this.ptr = ptr;
		this.managed = false;
		init();
	}

	protected void init() {
		//Fetch our sync handler from the cached set of callbacks.
		synchronized(Callbacks.lock()) {
			Set<ICallback> callbacks = Callbacks.forPointer(ptr);
			if (callbacks != null && callbacks.size() > 0) {
				ICallback callback = callbacks.iterator().next();
				if (callback != null && callback instanceof SyncHandler)
					syncHandler = (SyncHandler)callback;
			}

			if (syncHandler == null) {
				Callbacks.register(ptr, syncHandler = new SyncHandler());

				gst_bus_set_sync_handler(ptr, null, Pointer.NULL);
				gst_bus_set_sync_handler(ptr, syncHandler, Pointer.NULL);
			}
		}
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

	//<editor-fold defaultstate="collapsed" desc="Classes">
	private class SyncHandler implements ICallback {
		//<editor-fold defaultstate="collapsed" desc="Variables">
		private IBusSyncHandler handler = null;
		private final Map<MessageType, Set<ISignal>> messageSignalCallbacks = new HashMap<MessageType, Set<ISignal>>(0);
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		public SyncHandler() {
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
		public IBusSyncHandler getHandler() {
			return handler;
		}

		public void setHandler(IBusSyncHandler handler) {
			this.handler = handler;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Callback">
		public int callback(Pointer pBus, Pointer pMsg, Pointer pData) {
			Bus bus = Bus.from(pBus);
			Message message = Message.from(pMsg);

			BusSyncReply reply = (handler != null ? handler.handle(bus, message, message.getSource(), pData) : BusSyncReply.Pass);

			if (reply != BusSyncReply.Drop) {
				//Now call our "bus signals"
				MessageType messageType = message.getMessageType();
				Set<ISignal> signals = messageSignalCallbacks.get(messageType);
				if (signals != null && signals.size() > 0) {
					switch(messageType) {
						case StateChanged:
							onStateChanged(messageType, message, signals);
							break;
						case EOS:
							onEndOfStream(messageType, message, signals);
							break;
						case Error:
							onError(messageType, message, signals);
							break;
						case Buffering:
							onBuffering(messageType, message, signals);
							break;
						case SegmentDone:
							onSegmentDone(messageType, message, signals);
							break;
					}
				}
			}

			if (reply == BusSyncReply.Drop)
				message.unref();

			bus.dispose();
			return reply.nativeValue;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Bus Message Handlers">
		protected void onStateChanged(MessageType msgType, Message msg, Set<ISignal> signals) {
			IntByReference refOldState = new IntByReference();
			IntByReference refNewState = new IntByReference();
			IntByReference refPendingState = new IntByReference();

			gst_message_parse_state_changed(msg.getPointer(), refOldState, refNewState, refPendingState);

			State oldState = State.fromNative(refOldState.getValue());
			State newState = State.fromNative(refNewState.getValue());
			State pendingState = State.fromNative(refPendingState.getValue());

			Pointer src = msg.getSource();
			
			for(ISignal signal : signals)
				((IStateChanged)signal).stateChanged(src, oldState, newState, pendingState);
		}

		protected void onEndOfStream(MessageType msgType, Message msg, Set<ISignal> signals) {
			Pointer src = msg.getSource();
			
			for(ISignal signal : signals)
				((IEndOfStream)signal).endOfStream(src);
		}

		protected void onError(MessageType msgType, Message msg, Set<ISignal> signals) {
			Pointer src = msg.getSource();

			PointerByReference refErr = new PointerByReference();
			PointerByReference refDebug = new PointerByReference();

			gst_message_parse_error(msg.getPointer(), refErr, refDebug);

			Pointer pErr = refErr.getValue();
			Pointer pDebug = refDebug.getValue();

			try {
				String errDebug = pDebug.getString(0L);
				GError err = new GError(pErr);
				int errCode = err.getCode();
				String errMsg = err.getMessage();
				
				for(ISignal signal : signals)
					((IError)signal).error(src, errCode, errMsg);
			} finally {
				g_error_free(pErr);
				g_free(pDebug);
			}
		}

		protected void onBuffering(MessageType msgType, Message msg, Set<ISignal> signals) {
			Pointer src = msg.getSource();

			IntByReference refPercent = new IntByReference();

			gst_message_parse_buffering(msg.getPointer(), refPercent);

			int percent = refPercent.getValue();

			for(ISignal signal : signals)
				((IBuffering)signal).buffering(src, percent);
		}

		protected void onSegmentDone(MessageType msgType, Message msg, Set<ISignal> signals) {
			Pointer src = msg.getSource();

			IntByReference refFormat = new IntByReference();
			LongByReference refPosition = new LongByReference();

			gst_message_parse_segment_done(src, refFormat, refPosition);

			Format format = Format.fromNative(refFormat.getValue());
			long position = refPosition.getValue();

			for(ISignal signal : signals)
				((ISegmentDone)signal).segmentDone(src, format, position);
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Public Methods">
		public ISignalConnectResponse register(String signalName, ISignal signal) {
			if (signal == null)
				return null;

			//Locate the message type for this signal. If there's no class for it,
			//then get out of here.
			MessageType typeForSignal = MessageType.fromSignalClass(signal.getClass());
			if (typeForSignal == MessageType.Unknown)
				return null;

			synchronized(messageSignalCallbacks) {
				Set<ISignal> callbacks;
				if ((callbacks = messageSignalCallbacks.get(typeForSignal)) == null)
					messageSignalCallbacks.put(typeForSignal, callbacks = new LinkedHashSet<ISignal>(1));

				if (!callbacks.add(signal))
					return null;

				return new SignalConnectResponse(signal, signalName, ptr, null);
			}
		}

		public boolean unregister(ISignalConnectResponse response) {
			if (response == null)
				return false;

			ISignal signal = response.getSignal();
			MessageType typeForSignal = MessageType.fromSignalClass(signal.getClass());
			if (typeForSignal == MessageType.Unknown)
				return false;

			synchronized(messageSignalCallbacks) {
				Set<ISignal> callbacks;
				if ((callbacks = messageSignalCallbacks.get(typeForSignal)) == null)
					return false;

				//Take him out of the list of callbacks.
				boolean ret = callbacks.remove(signal);

				//If the callbacks are now empty, then remove the set
				//as well to keep memory usage at a minimum.
				if (callbacks.size() <= 0)
					messageSignalCallbacks.remove(typeForSignal);
				
				return ret;
			}
		}
		//</editor-fold>
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public ISignalConnectResponse connect(String signalName, ISignal signal) {
		return syncHandler.register(signalName, signal);
	}

	@Override
	public boolean disconnect(ISignalConnectResponse response) {
		return syncHandler.unregister(response);
	}

	@Override
	public void syncHandler(final IBusSyncHandler handler) {
		syncHandler.setHandler(handler);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static Bus from(Pointer ptr) {
		return new Bus(ptr);
	}
	//</editor-fold>
}

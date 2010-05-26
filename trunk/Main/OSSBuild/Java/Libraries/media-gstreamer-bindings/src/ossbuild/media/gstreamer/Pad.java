
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Pad extends BaseGstObject {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	boolean ownedByParent = false;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Pad(String name, PadDirection direction) {
		super(name, direction);
		this.managed = true;
		this.ownedByParent = false;
	}

	public Pad(String name, PadTemplate template) {
		super(name, template);
		this.managed = true;
		this.ownedByParent = false;
	}

	protected Pad(Pointer p) {
		super(p);
		this.managed = false;
		this.ownedByParent = true;
	}

	protected Pad(Pointer p, boolean incRef) {
		super(p, incRef);
		this.managed = false;
		this.ownedByParent = true;
	}

	@Override
	protected Pointer createNativeObject(Object... arguments) {
		if (arguments.length == 2) {
			String name = (String)arguments[0];
			if (arguments[1] instanceof PadDirection) {

				PadDirection direction = (PadDirection)arguments[1];
				return gst_pad_new(name, direction.getNativeValue());

			} else if (arguments[1] instanceof PadTemplate) {

				PadTemplate template = (PadTemplate)arguments[1];
				return gst_pad_new_from_template(template.getPointer(), name);

			}
		}
		return Pointer.NULL;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		if (ptr == Pointer.NULL)
			return;

		synchronized(this) {
			if (!ownedByParent)
				unref();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public Caps getCaps() {
		Pointer p = gst_pad_get_caps(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		return Caps.from(p, false, false);
	}

	public boolean setCaps(Caps caps) {
		boolean ret = gst_pad_set_caps(ptr, caps.getPointer());
		//According to the documentation:
		//
		//Any previous caps on the pad will be unreffed. This function
		//refs the caps so you should unref if as soon as you don't need
		//it anymore.
		if (ret && caps != null)
			caps.dispose();
		return ret;
	}

	public Caps getAllowedCaps() {
		Pointer p = gst_pad_get_allowed_caps(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		return Caps.from(p, false, false);
	}

	public Caps getNegotiatedCaps() {
		Pointer p = gst_pad_get_negotiated_caps(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		return Caps.from(p, false, false);
	}

	public Pad getPeer() {
		Pointer p = gst_pad_get_peer(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		return Pad.from(p, false, true);
	}

	public boolean isLinked() {
		return gst_pad_is_linked(ptr);
	}

	public PadDirection getDirection() {
		return PadDirection.fromNative(gst_pad_get_direction(ptr));
	}

	public IElement getParentElement() {
		Pointer p = gst_pad_get_parent_element(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		return Element.from(p, false);
	}

	public boolean isActive() {
		return gst_pad_is_active(ptr);
	}
	
	public boolean setActive(boolean active) {
		return gst_pad_set_active(ptr, active);
	}

	public boolean isBlocked() {
		return gst_pad_is_blocked(ptr);
	}

	public boolean setBlocked(boolean block) {
		return gst_pad_set_blocked(ptr, block);
	}

	public boolean isBlocking() {
		return gst_pad_is_blocking(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Package Methods">
	Object ownershipLock() {
		return this;
	}

	void takeOwnership() {
		synchronized(this) {
			//See disposeObject() documentation
			ownedByParent = true;
		}
	}

	void releaseOwnership() {
		synchronized(this) {
			//See disposeObject() documentation
			ownedByParent = false;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public boolean activate() {
		return setActive(true);
	}

	public boolean deactivate() {
		return setActive(false);
	}

	public boolean block() {
		return setBlocked(true);
	}

	public boolean unblock() {
		return setBlocked(false);
	}
	
	public boolean acceptsCaps(Caps caps) {
		return gst_pad_accept_caps(ptr, caps.getPointer());
	}

	public boolean peerAcceptsCaps(Caps caps) {
		return gst_pad_peer_accept_caps(ptr, caps.getPointer());
	}

	public PadLinkReturn link(Pad pad) {
		return PadLinkReturn.fromNative(gst_pad_link(ptr, pad.getPointer()));
	}

	public boolean unlink(Pad pad) {
		return gst_pad_unlink(ptr, pad.getPointer());
	}

	public boolean canLink(Pad pad) {
		return gst_pad_can_link(ptr, pad.getPointer());
	}

	public boolean sendEvent(IEvent event) {
		if (event == null)
			return false;

		//According to the documentation:
		//
		//This function takes owership of the provided event so you should
		//gst_event_ref() it if you want to reuse the event after this call.
		//
		//So we ref() it here b/c if you've created it from hand using
		//gst_event_new() or like methods, the ref count == 1. Taking
		//ownership means that the parent will then be responsbile for
		//unreffing it, but the ref count will remain the same. By doing
		//a ref() here, we ensure that the ref count will be zero after
		//the parent unrefs and the object is disposed or finalized.
		event.ref();
		boolean ret = gst_pad_send_event(ptr, event.getPointer());
		//If the call didn't work for some reason, then make sure that
		//we unref() so there's not a memory leak when the object is
		//disposed or finalized.
		if (!ret)
			event.unref();
		return ret;
	}

	public boolean pushEvent(IEvent event) {
		if (event == null)
			return false;

		//According to the documentation:
		//
		//This function takes owership of the provided event so you should
		//gst_event_ref() it if you want to reuse the event after this call.
		//
		//So we ref() it here b/c if you've created it from hand using
		//gst_event_new() or like methods, the ref count == 1. Taking
		//ownership means that the parent will then be responsbile for
		//unreffing it, but the ref count will remain the same. By doing
		//a ref() here, we ensure that the ref count will be zero after
		//the parent unrefs and the object is disposed or finalized.
		event.ref();
		boolean ret = gst_pad_push_event(ptr, event.getPointer());
		//If the call didn't work for some reason, then make sure that
		//we unref() so there's not a memory leak when the object is
		//disposed or finalized.
		if (!ret)
			event.unref();
		return ret;
	}

	public FlowReturn chain(Buffer buffer) {
		return FlowReturn.fromNative(gst_pad_chain(ptr, buffer.getPointer()));
	}

	public Buffer pullRange(long offset, int size) {
		PointerByReference ref = new PointerByReference();
		FlowReturn ret = FlowReturn.fromNative(gst_pad_pull_range(ptr, offset, size, ref));
		if (ret != FlowReturn.OK)
			return null;
		return Buffer.from(ref.getValue(), false);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static Pad from(Pointer p) {
		return new Pad(p);
	}

	public static Pad from(Pointer p, boolean incRef) {
		return new Pad(p, incRef);
	}

	public static Pad from(Pointer p, boolean incRef, boolean unref) {
		Pad pad = new Pad(p, incRef);
		if (unref)
			pad.unref();
		return pad;
	}
	//</editor-fold>
}

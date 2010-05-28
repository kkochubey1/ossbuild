
package ossbuild.media.gstreamer;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import ossbuild.StringUtil;
import static ossbuild.media.gstreamer.api.GLib.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;
import static ossbuild.media.gstreamer.api.GStreamerInterfaces.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
abstract class BaseGstElement extends BaseGstObject implements IElement {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected Integer factoryRank;
	protected String factoryName, factoryClass;
	boolean ownedByParent = false;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public BaseGstElement(String factoryName) {
		this(factoryName, null);
	}

	public BaseGstElement(String factoryName, String elementName) {
		super(factoryName, elementName);
		this.managed = true;
		this.ownedByParent = false;
	}

	BaseGstElement(Pointer ptr) {
		super(ptr);
		this.managed = false;
		this.ownedByParent = true;
	}

	BaseGstElement(Pointer ptr, boolean incRef) {
		super(ptr, incRef);
		this.managed = false;
		this.ownedByParent = true;
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

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	@Override
	public boolean hasParent() {
		return parentExists(this);
	}

	@Override
	public String getName() {
		if (ptr == null || ptr == Pointer.NULL)
			return StringUtil.empty;

		Pointer pName = gst_object_get_name(ptr);
		String name = pName.getString(0L);
		g_free(pName);

		return name;
	}

	@Override
	public int getFactoryRank() {
		if (factoryRank != null)
			return factoryRank;
		return (factoryRank = factoryRank(this));
	}

	@Override
	public String getFactoryName() {
		if (factoryName != null)
			return factoryName;
		return (factoryName = factoryName(this));
	}

	@Override
	public String getFactoryClass() {
		if (factoryClass != null)
			return factoryClass;
		return (factoryClass = factoryClass(this));
	}

	@Override
	public boolean setCaps(Caps caps) {
		//TODO: Check for memory leaks
		boolean ret = false;
		synchronized(caps.ownershipLock()) {
			caps.takeOwnership();
			try {
				ret = set("caps", caps);
			} finally {
				if (!ret)
					caps.releaseOwnership();
			}
		}
		return ret;
	}

	@Override
	public long getBaseTime() {
		return gst_element_get_base_time(ptr).longValue();
	}

	@Override
	public long getStartTime() {
		return gst_element_get_start_time(ptr).longValue();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return getName();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="State">
	@Override
	public State requestState() {
		return requestState(TimeUnit.NANOSECONDS, 0L);
	}

	@Override
	public State requestState(long timeout) {
		return requestState(TimeUnit.NANOSECONDS, timeout);
	}

	@Override
	public State requestState(TimeUnit unit, long timeout) {
		IntByReference state = new IntByReference();
		IntByReference pending = new IntByReference();
		gst_element_get_state(ptr, state, pending, unit.toNanos(timeout));
		return State.fromNative(state.getValue());
	}

	@Override
	public StateChangeReturn changeState(State state) {
		return StateChangeReturn.fromNative(gst_element_set_state(ptr, state.nativeValue));
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
	@Override
	public boolean sendEvent(Event ev) {
		if (ev == null)
			return false;
		ev.ref();
		boolean ret = gst_element_send_event(ptr, ev.getPointer());
		if (!ret)
			ev.unref();
		return ret;
	}

	@Override
	public Pad staticPad(String name) {
		Pointer p = gst_element_get_static_pad(ptr, name);
		if (p == null || p == Pointer.NULL)
			return null;
		return Pad.from(p, false, true);
	}

	@Override
	public boolean addPad(Pad pad) {
		//TODO: Check for memory leaks
		if (pad == null)
			return false;
		
		boolean ret = false;
		synchronized(pad.ownershipLock()) {
			pad.takeOwnership();
			try {
				ret = gst_element_add_pad(ptr, pad.getPointer());
			} finally {
				if (!ret)
					pad.releaseOwnership();
			}
		}
		return ret;
	}

	@Override
	public boolean removePad(Pad pad) {
		//TODO: Check for memory leaks
		if (pad == null)
			return false;
		return gst_element_remove_pad(ptr, pad.getPointer());
	}

	@Override
	public boolean postMessage(Message msg) {
		//TODO: Check for memory leaks
		if (msg == null)
			return false;
		msg.ref();
		boolean ret = gst_element_post_message(ptr, msg.getPointer());
		if (!ret)
			msg.unref();
		return ret;
	}

	@Override
	public boolean postMessage(Pointer msg) {
		return gst_element_post_message(ptr, msg);
	}

	@Override
	public boolean postStateChangeMessage(State state) {
		return postStateChangeMessage(state, state, State.VoidPending);
	}

	@Override
	public boolean postStateChangeMessage(State oldState, State newState, State pending) {
		return gst_element_post_message(ptr, gst_message_new_state_changed(ptr, oldState.getNativeValue(), newState.getNativeValue(), pending.getNativeValue()));
	}

	@Override
	public void visitPads(IPadVisitor visitor) {
		if (visitor == null)
			return;
		Pointer pIterator = gst_element_iterate_pads(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;
		
		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					Pad pad = Pad.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, pad);
					pad.unref();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}

	@Override
	public void visitSrcPads(IPadVisitor visitor) {
		if (visitor == null)
			return;
		Pointer pIterator = gst_element_iterate_src_pads(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;

		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					Pad pad = Pad.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, pad);
					pad.unref();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}

	@Override
	public void visitSinkPads(IPadVisitor visitor) {
		if (visitor == null)
			return;
		Pointer pIterator = gst_element_iterate_sink_pads(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;

		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					Pad pad = Pad.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, pad);
					pad.unref();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static String factoryName(IElement element) {
		if (element == null)
			return StringUtil.empty;

		Pointer factory = gst_element_get_factory(element.getPointer());
		if (factory == null || factory == Pointer.NULL)
			return StringUtil.empty;

		return gst_plugin_feature_get_name(factory);
	}

	public static String factoryClass(IElement element) {
		if (element == null)
			return StringUtil.empty;

		Pointer factory = gst_element_get_factory(element.getPointer());
		if (factory == null || factory == Pointer.NULL)
			return StringUtil.empty;

		return gst_element_factory_get_klass(factory);
	}

	public static int factoryRank(IElement element) {
		if (element == null)
			return Rank.None.asInt();

		Pointer factory = gst_element_get_factory(element.getPointer());
		if (factory == null || factory == Pointer.NULL)
			return Rank.None.asInt();

		return gst_plugin_feature_get_rank(factory);
	}

	public static Set<String> factoryClassAsList(IElement element) {
		String cls = factoryClass(element);
		Set<String> ret;
		if (!StringUtil.isNullOrEmpty(cls)) {
			String[] arr = cls.split("/");
			ret = new HashSet<String>(arr.length);
			Collections.addAll(ret, arr);
		} else {
			ret = Collections.emptySet();
		}
		return Collections.unmodifiableSet(ret);
	}

	public static boolean parentExists(IElement element) {
		if (element == null)
			return false;

		Pointer parent = gst_object_get_parent(element.getPointer());
		boolean exists = (parent != null && parent != Pointer.NULL);
		if (exists)
			gst_object_unref(parent);
		return exists;
	}

	public static boolean link(IElement element1, IElement element2) {
		if (element1 == null || element2 == null)
			return false;
		return gst_element_link(element1.getPointer(), element2.getPointer());
	}

	public static boolean unlink(IElement element1, IElement element2) {
		if (element1 == null || element2 == null)
			return false;
		gst_element_unlink(element1.getPointer(), element2.getPointer());
		return true;
	}

	public static boolean linkMany(IElement...elements) {
		if (elements == null || elements.length <= 0)
			return true;

		boolean ret = true;
		Pointer curr = null;
		Pointer last = null;
		for(int i = 0; i < elements.length; ++i) {
			if (elements[i] == null)
				continue;

			curr = elements[i].getPointer();
			if (last != null)
				ret = ret && gst_element_link(last, curr);
			last = curr;
		}

		return ret;
	}

	public static boolean unlinkMany(IElement...elements) {
		if (elements == null || elements.length <= 0)
			return true;

		Pointer curr = null;
		Pointer last = null;
		for(int i = elements.length - 1; i >= 0; --i) {
			if (elements[i] == null)
				continue;

			curr = elements[i].getPointer();
			if (last != null)
				gst_element_unlink(last, curr);
			last = curr;
		}

		return true;
	}

	public static boolean linkPads(IElement src, String srcPadName, IElement dest, String destPadName) {
		return gst_element_link_pads(src.getPointer(), srcPadName, dest.getPointer(), destPadName);
	}

	public static boolean linkPadsFiltered(IElement src, String srcPadName, IElement dest, String destPadName, Caps caps) {
		return gst_element_link_pads_filtered(src.getPointer(), srcPadName, dest.getPointer(), destPadName, caps.getPointer());
	}

	public static boolean unlinkPads(IElement src, String srcPadName, IElement dest, String destPadName) {
		if (src == null || dest == null)
			return false;
		gst_element_unlink_pads(src.getPointer(), srcPadName, dest.getPointer(), destPadName);
		return true;
	}

	public static boolean xoverlayWindowID(IElement src, long window) {
		gst_x_overlay_set_xwindow_id(src.getPointer(), new NativeLong(window));
		return true;
	}

	public static boolean xoverlayWindowID(IElement src, NativeLong window) {
		gst_x_overlay_set_xwindow_id(src.getPointer(), window);
		return true;
	}

	public static boolean xoverlayWindowID(IElement src, Pointer window) {
		gst_x_overlay_set_xwindow_id(src.getPointer(), window);
		return true;
	}
	//</editor-fold>
}

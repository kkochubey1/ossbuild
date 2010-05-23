
package ossbuild.media.gstreamer;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
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
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public BaseGstElement(String factoryName) {
		this(factoryName, null);
	}

	public BaseGstElement(String factoryName, String elementName) {
		super(factoryName, elementName);
		this.managed = true;
	}

	BaseGstElement(Pointer ptr) {
		super();
		this.ptr = ptr;
		this.managed = false;
		ref();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		if (ptr == Pointer.NULL)
			return;

		unref();
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

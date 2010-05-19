
package ossbuild.gst;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.concurrent.TimeUnit;
import ossbuild.StringUtil;
import ossbuild.gst.api.Utils;
import static ossbuild.gst.api.GLib.*;
import static ossbuild.gst.api.GObject.*;
import static ossbuild.gst.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Bin extends GObject implements IBin, IElement {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private Integer factoryRank;
	private String factoryName, factoryClass;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Bin() {
		this(StringUtil.empty);
	}

	public Bin(String elementName) {
		super();
		this.ptr = gst_bin_new(elementName);
		this.managed = true;
	}

	Bin(Pointer ptr) {
		this.ptr = ptr;
		this.managed = false;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public boolean hasParent() {
		return Element.parentExists(this);
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
		return (factoryRank = Element.factoryRank(this));
	}

	@Override
	public String getFactoryName() {
		if (factoryName != null)
			return factoryName;
		return (factoryName = Element.factoryName(this));
	}

	@Override
	public String getFactoryClass() {
		if (factoryClass != null)
			return factoryClass;
		return (factoryClass = Element.factoryClass(this));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		if (ptr == Pointer.NULL)
			return;
		if (managed && !hasParent())
			g_object_unref(ptr);
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

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public boolean add(IElement element) {
		return gst_bin_add(ptr, element.getPointer());
	}

	@Override
	public boolean addMany(IElement... elements) {
		if (elements == null || elements.length <= 0)
			return true;
		for(int i = 0; i < elements.length; ++i)
			if (elements[i] != null)
				gst_bin_add(ptr, elements[i].getPointer());
		return true;
	}

	@Override
	public boolean addAndLinkMany(IElement... elements) {
		if (!addMany(elements))
			return false;
		return Element.linkMany(elements);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static IBin make() {
		return new Bin();
	}

	public static IBin make(String elementName) {
		return new Bin(elementName);
	}
	//</editor-fold>
}

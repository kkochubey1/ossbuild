
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Caps extends BaseGObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Caps() {
		this(false);
	}

	public Caps(boolean any) {
		super(!any ? gst_caps_new_empty() : gst_caps_new_any());
		this.managed = true;
	}

	public Caps(String caps) {
		super(gst_caps_from_string(caps));
		this.managed = true;
	}

	public Caps(Caps caps) {
		super(gst_caps_copy(caps.getPointer()));
		this.managed = true;
	}

	Caps(Pointer ptr) {
		super(ptr);
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
	public void set(String field, Object value) {
		//Utils.setGValue();
		//gst_caps_set_value();
	}

	public void setInteger(String field, Integer value) {
		//gst_caps_set_simple(this, field, value, null);
	}

	public boolean isAny() {
		return gst_caps_is_any(ptr);
	}

	public boolean isEmpty() {
		return gst_caps_is_empty(ptr);
	}

	public boolean isFixed() {
		return gst_caps_is_fixed(ptr);
	}

	public boolean isEqual(Caps other) {
		return gst_caps_is_equal(ptr, other.getPointer());
	}

	public boolean isEqualFixed(Caps other) {
		return gst_caps_is_equal_fixed(ptr, other.getPointer());
	}

	public boolean isSubset(Caps superset) {
		return gst_caps_is_subset(ptr, superset.getPointer());
	}

	public boolean isAlwaysCompatible(Caps other) {
		return gst_caps_is_always_compatible(ptr, other.getPointer());
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public int size() {
		return gst_caps_get_size(ptr);
	}

	public Caps copy() {
		return new Caps(gst_caps_copy(ptr));
	}

	public Caps union(Caps other) {
		return new Caps(gst_caps_union(ptr, other.getPointer()));
	}

	public Caps intersect(Caps other) {
		return new Caps(gst_caps_intersect(ptr, other.getPointer()));
	}

	public Caps subtract(Caps subtract) {
		return new Caps(gst_caps_subtract(ptr, subtract.getPointer()));
	}

	public Caps normalize() {
		return new Caps(gst_caps_normalize(ptr));
	}

	public boolean simplify() {
		return gst_caps_do_simplify(ptr);
	}

	public void appendCaps(Caps caps) {
		gst_caps_append(ptr, caps.getPointer());
	}

	public void appendStructure(Structure struct) {
		gst_caps_append_structure(ptr, struct.getPointer());
	}

	public void removeStructure(int index) {
		gst_caps_remove_structure(ptr, index);
	}

	public void mergeCaps(Caps other) {
		gst_caps_merge(ptr, other.getPointer());
	}

	public void mergeStructure(Structure structure) {
		gst_caps_merge_structure(ptr, structure.getPointer());
	}

	public Caps makeWritable() {
		return new Caps(gst_caps_make_writable(ptr));
	}

	public Structure structureAt(int index) {
		return Structure.from(gst_caps_get_structure(ptr, index));
	}

	public void truncate() {
		gst_caps_truncate(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return gst_caps_to_string(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Equals">
	@Override
	public int hashCode() {
		int hash = 7;
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Caps))
			return false;
		return equals((Caps)o);
	}

	public boolean equals(Caps caps) {
		if (caps == null)
			return false;
		return (caps == this || gst_caps_is_equal(ptr, ((Caps)caps).getPointer()) || super.equals(caps));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	@Override
	public void ref() {
		gst_caps_ref(ptr);
	}

	@Override
	public void ref(int times) {
		for(int i = 0; i < times; ++i)
			gst_caps_ref(ptr);
	}

	@Override
	public void unref() {
		gst_caps_unref(ptr);
	}

	@Override
	public void unref(int times) {
		for(int i = 0; i < times; ++i)
			gst_caps_unref(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Signals">
	@Override
	public ISignalConnectResponse connect(ISignal signal) {
		throw new RuntimeException("Invalid operation for this class");
	}

	@Override
	public boolean disconnect(ISignalConnectResponse response) {
		throw new RuntimeException("Invalid operation for this class");
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static Caps newEmpty() {
		return new Caps(false);
	}

	public static Caps newAny() {
		return new Caps(true);
	}

	public static Caps from(String caps) {
		return new Caps(caps);
	}
	//</editor-fold>
}

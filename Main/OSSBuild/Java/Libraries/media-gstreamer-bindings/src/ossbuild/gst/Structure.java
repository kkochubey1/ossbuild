
package ossbuild.gst;

import com.sun.jna.Pointer;
import ossbuild.StringUtil;
import ossbuild.gst.api.GType;
import ossbuild.gst.api.GstStructure;
import static ossbuild.gst.api.GObject.*;
import static ossbuild.gst.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Structure extends BaseGObject {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private GType gType;
	private GstStructure struct;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Structure() {
		this(StringUtil.empty);
	}

	public Structure(String name) {
		this.ptr = gst_structure_empty_new(name);
		this.struct = new GstStructure(ptr);
		this.gType = GType.fromNative(struct.type);
		this.managed = true;
	}

	Structure(Pointer ptr) {
		this.ptr = ptr;
		this.struct = new GstStructure(ptr);
		this.gType = GType.fromNative(struct.type);
		this.managed = false;
	}

	static Structure from(Pointer ptr) {
		return new Structure(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public GType getGType() {
		return gType;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="toString">
	@Override
	public String toString() {
		return gType.name();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
	}
	//</editor-fold>
}

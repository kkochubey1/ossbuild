
package ossbuild.gst;

import com.sun.jna.Pointer;
import static ossbuild.gst.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GObject extends BaseGObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GObject(Pointer ptr) {
		super();
		this.ptr = ptr;
		this.managed = true;
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

	//<editor-fold defaultstate="collapsed" desc="Casts">
	public IElement asElement() {
		g_object_ref(ptr);
		return new Element(ptr, true);
	}

	public IBin asBin() {
		g_object_ref(ptr);
		return new Bin(ptr, true);
	}

	public IPipeline asPipeline() {
		g_object_ref(ptr);
		return new Pipeline(ptr, true);
	}
	//</editor-fold>
}

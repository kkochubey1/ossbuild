
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
class GObject extends BaseGObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GObject(Pointer ptr) {
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

	//<editor-fold defaultstate="collapsed" desc="Casts">
	public IElement asElement() {
		return new Element(ptr);
	}

	public IBin asBin() {
		return new Bin(ptr);
	}

	public IPipeline asPipeline() {
		return new Pipeline(ptr);
	}
	//</editor-fold>
}

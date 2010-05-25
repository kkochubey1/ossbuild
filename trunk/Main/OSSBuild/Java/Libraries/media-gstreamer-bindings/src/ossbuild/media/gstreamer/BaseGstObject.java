
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
abstract class BaseGstObject extends BaseGObject implements IGObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public BaseGstObject(Object... arguments) {
		super();
		this.ptr = createNativeObject(arguments);
		this.managed = true;
	}

	BaseGstObject(Pointer ptr) {
		super(ptr);
		this.managed = false;
		ref();
	}

	BaseGstObject(Pointer ptr, boolean incRef) {
		super(ptr);
		this.managed = false;
		if (incRef)
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

	protected abstract Pointer createNativeObject(Object... arguments);
}

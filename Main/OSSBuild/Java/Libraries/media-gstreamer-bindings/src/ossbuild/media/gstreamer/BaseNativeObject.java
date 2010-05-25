
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.GStreamer;
import static ossbuild.media.gstreamer.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
abstract class BaseNativeObject implements INativeObject, IDisposable {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected boolean managed = true;
	protected boolean disposed = false;
	protected Pointer ptr = Pointer.NULL;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		GStreamer.initialize();
	}

	protected BaseNativeObject() {
	}

	protected BaseNativeObject(Pointer ptr) {
		this.ptr = ptr;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public final Pointer getPointer() {
		return ptr;
	}

	@Override
	public final boolean isManaged() {
		return managed;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	public final boolean isDisposed() {
		return disposed;
	}

	@Override
	protected final void finalize() throws Throwable {
		super.finalize();
		dispose();
	}

	@Override
	public synchronized final void dispose() {
		if (disposed)
			return;
		disposed = true;
		disposeObject();
	}

	protected void disposeObject() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Equals">
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 17 * hash + (this.ptr != null ? this.ptr.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return (ptr == Pointer.NULL);
		if (obj instanceof BaseNativeObject)
			return equals((BaseNativeObject)obj);
		return super.equals(obj);
	}

	@Override
	public boolean equals(INativeObject obj) {
		if (obj == null)
			return (ptr == Pointer.NULL);
		return equals(obj.getPointer());
	}

	boolean equals(BaseNativeObject obj) {
		if (obj == null)
			return (ptr == Pointer.NULL);
		return equals(obj.ptr);
	}

	@Override
	public boolean equals(Pointer objPtr) {
		if (ptr != Pointer.NULL && (objPtr == null || objPtr == Pointer.NULL))
			return false;
		else if (ptr == Pointer.NULL && (objPtr == null || objPtr == Pointer.NULL))
			return true;
		else if (ptr == Pointer.NULL && (objPtr != null && objPtr != Pointer.NULL))
			return false;
		else
			return ptr.equals(objPtr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	@Override
	public void ref() {
		throw new RuntimeException(getClass().getSimpleName() + " does not support GObject ref counts");
	}

	@Override
	public void unref() {
		throw new RuntimeException(getClass().getSimpleName() + " does not support GObject ref counts");
	}

	@Override
	public int refCount() {
		throw new RuntimeException(getClass().getSimpleName() + " does not support GObject ref counts");
	}

	public void ref(int times) {
		for (int i = 0; i < times; ++i)
			ref();
	}

	public void unref(int times) {
		for (int i = 0; i < times; ++i)
			g_object_unref(ptr);
	}
	//</editor-fold>
}

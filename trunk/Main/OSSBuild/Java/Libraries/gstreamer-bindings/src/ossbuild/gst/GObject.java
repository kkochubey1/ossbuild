
package ossbuild.gst;

import com.sun.jna.Pointer;
import ossbuild.gst.api.GStreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class GObject implements INativeObject, IDisposable {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	boolean managed = true;
	boolean disposed = false;
	Pointer ptr = Pointer.NULL;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		GStreamer.initialize();
	}

	protected GObject() {
	}

	protected GObject(Pointer ptr) {
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
		if (obj instanceof GObject)
			return equals((GObject)obj);
		return super.equals(obj);
	}

	@Override
	public boolean equals(INativeObject obj) {
		if (obj == null)
			return (ptr == Pointer.NULL);
		return equals(obj.getPointer());
	}

	public boolean equals(GObject obj) {
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
}


package ossbuild.gst;

import com.sun.jna.Pointer;
import ossbuild.gst.api.GError;
import static ossbuild.gst.api.GObject.*;
import static ossbuild.gst.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Error extends GObject {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private GError err;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	Error(Pointer ptr) {
		this.ptr = ptr;
		this.err = new GError(ptr);
		this.managed = false;
	}

	static Error from(Pointer ptr) {
		return new Error(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNativeCode() {
		return err.code;
	}
	
	public String getMessage() {
		return err.message;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return getMessage();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
	}
	//</editor-fold>
}

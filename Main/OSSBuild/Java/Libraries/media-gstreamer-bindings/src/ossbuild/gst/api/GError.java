
package ossbuild.gst.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GError extends Structure {
	public int domain; /* GQuark */
	public int code;
	public String message;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GError() {
	}

	public GError(GError instance) {
		this(instance.getPointer());
	}

	public GError(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GstMessage implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GError instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GstMessage implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GError instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

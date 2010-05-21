
package ossbuild.gst.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GTypeInstance extends Structure {
	public Pointer g_class;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GTypeInstance() {
	}

	public GTypeInstance(GTypeInstance instance) {
		this(instance.getPointer());
	}

	public GTypeInstance(Pointer ptr) {
		useMemory(ptr);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GstMessage implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GTypeInstance instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GstMessage implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GTypeInstance instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

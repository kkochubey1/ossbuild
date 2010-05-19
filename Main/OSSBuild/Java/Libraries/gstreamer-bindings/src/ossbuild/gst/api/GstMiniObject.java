
package ossbuild.gst.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GstMiniObject extends Structure {
	public GTypeInstance instance;
	public int refcount;
	public int flags;
	public Pointer _gst_reserved;
	
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GstMiniObject() {
	}

	public GstMiniObject(GstMiniObject instance) {
		this(instance.getPointer());
	}

	public GstMiniObject(Pointer ptr) {
		useMemory(ptr);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GstMiniObject implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GstMiniObject instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GstMiniObject implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GstMiniObject instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

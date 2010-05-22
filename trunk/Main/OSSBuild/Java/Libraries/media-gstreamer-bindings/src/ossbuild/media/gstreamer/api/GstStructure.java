
package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GstStructure extends Structure {
	public NativeLong type;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GstStructure() {
	}

	public GstStructure(GstStructure instance) {
		this(instance.getPointer());
	}

	public GstStructure(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GstStructure implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GstStructure instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GstStructure implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GstStructure instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

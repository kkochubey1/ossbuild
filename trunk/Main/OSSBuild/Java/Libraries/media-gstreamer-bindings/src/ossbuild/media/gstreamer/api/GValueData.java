
package ossbuild.media.gstreamer.api;

import com.sun.jna.Pointer;
import com.sun.jna.Union;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GValueData extends Union {
	public int v_int;
	public long v_long;
	public long v_int64;
	public float v_float;
	public double v_double;
	public Pointer v_pointer;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GValueData() {
		super();
	}

	public GValueData(GValueData instance) {
		this(instance.getPointer());
	}

	public GValueData(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GValueData implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GValueData instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GValueData implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GValueData instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

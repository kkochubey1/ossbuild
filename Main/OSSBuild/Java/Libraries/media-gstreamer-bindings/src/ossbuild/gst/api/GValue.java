package ossbuild.gst.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class GValue extends Structure {
	/*< private >*/
	public long g_type;
	/* public for GTypeValueTable methods */
	public GValueData data[] = new GValueData[2];

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GValue() {
		super();
	}

	public GValue(GValue instance) {
		this(instance.getPointer());
	}

	public GValue(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GValue implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GValue instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GValue implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GValue instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

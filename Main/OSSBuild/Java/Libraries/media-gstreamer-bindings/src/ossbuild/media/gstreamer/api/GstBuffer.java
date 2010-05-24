
package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GstBuffer extends com.sun.jna.Structure {
	public GstMiniObject mini_object;
	public Pointer data;
	public int size;
	public NativeLong timestamp;
	public NativeLong duration;
	public Pointer caps;
	public long offset;
	public long offset_end;
	public Pointer malloc_data;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GstBuffer() {
	}

	public GstBuffer(GstBuffer instance) {
		this(instance.getPointer());
	}

	public GstBuffer(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GstBuffer implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GstBuffer instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GstBuffer implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GstBuffer instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

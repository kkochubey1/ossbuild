
package ossbuild.gst.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GstMessage extends Structure {
	public GstMiniObject mini_object;
	public volatile Pointer lock;
	public volatile Pointer cond;
	public int type;
	public long timestamp;
	public Pointer src;
	public Pointer structure;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GstMessage() {
	}

	public GstMessage(GstMessage msg) {
		this(msg.getPointer());
	}

	public GstMessage(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GstMessage implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GstMessage msg) {
			super(msg.getPointer());
		}
	}

	public static class ByReference extends GstMessage implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GstMessage msg) {
			super(msg.getPointer());
		}
	}
	//</editor-fold>
}

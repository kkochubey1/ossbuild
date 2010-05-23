
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class PadTemplate extends BaseGstObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public PadTemplate(String nameTemplate, PadDirection direction, Caps caps) {
		super(nameTemplate, direction, PadPresence.Always, caps);
	}

	public PadTemplate(String nameTemplate, PadDirection direction, PadPresence presence, Caps caps) {
		super(nameTemplate, direction, presence, caps);
	}

	PadTemplate(Pointer ptr) {
		super(ptr);
	}

	@Override
	protected Pointer createNativeObject(Object... arguments) {
		String nameTemplate    = (String)arguments[0];
		PadDirection direction = (PadDirection)arguments[1];
		PadPresence presence   = (PadPresence)arguments[2];
		Caps caps              = (Caps)arguments[3];

		synchronized(caps.ownershipLock()) {
			if (caps.isDisposed())
				throw new NullPointerException("Cannot add a disposed caps to a pad template");
			
			caps.takeOwnership();
			Pointer p = gst_pad_template_new(nameTemplate, direction.getNativeValue(), presence.getNativeValue(), caps.getPointer());
			if (p == null || p == Pointer.NULL) {
				//Release if we weren't able to create the pad template for some reason
				caps.releaseOwnership();
				return null;
			}
			//gst_object_ref_sink(p);
			return p;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static PadTemplate make(String nameTemplate, PadDirection direction, Caps caps) {
		return new PadTemplate(nameTemplate, direction, caps);
	}

	public static PadTemplate make(String nameTemplate, PadDirection direction, PadPresence presence, Caps caps) {
		return new PadTemplate(nameTemplate, direction, presence, caps);
	}
	//</editor-fold>
}

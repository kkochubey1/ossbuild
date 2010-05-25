
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Event extends BaseGstMiniObject implements IEvent {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	protected Event() {
		super();
		this.managed = true;
	}

	protected Event(Pointer ptr) {
		super(ptr);
		this.managed = false;
		ref();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public Structure getStructure() {
		return Structure.from(gst_event_get_structure(ptr));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	@Override
	public void unref() {
		//Note: gst_message_unref() has been inlined out of the lib. Calling
		//gst_mini_object_unref() results in a fatal crash. Our options were
		//to find a way to cast it, ensure the function isn't inlined, or
		//live w/ the memory leak.
		//
		//We chose to cast it ourselves.
		gst_mini_object_unref(g_type_check_instance_cast(ptr, gst_mini_object_get_type()));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static Event from(Pointer ptr) {
		return new Event(ptr);
	}
	//</editor-fold>
}

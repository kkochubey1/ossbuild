
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Message extends BaseGstMiniObject {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private MessageType msgType;
	private ossbuild.media.gstreamer.api.GstMessage msg;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	Message(Pointer ptr) {
		super(ptr);
		this.msg = new ossbuild.media.gstreamer.api.GstMessage(ptr);
		this.msgType = MessageType.fromNative(msg.type);
		this.managed = false;
		ref();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public Pointer getSource() {
		return msg.src;
	}

	public MessageType getMessageType() {
		return msgType;
	}

	public Structure getStructure() {
		Pointer p = gst_message_get_structure(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		return Structure.from(p);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return msgType.name();
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
	public static Message from(Pointer ptr) {
		return new Message(ptr);
	}
	//</editor-fold>
}


package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GhostPad extends Pad {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GhostPad(String name, Pad target) {
		super(gst_ghost_pad_new(name, target.getPointer()), false);
		this.managed = true;
		this.ownedByParent = false;
	}

	public GhostPad(String name, Pad target, PadTemplate template) {
		super(gst_ghost_pad_new_from_template(name, target.getPointer(), template.getPointer()), false);
		this.managed = true;
		this.ownedByParent = false;
	}

	public GhostPad(String name, PadDirection direction) {
		super(gst_ghost_pad_new_no_target(name, direction.getNativeValue()), false);
		this.managed = true;
		this.ownedByParent = false;
	}

	protected GhostPad(Pointer p) {
		super(p);
		this.managed = false;
		this.ownedByParent = true;
	}

	protected GhostPad(Pointer p, boolean incRef) {
		super(p, incRef);
		this.managed = false;
		this.ownedByParent = true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	public Pad getTarget() {
		return Pad.from(gst_ghost_pad_get_target(ptr), false);
	}
	
	public boolean setTarget(Pad pad) {
		return gst_ghost_pad_set_target(ptr, pad != null ? pad.getPointer() : Pointer.NULL);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static GhostPad from(String name, Pad target) {
		return new GhostPad(name, target);
	}

	public static GhostPad from(String name, PadDirection direction) {
		return new GhostPad(name, direction);
	}

	public static GhostPad from(String name, Pad target, PadTemplate template) {
		return new GhostPad(name, target, template);
	}

	public static GhostPad from(Pointer p, boolean incRef) {
		return new GhostPad(p, incRef);
	}

	public static GhostPad from(Pointer p) {
		return new GhostPad(p);
	}
	//</editor-fold>
}

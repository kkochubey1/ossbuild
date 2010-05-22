
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import ossbuild.StringUtil;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Structure extends BaseNativeObject {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	boolean ownedByParent = true;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Structure(String name) {
		super();
		
		if (StringUtil.isNullOrEmpty(name))
			throw new IllegalArgumentException("name cannot be empty, it must start with a letter, and can then follow with any letter, number, or the following symbols: /-_.:");
		
		this.ptr = gst_structure_empty_new(name);
		this.managed = true;
		this.ownedByParent = false;
	}

	Structure(Pointer ptr) {
		super(ptr);
		this.managed = false;
		this.ownedByParent = true;
	}

	static Structure from(Pointer ptr) {
		return new Structure(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		//TODO: There's no current way to figure out if a GstStructure has
		//      a parent or not. As such, when you create a structure from
		//      scratch that isn't parented, it needs to be freed. 99% of the
		//      time it isn't a problem b/c we're not creating our own
		//      structure - we're using whatever's in the caps (for example).
		//      Any object that's taking ownership must call takeOwnership()
		//      in order to prevent this from being freed and causing a crash.
		if (!ownedByParent)
			gst_structure_free(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public boolean isOwnedByParent() {
		return ownedByParent;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return gst_structure_to_string(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public void takeOwnership() {
		//See disposeObject() documentation
		ownedByParent = true;
	}

	public void releaseOwnership() {
		//See disposeObject() documentation
		ownedByParent = false;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static Structure newEmpty(String name) {
		return new Structure(name);
	}

	public static Structure from(String structure) {
		Structure s = new Structure(gst_structure_from_string(structure, null));
		//This is a managed object that needs to be tracked and freed if necessary.
		s.managed = true;
		return s;
	}
	//</editor-fold>
}

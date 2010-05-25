
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import java.io.File;
import ossbuild.StringUtil;
import ossbuild.Sys;
import static ossbuild.media.gstreamer.api.GLib.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
abstract class BaseGstBin extends BaseGstElement implements IBin {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public BaseGstBin() {
		super((String)null, (String)null);
	}

	public BaseGstBin(String elementName) {
		super((String)null, elementName);
	}

	public BaseGstBin(String factoryName, String elementName) {
		super(factoryName, elementName);
	}

	BaseGstBin(Pointer ptr) {
		super(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected final void disposeObject() {
		if (ptr == Pointer.NULL)
			return;

		disposeBin();

		unref();
	}

	protected void disposeBin() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public File writeToDotFile(DebugGraphDetails... details) {
		return writeToDotFile(null, false, details);
	}

	@Override
	public File writeToDotFile(String name, DebugGraphDetails... details) {
		return writeToDotFile(name, false, details);
	}

	@Override
	public File writeToDotFile(boolean includeTimeStampInFileName, DebugGraphDetails... details) {
		return writeToDotFile(null, includeTimeStampInFileName, details);
	}
	
	@Override
	public File writeToDotFile(String name, boolean includeTimeStampInFileName, DebugGraphDetails... details) {
		//This won't output a dot file if we haven't defined this env var.
		//It should have been set in GStreamer.initialize() however. But we're doing
		//a sanity check just in case.
		if (StringUtil.isNullOrEmpty(Sys.getEnvironmentVariable("GST_DEBUG_DUMP_DOT_DIR")))
			return null;

		if (name == null) {
			name = g_get_application_name();
			if (name == null)
				name = "unnamed";
		}
		
		if (includeTimeStampInFileName)
			name = name + "-" + System.currentTimeMillis();

		_gst_debug_bin_to_dot_file(ptr, DebugGraphDetails.toNative(details), name);

		//Return the path to the newly created file
		return new File(Sys.getEnvironmentVariable("GST_DEBUG_DUMP_DOT_DIR"), name + ".dot");
	}

	@Override
	public IElement elementFromName(String name) {
		Pointer p = gst_bin_get_by_name(ptr, name);
		if (p == null || p == Pointer.NULL)
			return null;
		return Element.from(p);
	}

	@Override
	public IElement elementFromNameRecurseUp(String name) {
		Pointer p = gst_bin_get_by_name_recurse_up(ptr, name);
		if (p == null || p == Pointer.NULL)
			return null;
		return Element.from(p);
	}

	@Override
	public boolean add(IElement element) {
		if (gst_bin_add(ptr, element.getPointer())) {
			//gst_object_ref(ptr);
			return true;
		}
		return false;
	}

	@Override
	public boolean addMany(IElement... elements) {
		if (elements == null || elements.length <= 0)
			return true;
		for(int i = 0; i < elements.length; ++i) {
			if (elements[i] != null) {
				if (!gst_bin_add(ptr, elements[i].getPointer()))
					return false;
				//gst_object_ref(ptr);
			}
		}
		return true;
	}

	@Override
	public boolean addAndLinkMany(IElement... elements) {
		if (!addMany(elements))
			return false;
		return Element.linkMany(elements);
	}

	@Override
	public void visitElements(IElementVisitor visitor) {
		if (visitor == null)
			return;
		
		Pointer pIterator = gst_bin_iterate_elements(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;

		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					IElement element = Element.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, element);
					element.dispose();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}

	@Override
	public void visitElementsSorted(IElementVisitor visitor) {
		if (visitor == null)
			return;

		Pointer pIterator = gst_bin_iterate_sorted(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;

		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					IElement element = Element.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, element);
					element.dispose();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}

	@Override
	public void visitElementsRecursive(IElementVisitor visitor) {
		if (visitor == null)
			return;

		Pointer pIterator = gst_bin_iterate_recurse(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;

		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					IElement element = Element.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, element);
					element.dispose();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}

	@Override
	public void visitSinks(IElementVisitor visitor) {
		if (visitor == null)
			return;

		Pointer pIterator = gst_bin_iterate_sinks(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;

		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					IElement element = Element.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, element);
					element.dispose();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}

	@Override
	public void visitSources(IElementVisitor visitor) {
		if (visitor == null)
			return;

		Pointer pIterator = gst_bin_iterate_sources(ptr);
		if (pIterator == null || pIterator == Pointer.NULL)
			return;

		boolean done = false;
		PointerByReference ref = new PointerByReference();
		while(!done) {
			//This will increase the ref counter -- so we need to create an object
			//that doesn't increase the ref counter when it's built so on dispose(),
			//the ref count will be zero.
			switch(IteratorResult.fromNative(gst_iterator_next(pIterator, ref))) {
				case OK:
					//Passing false here tells pad to not increase the ref count
					//when building a new pad from the pointer.
					IElement element = Element.from(ref.getValue(), false);
					boolean ret = visitor.visit(this, element);
					element.dispose();
					done = !ret;
					break;
				case Resync:
					gst_iterator_resync(pIterator);
					break;
				case Error:
				case Done:
				case Unknown:
					done = true;
					break;
				default:
					done = true;
					break;
			}
		}
		gst_iterator_free(pIterator);
	}
	//</editor-fold>
}

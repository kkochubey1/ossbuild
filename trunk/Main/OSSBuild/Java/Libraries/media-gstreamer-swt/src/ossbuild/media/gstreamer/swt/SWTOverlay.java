
package ossbuild.media.gstreamer.swt;

import java.lang.reflect.Field;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import ossbuild.Sys;
import ossbuild.media.gstreamer.GStreamerException;
import ossbuild.media.gstreamer.IElement;
import ossbuild.media.gstreamer.XOverlay;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SWTOverlay extends XOverlay {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public SWTOverlay(IElement element) {
		super(element);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public void setWindowID(Control control) {
		long handle = handle(control);
		if (handle == 0L)
			throw new GStreamerException("Unable to determine handle for xoverlay");
		setWindowID(handle);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static long getLinuxHandle(Control control) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Class<? extends Control> controlClass = control.getClass();
		Field embedHandleField = controlClass.getField("embeddedHandle");
		Class<?> t = embedHandleField.getType();
		if (t.equals(long.class))
			return embedHandleField.getLong(control);
		else if (t.equals(int.class))
			return embedHandleField.getInt(control);
		return 0L;
	}

	public static long handle(Control control) {
		// Composite style must be embedded
		if (control == null || ((control.getStyle() | SWT.EMBEDDED) == 0))
			return 0L;
		try {
			switch(Sys.getOSFamily()) {
				case Windows:
					return control.handle;
				case Unix:
				case Mac:
				case Solaris:
				case VMS:
					return getLinuxHandle(control);
				default:
					return 0L;
			}
		} catch(Throwable t) {
			return 0L;
		}
	}

	public static SWTOverlay wrap(IElement element) {
		return new SWTOverlay(element);
	}
	//</editor-fold>
}

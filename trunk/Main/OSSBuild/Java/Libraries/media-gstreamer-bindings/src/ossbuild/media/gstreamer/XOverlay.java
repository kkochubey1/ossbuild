/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2009 Levente Farkas <lfarkas@lfarkas.org>
 * Copyright (C) 2009 Tamas Korodi <kotyo@zamba.fm>
 * Copyright (C) 2008 Wayne Meissner
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *
 * This file is part of gstreamer-java.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package ossbuild.media.gstreamer;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import ossbuild.Sys;
import static ossbuild.media.gstreamer.api.GStreamerInterfaces.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class XOverlay extends BaseGstInterface {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public XOverlay(IElement element) {
		super(element, gst_x_overlay_get_type());
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public void setWindowID(long handle) {
		gst_x_overlay_set_xwindow_id(getPointerAsInterface(), new NativeLong(handle));
	}

	public void setWindowID(java.awt.Component window) {
		if (window == null) {
			gst_x_overlay_set_xwindow_id(getPointerAsInterface(), new NativeLong(0));
			return;
		}

		if (window.isLightweight())
			throw new IllegalArgumentException("Component must be a native window");

		switch(Sys.getOSFamily()) {
			case Windows:
				gst_x_overlay_set_xwindow_id(getPointerAsInterface(), Native.getComponentPointer(window));
				break;
			default:
				gst_x_overlay_set_xwindow_id(getPointerAsInterface(), new NativeLong(Native.getComponentID(window)));
				break;
		}
	}

	public void expose() {
		gst_x_overlay_expose(getPointerAsInterface());
	}

	public void renderRectangle(int x, int y, int width, int height) {
		gst_x_overlay_set_render_rectangle(getPointerAsInterface(), x, y, width, height);
	}

	public void clearRenderRectangle() {
		gst_x_overlay_set_render_rectangle(getPointerAsInterface(), -1, -1, -1, -1);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static XOverlay wrap(IElement element) {
		return new XOverlay(element);
	}
	//</editor-fold>
}

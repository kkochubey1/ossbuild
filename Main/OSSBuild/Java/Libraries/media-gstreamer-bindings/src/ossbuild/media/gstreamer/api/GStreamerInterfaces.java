/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2009 Levente Farkas
 * Copyright (C) 2007, 2008 Wayne Meissner
 *
 * This file contains code from the gstreamer-java project.
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

package ossbuild.media.gstreamer.api;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GStreamerInterfaces extends Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		  API_VERSION_MAJOR     = 0
		, API_VERSION_MINOR     = 10
		, API_VERSION_REVISION  = 0
	;

	public static final String
		  API_VERSION = API_VERSION_MAJOR + "." + API_VERSION_MINOR //0.10
	;

	public static final String
		  LIBRARY_NAME = "gstinterfaces-" + API_VERSION //gstinterfaces-0.10
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		GStreamer.init();
		initLibrary();
		init();
	}

	private static void initLibrary() {
		for (int i = 0; i < DEFAULT_LIBRARY_NAME_FORMATS.length; ++i) {
			try {
				Native.register(String.format(DEFAULT_LIBRARY_NAME_FORMATS[i], LIBRARY_NAME));
			} catch(IllegalArgumentException e) {
				throw e;
			} catch (Throwable t) {
				continue;
			}
			return;
		}
		throw new UnsatisfiedLinkError("Could not load library " + LIBRARY_NAME);
	}

	static void init() {
		
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="API">
	//<editor-fold defaultstate="collapsed" desc="GstXOverlay">
	public static native void gst_x_overlay_set_xwindow_id(Pointer /*GstXOverlay*/ overlay, NativeLong xwindow_id);
	public static native void gst_x_overlay_set_xwindow_id(Pointer /*GstXOverlay*/ overlay, Pointer xwindow_id);
	public static native void gst_x_overlay_expose(Pointer /*GstXOverlay*/ overlay);
	public static native void gst_x_overlay_handle_events(Pointer /*GstXOverlay*/ overlay, boolean handle_events);
	public static native boolean gst_x_overlay_set_render_rectangle(Pointer /*GstXOverlay*/ overlay, int x, int y, int width, int height);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Types">
	public static native NativeLong /*GType*/ gst_x_overlay_get_type();
	
	public static native NativeLong /*GType*/ gst_mixer_get_type();
	public static native NativeLong /*GType*/ gst_mixer_track_get_type();

	public static native NativeLong /*GType*/ gst_tuner_get_type();
	public static native NativeLong /*GType*/ gst_tuner_channel_get_type();
	public static native NativeLong /*GType*/ gst_tuner_norm_get_type();

	public static native NativeLong /*GType*/ gst_color_balance_get_type();
	public static native NativeLong /*GType*/ gst_color_balance_channel_get_type();
	//</editor-fold>
	//</editor-fold>
}

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
import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GLib extends Library {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final int
		  API_VERSION_MAJOR     = 2
		, API_VERSION_MINOR     = 0
		, API_VERSION_REVISION  = 0
	;

	public static final String
		  API_VERSION = API_VERSION_MAJOR + "." + API_VERSION_MINOR //0.10
	;

	public static final String
		  LIBRARY_NAME = "glib-" + API_VERSION //gobject-0.10
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		initLibrary();
		init();
	}

	private static void initLibrary() {
		for (int i = 0; i < DEFAULT_LIBRARY_NAME_FORMATS.length; ++i) {
			try {
				Native.register(String.format(DEFAULT_LIBRARY_NAME_FORMATS[i], LIBRARY_NAME));
			} catch (UnsatisfiedLinkError ex) {
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
	//<editor-fold defaultstate="collapsed" desc="Alloc/Free">
	public static native void g_free(Pointer ptr);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Error">
	public static native void g_error_free(Pointer error);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Main Loop">
	public static native Pointer g_main_loop_new(Pointer context, boolean running);
	public static native void g_main_loop_run(Pointer loop);
	public static native boolean g_main_loop_is_running(Pointer loop);
	public static native Pointer g_main_loop_get_context(Pointer loop);
	public static native void g_main_loop_quit(Pointer loop);
	public static native void g_main_loop_ref(Pointer ptr);
	public static native void g_main_loop_unref(Pointer ptr);
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Main Context">
	public static native Pointer g_main_context_new();
	public static native void g_main_context_ref(Pointer context);
	public static native void g_main_context_unref(Pointer context);
	public static native Pointer g_main_context_default();
	public static native boolean g_main_context_pending(Pointer context);
	public static native boolean g_main_context_acquire(Pointer context);
	public static native void g_main_context_release(Pointer context);
	public static native boolean g_main_context_is_owner(Pointer context);
	public static native boolean g_main_context_wait(Pointer context);
	//</editor-fold>
	//</editor-fold>
}

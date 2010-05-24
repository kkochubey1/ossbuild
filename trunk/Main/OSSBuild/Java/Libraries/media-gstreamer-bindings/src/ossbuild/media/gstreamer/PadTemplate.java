/*
 * Copyright (C) 2009 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2009 Levente Farkas
 * Copyright (C) 2007, 2008 Wayne Meissner
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
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

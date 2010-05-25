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

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GstMessage extends Structure {
	public GstMiniObject mini_object;
	public volatile Pointer lock;
	public volatile Pointer cond;
	public int type;
	public long timestamp;
	public Pointer src;
	public Pointer structure;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GstMessage() {
	}

	public GstMessage(GstMessage instance) {
		this(instance.getPointer());
	}

	public GstMessage(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GstMessage implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GstMessage instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GstMessage implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GstMessage instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

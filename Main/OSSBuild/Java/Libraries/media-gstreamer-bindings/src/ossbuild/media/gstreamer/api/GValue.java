/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2009 Levente Farkas
 * Copyright (C) 2008 Andres Colubri
 * Copyright (C) 2008 Wayne Meissner
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

public class GValue extends Structure {
	/*< private >*/
	public long g_type;
	/* public for GTypeValueTable methods */
	public GValueData data[] = new GValueData[2];

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GValue() {
		super();
	}

	public GValue(GValue instance) {
		this(instance.getPointer());
	}

	public GValue(Pointer ptr) {
		useMemory(ptr, 0);
		read();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GValue implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GValue instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GValue implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GValue instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

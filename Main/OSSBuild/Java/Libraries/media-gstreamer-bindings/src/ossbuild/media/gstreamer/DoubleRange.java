/* 
 * Copyright (c) 2009 Levente Farkas
 * Copyright (C) 2009 Tamas Korodi <kotyo@zamba.fm>
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

import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.api.GValue;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 * Represents a range of float, double, int, fraction types stored in a GValue 
 * @author kotyo
 *
 */
public class DoubleRange {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private Pointer /*GValue*/ ptr;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	DoubleRange(GValue value) {
		this(value.getPointer());
	}

	DoubleRange(Pointer ptr) {
		this.ptr = ptr;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public double getMinimum() {
		return gst_value_get_double_range_min(ptr);
	}

	public double getMaximum() {
		return gst_value_get_double_range_max(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return "[ " + getMinimum() + ", " + getMaximum() + " ]";
	}
	//</editor-fold>
}

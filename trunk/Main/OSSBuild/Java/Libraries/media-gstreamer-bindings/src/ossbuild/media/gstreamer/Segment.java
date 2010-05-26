/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
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

/**
 *
 */
public class Segment {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private final double rate;
	private final Format format;
	private final long startValue;
	private final long stopValue;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	Segment(double rate, Format format, long startValue, long stopValue) {
		this.rate = rate;
		this.format = format;
		this.stopValue = stopValue;
		this.startValue = startValue;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public double getRate() {
		return rate;
	}

	public Format getFormat() {
		return format;
	}

	public long getStartValue() {
		return startValue;
	}

	public long getStopValue() {
		return stopValue;
	}
	//</editor-fold>
}

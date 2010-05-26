/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2007,2008 Wayne Meissner
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2004,2005 Wim Taymans <wim@fluendo.com>
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
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import java.util.concurrent.TimeUnit;
import ossbuild.StringUtil;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Pipeline extends Bin implements IPipeline {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final long
		QUERY_TIME_FAILED = -1L
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected Bus bus;
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Pipeline() {
		super();
		init();
	}

	public Pipeline(String elementName) {
		super(elementName);
		init();
	}

	public Pipeline(String factoryName, String elementName) {
		super(factoryName, elementName);
		init();
	}

	Pipeline(Pointer ptr) {
		super(ptr);
		init();
	}

	@Override
	protected Pointer createNativeObject(Object... arguments) {
		String elementFactoryName = (String)arguments[0];
		String elementName = (String)arguments[1];
		Pointer p = (!StringUtil.isNullOrEmpty(elementFactoryName) ? gst_element_factory_make(elementFactoryName, elementName) : gst_pipeline_new(elementName));
		gst_object_ref_sink(p);
		return p;
	}

	protected void init() {
		this.bus = Bus.from(gst_pipeline_get_bus(ptr));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	@Override
	public IBus getBus() {
		return bus;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeBin() {
		bus.dispose();

		//Need to unref him ourselves b/c Bus.from() will create an
		//unmanaged reference (IOW, someone else will clean him up)
		g_object_unref(bus.ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	@Override
	public void busSyncHandler(final IBusSyncHandler handler) {
		if (bus != null)
			bus.syncHandler(handler);
	}

	@Override
	public boolean seek(long time) {
		return seek(time, TimeUnit.NANOSECONDS);
	}

	@Override
	public boolean seek(long time, TimeUnit unit) {
		return seek(1.0, Format.Time, SeekType.Set, TimeUnit.NANOSECONDS.convert(time, unit), SeekType.None, -1, SeekFlags.Flush, SeekFlags.KeyUnit);
	}

	@Override
	public boolean seek(double rate, Format format, SeekType startType, long start, SeekType stopType, long stop, SeekFlags... flags) {
		return seek(rate, format, SeekFlags.toNative(flags), startType, start, stopType, stop);
	}

	@Override
	public boolean seek(double rate, Format format, int flags, SeekType startType, long start, SeekType stopType, long stop) {
		return gst_element_seek(ptr, rate, format.getNativeValue(), flags, startType.getNativeValue(), start, stopType.getNativeValue(), stop);
	}

	@Override
	public long queryPosition() {
		return queryPosition(Format.Time);
	}

	@Override
	public long queryPosition(TimeUnit unit) {
		return unit.convert(queryPosition(Format.Time), TimeUnit.NANOSECONDS);
	}

	@Override
	public long queryPosition(Format format) {
		IntByReference refFormat = new IntByReference(format.getNativeValue());
		LongByReference refPos = new LongByReference();
		if (!gst_element_query_position(ptr, refFormat, refPos))
			return QUERY_TIME_FAILED;
		return refPos.getValue();
	}

	@Override
	public long queryDuration() {
		return queryDuration(Format.Time);
	}

	@Override
	public long queryDuration(TimeUnit unit) {
		return unit.convert(queryDuration(Format.Time), TimeUnit.NANOSECONDS);
	}

	@Override
	public long queryDuration(Format format) {
		IntByReference refFormat = new IntByReference(format.getNativeValue());
		LongByReference refPos = new LongByReference();
		if (!gst_element_query_duration(ptr, refFormat, refPos))
			return QUERY_TIME_FAILED;
		return refPos.getValue();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static IPipeline make() {
		return new Pipeline();
	}

	public static IPipeline make(String elementName) {
		return new Pipeline(elementName);
	}

	public static IPipeline make(String factoryName, String elementName) {
		return new Pipeline(factoryName, elementName);
	}

	public static IPipeline from(Pointer pElement) {
		return new Pipeline(pElement);
	}
	//</editor-fold>
}

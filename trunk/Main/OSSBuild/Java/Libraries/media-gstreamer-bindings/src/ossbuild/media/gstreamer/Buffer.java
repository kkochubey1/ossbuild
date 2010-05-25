/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2007 Wayne Meissner
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

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.nio.ByteBuffer;
import ossbuild.media.gstreamer.api.GstBuffer;
import ossbuild.media.gstreamer.api.IGTypeCacheRecord;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 */
public class Buffer extends BaseGstMiniObject {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private GstBuffer nativeBuffer;
	private ByteBuffer bb;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Buffer() {
		super();
		this.ptr = gst_buffer_new();
		this.managed = true;
	}
	
	public Buffer(int size) {
		super();
		Pointer p = gst_buffer_new_and_alloc(size);
		if (p == null || p == Pointer.NULL)
			throw new OutOfMemoryError("Unable to allocate buffer of size " + size);
		this.ptr = p;
		this.managed = true;
	}

	protected Buffer(Pointer ptr) {
		super(ptr);
		this.nativeBuffer = new GstBuffer(ptr);
		this.managed = false;
		ref();
	}

	protected Buffer(Pointer ptr, boolean incRef) {
		super(ptr);
		this.nativeBuffer = new GstBuffer(ptr);
		this.managed = false;
		if (incRef)
			ref();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public Pointer getAddress() {
		return ptr;
	}

	public boolean isMetadataWritable() {
		return false;
	}

	public int getBufferSize() {
		return bufferSize();
	}

	public long getDuration() {
		return ((NativeLong)nativeBuffer.readField("duration")).longValue();
	}

	public void setDuration(long dur) {
		nativeBuffer.duration = new NativeLong(dur);
		nativeBuffer.writeField("duration");
	}

	public long getTimestamp() {
		return ((NativeLong)nativeBuffer.readField("timestamp")).longValue();
	}

	public void setTimestamp(long timestamp) {
		nativeBuffer.timestamp = new NativeLong(timestamp);
		nativeBuffer.writeField("timestamp");
	}

	public Caps getCaps() {
		return Caps.from(gst_buffer_get_caps(ptr));
	}

	public void setCaps(Caps caps) {
		gst_buffer_set_caps(ptr, caps.getPointer());
	}

	public long getOffset() {
		return (Long)nativeBuffer.readField("offset");
	}

	public void setOffset(long offset) {
		nativeBuffer.offset = offset;
		nativeBuffer.writeField("offset");
	}

	public long getLastOffset() {
		return (Long)nativeBuffer.readField("offset_end");
	}

	public void setLastOffset(long offset) {
		nativeBuffer.offset_end = offset;
		nativeBuffer.writeField("offset_end");
	}

	public int getFlags() {
		return nativeBuffer.mini_object.flags;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public int bufferSize() {
		return (Integer)nativeBuffer.readField("size");
	}
	
	public Buffer subBuffer(int offset, int size) {
		Pointer p = gst_buffer_create_sub(ptr, offset, size);
		if (p == null || p == Pointer.NULL)
			throw new IllegalArgumentException("offset and size were not valid for creating a sub-buffer");
		return new Buffer(p);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Buffer makeWritable() {
		Pointer p = gst_mini_object_make_writable(ptr);
		if (p == null || p == Pointer.NULL)
			return null;
		if (ptr.equals(p))
			return this;
		return new Buffer(p);
	}

	public synchronized ByteBuffer asByteBuffer() {
		if (bb == null) {
			int size = bufferSize();
			Pointer data = (Pointer)nativeBuffer.readField("data");
			if (data != null && size > 0)
				bb = data.getByteBuffer(0, size);
		}
		return bb;
	}
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static Buffer from(Pointer ptr) {
		return new Buffer(ptr);
	}

	public static Buffer from(Pointer ptr, boolean incRef) {
		return new Buffer(ptr, incRef);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Classes">
	public static class GTypeCacheRecord implements IGTypeCacheRecord {
		@Override
		public Class<? extends INativeObject> getJavaClass() {
			return Buffer.class;
		}

		@Override
		public INativeObject instantiateFromPointer(Pointer ptr) {
			return Buffer.from(gst_value_get_mini_object(ptr));
		}
	}
	//</editor-fold>
}

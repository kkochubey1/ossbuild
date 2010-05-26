/*
 * Copyright (C) 2010 David Hoyt <dhoyt@hoytsoft.org>
 * Copyright (C) 2009 Levente Farkas
 * Copyright (C) 2007 Wayne Meissner
 * Copyright (C) <2003> David A. Schleef <ds@schleef.org>
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 */
public class Caps extends BaseGObject implements List<Structure> {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	boolean ownedByParent = false;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Caps() {
		this(false);
	}

	public Caps(boolean any) {
		super(!any ? gst_caps_new_empty() : gst_caps_new_any());
		this.managed = true;
		this.ownedByParent = false;
	}

	public Caps(String caps) {
		super(gst_caps_from_string(caps));
		this.managed = true;
		this.ownedByParent = false;
	}

	public Caps(Caps caps) {
		super(gst_caps_copy(caps.getPointer()));
		this.managed = true;
		this.ownedByParent = false;
	}

	Caps(Pointer ptr) {
		super(ptr);
		this.managed = false;
		this.ownedByParent = true;
		ref();
	}

	Caps(Pointer ptr, boolean incRef) {
		super(ptr);
		this.managed = false;
		this.ownedByParent = true;
		if (incRef)
			ref();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		if (ptr == Pointer.NULL)
			return;

		synchronized(this) {
			if (!ownedByParent)
				unref();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	public boolean isAnyCaps() {
		return gst_caps_is_any(ptr);
	}

	public boolean isEmptyCaps() {
		return gst_caps_is_empty(ptr);
	}

	public boolean isFixedCaps() {
		return gst_caps_is_fixed(ptr);
	}

	public boolean isEqual(Caps other) {
		return gst_caps_is_equal(ptr, other.getPointer());
	}

	public boolean isEqualFixed(Caps other) {
		return gst_caps_is_equal_fixed(ptr, other.getPointer());
	}

	public boolean isSubset(Caps superset) {
		return gst_caps_is_subset(ptr, superset.getPointer());
	}

	public boolean isAlwaysCompatible(Caps other) {
		return gst_caps_is_always_compatible(ptr, other.getPointer());
	}

	public boolean containsStructures() {
		return (size() > 0);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public Caps copy() {
		return new Caps(gst_caps_copy(ptr));
	}

	public Caps union(Caps other) {
		return new Caps(gst_caps_union(ptr, other.getPointer()));
	}

	public Caps intersect(Caps other) {
		return new Caps(gst_caps_intersect(ptr, other.getPointer()));
	}

	public Caps subtract(Caps subtract) {
		return new Caps(gst_caps_subtract(ptr, subtract.getPointer()));
	}

	public Caps normalize() {
		return new Caps(gst_caps_normalize(ptr));
	}

	public boolean simplify() {
		return gst_caps_do_simplify(ptr);
	}

	public void appendCaps(Caps caps) {
		gst_caps_append(ptr, caps.getPointer());
	}

	public void appendStructure(Structure structure) {
		synchronized(structure) {
			gst_caps_append_structure(ptr, structure.getPointer());
			structure.takeOwnership();
		}
	}

	public void removeStructure(int index) {
		gst_caps_remove_structure(ptr, index);
	}

	public void mergeCaps(Caps other) {
		gst_caps_merge(ptr, other.getPointer());
	}

	public void mergeStructure(Structure structure) {
		synchronized(structure) {
			gst_caps_merge_structure(ptr, structure.getPointer());
			structure.takeOwnership();
		}
	}

	public void makeWritable() {
		this.ptr = gst_caps_make_writable(ptr);
	}

	public Structure structureAt(int index) {
		return Structure.from(gst_caps_get_structure(ptr, index));
	}

	public void truncate() {
		gst_caps_truncate(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return gst_caps_to_string(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Equals">
	@Override
	public int hashCode() {
		int hash = 7;
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Caps))
			return false;
		return equals((Caps)o);
	}

	public boolean equals(Caps caps) {
		if (caps == null)
			return false;
		return (caps == this || gst_caps_is_equal(ptr, ((Caps)caps).getPointer()) || super.equals(caps));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	@Override
	public void ref() {
		gst_caps_ref(ptr);
	}

	@Override
	public void ref(int times) {
		for(int i = 0; i < times; ++i)
			gst_caps_ref(ptr);
	}

	@Override
	public void unref() {
		gst_caps_unref(ptr);
	}

	@Override
	public void unref(int times) {
		for(int i = 0; i < times; ++i)
			gst_caps_unref(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Signals">
	@Override
	public ISignalConnectResponse connect(ISignal signal) {
		throw new RuntimeException("Invalid operation for this class");
	}

	@Override
	public boolean disconnect(ISignalConnectResponse response) {
		throw new RuntimeException("Invalid operation for this class");
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="List Operations">
	//<editor-fold defaultstate="collapsed" desc="StructureIterator">
	private class StructureIterator implements ListIterator<Structure> {
		int index = -1;

		public StructureIterator() {
			this(-1);
		}

		public StructureIterator(int startingIndex) {
			index = Math.max(0, Math.min(size(), startingIndex)) - 1;
		}
		
		@Override
		public boolean hasNext() {
			return (index >= 0 && index < size());
		}

		@Override
		public Structure next() {
			index = Math.min(size() - 1, Math.max(index, -1) + 1);
			return structureAt(index);
		}

		@Override
		public boolean hasPrevious() {
			return (index > 0 && index < size());
		}

		@Override
		public Structure previous() {
			index = Math.max(-1, Math.min(index, size() - 1) - 1);
			if (index < 0)
				return null;
			return structureAt(index);
		}

		@Override
		public int nextIndex() {
			return Math.min(index + 1, size());
		}

		@Override
		public int previousIndex() {
			return Math.max(-1, index - 1);
		}

		@Override
		public void remove() {
			removeStructure(index);
		}

		@Override
		public void set(Structure e) {
			throw new UnsupportedOperationException("Not supported");
		}

		@Override
		public void add(Structure e) {
			appendStructure(e);
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="toArray">
	@Override
	public Object[] toArray() {
		Structure[] arr = new Structure[size()];
		for(int i = 0; i < arr.length; ++i)
			arr[i] = structureAt(i);
		return arr;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		return (T[])toArray();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="contains">
	@Override
	public boolean contains(Object o) {
		return (indexOf(o) >= 0);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="get">
	@Override
	public boolean isEmpty() {
		return !containsStructures();
	}

	@Override
	public int size() {
		return gst_caps_get_size(ptr);
	}

	@Override
	public Structure get(int index) {
		if (index < 0 || index > size())
			throw new IndexOutOfBoundsException();
		return structureAt(index);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="add/remove">
	@Override
	public boolean add(Structure structure) {
		appendStructure(structure);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		int index = indexOf(o);
		if (index < 0 || index > size())
			return false;

		removeStructure(index);
		return true;
	}

	@Override
	public Structure remove(int index) {
		if (index < 0 || index > size())
			return null;
		Structure s = structureAt(index);
		removeStructure(index);
		return s;
	}

	@Override
	public boolean addAll(Collection<? extends Structure> c) {
		boolean changed = false;
		for(Structure s : c) {
			if (s != null && !contains(s)) {
				appendStructure(s);
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		int index = -1;
		boolean changed = false;
		for(Object o : c) {
			if (o != null && (index = indexOf(o)) >= 0) {
				removeStructure(index);
				changed = true;
			}
		}
		return changed;
	}

	@Override
	public void clear() {
		while(size() > 0)
			removeStructure(0);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="indexOf">
	@Override
	public int indexOf(Object o) {
		if (o == null || !(o instanceof Structure))
			return -1;

		Structure s = (Structure)o;
		Pointer pOf = s.getPointer();
		if (pOf == null || pOf == Pointer.NULL)
			return -1;

		Pointer p;
		int size = size();
		for(int i = 0; i < size; ++i)
			if ((p = gst_caps_get_structure(ptr, i)) != null && p != Pointer.NULL && p.equals(pOf))
				return i;
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		if (o == null || !(o instanceof Structure))
			return -1;

		Structure s = (Structure)o;
		Pointer pOf = s.getPointer();
		if (pOf == null || pOf == Pointer.NULL)
			return -1;

		int index = -1;
		Pointer p;
		int size = size();
		for(int i = 0; i < size; ++i)
			if ((p = gst_caps_get_structure(ptr, i)) != null && p != Pointer.NULL && p.equals(pOf))
				index = i;
		
		return index;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="iterator">
	@Override
	public Iterator<Structure> iterator() {
		return new StructureIterator();
	}

	@Override
	public ListIterator<Structure> listIterator() {
		return new StructureIterator();
	}

	@Override
	public ListIterator<Structure> listIterator(int index) {
		return new StructureIterator(index);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Unsupported Operations">
	@Override
	public Structure set(int index, Structure element) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void add(int index, Structure element) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public List<Structure> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public boolean addAll(int index, Collection<? extends Structure> c) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported");
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Package Methods">
	Object ownershipLock() {
		return this;
	}
	
	void takeOwnership() {
		synchronized(this) {
			//See disposeObject() documentation
			ownedByParent = true;
		}
	}

	void releaseOwnership() {
		synchronized(this) {
			//See disposeObject() documentation
			ownedByParent = false;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static Caps newEmpty() {
		return new Caps(false);
	}

	public static Caps newAny() {
		return new Caps(true);
	}

	public static Caps from(String caps) {
		return new Caps(caps);
	}

	public static Caps from(Pointer p) {
		return new Caps(p);
	}

	public static Caps from(Pointer p, boolean incRef) {
		return new Caps(p, incRef);
	}

	public static Caps from(Pointer p, boolean incRef, boolean unref) {
		Caps obj = new Caps(p, incRef);
		if (unref)
			obj.unref();
		return obj;
	}
	//</editor-fold>
}

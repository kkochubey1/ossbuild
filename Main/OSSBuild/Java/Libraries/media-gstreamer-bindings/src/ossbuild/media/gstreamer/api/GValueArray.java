
package ossbuild.media.gstreamer.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GValueArray extends Structure {
	public int n_values;
	public GValue[] values;
	//public volatile Pointer values;
	//< private >
	public int n_prealloced;

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GValueArray() {
		clear();
	}

	public GValueArray(Pointer pointer) {
		// pointer points to a structure with three integer fields: n_values (an integer),
		// values (an array of GValues, therefore also a pointer, i.e.: an integer
		// memory address) and n_prealloced (an integer). Then this structure
		// occupies 12 bytes (4 for n_values, 4 for the array pointer and 4 for
		// n_prealloced.

		// Viewing the data pointed by the pointer as an array of three integers.
		int[] intArray = pointer.getIntArray(0, 3);
		n_values = intArray[0];     // the first element of the array is n_values.
		n_prealloced = intArray[2]; // the third element of the array n_prealloced.

		// By constructing a new pointer taking the original and offsetting it by
		// 4 bytes, we get the pointer to the GValues array.
		Pointer pointerToArray = pointer.getPointer(4);

		// This is how to construct an array of structures from a given pointer.
		// First, a single instance of GValue is created from the pointer data.
		GValue val = new GValue(pointerToArray);
		// The structure is converted into an array with the appropriate number
		// of elements.
		values = (GValue[])val.toArray(n_values);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	@SuppressWarnings("unused")
	private static GValueArray valueOf(Pointer ptr) {
		return ptr != null ? new GValueArray(ptr) : null;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNValues() {
		return n_values;
	}

	public Object getValue(int i) {
		return Utils.getGValue(values[i].getPointer(), values[i].g_type);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ByValue/ByReference">
	public static class ByValue extends GValueArray implements com.sun.jna.Structure.ByValue {
		public ByValue() {
		}

		public ByValue(GValueArray instance) {
			super(instance.getPointer());
		}
	}

	public static class ByReference extends GValueArray implements com.sun.jna.Structure.ByReference {
		public ByReference() {
		}

		public ByReference(GValueArray instance) {
			super(instance.getPointer());
		}
	}
	//</editor-fold>
}

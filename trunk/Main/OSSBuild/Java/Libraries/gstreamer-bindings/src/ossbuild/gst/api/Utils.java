
package ossbuild.gst.api;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import ossbuild.gst.INativeObject;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Utils {

	public static Pointer[] createEmptyPointerArray() {
		return new Pointer[] { null };
	}

	public static String[] joinBefore(final String[] array, String...values) {
		if (array == null || array.length <= 0)
			return values;

		int i = 0;
		String[] arr = new String[array.length + values.length];

		for(i = 0; i < values.length; ++i)
			arr[i] = values[i];
		for(i = 0; i < array.length; ++i)
			arr[i + array.length] = array[i];

		return arr;
	}

	public static String[] joinAfter(final String[] array, String...values) {
		if (array == null || array.length <= 0)
			return values;

		int i = 0;
		String[] arr = new String[array.length + values.length];
		for(i = 0; i < array.length; ++i)
			arr[i] = array[i];
		for(i = 0; i < values.length; ++i)
			arr[i + array.length] = values[i];
		
		return arr;
	}

	public static Pointer[] createArgumentList(INativeObject...nativeObjects) {
		//Memory m = new Memory(Pointer.SIZE * nativeObjects.length);
		//for(int i = 0; i < nativeObjects.length; ++i)
		//	m.setPointer(i * Pointer.SIZE, nativeObjects[i].getPointer());
		Pointer[] p = new Pointer[nativeObjects.length];
		for(int i = 0; i < nativeObjects.length; ++i)
			p[i] = nativeObjects[i].getPointer();
		//PointerByReference p = new PointerByReference(m);
		//p.setPointer(m);
		//p.setValue(m);
		//return m;
		return p;
	}
}


package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URI;
import ossbuild.StringUtil;
import ossbuild.media.gstreamer.INativeObject;
import static ossbuild.media.gstreamer.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Utils {
	//<editor-fold defaultstate="collapsed" desc="Overloads">
	//<editor-fold defaultstate="collapsed" desc="setGValue">
	public static boolean setGValue(GValue value, GType type, Object data) {
		return setGValue(value.getPointer(), type, data);
	}

	public static boolean setGValue(GValue value, long type, Object data) {
		return setGValue(value.getPointer(), GType.fromNative(type), data);
	}

	public static boolean setGValue(GValue value, NativeLong type, Object data) {
		return setGValue(value.getPointer(), GType.fromNative(type), data);
	}

	public static boolean setGValue(Pointer g_value_ptr, long type, Object data) {
		return setGValue(g_value_ptr, GType.fromNative(type), data);
	}

	public static boolean setGValue(Pointer g_value_ptr, NativeLong type, Object data) {
		return setGValue(g_value_ptr, GType.fromNative(type), data);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="getGValue">
	public static Object getGValue(GValue value, GType type) {
		return getGValue(value.getPointer(), type);
	}

	public static Object getGValue(GValue value, long type) {
		return getGValue(value.getPointer(), GType.fromNative(type));
	}

	public static Object getGValue(GValue value, NativeLong type) {
		return getGValue(value.getPointer(), GType.fromNative(type));
	}

	public static Object getGValue(Pointer g_value_ptr, long type) {
		return getGValue(g_value_ptr, GType.fromNative(type));
	}

	public static Object getGValue(Pointer g_value_ptr, NativeLong type) {
		return getGValue(g_value_ptr, GType.fromNative(type));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="transformGValue">
	public static GValue transformGValue(GValue src, GType dstType) {
		return transformGValue(src.getPointer(), dstType);
	}

	public static boolean transformGValue(Object data, GType type, GValue dst) {
		return transformGValue(data, type, dst.getPointer());
	}
	//</editor-fold>
	//</editor-fold>

	@SuppressWarnings("unchecked")
	public static <T> T findAnnotation(Class<?> cls, Class<? extends Annotation> annotation) {
		if (cls == null || annotation == null)
			return null;

		while(cls != null) {
			for(Annotation a : cls.getDeclaredAnnotations())
				if (annotation.isAssignableFrom(a.getClass()))
					return (T)a;
			Annotation a = null;
			for(Class icls : cls.getInterfaces())
				if ((a = findAnnotation(icls, annotation)) != null)
					return (T)a;
			cls = cls.getSuperclass();
		}

		return null;
	}

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

	public static boolean booleanValue(Object value) {
		if (value instanceof Boolean)
			return ((Boolean) value).booleanValue();
		else if (value instanceof Number)
			return ((Number) value).intValue() != 0;
		else if (value instanceof String)
			return Boolean.parseBoolean((String) value);
		throw new IllegalArgumentException("Expected boolean value, not " + value.getClass());
	}

	public static int intValue(Object value) {
		if (value instanceof Number)
			return ((Number) value).intValue();
		else if (value instanceof String)
			return Integer.parseInt((String) value);
		throw new IllegalArgumentException("Expected integer value, not " + value.getClass());
	}
	
	public static long longValue(Object value) {
		if (value instanceof Number)
			return ((Number) value).longValue();
		else if (value instanceof String)
			return Long.parseLong((String) value);
		throw new IllegalArgumentException("Expected long value, not " + value.getClass());
	}

	public static float floatValue(Object value) {
		if (value instanceof Number)
			return ((Number) value).floatValue();
		else if (value instanceof String)
			return Float.parseFloat((String) value);
		throw new IllegalArgumentException("Expected float value, not " + value.getClass());
	}

	public static double doubleValue(Object value) {
		if (value instanceof Number)
			return  ((Number) value).doubleValue();
		else if (value instanceof String)
			return Double.parseDouble((String) value);
		throw new IllegalArgumentException("Expected double value, not " + value.getClass());
    }

	public static String toGstURI(File file) {
		if (file == null)
			return StringUtil.empty;

		String path = file.getAbsolutePath();
		return "file://" + ((!path.startsWith("/") ? "/" + path : path).replace("\\", "/"));
	}

	public static String toGstURI(URI uri) {
		if (uri == null)
			return StringUtil.empty;

		String uriString = uri.toString();

		// Need to fixup file:/ to be file:/// for gstreamer
		if ("file".equals(uri.getScheme()) && uri.getHost() == null) {
			final String path = uri.getRawPath();
			uriString = "file://" + path;
		}

		return uriString;
	}

	public static Pointer findPropertySpec(Pointer gobjectClassPtr, String name) {
		if (gobjectClassPtr == null || gobjectClassPtr == Pointer.NULL)
			return Pointer.NULL;
		return g_object_class_find_property(gobjectClassPtr, name);
	}

	public static boolean setGValue(Pointer g_value_ptr, GType type, Object data) {
		switch(type) {
			case Int:
				g_value_set_int(g_value_ptr, intValue(data));
				break;
			case UInt:
				g_value_set_uint(g_value_ptr, intValue(data));
				break;
			case Char:
				g_value_set_char(g_value_ptr, (byte)intValue(data));
				break;
			case UChar:
				g_value_set_uchar(g_value_ptr, (byte)intValue(data));
				break;
			case Long:
				g_value_set_long(g_value_ptr, new NativeLong(longValue(data)));
				break;
			case ULong:
				g_value_set_ulong(g_value_ptr, new NativeLong(longValue(data)));
				break;
			case Int64:
				g_value_set_int64(g_value_ptr, longValue(data));
				break;
			case UInt64:
				g_value_set_uint64(g_value_ptr, longValue(data));
				break;
			case Boolean:
				g_value_set_boolean(g_value_ptr, booleanValue(data));
				break;
			case Float:
				g_value_set_float(g_value_ptr, floatValue(data));
				break;
			case Double:
				g_value_set_double(g_value_ptr, doubleValue(data));
				break;
			case Enum:
				g_value_set_enum(g_value_ptr, intValue(data));
				break;
			case Object:
				if (!(data instanceof INativeObject))
					return false;
				g_value_set_object(g_value_ptr, ((INativeObject)data).getPointer());
				break;
			default:
				return false;
		}
		return true;
    }

	public static Object getGValue(Pointer g_value_ptr, GType type) {
		switch(type) {
			case Int:
				return g_value_get_int(g_value_ptr);
			case UInt:
				return g_value_get_uint(g_value_ptr);
			case Char:
				return g_value_get_char(g_value_ptr);
			case UChar:
				return g_value_get_uchar(g_value_ptr);
			case Long:
				return g_value_get_long(g_value_ptr).longValue();
			case ULong:
				return g_value_get_ulong(g_value_ptr).longValue();
			case Int64:
				return g_value_get_int64(g_value_ptr);
			case UInt64:
				return g_value_get_uint64(g_value_ptr);
			case Boolean:
				return g_value_get_boolean(g_value_ptr);
			case Float:
				return g_value_get_float(g_value_ptr);
			case Double:
				return g_value_get_double(g_value_ptr);
			case String:
				return g_value_get_string(g_value_ptr);
			case Object:
				return g_value_get_object(g_value_ptr);
			default:
				return null;
		}
	}

	public static GValue transformGValue(Pointer src, GType dstType) {
		GValue dst = new GValue();
		g_value_init(dst.getPointer(), dstType.asNativeLong());
		g_value_transform(src, dst.getPointer());
		//TODO: Is there a memory leak here when calling this from Element.set()?
		//Should we be using g_value_unset(src.getPointer())?
		return dst;
	}

	public static boolean transformGValue(Object data, GType type, Pointer dst) {
		GValue src = new GValue();
		Pointer pSrc = src.getPointer();
		g_value_init(pSrc, type.asNativeLong());
		setGValue(pSrc, type, data);
		boolean ret = g_value_transform(pSrc, dst);
		g_value_unset(pSrc);
		return ret;
	}
}


package ossbuild.media.gstreamer;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.io.File;
import java.net.URI;
import ossbuild.media.gstreamer.api.Callbacks;
import ossbuild.media.gstreamer.api.GParamSpec;
import ossbuild.media.gstreamer.api.GStreamer;
import ossbuild.media.gstreamer.api.GType;
import ossbuild.media.gstreamer.api.GTypeCache;
import ossbuild.media.gstreamer.api.GValue;
import ossbuild.media.gstreamer.api.IGTypeConverter;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;
import ossbuild.media.gstreamer.api.SignalCache;
import ossbuild.media.gstreamer.api.SignalConnectResponse;
import ossbuild.media.gstreamer.api.Utils;
import static ossbuild.media.gstreamer.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
abstract class BaseGObject extends BaseNativeObject implements IGObject {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	protected BaseGObject() {
		super();
	}

	protected BaseGObject(Pointer ptr) {
		super(ptr);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Ref/Unref">
	@Override
	public void ref() {
		g_object_ref(ptr);
	}

	@Override
	public void unref() {
		g_object_unref(ptr);
	}

	@Override
	public int refCount() {
		//It's one pointer off from this one b/c a GObject struct looks like:
		//struct _GObject
		//{
		//    GTypeInstance  g_type_instance; <-- Contains 1 pointer to the class
		//
		//    volatile guint ref_count;
		//    GData         *qdata;
		//};
		return ptr.getInt(Pointer.SIZE);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Object> T get(String propertyName) {
		return (T)findPropertyValue(this, propertyName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Object> T get(String propertyName, IGTypeConverter customConverter) {
		return (T)findPropertyValue(this, propertyName, customConverter);
	}

	@Override
	public boolean set(String propertyName, Object value) {
		return savePropertyValue(this, propertyName, value);
	}

	@Override
	public boolean set(String propertyName, Object value, IGTypeConverter customConverter) {
		return savePropertyValue(this, propertyName, value, customConverter);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Signals">
	@Override
	public ISignalConnectResponse connect(ISignal signal) {
		if (signal == null)
			return null;

		String signalName = SignalCache.findSignalName(signal);
		if (signalName == null)
			return null;


		NativeLong handlerID = GStreamer.connectSignal(ptr, signalName, signal);
		if (handlerID == null || handlerID.intValue() == 0)
			return null;

		try {
			Callbacks.register(ptr, signal);
		} catch(Throwable t) {
			GStreamer.disconnectSignal(ptr, handlerID);
			return null;
		}

		return new SignalConnectResponse(signal, signalName, ptr, handlerID);
	}

	@Override
	public boolean disconnect(ISignalConnectResponse response) {
		if (response == null)
			return false;

		if (!Callbacks.unregister(response.getInstance(), response.getSignal()))
			return false;

		return GStreamer.disconnectSignal(response.getInstance(), response.getHandlerID());
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static boolean propertyExists(IGObject element, String name) {
		return (findPropertySpec(element, name) != Pointer.NULL);
	}

	public static Pointer findPropertySpec(IGObject src, String name) {
		if (src == null)
			return Pointer.NULL;
		return Utils.findPropertySpec(src.getPointer().getPointer(0L), name);
	}

	@SuppressWarnings("unchecked")
	public static <T> T findPropertyValue(IGObject element, String name) {
		return (T)findPropertyValue(element, name, null);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> T findPropertyValue(IGObject element, String name, IGTypeConverter customConverter) {
		if (element == null)
			return null;

		Pointer pElement = element.getPointer();
		Pointer pPropertySpec = findPropertySpec(element, name);
		if (pPropertySpec == null || pPropertySpec == Pointer.NULL)
			return null;

		GParamSpec propertySpec = new GParamSpec(pPropertySpec);
		NativeLong propertyValueType = propertySpec.value_type;
		GType propType = GType.fromNative(propertyValueType);
		if (propType == GType.Invalid)
			return null;

		GValue propValue = new GValue();
		Pointer pPropValue = propValue.getPointer();

		g_value_init(pPropValue, propertyValueType);
		g_object_get_property(pElement, name, pPropValue);

		final Object value;

		try {
			if (customConverter == null) {
				switch(propType) {
					case Int:
						value = g_value_get_int(pPropValue);
						break;
					case UInt:
						value = g_value_get_uint(pPropValue);
						break;
					case Char:
						value = Integer.valueOf(g_value_get_char(pPropValue));
						break;
					case UChar:
						value = Integer.valueOf(g_value_get_uchar(pPropValue));
						break;
					case Long:
						value = g_value_get_long(pPropValue).longValue();
						break;
					case ULong:
						value = g_value_get_ulong(pPropValue).longValue();
						break;
					case Int64:
						value = g_value_get_int64(pPropValue);
						break;
					case UInt64:
						value = g_value_get_uint64(pPropValue);
						break;
					case Boolean:
						value = g_value_get_boolean(pPropValue);
						break;
					case Float:
						value = g_value_get_float(pPropValue);
						break;
					case Double:
						value = g_value_get_double(pPropValue);
						break;
					case String:
						value = g_value_get_string(pPropValue);
						break;
					case Enum:
						value = g_value_get_enum(pPropValue);
						break;
					case Object:
						value = new GObject(g_value_dup_object(pPropValue));
						break;
					default:
						if (propType.isGLibSystemType()) {
							if (g_value_type_transformable(propertyValueType, GType.Object.asNativeLong())) {
								value = new GObject(g_value_dup_object(Utils.transformGValue(pPropValue, GType.Object).getPointer()));
							} else if (g_value_type_transformable(propertyValueType, GType.Int.asNativeLong())) {
								value = g_value_get_int(Utils.transformGValue(propValue, GType.Int).getPointer());
							} else if (g_value_type_transformable(propertyValueType, GType.Int64.asNativeLong())) {
								value = g_value_get_int(Utils.transformGValue(propValue, GType.Int64).getPointer());
							} else {
								value = null;
							}
						} else if (propType == GType.RuntimeInstance) {
							IGTypeConverter converter = GType.customConverter(propertyValueType);
							if (converter != null) {
								value = converter.convertFromProperty(pElement, pPropertySpec, pPropValue, propertyValueType, propertySpec, propValue);
							} else {
								//Attempt to locate and create an object from the type cache
								if (g_value_type_transformable(propertyValueType, GType.Object.asNativeLong())) {
									Pointer p = g_value_dup_object(Utils.transformGValue(propValue, GType.Object).getPointer());
									if (p != null) {
										try {
											value = GTypeCache.instantiateForGType(propertyValueType, p);
										} catch(Throwable t) {
											if (t instanceof RuntimeException)
												throw (RuntimeException)t;
											else
												throw new RuntimeException(t);
										} finally {
											//Unref b/c we ref in the constructor and b/c g_value_dup_object() increments the reference count
											g_object_unref(p);
										}
									} else {
										value = null;
									}
								} else {
									value = null;
								}
							}
						} else {
							value = null;
						}
						break;
				}
			} else {
				value = customConverter.convertFromProperty(pElement, pPropertySpec, pPropValue, propertyValueType, propertySpec, propValue);
			}
		} finally {
			g_value_unset(pPropValue);
		}

		return (T)value;
	}

	public static boolean savePropertyValue(IGObject element, String name, Object value) {
		return savePropertyValue(element, name, value, null);
	}

	public static boolean savePropertyValue(IGObject element, String name, Object value, IGTypeConverter customConverter) {
		if (element == null)
			return false;

		if (value instanceof URI)
			value = Utils.toGstURI((URI)value);
		else if (value instanceof File)
			value = Utils.toGstURI((File)value);

		Pointer pElement = element.getPointer();
		Pointer pPropertySpec = findPropertySpec(element, name);
		if (pPropertySpec == null || pPropertySpec == Pointer.NULL)
			return false;

		GParamSpec propertySpec = new GParamSpec(pPropertySpec);
		NativeLong propertyValueType = propertySpec.value_type;
		GType propType = GType.fromNative(propertyValueType);
		if (propType == GType.Invalid)
			return false;

		GValue propValue = new GValue();
		Pointer pPropValue = propValue.getPointer();

		g_value_init(pPropValue, propertyValueType);

		//Explicitly set this so we know we've covered every option
		final boolean ret;

		try {
			if (customConverter == null) {
				switch(propType) {
					case Int:
						g_value_set_int(pPropValue, Utils.intValue(value));
						ret = true;
						break;
					case UInt:
						g_value_set_uint(pPropValue, Utils.intValue(value));
						ret = true;
						break;
					case Char:
						g_value_set_char(pPropValue, (byte)Utils.intValue(value));
						ret = true;
						break;
					case UChar:
						g_value_set_uchar(pPropValue, (byte)Utils.intValue(value));
						ret = true;
						break;
					case Long:
						g_value_set_long(pPropValue, new NativeLong(Utils.longValue(value)));
						ret = true;
						break;
					case ULong:
						g_value_set_ulong(pPropValue, new NativeLong(Utils.longValue(value)));
						ret = true;
						break;
					case Int64:
						g_value_set_int64(pPropValue, Utils.longValue(value));
						ret = true;
						break;
					case UInt64:
						g_value_set_uint64(pPropValue, Utils.longValue(value));
						ret = true;
						break;
					case Boolean:
						g_value_set_boolean(pPropValue, Utils.booleanValue(value));
						ret = true;
						break;
					case Float:
						g_value_set_float(pPropValue, Utils.floatValue(value));
						ret = true;
						break;
					case Double:
						g_value_set_double(pPropValue, Utils.doubleValue(value));
						ret = true;
						break;
					case String:
						g_value_set_string(pPropValue, value.toString());
						ret = true;
						break;
					case Object:
						g_value_set_object(pPropValue, ((INativeObject)value).getPointer());
						ret = true;
						break;
					default:
						if (g_value_type_transformable(propertyValueType, GType.Object.asNativeLong())) {
							ret = Utils.transformGValue(value, GType.Object, pPropValue);
						} else if (g_value_type_transformable(GType.Int64.asNativeLong(), propertyValueType)) {
							ret = Utils.transformGValue(value, GType.Int64, pPropValue);
						} else if (g_value_type_transformable(GType.Long.asNativeLong(), propertyValueType)) {
							ret = Utils.transformGValue(value, GType.Long, pPropValue);
						} else if (g_value_type_transformable(GType.Int.asNativeLong(), propertyValueType)) {
							ret = Utils.transformGValue(value, GType.Int, pPropValue);
						} else if (g_value_type_transformable(GType.Double.asNativeLong(), propertyValueType)) {
							ret = Utils.transformGValue(value, GType.Double, pPropValue);
						} else if (g_value_type_transformable(GType.Float.asNativeLong(), propertyValueType)) {
							ret = Utils.transformGValue(value, GType.Float, pPropValue);
						} else {
							if (propType == GType.RuntimeInstance) {
								IGTypeConverter converter = GType.customConverter(propertyValueType);
								if (converter != null)
									ret = converter.convertToProperty(pElement, pPropertySpec, pPropValue, propertyValueType, propertySpec, propValue, value);
								else
									ret = false;
							} else {
								ret = false;
							}
						}
						break;
				}
			} else {
				ret = customConverter.convertToProperty(pElement, pPropertySpec, pPropValue, propertyValueType, propertySpec, propValue, value);
			}
			g_object_set_property(pElement, name, pPropValue);
		} finally {
			g_value_unset(pPropValue);
		}
		return ret;
	}
	//</editor-fold>
}

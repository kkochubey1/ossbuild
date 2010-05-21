
package ossbuild.gst;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import ossbuild.StringUtil;
import ossbuild.gst.api.GParamSpec;
import ossbuild.gst.api.GType;
import ossbuild.gst.api.GTypeCache;
import ossbuild.gst.api.GValue;
import ossbuild.gst.api.IGTypeConverter;
import ossbuild.gst.api.Utils;
import static ossbuild.gst.api.GLib.*;
import static ossbuild.gst.api.GObject.*;
import static ossbuild.gst.api.GStreamer.*;
import static ossbuild.gst.api.GStreamerInterfaces.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class BaseGstObject extends BaseGObject implements IElement {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected Integer factoryRank;
	protected String factoryName, factoryClass;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public BaseGstObject(String factoryName) {
		this(factoryName, null);
	}

	public BaseGstObject(String factoryName, String elementName) {
		super();
		this.ptr = createNativeObject(factoryName, elementName);
		this.managed = true;
	}

	BaseGstObject(Pointer ptr) {
		super();
		this.ptr = ptr;
		this.managed = false;
		ref();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		if (ptr == Pointer.NULL)
			return;

		unref();
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

	@Override
	public boolean hasParent() {
		return parentExists(this);
	}

	@Override
	public String getName() {
		if (ptr == null || ptr == Pointer.NULL)
			return StringUtil.empty;

		Pointer pName = gst_object_get_name(ptr);
		String name = pName.getString(0L);
		g_free(pName);

		return name;
	}

	@Override
	public int getFactoryRank() {
		if (factoryRank != null)
			return factoryRank;
		return (factoryRank = factoryRank(this));
	}

	@Override
	public String getFactoryName() {
		if (factoryName != null)
			return factoryName;
		return (factoryName = factoryName(this));
	}

	@Override
	public String getFactoryClass() {
		if (factoryClass != null)
			return factoryClass;
		return (factoryClass = factoryClass(this));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="ToString">
	@Override
	public String toString() {
		return getName();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="State">
	@Override
	public State requestState() {
		return requestState(TimeUnit.NANOSECONDS, 0L);
	}

	@Override
	public State requestState(long timeout) {
		return requestState(TimeUnit.NANOSECONDS, timeout);
	}

	@Override
	public State requestState(TimeUnit unit, long timeout) {
		IntByReference state = new IntByReference();
		IntByReference pending = new IntByReference();
		gst_element_get_state(ptr, state, pending, unit.toNanos(timeout));
		return State.fromNative(state.getValue());
	}

	@Override
	public StateChangeReturn changeState(State state) {
		return StateChangeReturn.fromNative(gst_element_set_state(ptr, state.nativeValue));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static String factoryName(IElement element) {
		if (element == null)
			return StringUtil.empty;

		Pointer factory = gst_element_get_factory(element.getPointer());
		if (factory == null || factory == Pointer.NULL)
			return StringUtil.empty;

		return gst_plugin_feature_get_name(factory);
	}

	public static String factoryClass(IElement element) {
		if (element == null)
			return StringUtil.empty;

		Pointer factory = gst_element_get_factory(element.getPointer());
		if (factory == null || factory == Pointer.NULL)
			return StringUtil.empty;

		return gst_element_factory_get_klass(factory);
	}

	public static int factoryRank(IElement element) {
		if (element == null)
			return Rank.None.asInt();

		Pointer factory = gst_element_get_factory(element.getPointer());
		if (factory == null || factory == Pointer.NULL)
			return Rank.None.asInt();

		return gst_plugin_feature_get_rank(factory);
	}

	public static Set<String> factoryClassAsList(IElement element) {
		String cls = factoryClass(element);
		Set<String> ret;
		if (!StringUtil.isNullOrEmpty(cls)) {
			String[] arr = cls.split("/");
			ret = new HashSet<String>(arr.length);
			Collections.addAll(ret, arr);
		} else {
			ret = Collections.emptySet();
		}
		return Collections.unmodifiableSet(ret);
	}

	public static boolean parentExists(IElement element) {
		if (element == null)
			return false;

		Pointer parent = gst_object_get_parent(element.getPointer());
		boolean exists = (parent != null && parent != Pointer.NULL);
		if (exists)
			gst_object_unref(parent);
		return exists;
	}

	public static boolean link(IElement element1, IElement element2) {
		if (element1 == null || element2 == null)
			return false;
		return gst_element_link(element1.getPointer(), element2.getPointer());
	}

	public static boolean linkMany(IElement...elements) {
		if (elements == null || elements.length <= 0)
			return true;

		boolean ret = true;
		Pointer curr = null;
		Pointer last = null;
		for(int i = 0; i < elements.length; ++i) {
			if (elements[i] == null)
				continue;

			curr = elements[i].getPointer();
			if (last != null)
				ret = ret && gst_element_link(last, curr);
			last = curr;
		}

		return ret;
	}

	public static boolean xoverlayWindowID(IElement src, long window) {
		gst_x_overlay_set_xwindow_id(src.getPointer(), new NativeLong(window));
		return true;
	}

	public static boolean xoverlayWindowID(IElement src, NativeLong window) {
		gst_x_overlay_set_xwindow_id(src.getPointer(), window);
		return true;
	}

	public static boolean xoverlayWindowID(IElement src, Pointer window) {
		gst_x_overlay_set_xwindow_id(src.getPointer(), window);
		return true;
	}

	public static boolean propertyExists(IElement element, String name) {
		return (findPropertySpec(element, name) != Pointer.NULL);
	}

	public static Pointer findPropertySpec(IElement src, String name) {
		if (src == null)
			return Pointer.NULL;
		return Utils.findPropertySpec(src.getPointer().getPointer(0L), name);
	}

	@SuppressWarnings("unchecked")
	public static <T> T findPropertyValue(IElement element, String name) {
		return (T)findPropertyValue(element, name, null);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> T findPropertyValue(IElement element, String name, IGTypeConverter customConverter) {
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

	public static boolean savePropertyValue(IElement element, String name, Object value) {
		return savePropertyValue(element, name, value, null);
	}

	public static boolean savePropertyValue(IElement element, String name, Object value, IGTypeConverter customConverter) {
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

	protected abstract Pointer createNativeObject(String factoryName, String elementName);
}

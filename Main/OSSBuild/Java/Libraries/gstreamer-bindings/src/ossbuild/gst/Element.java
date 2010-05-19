
package ossbuild.gst;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import ossbuild.StringUtil;
import ossbuild.gst.api.GParamSpec;
import ossbuild.gst.api.GType;
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
public class Element extends BaseGObject implements IElement {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private Integer factoryRank;
	private String factoryName, factoryClass;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Element(String factoryName) {
		this(factoryName, StringUtil.empty);
	}

	public Element(String factoryName, String elementName) {
		super();
		this.ptr = gst_element_factory_make(factoryName, elementName);
		this.managed = true;
	}

	Element(Pointer ptr) {
		super();
		this.ptr = ptr;
		this.managed = false;
	}

	Element(Pointer ptr, boolean casting) {
		super();
		this.ptr = ptr;
		this.managed = casting;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public <T extends Object> T get(String propertyName) {
		return propertyValue(this, propertyName);
	}

	@Override
	public <T extends Object> T get(String propertyName, IGTypeConverter customConverter) {
		return propertyValue(this, propertyName, customConverter);
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

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	@Override
	protected void disposeObject() {
		if (ptr == Pointer.NULL)
			return;
		if (managed && !hasParent()) {
			g_object_unref(ptr);
		}
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
	public static IElement make(String factoryName) {
		return new Element(factoryName);
	}

	public static IElement make(String factoryName, String elementName) {
		return new Element(factoryName, elementName);
	}

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

	public static <T extends Object> T propertyValue(IElement element, String name) {
		return propertyValue(element, name, null);
	}

	public static <T extends Object> T propertyValue(IElement element, String name, IGTypeConverter customConverter) {
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
				case Object:
					value = new GObject(g_value_dup_object(pPropValue));
					break;
				case User:
					IGTypeConverter converter = GType.customConverter(propertyValueType);
					if (converter != null)
						value = converter.convertFromProperty(pElement, pPropertySpec, pPropValue, propertyValueType, propertySpec, propValue);
					else
						value = null;
					break;
				default:
					if (g_value_type_transformable(propType.asNativeLong(), GType.Object.asNativeLong()))
						value = new GObject(g_value_dup_object(pPropValue));
					else if (g_value_type_transformable(propType.asNativeLong(), GType.Int.asNativeLong()))
						value = g_value_get_int(Utils.transformGValue(propValue, GType.Int).getPointer());
					else if (g_value_type_transformable(propType.asNativeLong(), GType.Int64.asNativeLong()))
						value = g_value_get_int(Utils.transformGValue(propValue, GType.Int64).getPointer());
					else
						value = null;
					break;
			}
		} else {
			value = customConverter.convertFromProperty(pElement, pPropertySpec, pPropValue, propertyValueType, propertySpec, propValue);
		}
		g_value_unset(pPropValue);
		
		return (T)value;
	}

	static void setProperty(Pointer property, Object value) {
		
	}
	//</editor-fold>
}

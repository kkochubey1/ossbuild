
package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.Buffer;
import ossbuild.media.gstreamer.Caps;
import ossbuild.media.gstreamer.elements.VideoTestSrcPattern;
import static ossbuild.media.gstreamer.api.GObject.*;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GTypeConverters {
	//<editor-fold defaultstate="collapsed" desc="Converters">
	public static final IGTypeConverter VIDEO_TEST_SRC_PATTERN = new IGTypeConverter() {
		@Override
		public boolean convertToProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue, Object value) {
			if (value instanceof VideoTestSrcPattern)
				g_value_set_enum(pPropValue, ((VideoTestSrcPattern)value).getNativeValue());
			else if (value instanceof Number)
				g_value_set_enum(pPropValue, ((Number)value).intValue());
			else
				return false;
			return true;
		}

		@Override
		public Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue) {
			return VideoTestSrcPattern.fromNative(g_value_get_enum(pPropValue));
		}
	};

	public static final IGTypeConverter BUFFER = new IGTypeConverter() {
		@Override
		public boolean convertToProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue, Object value) {
			if (!(value instanceof Buffer))
				return false;
			gst_value_set_mini_object(pPropValue, ((Buffer)value).getPointer());
			return true;
		}

		@Override
		public Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue) {
			return Buffer.from(gst_value_get_mini_object(pPropValue));
		}
	};

	public static final IGTypeConverter CAPS = new IGTypeConverter() {
		@Override
		public boolean convertToProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue, Object value) {
			if (!(value instanceof Caps))
				return false;
			gst_value_set_caps(pPropValue, ((Caps)value).getPointer());
			return true;
		}

		@Override
		public Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue) {
			return Caps.from(gst_value_get_caps(pPropValue), false, false);
		}
	};
	//</editor-fold>

	public static void initialize() {
		//Some of these can't be added easily b/c their gtype value is set at runtime
		//GType.addCustomConverter(VideoTestSrcPattern.GTYPE, VIDEO_TEST_SRC_PATTERN);
		GType.addCustomConverter(gst_caps_get_type(), CAPS);
	}
}

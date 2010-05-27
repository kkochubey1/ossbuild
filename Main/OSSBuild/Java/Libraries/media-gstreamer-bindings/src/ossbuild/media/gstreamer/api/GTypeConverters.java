
package ossbuild.media.gstreamer.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import ossbuild.media.gstreamer.Bin;
import ossbuild.media.gstreamer.Buffer;
import ossbuild.media.gstreamer.Caps;
import ossbuild.media.gstreamer.IBin;
import ossbuild.media.gstreamer.IPipeline;
import ossbuild.media.gstreamer.Pipeline;
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

	public static final IGTypeConverter BIN = new IGTypeConverter() {
		@Override
		public boolean convertToProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue, Object value) {
			if (!(value instanceof IBin))
				return false;
			g_value_set_object(pPropValue, ((IBin)value).getPointer());
			return true;
		}

		@Override
		public Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue) {
			Pointer p = g_value_dup_object(Utils.transformGValue(propValue, GType.Object).getPointer());
			if (p != null && p != Pointer.NULL) {
				try {
					return Bin.from(p);
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
				return null;
			}
		}
	};

	public static final IGTypeConverter PIPELINE = new IGTypeConverter() {
		@Override
		public boolean convertToProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue, Object value) {
			if (!(value instanceof IPipeline))
				return false;
			g_value_set_object(pPropValue, ((IPipeline)value).getPointer());
			return true;
		}

		@Override
		public Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue) {
			Pointer p = g_value_dup_object(Utils.transformGValue(propValue, GType.Object).getPointer());
			if (p != null && p != Pointer.NULL) {
				try {
					return Pipeline.from(p);
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
				return null;
			}
		}
	};
	//</editor-fold>

	public static void initialize() {
		//Some of these can't be added easily b/c their gtype value is set at runtime
		//GType.addCustomConverter(VideoTestSrcPattern.GTYPE, VIDEO_TEST_SRC_PATTERN);
		GType.addCustomConverter(gst_pipeline_get_type(), PIPELINE);
		GType.addCustomConverter(gst_bin_get_type(), BIN);
		GType.addCustomConverter(gst_caps_get_type(), CAPS);
	}
}

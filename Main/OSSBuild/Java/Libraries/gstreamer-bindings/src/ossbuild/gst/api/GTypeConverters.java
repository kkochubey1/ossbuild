
package ossbuild.gst.api;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import ossbuild.gst.elements.VideoTestSrcPattern;
import static ossbuild.gst.api.GObject.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GTypeConverters {
	//<editor-fold defaultstate="collapsed" desc="Converters">
	public static final IGTypeConverter VIDEO_TEST_SRC_PATTERN = new IGTypeConverter() {
		@Override
		public Object convertFromProperty(Pointer pObject, Pointer pParamSpec, Pointer pPropValue, NativeLong propertyType, GParamSpec paramSpec, GValue propValue) {
			return VideoTestSrcPattern.fromNative(g_value_get_enum(pPropValue));
		}
	};
	//</editor-fold>

	public static void initialize() {
		//Some of these can't be added easily b/c their gtype value is set at runtime
		//GType.addCustomConverter(VideoTestSrcPattern.GTYPE, VIDEO_TEST_SRC_PATTERN);
	}
}

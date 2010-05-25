
package ossbuild.media.gstreamer;

import ossbuild.media.gstreamer.api.IGTypeConverter;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IGObject extends INativeObject {
	boolean hasProperty(String name);
	boolean set(String name, Object value);
	boolean set(String name, Object value, IGTypeConverter customConverter);
	<T extends Object> T get(String propertyName);
	<T extends Object> T get(String propertyName, IGTypeConverter converter);
	
	ISignalConnectResponse connect(ISignal signal);
	ISignalConnectResponse connect(String signalName, ISignal signal);
	boolean disconnect(ISignalConnectResponse response);
}

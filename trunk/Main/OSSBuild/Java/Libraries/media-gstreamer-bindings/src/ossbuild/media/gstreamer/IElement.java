
package ossbuild.media.gstreamer;

import java.util.concurrent.TimeUnit;
import ossbuild.media.gstreamer.api.IGTypeConverter;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IElement extends INativeObject {
	boolean set(String name, Object value);
	boolean set(String name, Object value, IGTypeConverter customConverter);
	<T extends Object> T get(String propertyName);
	<T extends Object> T get(String propertyName, IGTypeConverter converter);

	String getName();
	boolean hasParent();
	
	int getFactoryRank();
	String getFactoryName();
	String getFactoryClass();
	
	State requestState();
	State requestState(long timeout);
	State requestState(TimeUnit unit, long timeout);
	StateChangeReturn changeState(State state);

	ISignalConnectResponse connect(ISignal signal);
	boolean disconnect(ISignalConnectResponse response);
}

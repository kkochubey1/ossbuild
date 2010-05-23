
package ossbuild.media.gstreamer;

import java.util.concurrent.TimeUnit;
import ossbuild.media.gstreamer.api.IGTypeConverter;
import ossbuild.media.gstreamer.api.ISignal;
import ossbuild.media.gstreamer.api.ISignalConnectResponse;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IElement extends IGObject {
	String getName();
	boolean hasParent();
	
	int getFactoryRank();
	String getFactoryName();
	String getFactoryClass();
	
	State requestState();
	State requestState(long timeout);
	State requestState(TimeUnit unit, long timeout);
	StateChangeReturn changeState(State state);
}

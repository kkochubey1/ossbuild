
package ossbuild.gst;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IElement extends INativeObject, IDisposable {
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

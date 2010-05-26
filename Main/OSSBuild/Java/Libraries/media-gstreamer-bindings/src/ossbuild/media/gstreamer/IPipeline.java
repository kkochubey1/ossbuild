
package ossbuild.media.gstreamer;

import java.util.concurrent.TimeUnit;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IPipeline extends IBin {
	IBus getBus();
	void busSyncHandler(final IBusSyncHandler handler);

	boolean seek(long time);
	boolean seek(long time, TimeUnit unit);
	boolean seek(double rate, Format format, SeekType startType, long start, SeekType stopType, long stop, SeekFlags... flags);
	boolean seek(double rate, Format format, int flags, SeekType startType, long start, SeekType stopType, long stop);

	long queryPosition();
	long queryPosition(TimeUnit unit);
	long queryPosition(Format format);

	long queryDuration();
	long queryDuration(TimeUnit unit);
	long queryDuration(Format format);
}


package ossbuild.media.gstreamer.api;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface ISignalCacheRecord {
	String getSignalName();
	Class<? extends ISignal> getSignalClass();
}

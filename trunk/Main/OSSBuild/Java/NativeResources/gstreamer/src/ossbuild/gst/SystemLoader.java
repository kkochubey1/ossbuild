
package ossbuild.gst;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SystemLoader extends ossbuild.init.SystemLoader {
	@Override
	public void load() throws Throwable {
		Native.initialize();
	}

	@Override
	public void unload() throws Throwable {
	}
}

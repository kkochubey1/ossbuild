
package ossbuild.crypto;

import ossbuild.NativeResource;
import ossbuild.Sys;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SystemLoader extends ossbuild.init.SystemLoader {
	@Override
	public void load() throws Throwable {
		Sys.loadNativeResources(NativeResource.Crypto);
	}

	@Override
	public void unload() throws Throwable {
	}
}

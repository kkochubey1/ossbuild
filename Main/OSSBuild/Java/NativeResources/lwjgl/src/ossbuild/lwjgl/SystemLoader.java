
package ossbuild.lwjgl;

import ossbuild.NativeResource;
import ossbuild.Sys;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class SystemLoader extends ossbuild.init.SystemLoader {
	@Override
	public void load() throws Throwable {
		Sys.loadNativeResources(NativeResource.LWJGL);

		//Ask LWJGL to initialize itself
		org.lwjgl.Sys.initialize();
	}

	@Override
	public void unload() throws Throwable {
	}
}

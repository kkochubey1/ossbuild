
package ossbuild.lwjgl;

import ossbuild.NativeResource;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.extract.MissingResourceException;
import ossbuild.extract.Resources;
import ossbuild.init.RegistryReference;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public final class Register extends RegistryReference {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  REGISTRY_NAME             = NativeResource.LWJGL
	;

	public static final String
		  RESOURCE_PKG_PREFIX       = "resources.lwjgl"
	;

	public static final String
		  RESOURCE_DEFINITION_FILE  = "resources.xml"
	;

	public static final String
		  RESOURCE_DEFINITION_PREFIX
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		//Determine the prefix to use based off of what's available
		final String os = Sys.createPlatformPackageResourcePrefix(RESOURCE_PKG_PREFIX, Sys.getOS());
		final String osfamily = Sys.createPlatformPackageResourcePrefix(RESOURCE_PKG_PREFIX, Sys.getOSFamily());

		RESOURCE_DEFINITION_PREFIX = (
			Sys.isResourceAvailable(os + RESOURCE_DEFINITION_FILE) ? os :
				Sys.isResourceAvailable(osfamily + RESOURCE_DEFINITION_FILE) ? osfamily :
					StringUtil.empty
		);

		if (StringUtil.isNullOrEmpty(RESOURCE_DEFINITION_PREFIX))
			throw new MissingResourceException("Unable to locate the main resource extraction file");
	}
	//</editor-fold>

	@Override
	public Resources createResourceExtractor() {
		return Resources.newInstance(RESOURCE_DEFINITION_PREFIX + RESOURCE_DEFINITION_FILE);
	}
}

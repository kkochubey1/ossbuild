
package ossbuild.ffmpeg;

import ossbuild.NativeResource;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.extract.IVariableProcessor;
import ossbuild.extract.MissingResourceException;
import ossbuild.extract.Resources;
import ossbuild.extract.VariableProcessorFactory;
import ossbuild.init.RegistryReference;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public final class Register extends RegistryReference {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  REGISTRY_NAME             = NativeResource.FFmpeg
	;

	public static final String
		  RESOURCE_PKG_PREFIX       = "resources.ffmpeg"
	;

	public static final String
		  RESOURCE_DEFINITION_FILE  = "resources.xml"
	;

	public static final String
		  VAR_LICENSE = "lic"
	;

	public static final String
		  RESOURCE_DEFINITION_PREFIX
	;

	public static final boolean
		  GPL
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		//Determine the prefix to use based off of what's available
		//Check if GPL is available first.
		final String os_gpl = Sys.createPlatformPackageResourcePrefix(RESOURCE_PKG_PREFIX + ".gpl", Sys.getOS());
		final String osfamily_gpl = Sys.createPlatformPackageResourcePrefix(RESOURCE_PKG_PREFIX + ".gpl", Sys.getOSFamily());

		final String os_lgpl = Sys.createPlatformPackageResourcePrefix(RESOURCE_PKG_PREFIX + ".lgpl", Sys.getOS());
		final String osfamily_lgpl = Sys.createPlatformPackageResourcePrefix(RESOURCE_PKG_PREFIX + ".lgpl", Sys.getOSFamily());

		boolean isGPL = false;
		RESOURCE_DEFINITION_PREFIX = (
			(isGPL = Sys.isResourceAvailable(os_gpl + RESOURCE_DEFINITION_FILE)) ? os_gpl :
				(isGPL = Sys.isResourceAvailable(osfamily_gpl + RESOURCE_DEFINITION_FILE)) ? osfamily_gpl :
					(Sys.isResourceAvailable(os_lgpl + RESOURCE_DEFINITION_FILE)) ? os_lgpl :
						(Sys.isResourceAvailable(osfamily_lgpl + RESOURCE_DEFINITION_FILE)) ? osfamily_lgpl :
							StringUtil.empty
		);
		GPL = isGPL;

		if (StringUtil.isNullOrEmpty(RESOURCE_DEFINITION_PREFIX))
			throw new MissingResourceException("Unable to locate the main resource extraction file");
	}
	//</editor-fold>

	@Override
	public Resources createResourceExtractor() {
		//Adds a variable we can use to determine the license we're using.
		final IVariableProcessor varproc = VariableProcessorFactory.newInstance();
		varproc.saveVariable(VAR_LICENSE, GPL ? "gpl" : "lgpl");
		
		return Resources.newInstance(varproc, RESOURCE_DEFINITION_PREFIX + RESOURCE_DEFINITION_FILE);
	}
}

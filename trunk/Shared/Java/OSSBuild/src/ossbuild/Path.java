
package ossbuild;

import java.io.File;
import ossbuild.extract.ResourceUtils;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Path {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  directorySeparator = System.getProperty("file.separator")
		, pathSeparator = File.pathSeparator
	;

	public static final String
		  tempDirectory = clean(System.getProperty("java.io.tmpdir"))
		, homeDirectory = clean(System.getProperty("user.home"))
		, workingDirectory = clean(new File(".").getAbsolutePath())
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static String nativeDirectorySeparator(final String path) {
		if (path == null)
			return StringUtil.empty;
		return path.replace("/", directorySeparator).replace("\\", directorySeparator);
	}

	public static String clean(final String path) {
		if (StringUtil.isNullOrEmpty(path))
			return StringUtil.empty;

		if (path.endsWith(directorySeparator))
			return path;

		if (path.endsWith("/") || path.endsWith("\\"))
			return path.substring(0, path.length() - 1) + directorySeparator;

		return path + directorySeparator;
	}

	public static File combine(final File parent, final String child) {
		return new File(parent, child);
	}

	public static File combine(final String parent, final String child) {
		return new File(parent, child);
	}

	public static boolean exists(final String path) {
		return exists(new File(path));
	}

	public static boolean exists(final File path) {
		if (path == null)
			return false;
		return path.exists();
	}

	public static boolean delete(final String path) {
		return delete(new File(path));
	}

	public static boolean delete(final File path) {
		if (path == null)
			return false;
		
		try {
			//True b/c the intent of this function is satisfied -- the directory/file no longer exists!
			if (!path.exists())
				return true;

			if (path.isFile())
				return path.delete();
			else
				return ResourceUtils.deleteDirectory(path);
		} catch(SecurityException se) {
			return false;
		}
	}
	//</editor-fold>
}

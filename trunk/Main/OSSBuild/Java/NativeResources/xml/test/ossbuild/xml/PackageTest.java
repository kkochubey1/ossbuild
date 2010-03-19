package ossbuild.xml;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ossbuild.NativeResource;
import ossbuild.OSFamily;
import ossbuild.Path;
import ossbuild.Sys;
import static org.junit.Assert.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class PackageTest {
	//<editor-fold defaultstate="collapsed" desc="Setup">
	public PackageTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
		assertTrue("These unit tests require Windows to complete", Sys.isOSFamily(OSFamily.Windows));
	}

	@After
	public void tearDown() {
	}
	//</editor-fold>

	@Test
	public void testRegistry() {
		assertTrue(Sys.initializeRegistry());

		final File binDir = Path.combine(Path.nativeResourcesDirectory, "bin/");

		assertTrue(Path.delete(binDir));
		assertTrue(Sys.loadNativeResources(NativeResource.XML));

		//Shouldn't matter how many times we call this - it shouldn't do
		//anything different after its first initialization...
		for(int i = 0; i < 100; ++i)
			assertTrue(Sys.loadNativeResources(NativeResource.XML));

		assertTrue(Path.exists(binDir));

		switch(Sys.getOSFamily()) {
			case Unix:
				assertTrue(Path.exists(Path.combine(binDir, "libxml2.so.2")));
				break;
			case Windows:
				assertTrue(Path.exists(Path.combine(binDir, "libxml2-2.dll")));
				break;
			default:
				assertTrue("Unsupported test platform", false);
				break;
		}

		assertTrue(Sys.cleanRegistry());
	}
}

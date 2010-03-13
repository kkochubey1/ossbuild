package ossbuild.base;

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

		assertTrue(Path.delete(Path.combine(Path.tempDirectory, "ossbuild/bin/")));
		assertTrue(Sys.loadNativeResources(NativeResource.Base));
		
		//Shouldn't matter how many times we call this - it shouldn't do
		//anything different after its first initialization...
		for(int i = 0; i < 100; ++i)
			assertTrue(Sys.loadNativeResources(NativeResource.Base));

		assertTrue(Path.exists(Path.combine(Path.tempDirectory, "ossbuild/bin/")));

		final String sysPath = Sys.getEnvironmentVariable("PATH");
		assertNotNull(sysPath);
		assertTrue(sysPath.contains(Path.combine(Path.tempDirectory, "ossbuild/bin/").getAbsolutePath()));
		
		assertTrue(Sys.cleanRegistry());
	}
}

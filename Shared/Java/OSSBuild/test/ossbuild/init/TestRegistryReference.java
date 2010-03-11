
package ossbuild.init;

import static org.junit.Assert.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class TestRegistryReference extends RegistryReference {

	@Override
	public void register() throws Throwable {
		assertTrue(true);
	}

	@Override
	public void unregister() throws Throwable {
		assertTrue(true);
	}

}

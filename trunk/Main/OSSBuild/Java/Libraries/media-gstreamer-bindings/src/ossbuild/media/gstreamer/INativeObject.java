
package ossbuild.media.gstreamer;

import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface INativeObject extends IDisposable {
	boolean equals(Pointer ptr);
	boolean equals(INativeObject ptr);
	
	boolean isManaged();
	Pointer getPointer();

	void ref();
	void unref();
	int refCount();
}

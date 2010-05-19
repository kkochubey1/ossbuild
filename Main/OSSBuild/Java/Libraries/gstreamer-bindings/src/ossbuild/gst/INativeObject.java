
package ossbuild.gst;

import com.sun.jna.Pointer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface INativeObject {
	boolean equals(Pointer ptr);
	boolean equals(INativeObject ptr);
	
	boolean isManaged();
	Pointer getPointer();
}

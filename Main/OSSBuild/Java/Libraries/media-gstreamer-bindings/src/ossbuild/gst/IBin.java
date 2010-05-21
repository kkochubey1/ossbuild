
package ossbuild.gst;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IBin extends IElement {
	boolean add(IElement element);
	boolean addMany(IElement... elements);
	boolean addAndLinkMany(IElement... elements);
}

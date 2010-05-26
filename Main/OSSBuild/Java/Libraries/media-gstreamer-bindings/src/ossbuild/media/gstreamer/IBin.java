
package ossbuild.media.gstreamer;

import java.io.File;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IBin extends IElement {
	IBin binFromName(String name);
	IBin binFromNameRecurseUp(String name);
	IElement elementFromName(String name);
	IElement elementFromNameRecurseUp(String name);

	boolean add(IElement element);
	boolean addMany(IElement... elements);
	boolean addAndLinkMany(IElement... elements);

	boolean remove(IElement element);
	boolean removeMany(IElement... elements);
	boolean unlinkAndRemoveMany(IElement... elements);

	void visitElements(IElementVisitor visitor);
	void visitElementsSorted(IElementVisitor visitor);
	void visitElementsRecursive(IElementVisitor visitor);
	void visitSinks(IElementVisitor visitor);
	void visitSources(IElementVisitor visitor);

	File writeToDotFile(DebugGraphDetails... details);
	File writeToDotFile(String name, DebugGraphDetails... details);
	File writeToDotFile(boolean includeTimeStampInFileName, DebugGraphDetails... details);
	File writeToDotFile(String name, boolean includeTimeStampInFileName, DebugGraphDetails... details);

	//<editor-fold defaultstate="collapsed" desc="Visitors">
	public static interface IElementVisitor {
		boolean visit(IBin src, IElement element);
	}
	//</editor-fold>
}

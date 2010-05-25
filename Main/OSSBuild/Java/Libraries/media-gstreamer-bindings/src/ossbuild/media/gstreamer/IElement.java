
package ossbuild.media.gstreamer;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IElement extends IGObject {
	String getName();
	boolean hasParent();
	
	int getFactoryRank();
	String getFactoryName();
	String getFactoryClass();
	
	State requestState();
	State requestState(long timeout);
	State requestState(TimeUnit unit, long timeout);
	StateChangeReturn changeState(State state);

	boolean setCaps(Caps caps);

	long getBaseTime();
	long getStartTime();

	Pad staticPad(String name);
	boolean addPad(Pad pad);
	boolean removePad(Pad pad);
	boolean postMessage(Message msg);

	void visitPads(IPadVisitor visitor);
	void visitSrcPads(IPadVisitor visitor);
	void visitSinkPads(IPadVisitor visitor);

	//<editor-fold defaultstate="collapsed" desc="Visitors">
	public static interface IPadVisitor {
		boolean visit(IElement src, Pad pad);
	}
	//</editor-fold>
}

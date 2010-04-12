
package simple.swt;

import org.gstreamer.elements.DecodeBin;

/**
 *
 * @author David
 */
public class DecodeBin2 extends DecodeBin {
	public DecodeBin2(String name) {
		super(makeRawElement("decodebin2", name));
	}
}


package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Fraction {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private final int numerator;
	private final int denominator;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public Fraction(int numerator, int denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getNumerator() {
		return numerator;
	}

	public int getDenominator() {
		return denominator;
	}
	//</editor-fold>
}

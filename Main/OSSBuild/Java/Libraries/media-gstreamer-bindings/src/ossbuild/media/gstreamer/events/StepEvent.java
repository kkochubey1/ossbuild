
package ossbuild.media.gstreamer.events;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import ossbuild.media.gstreamer.Event;
import ossbuild.media.gstreamer.Format;
import static ossbuild.media.gstreamer.api.GStreamer.*;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class StepEvent extends Event {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private Format format;
	private long amount;
	private double rate;
	private boolean flush;
	private boolean intermediate;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public StepEvent(Format format, long amount, double rate, boolean flush, boolean intermediate) {
		super();

		this.ptr = gst_event_new_step(format.getNativeValue(), amount, rate, flush ? 1 : 0, intermediate ? 1 : 0);
		this.managed = true;
		
		this.format = format;
		this.amount = amount;
		this.rate = rate;
		this.flush = flush;
		this.intermediate = intermediate;
	}

	private StepEvent(Pointer event) {
		super(event);
		
		IntByReference refFormat = new IntByReference();
		LongByReference refAmount = new LongByReference();
		DoubleByReference refRate = new DoubleByReference();
		IntByReference refFlush = new IntByReference();
		IntByReference refIntermediate = new IntByReference();
		gst_event_parse_step(event, refFormat, refAmount, refRate, refFlush, refIntermediate);

		this.format = Format.fromNative(refFormat.getValue());
		this.amount = refAmount.getValue();
		this.rate = refRate.getValue();
		this.flush = refFlush.getValue() != 0;
		this.intermediate = refIntermediate.getValue() != 0;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public Format getFormat() {
		return format;
	}

	public long getAmount() {
		return amount;
	}

	public double getRate() {
		return rate;
	}

	public boolean isFlushing() {
		return flush;
	}

	public boolean isIntermediate() {
		return intermediate;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static StepEvent from(Pointer event) {
		return new StepEvent(event);
	}

	public static StepEvent from(Format format, long amount, double rate, boolean flush, boolean intermediate) {
		return new StepEvent(format, amount, rate, flush, intermediate);
	}
	//</editor-fold>
}

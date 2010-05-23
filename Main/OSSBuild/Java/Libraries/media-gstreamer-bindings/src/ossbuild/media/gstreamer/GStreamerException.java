package ossbuild.media.gstreamer;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GStreamerException extends RuntimeException {
	public GStreamerException() {
		super();
	}

	public GStreamerException(String msg) {
		super(msg);
	}
	
	public GStreamerException(Throwable cause) {
		super(cause);
	}

	public GStreamerException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

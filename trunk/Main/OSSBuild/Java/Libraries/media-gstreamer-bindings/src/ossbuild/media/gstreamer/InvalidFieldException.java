package ossbuild.media.gstreamer;

/**
 *
 */
public class InvalidFieldException extends GStreamerException {
	public InvalidFieldException() {
		super();
	}

	public InvalidFieldException(String msg) {
		super(msg);
	}
	
	public InvalidFieldException(Throwable cause) {
		super(cause);
	}

	public InvalidFieldException(String msg, Throwable cause) {
		super(msg, cause);
	}
}

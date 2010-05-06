
package simple.media;

import java.net.URI;

/**
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IMediaRequest {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final float
		  SOURCE_FPS                    = -1.0f
		, MINIMUM_FPS                   = 1.0f
	;

	public static final int
		  REPEAT_FOREVER                = -1
		, REPEAT_NONE                   = 0
	;

	public static final long
		  UNKNOWN_TIME                  = -1L
	;

	public static final float
		  DEFAULT_FPS                   = SOURCE_FPS
	;

	public static final int
		  DEFAULT_REPEAT_COUNT          = REPEAT_NONE
	;

	public static final long
		  DEFAULT_LAST_MODIFIED_TIME    = UNKNOWN_TIME
	;

	public static final boolean
		  DEFAULT_MAINTAIN_ASPECT_RATIO = true
	;
	//</editor-fold>

	long getCreatedTime();
	long getLastModifiedTime();

	boolean isLastModifiedTimeUnknown();
	boolean isAspectRatioMaintained();
	boolean isLiveSource();
	boolean isRepeatForever();
	boolean isRepeatNone();
	MediaRequestType getType();
	int getRepeatCount();
	Scheme getScheme();
	String getTitle();
	URI getURI();
	float getFPS();
}

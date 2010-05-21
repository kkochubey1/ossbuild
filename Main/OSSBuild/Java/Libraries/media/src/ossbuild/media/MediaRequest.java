
package ossbuild.media;

import java.net.URI;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class MediaRequest implements IMediaRequest {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	private boolean maintainAspectRatio;
	private boolean liveSource;
	private int repeatCount;
	private Scheme scheme;
	private String title;
	private URI uri;
	private float fps;
	private long createdTime;
	private long lastModifiedTime;
	private MediaRequestType requestType;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public MediaRequest(final MediaRequestType RequestType, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, float FPS, final Scheme Scheme, final URI URI) {
		init(RequestType, DEFAULT_LAST_MODIFIED_TIME, Title, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme, URI);
	}

	public MediaRequest(final MediaRequestType RequestType, final long LastModifiedTime, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, float FPS, final Scheme Scheme, final URI URI) {
		init(RequestType, LastModifiedTime, Title, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme, URI);
	}

	protected void init(final MediaRequestType RequestType, final long LastModifiedTime, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, float FPS, final Scheme Scheme, final URI URI) {
		this.uri = URI;
		this.fps = FPS;
		this.title = Title;
		this.scheme = Scheme;
		this.liveSource = LiveSource;
		this.repeatCount = RepeatCount;
		this.requestType = RequestType;
		this.lastModifiedTime = LastModifiedTime;
		this.maintainAspectRatio = MaintainAspectRatio;
		this.createdTime = System.currentTimeMillis();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	@Override
	public float getFPS() {
		return fps;
	}
	
	@Override
	public boolean isRepeatForever() {
		return (repeatCount == REPEAT_FOREVER);
	}

	@Override
	public boolean isRepeatNone() {
		return (repeatCount == REPEAT_NONE);
	}

	@Override
	public boolean isLastModifiedTimeUnknown() {
		return lastModifiedTime == UNKNOWN_TIME;
	}

	@Override
	public int getRepeatCount() {
		return repeatCount;
	}

	@Override
	public Scheme getScheme() {
		return scheme;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public boolean isLiveSource() {
		return liveSource;
	}

	@Override
	public long getCreatedTime() {
		return createdTime;
	}

	@Override
	public long getLastModifiedTime() {
		return lastModifiedTime;
	}

	@Override
	public boolean isAspectRatioMaintained() {
		return maintainAspectRatio;
	}

	@Override
	public MediaRequestType getType() {
		return requestType;
	}
	//</editor-fold>
}

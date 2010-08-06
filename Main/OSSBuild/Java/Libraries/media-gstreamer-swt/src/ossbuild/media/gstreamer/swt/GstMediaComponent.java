
package ossbuild.media.gstreamer.swt;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.swt.widgets.Composite;
import ossbuild.StringUtil;
import ossbuild.media.IMediaRequest;
import ossbuild.media.MediaRequest;
import ossbuild.media.MediaRequestType;
import ossbuild.media.Scheme;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class GstMediaComponent extends MediaComponentV4 {
	//<editor-fold defaultstate="collapsed" desc="Initialization">
	public GstMediaComponent(Composite parent, int style) {
		this(DEFAULT_VIDEO_ELEMENT, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public GstMediaComponent(final String videoElement, Composite parent, int style) {
		this(videoElement, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public GstMediaComponent(final String videoElement, final String audioElement, Composite parent, int style) {
		super(parent, style);
		init(videoElement, audioElement);
	}

	@Override
	protected void componentInitialize() {
	}

	@Override
	protected Runnable createPositionUpdater() {
		return positionUpdateRunnable;
	}

	private void init(String videoElement, String audioElement) {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Play">
	@Override
	public boolean play(int RepeatCount, String URI) {
		return play(MediaRequestType.Video, URI, false, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, RepeatCount, IMediaRequest.DEFAULT_FPS, Scheme.fromPrefix(URI), URI);
	}

	@Override
	public boolean play(boolean LiveSource, String URI) {
		return play(LiveSource ? MediaRequestType.Live : MediaRequestType.Video, URI, LiveSource, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, Scheme.fromPrefix(URI), URI);
	}

	@Override
	public boolean play(int RepeatCount, Scheme Scheme, String URI) {
		return play(MediaRequestType.Video, URI, false, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, RepeatCount, IMediaRequest.DEFAULT_FPS, Scheme, URI);
	}

	@Override
	public boolean play(String Title, int RepeatCount, Scheme Scheme, String URI) {
		return play(MediaRequestType.Video, Title, false, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, RepeatCount, IMediaRequest.DEFAULT_FPS, Scheme, URI);
	}

	@Override
	public boolean play(final String Title, final boolean LiveSource, final int RepeatCount, final Scheme Scheme, final String URI) {
		return play(LiveSource ? MediaRequestType.Live : MediaRequestType.Video, Title, LiveSource, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, RepeatCount, IMediaRequest.DEFAULT_FPS, Scheme, URI);
	}

	@Override
	public boolean play(final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final Scheme Scheme, final String URI) {
		return play(LiveSource ? MediaRequestType.Live : MediaRequestType.Video, Title, LiveSource, MaintainAspectRatio, RepeatCount, IMediaRequest.DEFAULT_FPS, Scheme, URI);
	}

	@Override
	public boolean play(File file) {
		if (file == null)
			return false;
		return play(false, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, file.toURI());
	}

	@Override
	public boolean play(String uri) {
		return play(false, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, uri);
	}

	public boolean play(final MediaRequestType RequestType, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, Scheme Scheme, final String URI) {
		if (StringUtil.isNullOrEmpty(URI))
			return false;
		try {
			return play(RequestType, Title, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme, new URI(URI));
		} catch(URISyntaxException e) {
			return false;
		}
	}

	public boolean play(final URI uri) {
		return play(false, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, uri);
	}

	public boolean play(final int RepeatCount, final URI uri) {
		return play(false, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, RepeatCount, IMediaRequest.DEFAULT_FPS, uri);
	}

	public boolean play(final int RepeatCount, final float FPS, final URI uri) {
		return play(false, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, RepeatCount, FPS, uri);
	}

	public boolean play(final boolean LiveSource, final int RepeatCount, final float FPS, final String URI) {
		return play(LiveSource ? MediaRequestType.Live : MediaRequestType.Video, StringUtil.empty, LiveSource, IMediaRequest.DEFAULT_MAINTAIN_ASPECT_RATIO, RepeatCount, FPS, Scheme.fromPrefix(URI), URI);
	}

	public boolean play(final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, final URI URI) {
		return play(LiveSource ? MediaRequestType.Live : MediaRequestType.Video, StringUtil.empty, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme.fromPrefix(URI.getScheme()), URI);
	}

	public boolean play(final MediaRequestType RequestType, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, final URI URI) {
		return play(RequestType, StringUtil.empty, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme.fromPrefix(URI.getScheme()), URI);
	}

	@Override
	public boolean play(final MediaRequestType RequestType, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, final URI URI) {
		return play(RequestType, Title, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme.fromPrefix(URI.getScheme()), URI);
	}

	@Override
	public boolean play(final MediaRequestType RequestType, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, Scheme Scheme, final URI URI) {
		return play(new MediaRequest(RequestType, Title, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme, URI));
	}

	@Override
	public boolean play(final MediaRequestType RequestType, final long LastModifiedTime, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, final Scheme Scheme, final URI URI) {
		return play(new MediaRequest(RequestType, LastModifiedTime, Title, LiveSource, MaintainAspectRatio, RepeatCount, FPS, Scheme, URI));
	}
	//</editor-fold>
}


package ossbuild.media.gstreamer.swt;

import java.awt.image.BufferedImage;
import ossbuild.media.IMediaRequest;
import ossbuild.media.MediaType;
import ossbuild.media.Scheme;
import java.util.concurrent.TimeUnit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import ossbuild.Sys;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class MediaComponent extends SWTMediaComponent {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final Scheme[] VALID_SCHEMES = new Scheme[] {
		  Scheme.HTTP
		, Scheme.HTTPS
		, Scheme.File
		, Scheme.RTP
		, Scheme.RTSP
		, Scheme.TCP
		, Scheme.UDP
	};

	public static final String
		  DEFAULT_VIDEO_ELEMENT
		, DEFAULT_AUDIO_ELEMENT
	;

	public static final double
		  DEFAULT_RATE = 1.0D
	;

	public static final int
		  SEEK_FLAG_SKIP = (1 << 4)
	;

	private static final long
		  SEEK_STOP_DURATION = TimeUnit.MILLISECONDS.toNanos(10L)
	;

	public static final String[] VALID_COLORSPACES = {
		  "video/x-raw-rgb"
		, "video/x-raw-yuv"
	};

	public static final String[] VALID_YUV_FORMATS = {
		  "YUY2"
	};

	public static final String[] VALID_DIRECTDRAW_COLORSPACES = {
		  "video/x-raw-rgb"
	};
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		String videoElement;
		String audioElement;
		switch (Sys.getOSFamily()) {
			case Windows:
				//videoElement = "dshowvideosink";
				videoElement = "directdrawsink";
				audioElement = "autoaudiosink";
				break;
			case Unix:
				videoElement = "xvimagesink"; //gconfaudiosink and gconfvideosink?
				audioElement = "autoaudiosink";
				break;
			default:
				videoElement = "xvimagesink";
				audioElement = "autoaudiosink";
				break;
		}
		DEFAULT_VIDEO_ELEMENT = videoElement;
		DEFAULT_AUDIO_ELEMENT = audioElement;
	}

	public MediaComponent(Composite parent, int style) {
		this(DEFAULT_VIDEO_ELEMENT, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponent(String videoElement, Composite parent, int style) {
		this(videoElement, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponent(String videoElement, String audioElement, Composite parent, int style) {
		super(parent, style | SWT.EMBEDDED | SWT.DOUBLE_BUFFERED);

		init();
	}

	protected void init() {
	}
	//</editor-fold>

	@Override
	protected void componentInitialize() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected Runnable createPositionUpdater() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Object getMediaLock() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Scheme[] getValidSchemes() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public MediaType getMediaType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getVideoWidth() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getVideoHeight() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public float getVideoFPS() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isMediaAvailable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isVideoAvailable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAudioAvailable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isSeekable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isPaused() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isStopped() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isPlaying() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getRepeatCount() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getPosition() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getDuration() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isMuted() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getVolume() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getBufferSize() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAspectRatioMaintained() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IMediaRequest getMediaRequest() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setBufferSize(long size) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public BufferedImage snapshot() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean mute() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean adjustVolume(int percent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean seekToBeginning() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean adjustPlaybackRate(double rate) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean stop() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean pause() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean unpause() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean stepForward() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean stepBackward() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playBlackBurst() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playBlackBurst(String Title) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playTestSignal() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playTestSignal(String Title) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(IMediaRequest Request) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}

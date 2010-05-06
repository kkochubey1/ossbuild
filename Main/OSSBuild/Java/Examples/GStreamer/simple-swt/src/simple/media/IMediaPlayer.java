
package simple.media;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import simple.media.events.IAudioListener;
import simple.media.events.IMediaEventListener;
import simple.media.events.IPositionListener;
import simple.media.events.IVideoCapsListener;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public interface IMediaPlayer {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final long
		  DEFAULT_BUFFER_SIZE = 4096
	;
	//</editor-fold>
	
	Object getMediaLock();
	Scheme[] getValidSchemes();

	boolean validateURI(final Scheme Scheme, final String URI);
	Scheme schemeFromURI(final String URI);

	//<editor-fold defaultstate="collapsed" desc="Listeners">
	//<editor-fold defaultstate="collapsed" desc="VideoCaps">
	boolean addVideoCapsListener(final IVideoCapsListener Listener);
	boolean removeVideoCapsListener(final IVideoCapsListener Listener);
	boolean containsVideoCapsListener(final IVideoCapsListener Listener);
	boolean clearVideoCapsListeners();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="MediaEvent">
	boolean addMediaEventListener(final IMediaEventListener Listener);
	boolean removeMediaEventListener(final IMediaEventListener Listener);
	boolean containsMediaEventListener(final IMediaEventListener Listener);
	boolean clearMediaEventListeners();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Position">
	boolean addPositionListener(final IPositionListener Listener);
	boolean removePositionListener(final IPositionListener Listener);
	boolean containsPositionListener(final IPositionListener Listener);
	boolean clearPositionListeners();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Audio">
	boolean addAudioListener(final IAudioListener Listener);
	boolean removeAudioListener(final IAudioListener Listener);
	boolean containsAudioListener(final IAudioListener Listener);
	boolean clearAudioListeners();
	//</editor-fold>
	//</editor-fold>

	MediaType getMediaType();
	int getVideoWidth();
	int getVideoHeight();
	float getVideoFPS();
	boolean isMediaAvailable();
	boolean isVideoAvailable();
	boolean isAudioAvailable();
	boolean isSeekable();
	boolean isPaused();
	boolean isStopped();
	boolean isPlaying();
	int getRepeatCount();
	long getPosition();
	long getDuration();
	boolean isMuted();
	int getVolume();
	long getBufferSize();
	boolean isAspectRatioMaintained();

	IMediaRequest getMediaRequest();

	void setBufferSize(long size);

	BufferedImage snapshot();

	boolean mute();
	boolean adjustVolume(int percent);

	boolean seekToBeginning();
	boolean adjustPlaybackRate(final double rate);

	boolean stop();
	boolean pause();
	boolean unpause();
	boolean stepForward();
	boolean stepBackward();
	boolean playBlackBurst();
	boolean playBlackBurst(final String Title);
	boolean playTestSignal();
	boolean playTestSignal(final String Title);
	boolean play(File file);
	boolean play(final String URI);
	boolean play(final boolean LiveSource, final String URI);
	boolean play(final int RepeatCount, final String URI);
	boolean play(final int RepeatCount, final Scheme Scheme, final String URI);
	boolean play(final String Title, final int RepeatCount, final Scheme Scheme, final String URI);
	boolean play(final String Title, final boolean LiveSource, final int RepeatCount, final Scheme Scheme, final String URI);
	boolean play(final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final Scheme Scheme, final String URI);
	boolean play(final MediaRequestType RequestType, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, final URI URI);
	boolean play(final MediaRequestType RequestType, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, final Scheme Scheme, final URI URI);
	boolean play(final MediaRequestType RequestType, final long LastModifiedTime, final String Title, final boolean LiveSource, final boolean MaintainAspectRatio, final int RepeatCount, final float FPS, final Scheme Scheme, final URI URI);
	boolean play(final IMediaRequest Request);
}

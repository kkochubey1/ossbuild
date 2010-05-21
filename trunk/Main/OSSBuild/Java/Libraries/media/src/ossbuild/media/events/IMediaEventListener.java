package ossbuild.media.events;

import ossbuild.media.IMediaPlayer;
import ossbuild.media.IMediaRequest;

public interface IMediaEventListener {
	void mediaPaused(final IMediaPlayer source);
	void mediaContinued(final IMediaPlayer source);
	void mediaStopped(final IMediaPlayer source);
	void mediaPlayRequested(final IMediaPlayer source, final IMediaRequest request);
	void mediaPlayed(final IMediaPlayer source);
}

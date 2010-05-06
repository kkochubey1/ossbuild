package simple.media.events;

import simple.media.IMediaPlayer;

public interface IMediaEventListener {
	void mediaPaused(final IMediaPlayer source);
	void mediaContinued(final IMediaPlayer source);
	void mediaStopped(final IMediaPlayer source);
	void mediaPlayRequested(final IMediaPlayer source);
	void mediaPlayed(final IMediaPlayer source);
}

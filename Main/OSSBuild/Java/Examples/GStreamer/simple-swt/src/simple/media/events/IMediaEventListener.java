package simple.media.events;

import simple.media.IMediaPlayer;
import simple.media.IMediaRequest;

public interface IMediaEventListener {
	void mediaPaused(final IMediaPlayer source);
	void mediaContinued(final IMediaPlayer source);
	void mediaStopped(final IMediaPlayer source);
	void mediaPlayRequested(final IMediaPlayer source, final IMediaRequest request);
	void mediaPlayed(final IMediaPlayer source);
}

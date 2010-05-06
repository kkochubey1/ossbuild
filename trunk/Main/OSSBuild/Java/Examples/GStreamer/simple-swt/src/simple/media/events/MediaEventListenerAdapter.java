package simple.media.events;

import simple.media.IMediaPlayer;
import simple.media.IMediaRequest;

public abstract class MediaEventListenerAdapter implements IMediaEventListener {
	@Override
	public void mediaPaused(IMediaPlayer source) {
	}

	@Override
	public void mediaContinued(IMediaPlayer source) {
	}

	@Override
	public void mediaStopped(IMediaPlayer source) {
	}

	@Override
	public void mediaPlayRequested(IMediaPlayer source, IMediaRequest request) {
	}

	@Override
	public void mediaPlayed(IMediaPlayer source) {
	}
}

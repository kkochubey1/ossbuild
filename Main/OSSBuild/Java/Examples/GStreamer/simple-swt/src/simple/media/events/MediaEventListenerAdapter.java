package simple.media.events;

import simple.media.IMediaPlayer;

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
	public void mediaPlayRequested(IMediaPlayer source) {
	}

	@Override
	public void mediaPlayed(IMediaPlayer source) {
	}
}

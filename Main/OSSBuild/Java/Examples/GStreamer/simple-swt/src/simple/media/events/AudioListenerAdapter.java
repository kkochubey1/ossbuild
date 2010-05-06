package simple.media.events;

import simple.media.IMediaPlayer;

public abstract class AudioListenerAdapter implements IAudioListener {
	@Override
	public void audioMuted(IMediaPlayer source) {
	}

	@Override
	public void audioUnmuted(IMediaPlayer source) {
	}

	@Override
	public void audioVolumeChanged(IMediaPlayer source, int percent) {
	}
}

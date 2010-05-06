package simple.media.events;

import simple.media.IMediaPlayer;

public interface IAudioListener {
	void audioMuted(final IMediaPlayer source);
	void audioUnmuted(final IMediaPlayer source);
	void audioVolumeChanged(final IMediaPlayer source, final int percent);
}

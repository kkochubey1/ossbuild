package ossbuild.media.events;

import ossbuild.media.IMediaPlayer;

public interface IAudioListener {
	void audioMuted(final IMediaPlayer source);
	void audioUnmuted(final IMediaPlayer source);
	void audioVolumeChanged(final IMediaPlayer source, final int percent);
}

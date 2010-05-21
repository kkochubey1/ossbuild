package ossbuild.media.events;

import ossbuild.media.IMediaPlayer;

public interface IPositionListener {
	void positionChanged(final IMediaPlayer source, final int percent, final long position, final long duration);
}

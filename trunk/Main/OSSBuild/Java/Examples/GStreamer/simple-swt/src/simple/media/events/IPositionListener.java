package simple.media.events;

import simple.media.IMediaPlayer;

public interface IPositionListener {
	void positionChanged(final IMediaPlayer source, final int percent, final long position, final long duration);
}

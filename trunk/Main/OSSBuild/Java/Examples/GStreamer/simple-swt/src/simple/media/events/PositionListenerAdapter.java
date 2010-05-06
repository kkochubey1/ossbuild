package simple.media.events;

import simple.media.IMediaPlayer;

public abstract class PositionListenerAdapter implements IPositionListener {
	@Override
	public void positionChanged(IMediaPlayer source, final int percent, long position, long duration) {
	}
}

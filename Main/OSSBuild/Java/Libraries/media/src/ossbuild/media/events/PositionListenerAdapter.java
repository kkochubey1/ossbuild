package ossbuild.media.events;

import ossbuild.media.IMediaPlayer;

public abstract class PositionListenerAdapter implements IPositionListener {
	@Override
	public void positionChanged(IMediaPlayer source, final int percent, long position, long duration) {
	}
}

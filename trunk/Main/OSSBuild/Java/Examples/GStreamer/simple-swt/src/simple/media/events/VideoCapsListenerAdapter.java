package simple.media.events;

import simple.media.IMediaPlayer;

public abstract class VideoCapsListenerAdapter implements IVideoCapsListener {
	@Override
	public void videoDimensionsNegotiated(final IMediaPlayer source, int videoWidth, int videoHeight) {
	}
}

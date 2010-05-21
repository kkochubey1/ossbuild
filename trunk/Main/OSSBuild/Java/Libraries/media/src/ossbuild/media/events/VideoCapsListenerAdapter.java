package ossbuild.media.events;

import ossbuild.media.IMediaPlayer;

public abstract class VideoCapsListenerAdapter implements IVideoCapsListener {
	@Override
	public void videoDimensionsNegotiated(final IMediaPlayer source, int videoWidth, int videoHeight) {
	}
}

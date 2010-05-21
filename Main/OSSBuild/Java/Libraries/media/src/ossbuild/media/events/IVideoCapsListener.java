package ossbuild.media.events;

import ossbuild.media.IMediaPlayer;

public interface IVideoCapsListener {
	void videoDimensionsNegotiated(final IMediaPlayer source, final int videoWidth, final int videoHeight);
}

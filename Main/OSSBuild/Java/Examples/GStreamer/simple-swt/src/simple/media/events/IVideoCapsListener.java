package simple.media.events;

import simple.media.IMediaPlayer;

public interface IVideoCapsListener {
	void videoDimensionsNegotiated(final IMediaPlayer source, final int videoWidth, final int videoHeight);
}

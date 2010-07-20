
package ossbuild.media.gstreamer.events;

import ossbuild.media.IMediaPlayer;
import ossbuild.media.IMediaRequest;
import ossbuild.media.gstreamer.ErrorType;

public interface IErrorListener {
	void handleError(final IMediaPlayer source, final IMediaRequest request, final ErrorType errorType, final int code, final String message);
}

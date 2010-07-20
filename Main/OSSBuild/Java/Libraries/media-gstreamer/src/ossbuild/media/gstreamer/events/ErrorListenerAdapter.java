
package ossbuild.media.gstreamer.events;

import ossbuild.media.IMediaPlayer;
import ossbuild.media.IMediaRequest;
import ossbuild.media.gstreamer.ErrorType;

public abstract class ErrorListenerAdapter implements IErrorListener {
	@Override
	public void handleError(IMediaPlayer source, IMediaRequest request, ErrorType errorType, int code, String message) {
	}
}

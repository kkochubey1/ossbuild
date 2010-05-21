
package ossbuild.media.gstreamer.swt;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import ossbuild.media.IMediaPlayer;
import ossbuild.media.IMediaRequest;
import ossbuild.media.Scheme;
import ossbuild.media.events.IAudioListener;
import ossbuild.media.events.IMediaEventListener;
import ossbuild.media.events.IPositionListener;
import ossbuild.media.events.IVideoCapsListener;

/**
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class SWTMediaComponent extends Canvas implements IMediaPlayer {
	//<editor-fold defaultstate="collapsed" desc="Variables">
	public static final ScheduledExecutorService
		TASK_EXECUTOR
	;

	protected volatile ScheduledFuture<?> positionTimer = null;
	protected Runnable positionUpdateRunnable;

	protected IMediaPlayer mediaPlayer;

	private List<IMediaEventListener> mediaEventListeners;
	private final Object mediaEventListenerLock = new Object();

	private List<IVideoCapsListener> videoCapsListeners;
	private final Object videoCapsListenerLock = new Object();

	private List<IPositionListener> positionListeners;
	private final Object positionListenerLock = new Object();

	private List<IAudioListener> audioListeners;
	private final Object audioListenerLock = new Object();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		TASK_EXECUTOR = Executors.newScheduledThreadPool(Math.min(2, Math.max(6, Runtime.getRuntime().availableProcessors())), new ThreadFactory() {
			private final AtomicInteger counter = new AtomicInteger(0);
			@Override
			public Thread newThread(final Runnable target) {
				final int count = counter.incrementAndGet();
				final Thread t = new Thread(Thread.currentThread().getThreadGroup(), target, "gstreamer media executor " + count);
				t.setDaemon(true);
				return t;
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if (TASK_EXECUTOR != null) {
					try {
						TASK_EXECUTOR.shutdown();
						TASK_EXECUTOR.awaitTermination(5000L, TimeUnit.MILLISECONDS);
					} catch(Throwable t) {
					} finally {
						TASK_EXECUTOR.shutdownNow();
					}
				}
			}
		}));
	}

	public SWTMediaComponent(final Composite parent, final int style) {
		super(parent, style | SWT.EMBEDDED);
		init();
	}

	private void init() {
		positionUpdateRunnable = createPositionUpdater();
		componentInitialize();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	protected boolean isUIThread() {
		return (Thread.currentThread() == getDisplay().getThread());
	}
	
	public IMediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	public static boolean execute(final Runnable task) {
		if (TASK_EXECUTOR == null)
			return false;
		TASK_EXECUTOR.submit(task);
		return true;
	}

	protected boolean ui(final Runnable run) {
		if (run == null)
			return false;
		if (getDisplay().isDisposed())
			return false;

		if (isUIThread())
			run.run();
		else
			getDisplay().syncExec(run);
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Listeners">
	//<editor-fold defaultstate="collapsed" desc="VideoCaps">
	public boolean addVideoCapsListener(final IVideoCapsListener Listener) {
		if (Listener == null)
			return false;
		synchronized(videoCapsListenerLock) {
			if (videoCapsListeners == null)
				videoCapsListeners = new CopyOnWriteArrayList<IVideoCapsListener>();
			return videoCapsListeners.add(Listener);
		}
	}

	public boolean removeVideoCapsListener(final IVideoCapsListener Listener) {
		if (Listener == null)
			return false;
		synchronized(videoCapsListenerLock) {
			if (videoCapsListeners == null || videoCapsListeners.isEmpty())
				return true;
			return videoCapsListeners.remove(Listener);
		}
	}

	public boolean containsVideoCapsListener(final IVideoCapsListener Listener) {
		if (Listener == null)
			return false;
		synchronized(videoCapsListenerLock) {
			if (videoCapsListeners == null || videoCapsListeners.isEmpty())
				return true;
			return videoCapsListeners.contains(Listener);
		}
	}

	public boolean clearVideoCapsListeners() {
		synchronized(videoCapsListenerLock) {
			if (videoCapsListeners == null || videoCapsListeners.isEmpty())
				return true;
			videoCapsListeners.clear();
			return true;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="MediaEvent">
	public boolean addMediaEventListener(final IMediaEventListener Listener) {
		if (Listener == null)
			return false;
		synchronized(mediaEventListenerLock) {
			if (mediaEventListeners == null)
				mediaEventListeners = new CopyOnWriteArrayList<IMediaEventListener>();
			return mediaEventListeners.add(Listener);
		}
	}

	public boolean removeMediaEventListener(final IMediaEventListener Listener) {
		if (Listener == null)
			return false;
		synchronized(mediaEventListenerLock) {
			if (mediaEventListeners == null || mediaEventListeners.isEmpty())
				return true;
			return mediaEventListeners.remove(Listener);
		}
	}

	public boolean containsMediaEventListener(final IMediaEventListener Listener) {
		if (Listener == null)
			return false;
		synchronized(mediaEventListenerLock) {
			if (mediaEventListeners == null || mediaEventListeners.isEmpty())
				return true;
			return mediaEventListeners.contains(Listener);
		}
	}

	public boolean clearMediaEventListeners() {
		synchronized(mediaEventListenerLock) {
			if (mediaEventListeners == null || mediaEventListeners.isEmpty())
				return true;
			mediaEventListeners.clear();
			return true;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Position">
	public boolean addPositionListener(final IPositionListener Listener) {
		if (Listener == null)
			return false;
		synchronized(positionListenerLock) {
			if (positionListeners == null)
				positionListeners = new CopyOnWriteArrayList<IPositionListener>();
			boolean startTimer = positionListeners.isEmpty();
			boolean ret = positionListeners.add(Listener);

			if (ret && startTimer)
				positionTimer = TASK_EXECUTOR.scheduleAtFixedRate(positionUpdateRunnable, 1, 1, TimeUnit.SECONDS);
			return ret;
		}
	}

	public boolean removePositionListener(final IPositionListener Listener) {
		if (Listener == null)
			return false;
		synchronized(positionListenerLock) {
			if (positionListeners == null || positionListeners.isEmpty())
				return true;

			boolean ret = positionListeners.remove(Listener);
			if (ret && positionTimer != null) {
				positionTimer.cancel(true);
				positionTimer = null;
			}
			return ret;
		}
	}

	public boolean containsPositionListener(final IPositionListener Listener) {
		if (Listener == null)
			return false;
		synchronized(positionListenerLock) {
			if (positionListeners == null || positionListeners.isEmpty())
				return true;
			return positionListeners.contains(Listener);
		}
	}

	public boolean clearPositionListeners() {
		synchronized(positionListenerLock) {
			if (positionListeners == null || positionListeners.isEmpty())
				return true;
			positionListeners.clear();
			if (positionTimer != null) {
				positionTimer.cancel(true);
				positionTimer = null;
			}
			return true;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Audio">
	public boolean addAudioListener(final IAudioListener Listener) {
		if (Listener == null)
			return false;
		synchronized(audioListenerLock) {
			if (audioListeners == null)
				audioListeners = new CopyOnWriteArrayList<IAudioListener>();
			return audioListeners.add(Listener);
		}
	}

	public boolean removeAudioListener(final IAudioListener Listener) {
		if (Listener == null)
			return false;
		synchronized(audioListenerLock) {
			if (audioListeners == null || audioListeners.isEmpty())
				return true;

			return audioListeners.remove(Listener);
		}
	}

	public boolean containsAudioListener(final IAudioListener Listener) {
		if (Listener == null)
			return false;
		synchronized(audioListenerLock) {
			if (audioListeners == null || audioListeners.isEmpty())
				return true;
			return audioListeners.contains(Listener);
		}
	}

	@Override
	public boolean clearAudioListeners() {
		synchronized(audioListenerLock) {
			if (audioListeners == null || audioListeners.isEmpty())
				return true;
			audioListeners.clear();
			return true;
		}
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Events">
	//<editor-fold defaultstate="collapsed" desc="VideoCaps">
	protected void fireVideoDimensionsNegotiated(final int videoWidth, final int videoHeight) {
		if (videoCapsListeners == null || videoCapsListeners.isEmpty())
			return;
		for(IVideoCapsListener listener : videoCapsListeners)
			listener.videoDimensionsNegotiated(this, videoWidth, videoHeight);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="MediaEvent">
	protected void fireMediaEventPaused() {
		if (mediaEventListeners == null || mediaEventListeners.isEmpty())
			return;
		for(IMediaEventListener listener : mediaEventListeners)
			listener.mediaPaused(this);
	}

	protected void fireMediaEventContinued() {
		if (mediaEventListeners == null || mediaEventListeners.isEmpty())
			return;
		for(IMediaEventListener listener : mediaEventListeners)
			listener.mediaContinued(this);
	}

	protected void fireMediaEventStopped() {
		if (mediaEventListeners == null || mediaEventListeners.isEmpty())
			return;
		for(IMediaEventListener listener : mediaEventListeners)
			listener.mediaStopped(this);
	}

	protected void fireMediaEventPlayRequested(final IMediaRequest request) {
		if (mediaEventListeners == null || mediaEventListeners.isEmpty())
			return;
		for(IMediaEventListener listener : mediaEventListeners)
			listener.mediaPlayRequested(this, request);
	}

	protected void fireMediaEventPlayed() {
		if (mediaEventListeners == null || mediaEventListeners.isEmpty())
			return;
		for(IMediaEventListener listener : mediaEventListeners)
			listener.mediaPlayed(this);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Position">
	protected void firePositionChanged(final int percent, final long position, final long duration) {
		if (positionListeners == null || positionListeners.isEmpty())
			return;
		for(IPositionListener listener : positionListeners)
			listener.positionChanged(this, percent, position, duration);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Audio">
	protected void fireAudioMuted() {
		if (audioListeners == null || audioListeners.isEmpty())
			return;
		for(IAudioListener listener : audioListeners)
			listener.audioMuted(this);
	}

	protected void fireAudioUnmuted() {
		if (audioListeners == null || audioListeners.isEmpty())
			return;
		for(IAudioListener listener : audioListeners)
			listener.audioUnmuted(this);
	}

	protected void fireAudioVolumeChanged(int percent) {
		if (audioListeners == null || audioListeners.isEmpty())
			return;
		for(IAudioListener listener : audioListeners)
			listener.audioVolumeChanged(this, percent);
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	public Scheme schemeFromURI(String URI) {
		return Scheme.fromPrefix(URI);
	}

	public boolean validateURI(Scheme Scheme, String URI) {
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Abstract Methods">
	protected abstract void componentInitialize();
	protected abstract Runnable createPositionUpdater();
	//</editor-fold>
}

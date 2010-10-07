
package ossbuild.media.gstreamer.swt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gstreamer.Buffer;
import org.gstreamer.swt.overlay.SWTOverlay;
import ossbuild.OSFamily;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.media.IMediaRequest;
import ossbuild.media.MediaRequest;
import ossbuild.media.MediaRequestType;
import ossbuild.media.MediaType;
import ossbuild.media.Scheme;
import ossbuild.media.gstreamer.Colorspace;
import ossbuild.media.gstreamer.ErrorType;
import ossbuild.media.gstreamer.VideoTestSrcPattern;
import ossbuild.media.gstreamer.events.IErrorListener;
import ossbuild.media.gstreamer.swt.GstPlayer.Event;
import ossbuild.media.gstreamer.swt.GstPlayer.Response;
import ossbuild.media.swt.MediaComponent;

/**
 *
 * @author David Hoyt <dhoyt@llnl.gov>
 */
public abstract class MediaComponentV5 extends MediaComponent {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final Lock
		  GST_LOCK = new ReentrantLock()
	;
	
	public static final long 
		  LOCK_ATTEMPT_TIMEOUT = 1L
	;

	public static final int
		  MAX_LOCK_ATTEMPTS = 5000
	;

	public static final Scheme[] VALID_SCHEMES = new Scheme[] {
		  Scheme.HTTP
		, Scheme.HTTPS
		, Scheme.File
		, Scheme.RTP
		, Scheme.RTSP
		, Scheme.TCP
		, Scheme.UDP
	};

	public static final String
		  DEFAULT_VIDEO_ELEMENT
		, DEFAULT_AUDIO_ELEMENT
	;

	public static final double
		  DEFAULT_RATE = 1.0D
	;

	public static final boolean
		  DEFAULT_VIDEO_ACCELERATED = true
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected GstPlayer player;

	protected final long nativeHandle;
	protected final ReentrantLock lock = new ReentrantLock();
	protected String videoElement;
	protected String audioElement;

	protected GstPlayer.IErrorListener errorListener;
	protected GstPlayer.IEventListener eventListener;
	protected GstPlayer.IResponseListener responseListener;

	protected SWTOverlay xoverlay = null;

	private boolean hasMultipartDemux = false;
	private int videoWidth = 0;
	private int videoHeight = 0;
	private float actualFPS;
	private boolean emitPositionUpdates = true;
	private boolean currentLiveSource;
	private int currentRepeatCount;
	private int numberOfRepeats;
	private boolean maintainAspectRatio = true;
	protected double currentRate = DEFAULT_RATE;
	private long bufferSize = DEFAULT_BUFFER_SIZE;
	private long lastPosition = 0L;
	private long lastDuration = 0L;
	protected MediaType mediaType = MediaType.Unknown;
	protected ImageData singleImage = null;
	protected int volume = 100;
	protected boolean muted = false;
	protected IMediaRequest mediaRequest = null;

	private Canvas acceleratedVideoCanvas;
	private StackLayout layout;
	protected final Display display;
	private ImageData currentFrame = null;

	protected boolean acceleratedVideo = DEFAULT_VIDEO_ACCELERATED;

	private List<IErrorListener> errorListeners;
	private final Object errorListenerLock = new Object();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		//Determine the default video/audio sinks for each platform
		String audioElement;
		String videoElement;
		switch (Sys.getOSFamily()) {
			case Windows:
				audioElement = "autoaudiosink";
				//videoElement = "autovideosink";
				//videoElement = "glimagesink";
				//videoElement = "directdrawsink";
				//videoElement = "dshowvideosink";
				videoElement = "d3dvideosink";
				break;
			case Unix:
				//audioElement = "autoaudiosink";
				//audioElement = "gconfaudiosink";
				audioElement = "alsasink";
				//videoElement = "autovideosink";
				//videoElement = "glimagesink";
				//videoElement = "xvimagesink"; //gconfaudiosink and gconfvideosink?
				videoElement = "ximagesink";
				break;
			default:
				audioElement = "autoaudiosink";
				videoElement = "ximagesink";
				break;
		}
		DEFAULT_AUDIO_ELEMENT = audioElement;
		DEFAULT_VIDEO_ELEMENT = videoElement;
	}

	public MediaComponentV5(Composite parent, int style) {
		this(DEFAULT_VIDEO_ELEMENT, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponentV5(String videoElement, Composite parent, int style) {
		this(videoElement, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	@SuppressWarnings("OverridableMethodCallInConstructor")
	public MediaComponentV5(String videoElement, String audioElement, Composite parent, int style) {
		super(parent, style | swtDoubleBufferStyle());

		this.acceleratedVideoCanvas = new Canvas(this, SWT.EMBEDDED | SWT.NO_FOCUS | swtDoubleBufferStyle());
		this.nativeHandle = SWTOverlay.handle(acceleratedVideoCanvas);
		this.display = getDisplay();

		this.audioElement = (!StringUtil.isNullOrEmpty(audioElement) ? audioElement : DEFAULT_AUDIO_ELEMENT);
		this.videoElement = (!StringUtil.isNullOrEmpty(videoElement) && !VIDEO_SINK_UNACCELERATED.equalsIgnoreCase(videoElement) ? videoElement : DEFAULT_VIDEO_ELEMENT);
		this.acceleratedVideo = (!VIDEO_SINK_UNACCELERATED.equalsIgnoreCase(videoElement));

		this.positionUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				if (!lock.tryLock())
					return;
				try {
					if (player == null)
						return;
					player.queryPosition();
				} finally {
					lock.unlock();
				}
			}
		};

		this.errorListener = new GstPlayer.IErrorListener() {
			@Override
			public void handleError(final GstPlayer source, final IMediaRequest request, final int code, final String message) {
				fireHandleError(request, ErrorType.fromNativeValue(code), code, message);
			}
		};

		this.eventListener = new GstPlayer.IEventListener() {
			@Override
			public void handleEvent(GstPlayer source, IMediaRequest request, Event event, Object[] args) {
				if (args == null || args.length <= 0)
					return;

				switch(event) {
					case WindowSize:
						if (args.length >= 2)
							fireVideoDimensionsNegotiated((Integer)args[0], (Integer)args[1]);
						break;
					case Playing:
						fireMediaEventPlayed();
						fireMediaEventContinued();
						break;
					case Paused:
						fireMediaEventPaused();
						break;
					case Stopped:
						fireMediaEventStopped();
						stop();
						break;
					default:
						break;
				}
			}
		};

		this.responseListener = new GstPlayer.IResponseListener() {
			@Override
			public void handleResponse(GstPlayer source, IMediaRequest request, Response event, Object[] args) {
				switch(event) {
					case QueryPosition:
						onPositionUpdate();
						break;
				}
			}
		};

		//<editor-fold defaultstate="collapsed" desc="SWT">
		//this.setLayout(new FillLayout());
		acceleratedVideoCanvas.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		this.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		this.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent ce) {
				expose();
			}
		});
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent de) {
				//Make sure that any calls to perform operations on this
				//component are ignored by locking it. Other methods will
				//use lock.tryLock() which should now fail.
				lock.lock();
				Dispose();
			}
		});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Layout">
		//Setup SWT layout
		this.setLayout((layout = new StackLayout()));
		layout.marginHeight = layout.marginWidth = 0;
		layout.topControl = null;
		//</editor-fold>

		showNone();
		init();
	}

	protected void init() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	public void Dispose() {

		if (player == null)
			return;
		player.destroy();
		player.Dispose();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	public boolean isVideoAccelerated() {
		return true;
	}

	public void setVideoAccelerated(boolean value) {
		//Do nothing
	}

	public String getDefaultVideoSink() {
		return videoElement;
	}

	public void setDefaultVideoSink(String value) {
		this.videoElement = (!StringUtil.isNullOrEmpty(value) ? value : DEFAULT_VIDEO_ELEMENT);
	}

	public String getDefaultAudioSink() {
		return audioElement;
	}

	public void setDefaultAudioSink(String value) {
		this.audioElement = (!StringUtil.isNullOrEmpty(value) ? value : DEFAULT_AUDIO_ELEMENT);
	}

	@Override
	public Lock getMediaLock() {
		return lock;
	}

	@Override
	public Scheme[] getValidSchemes() {
		return VALID_SCHEMES;
	}

	@Override
	public int getVideoWidth() {
		if (!lock.tryLock())
			return 0;
		try {
			if (player == null)
				return 0;
			return player.getVideoWidth();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int getVideoHeight() {
		if (!lock.tryLock())
			return 0;
		try {
			if (player == null)
				return 0;
			return player.getVideoHeight();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isMediaAvailable() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isRunning();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isPaused() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isPaused();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isStopped() {
		if (!lock.tryLock())
			return true;
		try {
			if (player == null)
				return true;
			return !player.isPlaying();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isPlaying() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isPlaying();
		} finally {
			lock.unlock();
		}
	}

	public boolean isLiveSource() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isLive();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isSeekable() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isSeekable();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int getRepeatCount() {
		if (!lock.tryLock())
			return 0;
		try {
			if (player == null)
				return 0;
			return player.getRepeatCount();
		} finally {
			lock.unlock();
		}
	}

	public boolean isRepeatingForever() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isRepeatingForever();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public float getVideoFPS() {
		if (!lock.tryLock())
			return 0.0f;
		try {
			if (player == null)
				return 0.0f;
			return player.getVideoFPS();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public long getPosition() {
		if (!lock.tryLock())
			return 0L;
		try {
			//TODO: Fill this in
			return 0L;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public long getDuration() {
		if (!lock.tryLock())
			return 0L;
		try {
			//TODO: Fill this in
			return 0L;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isMuted() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isMuted();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int getVolume() {
		if (!lock.tryLock())
			return 100;
		try {
			if (player == null)
				return 100;
			return player.getVolume();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isAudioAvailable() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isAudioAvailable();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isVideoAvailable() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			return player.isVideoAvailable();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public long getBufferSize() {
		if (!lock.tryLock())
			return 0L;
		try {
			if (player == null)
				return 0L;
			return player.getBufferSize();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public MediaType getMediaType() {
		if (!lock.tryLock())
			return MediaType.Unknown;
		try {
			if (player == null)
				return MediaType.Unknown;
			return player.getCommonMediaType();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isAspectRatioMaintained() {
		return maintainAspectRatio;
	}

	@Override
	public IMediaRequest getMediaRequest() {
		return mediaRequest;
	}

	@Override
	public void setBufferSize(long size) {
		this.bufferSize = size;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	private static int swtDoubleBufferStyle() {
		//Macs force double buffering, so using SWT double buffering on top of that would
		//be triple buffering and an unnecessary performance hit.
		if (!Sys.isOSFamily(OSFamily.Mac))
			return SWT.DOUBLE_BUFFERED;
		else
			return 0;
	}

	protected void showVideoCanvas() {
		//<editor-fold defaultstate="collapsed" desc="Check UI Thread">
		if (!isUIThread()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					showVideoCanvas();
				}
			});
			return;
		}
		//</editor-fold>

		layout.topControl = acceleratedVideoCanvas;
		acceleratedVideoCanvas.setVisible(acceleratedVideo);
		layout();
		acceleratedVideoCanvas.redraw();
		//System.out.println("SHOWING VIDEO: " + (acceleratedVideo ? "accelerated" : "unaccelerated"));
	}

	protected void showNone() {
		//<editor-fold defaultstate="collapsed" desc="Check UI Thread">
		if (!isUIThread()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					showNone();
				}
			});
			return;
		}
		//</editor-fold>

		layout.topControl = null;
		acceleratedVideoCanvas.setVisible(false);
		layout();
		redraw();
		//System.out.println("SHOWING NONE");
	}

	protected boolean acquireLock() {
		try {
			for(int i = 0; i < MAX_LOCK_ATTEMPTS; ++i)
				if (lock.tryLock(LOCK_ATTEMPT_TIMEOUT, TimeUnit.MILLISECONDS))
					return true;
			return false;
		} catch(Throwable t) {
			return false;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Listeners">
	//<editor-fold defaultstate="collapsed" desc="Error">
	public boolean addErrorListener(final IErrorListener Listener) {
		if (Listener == null)
			return false;
		synchronized(errorListenerLock) {
			if (errorListeners == null)
				errorListeners = new CopyOnWriteArrayList<IErrorListener>();
			return errorListeners.add(Listener);
		}
	}

	public boolean removeErrorListener(final IErrorListener Listener) {
		if (Listener == null)
			return false;
		synchronized(errorListenerLock) {
			if (errorListeners == null || errorListeners.isEmpty())
				return true;
			return errorListeners.remove(Listener);
		}
	}

	public boolean containsErrorListener(final IErrorListener Listener) {
		if (Listener == null)
			return false;
		synchronized(errorListenerLock) {
			if (errorListeners == null || errorListeners.isEmpty())
				return true;
			return errorListeners.contains(Listener);
		}
	}

	public boolean clearErrorListeners() {
		synchronized(errorListenerLock) {
			if (errorListeners == null || errorListeners.isEmpty())
				return true;
			errorListeners.clear();
			return true;
		}
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Events">
	//<editor-fold defaultstate="collapsed" desc="Error">
	protected void fireHandleError(final IMediaRequest request, final ErrorType errorType, final int code, final String message) {
		if (errorListeners == null || errorListeners.isEmpty())
			return;
		for(IErrorListener listener : errorListeners)
			listener.handleError(this, request, errorType, code, message);
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	//<editor-fold defaultstate="collapsed" desc="Lock">
	@Override
	public boolean lock() {
		lock.lock();
		return true;
	}

	@Override
	public boolean unlock() {
		lock.unlock();
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Snapshots">
	public boolean saveSnapshot(final File File) {
		if (File == null)
			return false;

		FileOutputStream fos = null;
		try {

			final ImageData data = swtImageDataSnapshot();
			if (data == null)
				return false;

			final ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { data };

			fos = new FileOutputStream(File);
			loader.save(fos, SWT.IMAGE_JPEG);
			return true;
		} catch(Throwable t) {
			return false;
		} finally {
			if (fos != null) {
				try { fos.close(); } catch(IOException ie) { }
			}
		}
	}

	public boolean saveSnapshot(final OutputStream Output) {
		if (Output == null)
			return false;

		final ImageData data = swtImageDataSnapshot();
		if (data == null)
			return false;

		final ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };
		loader.save(Output, SWT.IMAGE_JPEG);
		return true;
	}

	public Image swtSnapshot() {
		final ImageData data = swtImageDataSnapshot();
		if (data == null)
			return null;
		//Caller will be responsible for disposing this image
		return new Image(display, data);
	}

	public ImageData swtImageDataSnapshot() {
		Buffer buffer = null;
		try {
			lock.lock();
			try {
				//TODO: Fill this in
				//buffer = playbinFrame(pipeline);
				if (buffer == null) {
					if (currentFrame != null)
						return currentFrame;
					return null;
				}

				return swtImageDataSnapshot(buffer);
			} finally {
				lock.unlock();
			}
		} catch(Throwable t) {
			return null;
		} finally {
			if (buffer != null)
				buffer.dispose();
		}
	}

	public ImageData swtImageDataSnapshot(Buffer buffer) {
		try {
			//Convert to RGB using the provided direct buffer
			final Colorspace.Frame frame = Colorspace.createRGBFrame(buffer);
			if (frame == null)
				return null;

			final IntBuffer rgb = frame.getBuffer();
			if (rgb == null)
				return null;

			int[] pixels = new int[rgb.remaining()];
			ImageData imageData = new ImageData(frame.getWidth(), frame.getHeight(), 24, new PaletteData(0x000000FF, 0x0000FF00, 0x00FF0000));
			rgb.get(pixels, 0, rgb.remaining());
			imageData.setPixels(0, 0, pixels.length, pixels, 0);

			return imageData;
		} catch(Throwable t) {
			return null;
		} finally {
		}
	}

	@Override
	public BufferedImage snapshot() {
		Buffer buffer = null;
		try {
			if (!lock.tryLock())
				return null;
			try {
				//TODO: Fill this in
				return null;
			} finally {
				lock.unlock();
			}
		} catch(Throwable t) {
			return null;
		} finally {
			if (buffer != null)
				buffer.dispose();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Expose">
	public boolean expose() {
		if (isDisposed())
			return false;

		//<editor-fold defaultstate="collapsed" desc="Check UI Thread">
		if (!isUIThread()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					expose();
				}
			});
			return true;
		}
		//</editor-fold>

		if (mediaType == MediaType.Image && singleImage != null) {
			redraw();
			return true;
		}

		acceleratedVideoCanvas.redraw();
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Volume">
	@Override
	public boolean mute() {
		if (!lock.tryLock())
			return false;
		try {
			boolean shouldMute = !isMuted();
			muted = shouldMute;

			//TODO: Fill this in

			if (!shouldMute)
				adjustVolume(volume);
			if (shouldMute)
				fireAudioMuted();
			else
				fireAudioUnmuted();
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public boolean adjustVolume(int percent) {
		if (!lock.tryLock())
			return false;
		try {
			int oldVolume = getVolume();
			int newVolume = Math.max(0, Math.min(100, percent));

			volume = newVolume;

			//TODO: Fill this in
			
			if (oldVolume != newVolume)
				fireAudioVolumeChanged(newVolume);
		} finally {
			lock.unlock();
		}
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Seek">
	@Override
	public boolean seekToBeginning() {
		if (!lock.tryLock())
			return false;
		try {
			if (isLiveSource())
				return true;
			if (!seek(currentRate, 0L)) {
				return false;
			}
			return true;
		} finally {
			lock.unlock();
		}
	}

	public boolean seekToBeginningAndPause() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			if (isLiveSource())
				return pause();
			
			return player.pause() && player.seek(0L);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean adjustPlaybackRate(final double rate) {
		//TODO: Figure out why playing backwards (rate is negative) isn't working

		if (rate < 0.0f)
			return false;

		if (rate == 0.0f)
			return pause();

		if (!lock.tryLock())
			return false;
		try {
			if (isLiveSource())
				return false;

			//TODO: Fill this in
			currentRate = rate;

			return false;
		} finally {
			lock.unlock();
		}
	}

	public boolean seek(final long positionNanoSeconds) {
		return seek(currentRate, positionNanoSeconds);
	}

	public boolean seek(final double rate, final long positionNanoSeconds) {
		return segmentSeek(rate, positionNanoSeconds);
	}

	private boolean segmentSeek(final double rate, final long positionNanoSeconds) {
		if (rate == 0.0f)
			return pause();

		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return false;
			
			if (isLiveSource())
				return false;

			final boolean success = player.seek(TimeUnit.NANOSECONDS.toMillis(positionNanoSeconds));

			if (success) {
				currentRate = rate;
				onPositionUpdate();
			}

			return success;
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Step">
	@Override
	public boolean stepForward() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return true;
			return player.stepForward();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean stepBackward() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return true;
			return player.stepBackward();
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Pause">
	@Override
	public boolean pause() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return true;
			return player.pause();
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Continue">
	@Override
	public boolean unpause() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return true;
			return player.unpause();
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Stop">
	@Override
	public boolean stop() {
		if (!lock.tryLock())
			return false;
		try {
			if (player == null)
				return true;
			
			if (player.stop()) {
				showNone();
				return true;
			} else {
				return false;
			}
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Play">
	@Override
	public boolean playBlackBurst() {
		return playBlackBurst(StringUtil.empty);
	}

	@Override
	public boolean playBlackBurst(String title) {
		return playPattern(title, VideoTestSrcPattern.BLACK);
	}

	@Override
	public boolean playTestSignal() {
		return playTestSignal(StringUtil.empty);
	}

	@Override
	public boolean playTestSignal(String title) {
		return playPattern(title, VideoTestSrcPattern.SMPTE);
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="The Meat">
	//<editor-fold defaultstate="collapsed" desc="Signals">
	protected void onPositionUpdate() {
		if (!emitPositionUpdates)
			return;
		if (lock.tryLock()) {
			try {
				if (player != null) {
					//if (!isSeekable())
					//	return;
				
					final long position = player.getLastKnownPosition();
					final long duration = player.getLastKnownDuration();
					final int percent = (duration > 0 ? Math.max(0, Math.min(100, (int)(((double)position / (double)duration) * 100.0D))) : -1);
					final boolean positionChanged = (position != lastPosition && position >= 0L);
					final boolean last = (position <= 0L && lastPosition > 0L);
				
					if (last && (positionChanged || currentLiveSource))
						firePositionChanged(100, lastDuration, lastDuration);
				
					lastPosition = position;
					lastDuration = duration;
					if (positionChanged || currentLiveSource)
						firePositionChanged(percent, position, duration);
				} else {
					lastPosition = 0L;
				}
			} finally {
				lock.unlock();
			}
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Play">
	@SuppressWarnings("empty-statement")
	public boolean playPattern(final String title, final VideoTestSrcPattern pattern) {
		if (!acquireLock())
			return false;
		try {
			if (player != null) {
				player.destroy();
				player.removeErrorListener(errorListener);
				player.removeEventListener(eventListener);
				player.removeResponseListener(responseListener);
			}

			final IMediaRequest newRequest = new MediaRequest(
				MediaRequestType.TestVideo,
				!StringUtil.isNullOrEmpty(title) ? title : pattern.name(),
				false,
				true,
				IMediaRequest.REPEAT_NONE,
				15.0f,
				Scheme.Local,
				new URI("local://pattern/" + pattern.name())
			);

			//Reset these values
			hasMultipartDemux = false;
			videoWidth = 0;
			videoHeight = 0;
			numberOfRepeats = 0;
			actualFPS = 0.0f;
			maintainAspectRatio = true;
			mediaType = MediaType.Video;

			//Save these values
			currentLiveSource = false;
			currentRepeatCount = 0;
			currentRate = 1.0D;
			mediaRequest = newRequest;

			fireMediaEventPlayRequested(newRequest);


			emitPositionUpdates = true;

			//Start playing
			showVideoCanvas();

			player = new GstPlayer();
			player.addErrorListener(errorListener);
			player.addEventListener(eventListener);
			player.addResponseListener(responseListener);
			return player.launch(videoElement, audioElement, nativeHandle, muted, volume, bufferSize, newRequest);
		} catch(Throwable t) {
			return false;
		} finally {
			lock.unlock();
		}
	}

	@Override
	@SuppressWarnings("empty-statement")
	public boolean play(final IMediaRequest request) {
		//<editor-fold defaultstate="collapsed" desc="Check params">
		if (request == null)
			return false;

		final URI uri = request.getURI();
		if (uri == null)
			return false;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Check for pattern URI">
		if ("local".equalsIgnoreCase(uri.getScheme()) && "pattern".equalsIgnoreCase(uri.getHost())) {
			try {
				String path = uri.getPath();
				if (path.startsWith("/") || path.startsWith("\\"))
					path = path.substring(1);
				return playPattern(request.getTitle(), VideoTestSrcPattern.valueOf(path));
			} catch(Throwable t) {
				//If we weren't able to determine a test pattern, then
				//just proceed normally.
			}
		}
		//</editor-fold>

		if (!acquireLock())
			return false;
		try {
			if (player != null) {
				player.destroy();
				player.removeErrorListener(errorListener);
				player.removeEventListener(eventListener);
				player.removeResponseListener(responseListener);
			}

			//Reset these values
			xoverlay = null;
			hasMultipartDemux = false;
			videoWidth = 0;
			videoHeight = 0;
			numberOfRepeats = 0;
			actualFPS = 0.0f;
			mediaType = MediaType.Unknown;

			//Save these values
			mediaRequest = request;
			currentLiveSource = request.isLiveSource();
			currentRepeatCount = request.getRepeatCount();
			maintainAspectRatio = request.isAspectRatioMaintained();

			currentRate = 1.0D;
			emitPositionUpdates = true;

			fireMediaEventPlayRequested(request);

			showVideoCanvas();

			player = new GstPlayer();
			player.addErrorListener(errorListener);
			player.addEventListener(eventListener);
			player.addResponseListener(responseListener);
			return player.launch(videoElement, audioElement, nativeHandle, muted, volume, bufferSize, request);
		} catch(Throwable t) {
			//t.printStackTrace();
			return false;
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>
	//</editor-fold>
}

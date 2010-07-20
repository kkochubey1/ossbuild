
package ossbuild.media.gstreamer.swt;

import ossbuild.media.Scheme;
import com.sun.jna.Pointer;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gstreamer.Bin;
import org.gstreamer.Buffer;
import org.gstreamer.Bus;
import org.gstreamer.BusSyncReply;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Format;
import org.gstreamer.Fraction;
import org.gstreamer.GhostPad;
import org.gstreamer.GstObject;
import org.gstreamer.Message;
import org.gstreamer.MiniObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.SeekFlags;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;
import org.gstreamer.Structure;
import org.gstreamer.event.BusSyncHandler;
import org.gstreamer.event.StepEvent;
import org.gstreamer.lowlevel.GstAPI.GstCallback;
import org.gstreamer.swt.overlay.SWTOverlay;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.media.gstreamer.ErrorType;
import ossbuild.media.gstreamer.VideoTestSrcPattern;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class MediaComponentOld extends Canvas {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final Scheme[] VALID_SCHEMES = new Scheme[] {
		  Scheme.HTTP
		, Scheme.HTTPS
		, Scheme.File
		, Scheme.RTP
		, Scheme.RTSP
		, Scheme.TCP
		, Scheme.UDP
	};

	public static final ScheduledExecutorService
		TASK_EXECUTOR
	;

	public static final String
		  DEFAULT_VIDEO_ELEMENT
		, DEFAULT_AUDIO_ELEMENT
	;

	public static final int
		  DEFAULT_FPS = -1
		, MINIMUM_FPS = 1
	;

	public static final int
		  DEFAULT_REPEAT_COUNT = 0
		, REPEAT_FOREVER = -1
	;

	public static final double
		  DEFAULT_RATE = 1.0D
	;

	public static final int
		  SEEK_FLAG_SKIP = (1 << 4)
	;

	private static final long
		  SEEK_STOP_DURATION = TimeUnit.MILLISECONDS.toNanos(10L)
	;

	public static final String[] VALID_COLORSPACES = {
		  "video/x-raw-rgb"
		, "video/x-raw-yuv"
	};

	public static final String[] VALID_YUV_FORMATS = {
		  "YUY2"
	};

	public static final String[] VALID_DIRECTDRAW_COLORSPACES = {
		  "video/x-raw-rgb"
	};
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected final long nativeHandle;
	protected final Lock lock = new ReentrantLock();
	protected final String videoElement;
	protected final String audioElement;
	protected final Display display;

	protected Pipeline pipeline;
	protected Element currentVideoSink;
	protected Element currentAudioSink;
	protected Element currentAudioVolumeElement;
	protected SWTOverlay xoverlay = null;

	private boolean hasAudio = false;
	private boolean hasVideo = false;

	private int fullVideoWidth = 0;
	private int fullVideoHeight = 0;
	protected final Runnable redrawRunnable;
	//private final Runnable seekFinishedRunnable;
	private final Runnable positionUpdateRunnable;
	protected final Runnable xoverlayRunnable;

	private float actualFPS;

	private boolean emitPositionUpdates = true;
	private boolean currentLiveSource;
	private int currentRepeatCount;
	private int currentFPS;
	private URI currentURI;
	private int numberOfRepeats;
	private double currentRate = DEFAULT_RATE;

	protected volatile State currentState = State.NULL;
	private volatile ScheduledFuture<?> positionTimer = null;

	private AtomicBoolean isSeeking = new AtomicBoolean(false);
	//private long seekingPos = 0L;
	//private double seekingRate = DEFAULT_RATE;

	private CountDownLatch latch = new CountDownLatch(1);

	private List<IMediaEventListener> mediaEventListeners;
	private final Object mediaEventListenerLock = new Object();

	private List<IVideoCapsListener> videoCapsListeners;
	private final Object videoCapsListenerLock = new Object();

	private List<IErrorListener> errorListeners;
	private final Object errorListenerLock = new Object();

	private List<IPositionListener> positionListeners;
	private final Object positionListenerLock = new Object();

	private List<IAudioListener> audioListeners;
	private final Object audioListenerLock = new Object();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		String videoElement;
		String audioElement;
		switch (Sys.getOSFamily()) {
			case Windows:
				//videoElement = "dshowvideosink";
				videoElement = "directdrawsink";
				audioElement = "autoaudiosink";
				break;
			case Unix:
				videoElement = "xvimagesink"; //gconfaudiosink and gconfvideosink?
				audioElement = "autoaudiosink";
				break;
			default:
				videoElement = "xvimagesink";
				audioElement = "autoaudiosink";
				break;
		}
		DEFAULT_VIDEO_ELEMENT = videoElement;
		DEFAULT_AUDIO_ELEMENT = audioElement;

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

	public MediaComponentOld(Composite parent, int style) {
		this(DEFAULT_VIDEO_ELEMENT, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponentOld(String videoElement, Composite parent, int style) {
		this(videoElement, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponentOld(String videoElement, String audioElement, Composite parent, int style) {
		super(parent, style | SWT.EMBEDDED | SWT.DOUBLE_BUFFERED);

		this.nativeHandle = SWTOverlay.handle(this);
		this.display = getDisplay();
		this.audioElement = audioElement;
		this.videoElement = videoElement;
		this.setLayout(new FillLayout());

		this.redrawRunnable = new Runnable() {
			@Override
			public void run() {
				if (!isDisposed())
					redraw();
			}
		};

		//this.seekFinishedRunnable = new Runnable() {
		//	@Override
		//	public void run() {
		//		seekFinished();
		//	}
		//};

		this.positionUpdateRunnable = new Runnable() {
			private long lastPosition = 0L;
			private long lastDuration = 0L;

			@Override
			public void run() {
				if (!emitPositionUpdates)
					return;
				if (lock.tryLock()) {
					try {
						if (pipeline != null) {
							final long position = pipeline.queryPosition(TimeUnit.MILLISECONDS);
							final long duration = Math.max(position, pipeline.queryDuration(TimeUnit.MILLISECONDS));
							final int percent = (duration > 0 ? Math.max(0, Math.min(100, (int)(((double)position / (double)duration) * 100.0D))) : -1);
							final boolean positionChanged = (position != lastPosition && position >= 0L);
							final boolean last = (position <= 0L && lastPosition > 0L);

							if (last && positionChanged && !currentLiveSource)
								firePositionChanged(100, lastDuration, lastDuration);

							lastPosition = position;
							lastDuration = duration;
							if (positionChanged && !currentLiveSource)
								firePositionChanged(percent, position, duration);
						} else {
							lastPosition = 0L;
						}
					} finally {
						lock.unlock();
					}
				}
			}
		};

		this.xoverlayRunnable = new Runnable() {
			@Override
			public void run() {
				synchronized(display) {
					xoverlay.setWindowID(MediaComponentOld.this);
				}
			}
		};

		//Ensure that we're at 0 so the first call to play can proceed immediately
		latch.countDown();

		//<editor-fold defaultstate="collapsed" desc="SWT Events">
		this.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent ce) {
				expose();
			}
		});
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent de) {
				stop();
				if (pipeline != null) {
					pipeline.setState(State.NULL);
					if (pipeline.getState(10000L, TimeUnit.MILLISECONDS) == State.NULL)
						pipeline.dispose();
					pipeline = null;
				}
			}
		});
		this.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent pe) {
				expose();
			}
		});
		//</editor-fold>
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	public void Dispose() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public Lock getMediaLock() {
		return lock;
	}

	public Scheme[] getValidSchemes() {
		return VALID_SCHEMES;
	}
	
	public int getFullVideoWidth() {
		return fullVideoWidth;
	}

	public int getFullVideoHeight() {
		return fullVideoHeight;
	}

	public boolean hasMedia() {
		lock.lock();
		try {
			return (pipeline != null && pipeline.getState(0L) != State.NULL);
		} finally {
			lock.unlock();
		}
	}

	public boolean isPaused() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;
			final State state = pipeline.getState(0L);
			return (state == State.PAUSED || state == State.READY);
		} finally {
			lock.unlock();
		}
	}

	public boolean isStopped() {
		lock.lock();
		try {
			if (pipeline == null)
				return true;
			final State state = pipeline.getState(0L);
			return (state == State.NULL || state == State.READY);
		} finally {
			lock.unlock();
		}
	}

	public boolean isPlaying() {
		lock.lock();
		try {
			if (pipeline == null)
				return true;
			final State state = pipeline.getState(0L);
			return (state == State.PLAYING);
		} finally {
			lock.unlock();
		}
	}

	public boolean isLiveSource() {
		return currentLiveSource;
	}

	public boolean isSeekable() {
		return !currentLiveSource && emitPositionUpdates;
	}

	public int getRepeatCount() {
		return currentRepeatCount;
	}

	public boolean isRepeatingForever() {
		return currentRepeatCount == REPEAT_FOREVER;
	}

	public float getActualFPS() {
		lock.lock();
		try {
			if (pipeline == null)
				return 0.0f;
			return actualFPS;
		} finally {
			lock.unlock();
		}
	}

	public URI getRequestedURI() {
		return currentURI;
	}

	public int getRequestedFPS() {
		lock.lock();
		try {
			if (pipeline == null)
				return DEFAULT_FPS;
			return currentFPS;
		} finally {
			lock.unlock();
		}
	}

	public long getPosition() {
		lock.lock();
		try {
			if (pipeline == null)
				return 0L;
			final State state = pipeline.getState(0L);
			if (state != State.PLAYING || state != State.PAUSED)
				return 0L;
			return pipeline.queryPosition(TimeUnit.MILLISECONDS);
		} finally {
			lock.unlock();
		}
	}

	public long getDuration() {
		lock.lock();
		try {
			if (pipeline == null)
				return 0L;
			final State state = pipeline.getState(0L);
			if (state != State.PLAYING || state != State.PAUSED)
				return 0L;
			return pipeline.queryDuration(TimeUnit.MILLISECONDS);
		} finally {
			lock.unlock();
		}
	}

	public boolean isMuted() {
		lock.lock();
		try {
			if (pipeline == null || currentAudioVolumeElement == null)
				return false;

			return (Boolean)currentAudioVolumeElement.get("mute");
		} finally {
			lock.unlock();
		}
	}

	public int getVolume() {
		lock.lock();
		try {
			if (pipeline == null || currentAudioVolumeElement == null)
				return 100;

			return Math.max(0, Math.min(100, (int)((Double)currentAudioVolumeElement.get("volume") * 100.0D)));
		} finally {
			lock.unlock();
		}
	}

	public boolean hasAudio() {
		return this.hasAudio;
	}

	public boolean hasVideo() {
		return this.hasVideo;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	public static boolean execute(final Runnable task) {
		if (TASK_EXECUTOR == null)
			return false;
		TASK_EXECUTOR.submit(task);
		return true;
	}
	
	public static IntBuffer convertToRGB(final ByteBuffer bb, final int width, final int height, final String colorspace, final String fourcc) {
		if (!isValidColorspace(colorspace))
			return null;

		if (isRGBColorspace(colorspace)) {
			return bb.asIntBuffer();
		} else if(isYUVColorspace(colorspace)) {
			if ("YUY2".equalsIgnoreCase(fourcc) || "YUYV".equalsIgnoreCase(fourcc) || "YUNV".equalsIgnoreCase(fourcc) || "V422".equalsIgnoreCase(fourcc))
				return yuyv2rgb(bb, width, height);
			else
				return null;
		} else {
			return null;
		}
	}

	public static IntBuffer yuyv2rgb(final ByteBuffer bb, final int width, final int height) {
		//Courtesy jcam
		//    http://www.stenza.org/packages/jcam.tgz

		final ByteBuffer destbb = ByteBuffer.allocate(4 * width * height);
		destbb.order(ByteOrder.BIG_ENDIAN);
		bb.order(ByteOrder.BIG_ENDIAN);

		int y1, u, y2, v;
		int cb, cr, cg;
		int r, g, b;

		int halfWidth = width / 2;
		int sstride = width*2;
		int dstride = width*4;

		int isrcindex, idestindex;

		for (int i = 0; i < height; ++i) {
			for (int j = 0; j < halfWidth; ++j) {
				isrcindex = i * sstride + 4*j;
				idestindex = i * dstride + 8*j;

				y1 = bb.get(isrcindex + 0)&0xff;
				u  = bb.get(isrcindex + 1)&0xff;
				y2 = bb.get(isrcindex + 2)&0xff;
				v  = bb.get(isrcindex + 3)&0xff;

				cb = ((u-128) * 454) >> 8;
				cr = ((v-128) * 359) >> 8;
				cg = ((v-128) * 183 + (u-128) * 88) >> 8;

				r = y1 + cr;
				b = y1 + cb;
				g = y1 - cg;

				destbb.put(idestindex + 0, (byte)0);
				destbb.put(idestindex + 1, (byte)Math.max(0, Math.min(255, r)));
				destbb.put(idestindex + 2, (byte)Math.max(0, Math.min(255, g)));
				destbb.put(idestindex + 3, (byte)Math.max(0, Math.min(255, b)));

				r = y2 + cr;
				b = y2 + cb;
				g = y2 - cg;

				destbb.put(idestindex + 4, (byte)0);
				destbb.put(idestindex + 5, (byte)Math.max(0, Math.min(255, r)));
				destbb.put(idestindex + 6, (byte)Math.max(0, Math.min(255, g)));
				destbb.put(idestindex + 7, (byte)Math.max(0, Math.min(255, b)));
			}
		}

		//destbb.flip();
		return destbb.asIntBuffer();
	}

	public static byte clamp(int min, int max, int value) {
		if (value < min)
			return (byte)min;
		if (value > max)
			return (byte)max;
		return (byte)(value);
	}

	public static boolean isRGBColorspace(final String colorspace) {
		return VALID_COLORSPACES[0].equalsIgnoreCase(colorspace);
	}

	public static boolean isYUVColorspace(final String colorspace) {
		return VALID_COLORSPACES[1].equalsIgnoreCase(colorspace);
	}

	public static boolean isValidYUVFormat(final String yuvFormat) {
		if (StringUtil.isNullOrEmpty(yuvFormat))
			return false;
		for(String cs : VALID_YUV_FORMATS)
			if (cs.equalsIgnoreCase(yuvFormat))
				return true;
		return false;
	}

	public static boolean isValidColorspace(final String colorspace) {
		if (StringUtil.isNullOrEmpty(colorspace))
			return false;
		for(String cs : VALID_COLORSPACES)
			if (cs.equalsIgnoreCase(colorspace))
				return true;
		return false;
	}

	private static String createColorspaceFilter(final MediaComponentOld src, final int fps) {
		final String framerate = (fps == DEFAULT_FPS || src.currentLiveSource ? null : ", framerate=" + fps + "/1");
		final StringBuilder sb = new StringBuilder(256);

		sb.append("video/x-raw-rgb, bpp=32, depth=24");
		for(int i = 1; i < VALID_COLORSPACES.length; ++i) {
			sb.append(';');
			sb.append(VALID_COLORSPACES[i]);
			if (framerate != null)
				sb.append(framerate);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}

	private static String uriString(URI uri) {
		//Courtesy http://code.google.com/p/gstreamer-java/source/browse/trunk/gstreamer-java/src/org/gstreamer/GObject.java
		String uriString = uri.toString();
		 // Need to fixup file:/ to be file:/// for gstreamer
		 if ("file".equals(uri.getScheme()) && uri.getHost() == null) {
			 final String path = uri.getRawPath();
			 uriString = "file://" + path;
		 }
		return uriString;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Interfaces">
	public static interface IVideoCapsListener {
		void videoDimensionsNegotiated(final MediaComponentOld source, final int videoWidth, final int videoHeight);
	}

	public static interface IErrorListener {
		void handleError(final MediaComponentOld source, final URI uri, final ErrorType errorType, final int code, final String message);
	}

	public static interface IMediaEventListener {
		void mediaPaused(final MediaComponentOld source);
		void mediaContinued(final MediaComponentOld source);
		void mediaStopped(final MediaComponentOld source);
		void mediaPlayed(final MediaComponentOld source);
	}

	public static interface IPositionListener {
		void positionChanged(final MediaComponentOld source, final int percent, final long position, final long duration);
	}

	public static interface IAudioListener {
		void audioMuted(final MediaComponentOld source);
		void audioUnmuted(final MediaComponentOld source);
		void audioVolumeChanged(final MediaComponentOld source, final int percent);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Adapters">
	public static abstract class VideoCapsListenerAdapter implements IVideoCapsListener {
		@Override
		public void videoDimensionsNegotiated(final MediaComponentOld source, int videoWidth, int videoHeight) {
		}
	}

	public static abstract class ErrorListenerAdapter implements IErrorListener {
		@Override
		public void handleError(final MediaComponentOld source, URI uri, ErrorType errorType, int code, String message) {
		}
	}

	public static abstract class MediaEventListenerAdapter implements IMediaEventListener {
		@Override
		public void mediaPaused(MediaComponentOld source) {
		}

		@Override
		public void mediaContinued(MediaComponentOld source) {
		}

		@Override
		public void mediaStopped(MediaComponentOld source) {
		}

		@Override
		public void mediaPlayed(MediaComponentOld source) {
		}
	}

	public static abstract class PositionListenerAdapter implements IPositionListener {
		@Override
		public void positionChanged(MediaComponentOld source, final int percent, long position, long duration) {
		}
	}

	public static abstract class AudioListenerAdapter implements IAudioListener {
		@Override
		public void audioMuted(MediaComponentOld source) {
		}

		@Override
		public void audioUnmuted(MediaComponentOld source) {
		}

		@Override
		public void audioVolumeChanged(MediaComponentOld source, int percent) {
		}
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

	//<editor-fold defaultstate="collapsed" desc="Error">
	protected void fireHandleError(final URI uri, final ErrorType errorType, final int code, final String message) {
		if (errorListeners == null || errorListeners.isEmpty())
			return;
		for(IErrorListener listener : errorListeners)
			listener.handleError(this, uri, errorType, code, message);
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

	protected void fireMediaEventStarted() {
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
	//<editor-fold defaultstate="collapsed" desc="Snapshots">
	public boolean saveSnapshot(final File File) {
		if (File == null)
			return false;

		FileOutputStream fos = null;
		try {

			final ImageData data = produceImageDataSnapshotForSWT();
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

		final ImageData data = produceImageDataSnapshotForSWT();
		if (data == null)
			return false;

		final ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };
		loader.save(Output, SWT.IMAGE_JPEG);
		return true;
	}

	public Image produceSnapshotForSWT() {
		final ImageData data = produceImageDataSnapshotForSWT();
		if (data == null)
			return null;
		//Caller will be responsible for disposing this image
		return new Image(display, data);
	}

	public ImageData produceImageDataSnapshotForSWT() {
		Buffer buffer = null;
		try {
			lock.lock();
			try {
				if (currentVideoSink == null)
					return null;

				final Pointer ptr = currentVideoSink.getPointer("last-buffer");
				if (ptr == null)
					return null;
				buffer = MiniObject.objectFor(ptr, Buffer.class, false);
				if (buffer == null)
					return null;

				final Caps caps = buffer.getCaps();
				final Structure struct = caps.getStructure(0);
				final int width = struct.getInteger("width");
				final int height = struct.getInteger("height");
				if (width < 1 || height < 1)
					return null;

				//Convert to RGB using the provided direct buffer
				final IntBuffer rgb = convertToRGB(buffer.getByteBuffer(), width, height, struct.getName(), struct.hasField("format") ? struct.getFourccString("format") : null);
				if (rgb == null)
					return null;

				int[] pixels = new int[rgb.remaining()];
				ImageData imageData = new ImageData(width, height, 24, new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));
				rgb.get(pixels, 0, rgb.remaining());
				imageData.setPixels(0, 0, pixels.length, pixels, 0);

				return imageData;
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

	public BufferedImage produceSnapshot() {
		Buffer buffer = null;
		try {
			lock.lock();
			try {
				if (currentVideoSink == null)
					return null;

				final Pointer ptr = currentVideoSink.getPointer("last-buffer");
				if (ptr == null)
					return null;
				buffer = MiniObject.objectFor(ptr, Buffer.class, false);
				if (buffer == null)
					return null;

				final Caps caps = buffer.getCaps();
				final Structure struct = caps.getStructure(0);
				final int width = struct.getInteger("width");
				final int height = struct.getInteger("height");
				if (width < 1 || height < 1)
					return null;

				//Convert to RGB using the provided direct buffer
				final IntBuffer rgb = convertToRGB(buffer.getByteBuffer(), width, height, struct.getName(), struct.hasField("format") ? struct.getFourccString("format") : null);
				if (rgb == null)
					return null;

				final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				img.setAccelerationPriority(0.001f);
				rgb.get(((DataBufferInt)img.getRaster().getDataBuffer()).getData(), 0, rgb.remaining());

				return img;
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
		State state;
		if (xoverlay != null && pipeline != null && ((state = pipeline.getState(0)) == State.PLAYING ||state == State.PAUSED)) {
			xoverlay.expose();
			return true;
		}
		return false;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Volume">
	public boolean mute() {
		lock.lock();
		try {
			if (pipeline == null || currentAudioVolumeElement == null)
				return false;

			boolean muted = !isMuted();
			currentAudioVolumeElement.set("mute", muted);
			if (muted)
				fireAudioMuted();
			else
				fireAudioUnmuted();
		} finally {
			lock.unlock();
		}
		return true;
	}

	public boolean adjustVolume(int percent) {
		lock.lock();
		try {
			if (pipeline == null || currentAudioVolumeElement == null)
				return false;

			int oldVolume = getVolume();
			int newVolume = Math.max(0, Math.min(100, percent));
			currentAudioVolumeElement.set("volume", (double)newVolume / 100.0D);
			if (oldVolume != newVolume)
				fireAudioVolumeChanged(newVolume);
		} finally {
			lock.unlock();
		}
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Seek">
	public boolean seekToBeginning() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;

			State state = pipeline.getState(0L);

			if (currentLiveSource && state != State.PLAYING)
				return pipeline.setState(State.PLAYING) == StateChangeReturn.SUCCESS;

			final double rate = currentRate;
			final boolean forwards = (rate >= 0.0);
			final long positionNanoSeconds = 0L;
			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
			final long stop = (forwards ? -1 : 0);

			final boolean success = pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, begin, SeekType.SET, stop);
			pipeline.setState(State.PLAYING);

			return success;
		} finally {
			lock.unlock();
		}
	}

	public boolean seekToBeginningAndPause() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;

			State state = pipeline.getState(0L);

			if (currentLiveSource && state != State.PLAYING)
				return pipeline.setState(State.PLAYING) == StateChangeReturn.SUCCESS;

			final double rate = currentRate;
			final boolean forwards = (rate >= 0.0);
			final long positionNanoSeconds = 0L;
			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
			final long stop = (forwards ? -1 : 0);

			final boolean success = pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, begin, SeekType.SET, stop);
			pipeline.setState(State.PAUSED);

			return success;
		} finally {
			lock.unlock();
		}
	}

	public boolean adjustPlaybackRate(final double rate) {
		//TODO: Figure out why playing backwards (rate is negative) isn't working
		if (rate < 0.0f)
			return false;
		
		if (rate == 0.0f)
			return pause();

		lock.lock();
		try {
			if (pipeline == null || currentLiveSource)
				return false;

			State state;
			if ((state = pipeline.getState(0L)) == State.NULL)
				return false;

			if (state == State.PLAYING) {
				pipeline.setState(State.PAUSED);
				if ((state = pipeline.getState(2000L, TimeUnit.MILLISECONDS)) != State.PAUSED)
					return false;
			}

			final boolean forwards = (rate >= 0.0);
			final long positionNanoSeconds = pipeline.queryPosition(Format.TIME);
			//final long stop = (forwards ? positionNanoSeconds + SEEK_STOP_DURATION : Math.max(0, positionNanoSeconds - SEEK_STOP_DURATION));
			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
			final long stop = (forwards ? -1 : 0);

			final boolean success = pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, begin, SeekType.SET, stop);
			pipeline.setState(State.PLAYING);

			if (success)
				currentRate = rate;
			
			return success;
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

		lock.lock();
		try {
			if (pipeline == null || currentLiveSource)
				return false;

			if (pipeline.getState(0L) == State.NULL)
				return false;

			final boolean forwards = (rate >= 0.0);
			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
			final long stop = (forwards ? -1 : 0);

			final boolean success = pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, begin, SeekType.SET, stop);
			pipeline.setState(State.PLAYING);

			if (success)
				currentRate = rate;

			return success;
		} finally {
			lock.unlock();
		}
	}

	private void seekFinished() {
		isSeeking.set(false);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Step">
	public boolean stepForward() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;

			State state;
			if ((state = pipeline.getState(0L)) == State.NULL)
				return false;

			if (state != State.PAUSED) {
				if (state == State.PLAYING) {
					pause();
				} else {
					seekToBeginningAndPause();
				}
				if (pipeline.getState(2000L, TimeUnit.MILLISECONDS) != State.PAUSED)
					return false;
			}

			return pipeline.sendEvent(new StepEvent(Format.BUFFERS, 1L, 1.0D, true, false));
		} finally {
			lock.unlock();
		}
	}

	public boolean stepBackward() {
		return false;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Pause">
	public boolean pause() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;
			State state;
			if ((state = pipeline.getState(0L)) == State.PAUSED || state == State.NULL || state == State.READY)
				return true;
			pipeline.setState(State.PAUSED);
		} finally {
			lock.unlock();
		}
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Continue">
	public boolean unpause() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;

			State state;
			if ((state = pipeline.getState(0L)) == State.PLAYING)
				return true;
			if (state != State.PAUSED && !currentLiveSource)
				return seekToBeginning();

			pipeline.setState(State.PLAYING);
		} finally {
			lock.unlock();
		}
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Stop">
	public boolean stop() {
		lock.lock();
		try {
			if (pipeline == null)
				return true;

			State state;
			if ((state = pipeline.getState(0L)) == State.NULL || state == State.READY)
				return true;

			pipeline.setState(State.READY);
		} finally {
			lock.unlock();
		}
		this.redraw();
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Play">
	public boolean playBlackBurst() {
		return playPattern(VideoTestSrcPattern.BLACK);
	}

	public boolean playTestSignal() {
		return playPattern(VideoTestSrcPattern.SMPTE);
	}

	public boolean play(File file) {
		if (file == null)
			return false;
		return play(false, DEFAULT_REPEAT_COUNT, DEFAULT_FPS, file.toURI());
	}

	public boolean play(String uri) {
		return play(false, DEFAULT_REPEAT_COUNT, DEFAULT_FPS, uri);
	}

	public boolean play(final URI uri) {
		return play(false, DEFAULT_REPEAT_COUNT, DEFAULT_FPS, uri);
	}

	public boolean play(final int repeat, final URI uri) {
		return play(false, repeat, DEFAULT_FPS, uri);
	}

	public boolean play(final int repeat, final int fps, final URI uri) {
		return play(false, repeat, fps, uri);
	}

	public boolean play(final boolean liveSource, final int repeat, final int fps, final String uri) {
		if (StringUtil.isNullOrEmpty(uri))
			return false;
		try {
			return play(liveSource, repeat, fps, new URI(uri));
		} catch(URISyntaxException e) {
			return false;
		}
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="The Meat">
	public boolean playPattern(final VideoTestSrcPattern pattern) {
		lock.lock();
		try {
			if (pipeline != null)
				pipeline.setState(State.NULL);

			//Reset these values
			hasVideo = false;
			hasAudio = false;
			fullVideoWidth = 0;
			fullVideoHeight = 0;
			numberOfRepeats = 0;
			actualFPS = 0.0f;
			currentVideoSink = null;
			currentAudioSink = null;
			currentAudioVolumeElement = null;

			//Save these values
			currentLiveSource = false;
			currentRepeatCount = 0;
			currentFPS = 15;
			currentURI = null;

			currentRate = 1.0D;

			final int checked_fps = (currentFPS >= MINIMUM_FPS ? currentFPS : DEFAULT_FPS);
			final Pipeline newPipeline = new Pipeline("pipeline");
			final Element videoTestSrc = ElementFactory.make("videotestsrc", "videoTestSrc");
			final Element videoQueue = ElementFactory.make("queue2", "videoQueue");
			final Element videoRate = ElementFactory.make("videorate", "videoRate");
			final Element videoColorspace = ElementFactory.make("ffmpegcolorspace", "videoColorspace");
			final Element videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
			final Element videoScale = ElementFactory.make("videoscale", "videoScale");
			final Element videoSink = createVideoSink(videoElement);

			videoRate.set("silent", true);
			videoCapsFilter.setCaps(Caps.fromString(createColorspaceFilter(this, checked_fps))); //framerate=25/1 means 25 FPS

			videoTestSrc.set("pattern", (long)pattern.intValue());

			newPipeline.addMany(videoTestSrc, videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
			Element.linkMany(videoTestSrc, videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
			
			//<editor-fold defaultstate="collapsed" desc="Signals">
			//<editor-fold defaultstate="collapsed" desc="Bus">
			final Bus bus = newPipeline.getBus();
			bus.connect(new Bus.STATE_CHANGED() {
				@Override
				public void stateChanged(GstObject source, State oldState, State newState, State pendingState) {
					if (source != newPipeline)
						return;

					if (newState == State.NULL && pendingState == State.NULL) {
						disposeAudioBin(newPipeline);
						disposeVideoBin(newPipeline);
						newPipeline.dispose();
						display.asyncExec(redrawRunnable);
					}

					switch (newState) {
						case PLAYING:
							if (currentState == State.NULL || currentState == State.READY || currentState == State.PAUSED) {
								if (currentState == State.PAUSED)
									fireMediaEventContinued();
								else
									fireMediaEventStarted();
								currentState = State.PLAYING;
							}
							break;
						case PAUSED:
							if (currentState == State.PLAYING || currentState == State.READY) {
								currentState = State.PAUSED;
								fireMediaEventPaused();
							}
							break;
						case NULL:
						case READY:
							if (currentState == State.PLAYING || currentState == State.PAUSED) {
								currentState = State.READY;
								fireMediaEventStopped();
							}
							break;
					}
				}
			});
			bus.connect(new Bus.ERROR() {
				@Override
				public void errorMessage(GstObject source, int code, String message) {
					//System.out.println("Error: code=" + code + " message=" + message);
					fireHandleError(currentURI, ErrorType.fromNativeValue(code), code, message);
				}
			});
			bus.connect(new Bus.SEGMENT_DONE() {
				@Override
				public void segmentDone(GstObject source, Format format, long position) {
					numberOfRepeats = 0;
					pipeline.setState(State.READY);
					display.asyncExec(redrawRunnable);
				}
			});
			bus.connect(new Bus.EOS() {
				@Override
				public void endOfStream(GstObject source) {
					numberOfRepeats = 0;
					pipeline.setState(State.READY);
					display.asyncExec(redrawRunnable);
				}
			});
			bus.setSyncHandler(new BusSyncHandler() {
				@Override
				public BusSyncReply syncMessage(Message msg) {
					Structure s = msg.getStructure();
					if (s == null || !s.hasName("prepare-xwindow-id"))
						return BusSyncReply.PASS;
					xoverlay.setWindowID(MediaComponentOld.this.nativeHandle);
					return BusSyncReply.DROP;
				}
			});
			//</editor-fold>
			//</editor-fold>

			pipeline = newPipeline;
			currentVideoSink = videoSink;
			xoverlay = SWTOverlay.wrap(videoSink);
			emitPositionUpdates = false;

			//Start playing
			pipeline.setState(State.PLAYING);
			return true;
		} catch(Throwable t) {
			t.printStackTrace();
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	public boolean play(final boolean liveSource, final int repeat, final int fps, final URI uri) {
		if (uri == null)
			return false;

		lock.lock();
		try {
			if (pipeline != null)
				pipeline.setState(State.NULL);

			//Reset these values
			hasVideo = false;
			hasAudio = false;
			fullVideoWidth = 0;
			fullVideoHeight = 0;
			numberOfRepeats = 0;
			actualFPS = 0.0f;
			currentVideoSink = null;
			currentAudioSink = null;
			currentAudioVolumeElement = null;

			//Save these values
			currentLiveSource = liveSource;
			currentRepeatCount = repeat;
			currentFPS = fps;
			currentURI = uri;

			currentRate = 1.0D;
			emitPositionUpdates = true;

			pipeline = createPipeline(uri);

			//Start playing
			//Attempts to ensure that we're using segment seeks (which signals SEGMENT_DONE) to look for repeats instead of EOS
			pipeline.seek(1.0D, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, 0L, SeekType.SET, -1L);
			pipeline.setState(State.PLAYING);
			return true;
		} finally {
			lock.unlock();
		}
	}

	protected Element createVideoSink(String suggestedVideoSink) {
		final Element videoSink = ElementFactory.make(suggestedVideoSink, "videoSink");
		videoSink.set("show-preroll-frame", true);
		videoSink.getStaticPad("sink").connect("notify::caps", Object.class, null, new GstCallback() {
			@SuppressWarnings("unused")
			public boolean callback(Pad pad, Pointer unused, Pointer dynamic) {
				final Caps caps = pad.getNegotiatedCaps();
				if (caps == null || caps.isEmpty())
					return false;
				final Structure struct = caps.getStructure(0);
				if (struct == null)
					return false;

				if (struct.hasField("framerate")) {
					Fraction framerate = struct.getFraction("framerate");
					actualFPS = (float)framerate.getNumerator() / (float)framerate.getDenominator();
				}

				if (struct.hasIntField("width") && struct.hasIntField("height")) {
					final int width = struct.getInteger("width");
					final int height = struct.getInteger("height");
					fullVideoWidth = width;
					fullVideoHeight = height;
					fireVideoDimensionsNegotiated(width, height);
				}
				return true;
			}
		});
		return videoSink;
	}

	protected Element createAudioSink(String suggestedAudioSink) {
		return ElementFactory.make(suggestedAudioSink, "audioSink");
	}

	protected Pad createAudioBin(final Pipeline newPipeline, final Bin audioBin, final Bin uridecodebin, final Pad pad) {

		//[ queue2 ! volume ! audioconvert ! audioresample ! scaletempo ! audioconvert ! audioresample ! autoaudiosink ]

		final Element audioQueue = ElementFactory.make("queue2", "audioQueue");
		final Element audioVolume = ElementFactory.make("volume", "audioVolume");
		final Element audioConvert = ElementFactory.make("audioconvert", "audioConvert");
		final Element audioResample = ElementFactory.make("audioresample", "audioResample");
		final Element audioScaleTempo = ElementFactory.make("scaletempo", "audioScaleTempo");
		final Element audioConvertAfterScaleTempo = ElementFactory.make("audioconvert", "audioConvertAfterScaleTempo");
		final Element audioResampleAfterScaleTempo = ElementFactory.make("audioresample", "audioResampleAfterScaleTempo");
		final Element audioSink = createAudioSink(audioElement);

		audioBin.addMany(audioQueue, audioVolume, audioConvert, audioResample, audioScaleTempo, audioConvertAfterScaleTempo, audioResampleAfterScaleTempo, audioSink);
		Element.linkMany(audioQueue, audioVolume, audioConvert, audioResample, audioScaleTempo, audioConvertAfterScaleTempo, audioResampleAfterScaleTempo, audioSink);

		currentAudioSink = audioSink;
		currentAudioVolumeElement = audioVolume;

		//Element to connect uridecodebin src pad to.
		return audioQueue.getStaticPad("sink");
	}

	protected Pad createVideoBin(final Pipeline newPipeline, final Bin videoBin, final Bin uridecodebin, final Pad pad) {

		//[ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]

		final int checked_fps = (currentFPS >= MINIMUM_FPS ? currentFPS : DEFAULT_FPS);

		final Element videoQueue;
		final Element videoRate;
		final Element videoColorspace;
		final Element videoCapsFilter;
		final Element videoScale;
		final Element videoSink;

		if (!currentLiveSource) {
			videoQueue = ElementFactory.make("queue2", "videoQueue");
			videoRate = ElementFactory.make("videorate", "videoRate");
			videoColorspace = ElementFactory.make("ffmpegcolorspace", "videoColorspace");
			videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
			videoScale = ElementFactory.make("videoscale", "videoScale");
			videoSink = createVideoSink(videoElement);
			
			videoRate.set("silent", true);
			videoCapsFilter.setCaps(Caps.fromString(createColorspaceFilter(this, checked_fps))); //framerate=25/1 means 25 FPS
			
			videoBin.addMany(videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
			Element.linkMany(/*teeVideo,*/ videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
		} else {
			videoQueue = ElementFactory.make("queue2", "videoQueue");
			videoRate = null;
			videoColorspace = ElementFactory.make("ffmpegcolorspace", "videoColorspace");
			//videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
			//videoScale = ElementFactory.make("videoscale", "videoScale");
			videoSink = createVideoSink(videoElement);
			
			//videoCapsFilter.setCaps(Caps.fromString(createColorspaceFilter(this, checked_fps))); //framerate=25/1 means 25 FPS

			videoBin.addMany(videoQueue, videoColorspace, /*videoCapsFilter, videoScale,*/ videoSink);
			Element.linkMany(/*teeVideo,*/ videoQueue, videoColorspace, /*videoCapsFilter, videoScale,*/ videoSink);
		}

		currentVideoSink = videoSink;
		xoverlay = SWTOverlay.wrap(videoSink);

		return videoQueue.getStaticPad("sink");
	}

	protected boolean disposeAudioBin(final Pipeline newPipeline) {
		final Element existingAudioBin = newPipeline.getElementByName("audioBin");

		if (existingAudioBin == null)
			return true;
		
		existingAudioBin.setState(State.NULL);
		if (existingAudioBin.getState() == State.NULL) {
			newPipeline.remove(existingAudioBin);
			existingAudioBin.dispose();
		}
		
		return true;
	}

	protected boolean disposeVideoBin(final Pipeline newPipeline) {
		final Element existingVideoBin = newPipeline.getElementByName("videoBin");

		if (existingVideoBin == null)
			return true;

		existingVideoBin.setState(State.NULL);
		if (existingVideoBin.getState() == State.NULL) {
			newPipeline.remove(existingVideoBin);
			existingVideoBin.dispose();
		}

		return true;
	}

	protected void onPadAdded(final Pipeline newPipeline, final Bin uridecodebin, final Pad pad) {
		//only link once
		if (pad.isLinked())
			return;

		//check media type
		final Caps caps = pad.getCaps();
		final Structure struct = caps.getStructure(0);
		final String padCaps = struct.getName();

		if (StringUtil.isNullOrEmpty(padCaps))
			return;

		if (padCaps.startsWith("audio/")) {
			disposeAudioBin(newPipeline);

			//Create audio bin
			final Bin audioBin = new Bin("audioBin");
			audioBin.addPad(new GhostPad("sink", createAudioBin(newPipeline, audioBin, uridecodebin, pad)));
			newPipeline.add(audioBin);
			pad.link(audioBin.getStaticPad("sink"));

			hasAudio = true;

			audioBin.setState(State.PLAYING);
		} else if (padCaps.startsWith("video/")) {
			disposeVideoBin(newPipeline);

			//Create video bin
			final Bin videoBin = new Bin("videoBin");
			videoBin.addPad(new GhostPad("sink", createVideoBin(newPipeline, videoBin, uridecodebin, pad)));
			newPipeline.add(videoBin);
			pad.link(videoBin.getStaticPad("sink"));

			hasVideo = true;

			videoBin.setState(State.PLAYING);
		}
	}

	protected void onUriDecodeBinElementAdded(final Pipeline newPipeline, final Bin uridecodebin, final Element element) {
		//<editor-fold defaultstate="collapsed" desc="Validate arguments">
		//We only care to modify the element if we're using a live source
		if (!currentLiveSource || element == null)
			return;
		final String factoryName = element.getFactory().getName();
		//</editor-fold>

		//if (factoryName.startsWith("souphttpsrc")) {
		//	element.set("do-timestamp", true);
		//} else if (factoryName.startsWith("neonhttpsrc")) {
		//	element.set("do-timestamp", true);
		//}
	}

	protected void onDecodeBinElementAdded(final Pipeline newPipeline, final Bin uridecodebin, final Bin decodebin, final Element element) {
		//<editor-fold defaultstate="collapsed" desc="Validate arguments">
		//We only care to modify the multipartdemux if we're using a live stream
		if (!currentLiveSource)
			return;
		//Determine if what we're looking at is a multipartdemux element
		final String factoryName = element.getFactory().getName();
		if (!"multipartdemux".equalsIgnoreCase(factoryName))
			return;
		//</editor-fold>

		try {
			//Informs multipartdemux elements that it needs to emit the pad added signal as
			//soon as it links the pads. Otherwise it could be some time before it happens.
			//This was primarily added to instantly connect to motion jpeg digital cameras
			//that have a low framerate (e.g. 1 or 2 FPS).
			//It could be an issue with a "live source" that is emitting multiple streams
			//via a multipart mux. There's really not much we can do about something like
			//that in an automatic way -- that is, you'd have to remove this and instead
			//use a custom pipeline to work w/ low framerate digital cameras.
			element.set("single-stream", true);
		} catch(IllegalArgumentException e) {
		}
	}

	protected Pipeline createPipeline(final URI uri) {
		//gst-launch uridecodebin use-buffering=false name=dec location=http://.../video.avi
		//    dec. ! [ queue ! audioconvert ! audioresample ! autoaudiosink ]
		//    dec. ! [ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]
		final Pipeline newPipeline = new Pipeline("pipeline");
		final Bin uridecodebin = (Bin)ElementFactory.make("uridecodebin", "uridecodebin");
		uridecodebin.set("use-buffering", false);
		uridecodebin.set("download", true);
		uridecodebin.set("buffer-duration", TimeUnit.MILLISECONDS.toNanos(500L));
		uridecodebin.set("uri", uriString(uri));
		newPipeline.addMany(uridecodebin);

		//<editor-fold defaultstate="collapsed" desc="Signals">
		//<editor-fold defaultstate="collapsed" desc="Uridecodebin">
		uridecodebin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element elem, Pad pad) {
				onPadAdded(newPipeline, uridecodebin, pad);
			}
		});
		uridecodebin.connect(new Bin.ELEMENT_ADDED() {
			@Override
			public void elementAdded(Bin bin, Element element) {
				//<editor-fold defaultstate="collapsed" desc="Validate arguments">
				if (element == null)
					return;
				final String factoryName = element.getFactory().getName();
				if (StringUtil.isNullOrEmpty(factoryName))
					return;
				//</editor-fold>

				//<editor-fold defaultstate="collapsed" desc="Connect to decodebin">
				if (factoryName.startsWith("decodebin")) {
					final Bin decodebin = (Bin)element;
					decodebin.connect(new Bin.ELEMENT_ADDED() {
						@Override
						public void elementAdded(Bin bin, Element element) {
							onDecodeBinElementAdded(newPipeline, uridecodebin, decodebin, element);
						}
					});
				}
				//</editor-fold>

				onUriDecodeBinElementAdded(newPipeline, uridecodebin, element);
			}
		});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Bus">
		final Bus bus = newPipeline.getBus();
		bus.connect(new Bus.STATE_CHANGED() {
			@Override
			public void stateChanged(GstObject source, State oldState, State newState, State pendingState) {
				if (source != newPipeline)
					return;
				
				if (newState == State.NULL && pendingState == State.NULL) {
					disposeAudioBin(newPipeline);
					disposeVideoBin(newPipeline);
					newPipeline.dispose();
					display.asyncExec(redrawRunnable);
				}

				switch (newState) {
					case PLAYING:
						if (currentState == State.NULL || currentState == State.READY || currentState == State.PAUSED) {
							if (currentState == State.PAUSED)
								fireMediaEventContinued();
							else
								fireMediaEventStarted();
							currentState = State.PLAYING;
						}
						break;
					case PAUSED:
						if (currentState == State.PLAYING || currentState == State.READY) {
							currentState = State.PAUSED;
							fireMediaEventPaused();
						}
						break;
					case NULL:
					case READY:
						if (currentState == State.PLAYING || currentState == State.PAUSED) {
							currentState = State.READY;
							fireMediaEventStopped();
						}
						break;
				}
			}
		});
		bus.connect(new Bus.ERROR() {
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				//System.out.println("Error: code=" + code + " message=" + message);
				fireHandleError(currentURI, ErrorType.fromNativeValue(code), code, message);
			}
		});
		bus.connect(new Bus.SEGMENT_DONE() {
			@Override
			public void segmentDone(GstObject source, Format format, long position) {
				if (currentRepeatCount == REPEAT_FOREVER || (currentRepeatCount > 0 && numberOfRepeats < currentRepeatCount)) {
					++numberOfRepeats;
					if (!seekToBeginning()) {
						stop();
						unpause();
					}
					return;
				}
				numberOfRepeats = 0;
				pipeline.setState(State.READY);
				display.asyncExec(redrawRunnable);
			}
		});
		bus.connect(new Bus.EOS() {
			@Override
			public void endOfStream(GstObject source) {
				if (currentRepeatCount == REPEAT_FOREVER || (currentRepeatCount > 0 && numberOfRepeats < currentRepeatCount)) {
					++numberOfRepeats;
					if (!seekToBeginning()) {
						stop();
						unpause();
					}
					return;
				}
				numberOfRepeats = 0;
				pipeline.setState(State.READY);
				display.asyncExec(redrawRunnable);
			}
		});
		bus.connect(new Bus.BUFFERING() {
			@Override
			public void bufferingData(GstObject source, int percent) {
				if (!currentLiveSource) {
					if (percent < 100) {
						pipeline.setState(State.PAUSED);
					} else if (percent >= 100) {
						pipeline.setState(State.PLAYING);
					}
				}
			}
		});
		bus.setSyncHandler(new BusSyncHandler() {
			@Override
			public BusSyncReply syncMessage(Message msg) {
				Structure s = msg.getStructure();
				if (s == null || !s.hasName("prepare-xwindow-id"))
					return BusSyncReply.PASS;
				xoverlay.setWindowID(MediaComponentOld.this.nativeHandle);
				return BusSyncReply.DROP;
			}
		});
		//</editor-fold>
		//</editor-fold>
		
		return newPipeline;
	}
	//</editor-fold>
}

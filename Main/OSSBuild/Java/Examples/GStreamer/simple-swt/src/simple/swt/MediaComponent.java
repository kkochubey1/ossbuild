
package simple.swt;

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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.Bin;
import org.gstreamer.Buffer;
import org.gstreamer.Bus;
import org.gstreamer.BusSyncReply;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Event;
import org.gstreamer.Format;
import org.gstreamer.Fraction;
import org.gstreamer.GhostPad;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Message;
import org.gstreamer.MiniObject;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.SeekFlags;
import org.gstreamer.SeekType;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.gstreamer.elements.PlayBin2;
import org.gstreamer.event.BusSyncHandler;
import org.gstreamer.lowlevel.GstAPI.GstCallback;
import org.gstreamer.lowlevel.GstNative;
import org.gstreamer.lowlevel.annotations.Invalidate;
import ossbuild.OSFamily;
import ossbuild.StringUtil;
import ossbuild.Sys;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class MediaComponent extends Canvas {
	//<editor-fold defaultstate="collapsed" desc="API">
	private static class StepEvent extends Event {
		private static interface API extends com.sun.jna.Library {
			Pointer gst_event_new_custom(int type, @Invalidate Structure structure);
			Pointer ptr_gst_event_new_step(Format format, long amount, double rate, boolean flush, boolean intermediate);
		}
		private static final API gst = GstNative.load(API.class);

		public StepEvent(Initializer init) {
			super(init);
		}

		public StepEvent(Format format, long amount, double rate, boolean flush, boolean intermediate) {
			//GstStructureAPI.GSTSTRUCTURE_API.
			//gst.gst_event_new_custom((19 << 4) | (1 << 0));
			super(initializer(gst.ptr_gst_event_new_step(format, amount, rate, flush, intermediate)));
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Constants">
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
		  SEEK_STOP_DURATION = TimeUnit.MILLISECONDS.toNanos(50L)
	;

	public static final String[] VALID_COLORSPACES = {
		  "video/x-raw-rgb"
		, "video/x-raw-yuv"
	};

	public static final String[] VALID_YUV_FORMATS = {
		  "YUY2"
	};
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected final Object lock = new Object();
	protected final String videoElement;
	protected final String audioElement;
	protected final Display display;

	protected Pipeline pipeline;
	protected Element currentVideoSink;
	protected CustomXOverlay xoverlay = null;
	private int fullVideoWidth = 0;
	private int fullVideoHeight = 0;
	protected final Runnable redrawRunnable;
	private final Runnable seekFinishedRunnable;
	private final Runnable positionUpdateRunnable;

	private float actualFPS;

	private boolean currentBuffering;
	private int currentRepeatCount;
	private int currentFPS;
	private URI currentURI;
	private int numberOfRepeats;

	protected volatile State currentState = State.NULL;
	private volatile ScheduledFuture<?> positionTimer = null;

	private AtomicBoolean isSeeking = new AtomicBoolean(false);
	private long seekingPos = 0L;
	private double seekingRate = DEFAULT_RATE;

	private CountDownLatch latch = new CountDownLatch(1);

	private List<IMediaEventListener> mediaEventListeners;
	private final Object mediaEventListenerLock = new Object();

	private List<IVideoCapsListener> videoCapsListeners;
	private final Object videoCapsListenerLock = new Object();

	private List<IErrorListener> errorListeners;
	private final Object errorListenerLock = new Object();

	private List<IPositionListener> positionListeners;
	private final Object positionListenerLock = new Object();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		String videoElement;
		String audioElement;
		switch (Sys.getOSFamily()) {
			case Windows:
				videoElement = "dshowvideosink";
				//videoElement = "directdrawsink";
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
	}

	public MediaComponent(Composite parent, int style) {
		this(DEFAULT_VIDEO_ELEMENT, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponent(String videoElement, Composite parent, int style) {
		this(videoElement, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponent(String videoElement, String audioElement, Composite parent, int style) {
		super(parent, style | SWT.EMBEDDED | SWT.DOUBLE_BUFFERED);

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

		this.seekFinishedRunnable = new Runnable() {
			@Override
			public void run() {
				seekFinished();
			}
		};

		this.positionUpdateRunnable = new Runnable() {
			private long lastPosition = 0;

			@Override
			public void run() {
				final long position = pipeline.queryPosition(TimeUnit.MILLISECONDS);
				final long duration = Math.max(position, pipeline.queryDuration(TimeUnit.MILLISECONDS));
				final int percent = (duration > 0 ? Math.max(0, Math.min(100, (int)(((double)position / (double)duration) * 100.0D))) : -1);
				final boolean positionChanged = (position != lastPosition && position >= 0L);
				lastPosition = position;
				if (positionChanged)
					firePositionChanged(percent, position, duration);
			}
		};

		//Ensure that we're at 0 so the first call to play can proceed immediately
		latch.countDown();

		//<editor-fold defaultstate="collapsed" desc="SWT Events">
		this.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent ce) {
				if (xoverlay != null && pipeline != null && (pipeline.getState(0) == State.PLAYING || pipeline.getState(0) == State.PAUSED))
					xoverlay.expose();
			}
		});
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent de) {
				stop();
			}
		});
		//</editor-fold>
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public int getFullVideoWidth() {
		return fullVideoWidth;
	}

	public int getFullVideoHeight() {
		return fullVideoHeight;
	}

	public boolean hasMedia() {
		synchronized(lock) {
			return (pipeline != null && pipeline.getState(0L) != State.NULL);
		}
	}

	public boolean isPaused() {
		synchronized(lock) {
			if (pipeline == null)
				return false;
			final State state = pipeline.getState(0L);
			return (state == State.PAUSED || state == State.READY);
		}
	}

	public boolean isStopped() {
		synchronized(lock) {
			if (pipeline == null)
				return true;
			final State state = pipeline.getState(0L);
			return (state == State.NULL || state == State.READY);
		}
	}

	public boolean isPlaying() {
		synchronized(lock) {
			if (pipeline == null)
				return true;
			final State state = pipeline.getState(0L);
			return (state == State.PLAYING);
		}
	}

	public int getRepeatCount() {
		return currentRepeatCount;
	}

	public boolean isRepeatingForever() {
		return currentRepeatCount == REPEAT_FOREVER;
	}

	public float getActualFPS() {
		synchronized(lock) {
			if (pipeline == null)
				return 0.0f;
			return actualFPS;
		}
	}

	public URI getRequestedURI() {
		return currentURI;
	}

	public int getRequestedFPS() {
		synchronized(lock) {
			if (pipeline == null)
				return DEFAULT_FPS;
			return currentFPS;
		}
	}

	public long getPosition() {
		synchronized(lock) {
			if (pipeline == null)
				return 0L;
			final State state = pipeline.getState(0L);
			if (state != State.PLAYING || state != State.PAUSED)
				return 0L;
			return pipeline.queryPosition(TimeUnit.MILLISECONDS);
		}
	}

	public long getDuration() {
		synchronized(lock) {
			if (pipeline == null)
				return 0L;
			final State state = pipeline.getState(0L);
			if (state != State.PLAYING || state != State.PAUSED)
				return 0L;
			return pipeline.queryDuration(TimeUnit.MILLISECONDS);
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
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

	private static String createColorspaceFilter(int fps) {
		final String framerate = (fps == DEFAULT_FPS ? null : ", framerate=" + fps + "/1");
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
		void videoDimensionsNegotiated(final MediaComponent source, final int videoWidth, final int videoHeight);
	}

	public static interface IErrorListener {
		void handleError(final MediaComponent source, final URI uri, final ErrorType errorType, final int code, final String message);
	}

	public static interface IMediaEventListener {
		void mediaPaused(final MediaComponent source);
		void mediaContinued(final MediaComponent source);
		void mediaStopped(final MediaComponent source);
		void mediaPlayed(final MediaComponent source);
	}

	public static interface IPositionListener {
		void positionChanged(final MediaComponent source, final int percent, final long position, final long duration);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Adapters">
	public static abstract class VideoCapsListenerAdapter implements IVideoCapsListener {
		@Override
		public void videoDimensionsNegotiated(final MediaComponent source, int videoWidth, int videoHeight) {
		}
	}

	public static abstract class ErrorListenerAdapter implements IErrorListener {
		@Override
		public void handleError(final MediaComponent source, URI uri, ErrorType errorType, int code, String message) {
		}
	}

	public static abstract class MediaEventListenerAdapter implements IMediaEventListener {
		@Override
		public void mediaPaused(MediaComponent source) {
		}

		@Override
		public void mediaContinued(MediaComponent source) {
		}

		@Override
		public void mediaStopped(MediaComponent source) {
		}

		@Override
		public void mediaPlayed(MediaComponent source) {
		}
	}

	public static class PositionListenerAdapter implements IPositionListener {
		@Override
		public void positionChanged(MediaComponent source, final int percent, long position, long duration) {
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
				positionTimer = Gst.getScheduledExecutorService().scheduleAtFixedRate(positionUpdateRunnable, 1, 1, TimeUnit.SECONDS);
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
			synchronized(lock) {
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

				//Get fourcc


				//Convert to RGB using the provided direct buffer
				final IntBuffer rgb = convertToRGB(buffer.getByteBuffer(), width, height, struct.getName(), struct.hasField("format") ? struct.getFourccString("format") : null);
				if (rgb == null)
					return null;

				int[] pixels = new int[rgb.remaining()];
				ImageData imageData = new ImageData(width, height, 24, new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));
				rgb.get(pixels, 0, rgb.remaining());
				imageData.setPixels(0, 0, pixels.length, pixels, 0);

				return imageData;
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
			synchronized(lock) {
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
			}
		} catch(Throwable t) {
			return null;
		} finally {
			if (buffer != null)
				buffer.dispose();
		}
	}
	//</editor-fold>

	public boolean seekToBeginning() {
		synchronized(lock) {
			if (pipeline == null)
				return false;

			State state;
			if ((state = pipeline.getState(0L)) != State.NULL && state != State.READY) {
				pipeline.setState(State.READY);
				if (pipeline.getState(2000L, TimeUnit.MILLISECONDS) != State.READY)
					return false;
			}

			pipeline.setState(State.PLAYING);
			return true;
		}
	}

	public boolean adjustPlaybackRate(final double rate) {
		if (rate == 0.0f)
			return pause();
		synchronized(lock) {
			if (pipeline == null)
				return false;
			return seek(rate, pipeline.queryPosition(Format.TIME));
		}
	}

	public boolean seek(final long positionNanoSeconds) {
		return seek(DEFAULT_RATE, positionNanoSeconds);
	}

	public boolean seek(final double rate, final long positionNanoSeconds) {
		return segmentSeek(rate, positionNanoSeconds);
	}

	private boolean segmentSeek(final double rate, final long positionNanoSeconds) {
		synchronized(lock) {
			if (pipeline == null)
				return false;

			if (pipeline.getState(0L) != State.PAUSED) {
				pipeline.setState(State.PAUSED);
				if (pipeline.getState(2000L, TimeUnit.MILLISECONDS) != State.PAUSED)
					return false;
			}

			final boolean forwards = (rate >= 0.0);

			isSeeking.set(true);
			seekingPos = positionNanoSeconds;
			seekingRate = rate;

			Gst.getExecutor().execute(new Runnable() {
				@Override
				public void run() {
					synchronized(lock) {
						if (pipeline == null)
							return;

						long stop = (forwards ? positionNanoSeconds + SEEK_STOP_DURATION : Math.max(0, positionNanoSeconds - SEEK_STOP_DURATION));
						pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, forwards ? positionNanoSeconds : stop, SeekType.SET, forwards ? stop : positionNanoSeconds);
						pipeline.setState(State.PLAYING);
					}
				}
			});
		}
		return true;
	}

	private void segmentDone(final long positionNanoSeconds) {
		final boolean forwards = (seekingRate >= 0.0);
		final boolean trickmode = (seekingRate > 1.0 || seekingRate < 1.0);

		Gst.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				pipeline.seek(seekingRate, Format.TIME, (trickmode ? SEEK_FLAG_SKIP : 0) | SeekFlags.NONE | SeekFlags.FLUSH | SeekFlags.KEY_UNIT, SeekType.SET, forwards ? positionNanoSeconds : 0, SeekType.SET, forwards ? -1 : positionNanoSeconds);
				pipeline.getState(SEEK_STOP_DURATION, TimeUnit.NANOSECONDS);
				display.asyncExec(seekFinishedRunnable);
			}
		});
	}

	private void seekFinished() {
		isSeeking.set(false);
	}

	public boolean stepForward() {
		return false;

//			synchronized(lock) {
//				if (pipeline == null)
//					return false;
//
//				pipeline.setState(State.PAUSED);
//				return pipeline.sendEvent(new StepEvent(Format.BUFFERS, 1L, 1.0D, true, false));
//			}
	}

	public boolean stepBackward() {
		synchronized(lock) {
			if (pipeline == null)
				return false;
			return false;
			//pipeline.setState(State.PAUSED);
			//return pipeline.sendEvent(new StepEvent(Format.BUFFERS, 1L, 1.0D, true, false));
		}
	}

	public boolean pause() {
		synchronized(lock) {
			if (pipeline == null)
				return false;
			State state;
			if ((state = pipeline.getState(0L)) == State.PAUSED || state == State.NULL || state == State.READY)
				return true;
			pipeline.setState(State.PAUSED);
		}
		return true;
	}

	public boolean unpause() {
		synchronized(lock) {
			if (pipeline == null)
				return false;

			State state;
			if ((state = pipeline.getState(0L)) == State.PLAYING)
				return true;
			if (state != State.PAUSED)
				return seekToBeginning();

			pipeline.setState(State.PLAYING);
		}
		return true;
	}

	public boolean stop() {
		synchronized(lock) {
			if (pipeline == null)
				return true;

			State state;
			if ((state = pipeline.getState(0L)) == State.NULL || state == State.READY)
				return true;

			pipeline.setState(State.READY);
		}
		this.redraw();
		return true;
	}

	public boolean play(File file) {
		if (file == null)
			return false;
		return play(file.toURI());
		//return play("http://www.warwick.ac.uk/newwebcam/cgi-bin/webcam.pl?dummy=garb");
	}

	public boolean play(String uri) {
		if (StringUtil.isNullOrEmpty(uri))
			return false;
		try {
			return play(new URI(uri));
		} catch(URISyntaxException e) {
			return false;
		}
	}

	public boolean play(final URI uri) {
		return play(DEFAULT_FPS, uri);
	}

	public boolean play(final int fps, final URI uri) {
		return play(false, fps, uri);
	}

	public boolean play(final int repeat, final int fps, final URI uri) {
		return play(false, repeat, fps, uri);
	}

	public boolean play(final boolean buffering, final int fps, final URI uri) {
		return play(buffering, DEFAULT_REPEAT_COUNT, fps, uri);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="The Meat">
	public boolean play(final boolean buffering, final int repeat, final int fps, final URI uri) {
		if (uri == null)
			return false;

		final int checked_fps = (fps >= MINIMUM_FPS ? fps : DEFAULT_FPS);

		synchronized(lock) {
			if (pipeline != null)
				pipeline.setState(State.NULL);

			try {
				if (!latch.await(2000L, TimeUnit.MILLISECONDS))
					return false;
			} catch(InterruptedException ie) {
				return false;
			}

			//gst-launch filesrc/souphttpsrc location=http://.../video.avi ! queue ! decodebin2 use-buffering=false name=dec
			//    dec. ! [ queue ! audioconvert ! audioresample ! autoaudiosink ]
			//    dec. ! [ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]
			pipeline = new Pipeline("pipeline");
			pipeline.setAutoFlushBus(true);

			//<editor-fold defaultstate="collapsed" desc="URI element">
			final String uriString = uriString(uri);
			final Pointer ptr = GstURIAPI.GSTURI_API.gst_element_make_from_uri(URIType.SRC, uriString, "uriSrc");
			if (ptr == null || ptr.equals(Pointer.NULL))
				return false;
			final Element urisrc = GstObject.objectFor(ptr, Element.class);
			if (!"file".equals(uri.getScheme()))
				urisrc.set("location", uriString);
			else
				//The filesrc element needs to be just the path without the scheme ("file://")
				urisrc.set("location", new File(uri).getAbsolutePath());
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Queue">
			final Element decodeQueue = ElementFactory.make("queue", "decodeQueue");
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Decodebin2 element">
			final DecodeBin2 decodebin2 = new DecodeBin2((String) null);
			//decodebin2.set("max-size-time", 500000000L); //500 milliseconds
			//decodebin2.set("max-size-bytes", 1024);
			decodebin2.set("use-buffering", buffering);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Link elements">
			pipeline.addMany(urisrc, decodeQueue, decodebin2);
			Element.linkMany(urisrc, decodeQueue, decodebin2);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Create audio output">
			/* create audio output */
			final Bin audioBin = new Bin("Audio Bin");

			final Element audioQueue = ElementFactory.make("queue", "audioQueue");
			final Element audioConvert = ElementFactory.make("audioconvert", "audioConvert");
			final Element audioResample = ElementFactory.make("audioresample", "audioResample");
			final Element audioSink = ElementFactory.make(audioElement, "sink");

			audioBin.addMany(audioQueue, audioConvert, audioResample, audioSink);
			Element.linkMany(audioQueue, audioConvert, audioResample, audioSink);
			audioBin.addPad(new GhostPad("sink", audioQueue.getStaticPad("sink")));
			pipeline.add(audioBin);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Create video output">
			//Create video output
			final Bin videoBin = new Bin("Video Bin");

			//<editor-fold defaultstate="collapsed" desc="Tee">
			//final Element teeQueue = ElementFactory.make("queue", "teeQueue");
			//final Element teeVideo = ElementFactory.make("tee", "videoTee");
			//videoBin.addMany(teeQueue, teeVideo);
			//Element.linkMany(teeQueue, teeVideo);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Video">
			final Element videoQueue = ElementFactory.make("queue", "videoQueue");
			final Element videoRate = ElementFactory.make("videorate", "videoRate");
			videoRate.set("silent", true);
			final Element videoColorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
			final Element videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
			videoCapsFilter.setCaps(Caps.fromString(createColorspaceFilter(checked_fps))); //framerate=25/1 means 25 FPS
			final Element videoScale = ElementFactory.make("videoscale", "videoScale");
			final Element videoSink = ElementFactory.make(videoElement, "videoSink");
			videoBin.addMany(videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
			Element.linkMany(/*teeVideo,*/ videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Thumbnail">
			/*
			//Older attempt by using a tee and getting auto colorspace conversion
			final Element thumbnailQueue = ElementFactory.make("queue", "thumbnailQueue");
			final Element thumbnailVideoRate = ElementFactory.make("videorate", "thumbnailVideoRate");
			thumbnailVideoRate.set("silent", true);
			final Element thumbnailColorspace = ElementFactory.make("ffmpegcolorspace", "thumbnailColorspace");
			final Element thumbnailCapsFilter = ElementFactory.make("capsfilter", "thumbnailCapsFilter");
			//final int rate = (int)Math.ceil((1000.0D / (double)interval) * 1000.0D);
			thumbnailCapsFilter.setCaps(Caps.fromString("video/x-raw-rgb, bpp=32, depth=24, framerate=5/1")); // 1/60 = once every minute, 25/1 = 25 times a second
			final AppSink thumbnailAppSink = (AppSink)ElementFactory.make("appsink", "thumbnailAppSink");
			thumbnailAppSink.set("emit-signals", true);
			thumbnailAppSink.set("sync", false);
			thumbnailAppSink.set("async", true);
			thumbnailAppSink.set("max-buffers", 1);
			thumbnailAppSink.set("drop", true);
			thumbnailAppSink.connect(new AppSink.NEW_BUFFER() {
				private long next = 0L;

				@Override
				public void newBuffer(Element elem, Pointer userData) {
					if (System.currentTimeMillis() < next)
						return;

					next = System.currentTimeMillis() + interval;

					final Buffer buffer = thumbnailAppSink.pullBuffer();
					final Caps caps = buffer.getCaps();
					final Structure struct = caps.getStructure(0);
					final int width = struct.getInteger("width");
					final int height = struct.getInteger("height");
					if (width < 1 || height < 1)
						return;

					//use direct buffer
					final IntBuffer rgb = buffer.getByteBuffer().asIntBuffer();

					//Do what you need here
					System.out.println("New frame: " + width + "x" + height);

					//Cleanup ASAP
					buffer.dispose();
				}
			});
			videoBin.addMany(thumbnailQueue, thumbnailVideoRate, thumbnailColorspace, thumbnailCapsFilter, thumbnailAppSink);
			Element.linkMany(teeVideo, thumbnailQueue, thumbnailVideoRate, thumbnailColorspace, thumbnailCapsFilter, thumbnailAppSink);
			/**/
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Configure video sink">
			//videoSink.set("force-aspect-ratio", true);
			videoSink.set("show-preroll-frame", true);
			final Pad videoSinkPad = videoSink.getStaticPad("sink");
			videoSinkPad.connect("notify::caps", Object.class, null, new GstCallback() {
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
			//</editor-fold>

			videoBin.addPad(new GhostPad("sink", /*teeQueue*/videoQueue.getStaticPad("sink")));
			pipeline.add(videoBin);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="XOverlay">
			xoverlay = CustomXOverlay.wrap(videoSink);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Configure decodebin">
			decodebin2.connect(new DecodeBin2.NEW_DECODED_PAD() {
				@Override
				public void newDecodedPad(Element elem, Pad pad, boolean last) {
					/* only link once */
					if (pad.isLinked())
						return;

					/* check media type */
					Caps caps = pad.getCaps();
					Structure struct = caps.getStructure(0);
					if (struct.getName().startsWith("audio/")) {
						//System.out.println("Linking audio pad: " + struct.getName());
						pad.link(audioBin.getStaticPad("sink"));
					} else if (struct.getName().startsWith("video/")) {
						//System.out.println("Linking video pad: " + struct.getName());
						pad.link(videoBin.getStaticPad("sink"));
					} else {
						//System.out.println("Unknown pad [" + struct.getName() + "]");
					}
				}
			});
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Configure bus">
			final Bus bus = pipeline.getBus();
			bus.connect(new Bus.STATE_CHANGED() {
				@Override
				public void stateChanged(GstObject source, State oldState, State newState, State pendingState) {
					if (newState == State.NULL && pendingState == State.NULL) {
						synchronized(lock) {
							pipeline.dispose();
							pipeline = null;
							currentVideoSink = null;
						}
						display.asyncExec(redrawRunnable);
						latch.countDown();
					}

					switch (newState) {
						case PLAYING:
							if (currentState == State.NULL || currentState == State.PAUSED) {
								currentState = State.PLAYING;
								fireMediaEventStarted();
							}
							break;
						case PAUSED:
							if (currentState == State.PLAYING) {
								fireMediaEventPaused();
								currentState = State.PAUSED;
							}
							break;
						case NULL:
						case READY:
							if (currentState == State.PLAYING) {
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
					fireHandleError(uri, ErrorType.fromNativeValue(code), code, message);
				}
			});
			bus.connect(new Bus.EOS() {
				@Override
				public void endOfStream(GstObject source) {
					if (currentRepeatCount == REPEAT_FOREVER || (currentRepeatCount > 0 && numberOfRepeats++ < currentRepeatCount)) {
						seekToBeginning();
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
					if (buffering) {
						if (percent < 100) {
							pipeline.setState(State.PAUSED);
						} else if (percent >= 100) {
							pipeline.setState(State.PLAYING);
						}
					}
				}
			});
			bus.connect(new Bus.SEGMENT_DONE() {
				@Override
				public void segmentDone(GstObject source, Format format, final long position) {
					display.syncExec(new Runnable() {
						@Override
						public void run() {
							MediaComponent.this.segmentDone(position);
						}
					});
				}
			});
			if (!Sys.isOSFamily(OSFamily.Windows)) {
				bus.setSyncHandler(new BusSyncHandler() {
					@Override
					public BusSyncReply syncMessage(Message msg) {
						Structure s = msg.getStructure();
						if (s == null || !s.hasName("prepare-xwindow-id"))
							return BusSyncReply.PASS;
						 xoverlay.setWindowID(MediaComponent.this);
						return BusSyncReply.DROP;
					}
				});
			} else {
				xoverlay.setWindowID(MediaComponent.this);
			}
			//</editor-fold>

			//Reset these values
			fullVideoWidth = 0;
			fullVideoHeight = 0;
			numberOfRepeats = 0;
			actualFPS = 0.0f;

			//Save these values
			currentBuffering = buffering;
			currentRepeatCount = repeat;
			currentFPS = fps;
			currentURI = uri;

			currentVideoSink = videoSink;
		}

		//Start playing
		pipeline.setState(State.PLAYING);

		return true;
	}
	//</editor-fold>
}

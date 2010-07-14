
package ossbuild.media.gstreamer.swt;

import org.eclipse.swt.events.PaintEvent;
import ossbuild.media.MediaType;
import ossbuild.media.Scheme;
import ossbuild.media.IMediaRequest;
import com.sun.jna.Pointer;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
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
import org.gstreamer.Segment;
import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;
import org.gstreamer.Structure;
import org.gstreamer.elements.FakeSink;
import org.gstreamer.event.BusSyncHandler;
import org.gstreamer.event.StepEvent;
import org.gstreamer.lowlevel.GstAPI.GstCallback;
import org.gstreamer.swt.overlay.SWTOverlay;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.media.MediaRequest;
import ossbuild.media.MediaRequestType;
import ossbuild.media.gstreamer.ErrorType;
import ossbuild.media.gstreamer.elements.VideoTestSrcPattern;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public abstract class MediaComponent extends SWTMediaComponent {
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

	public static final String
		  DEFAULT_VIDEO_ELEMENT
		, DEFAULT_AUDIO_ELEMENT
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
	protected final Map<Pipeline, Map<State, Queue<Runnable>>> stateQueue = new HashMap<Pipeline, Map<State, Queue<Runnable>>>(2);

	protected final Runnable redrawRunnable;
	protected final Runnable xoverlayRunnable;
	protected final PaintListener paintListener;
	//private final Runnable seekFinishedRunnable;

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
	private boolean hasMultipartDemux = false;
	private int videoWidth = 0;
	private int videoHeight = 0;
	private float actualFPS;
	private boolean emitPositionUpdates = true;
	private boolean currentLiveSource;
	private int currentRepeatCount;
	private int numberOfRepeats;
	private boolean maintainAspectRatio = true;
	private double currentRate = DEFAULT_RATE;
	private long bufferSize = DEFAULT_BUFFER_SIZE;
	private long lastPosition = 0L;
	private long lastDuration = 0L;
	private MediaType mediaType = MediaType.Unknown;
	private ImageData singleImage = null;
	private int volume = 100;
	private boolean muted = false;
	protected IMediaRequest mediaRequest = null;
	protected volatile State currentState = State.NULL;

	private AtomicBoolean isSeeking = new AtomicBoolean(false);
	//private long seekingPos = 0L;
	//private double seekingRate = DEFAULT_RATE;

	private List<IErrorListener> errorListeners;
	private final Object errorListenerLock = new Object();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		String videoElement;
		String audioElement;
		switch (Sys.getOSFamily()) {
			case Windows:
				//videoElement = "dshowvideosink";
				//videoElement = "directdrawsink";
				videoElement = "d3dvideosink";
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

		this.nativeHandle = SWTOverlay.handle(this);
		this.display = getDisplay();
		this.audioElement = audioElement;
		this.videoElement = videoElement;
		this.paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (mediaType != MediaType.Image || singleImage == null) {
					expose();
					return;
				}
				paintImage(e.gc, singleImage);
			}
		};

		this.redrawRunnable = new Runnable() {
			@Override
			public void run() {
				if (!isDisposed())
					redraw();
			}
		};
		this.positionUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				onPositionUpdate();
			}
		};
		this.xoverlayRunnable = new Runnable() {
			@Override
			public void run() {
				synchronized(display) {
					xoverlay.setWindowID(MediaComponent.this);
				}
			}
		};

		//<editor-fold defaultstate="collapsed" desc="SWT">
		this.setLayout(new FillLayout());
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
				stop();
				if (pipeline != null) {
					pipeline.setState(State.NULL);
					if (pipeline.getState(10000L, TimeUnit.MILLISECONDS) == State.NULL)
						pipeline.dispose();
					pipeline = null;
				}
			}
		});
		//</editor-fold>

		init();
	}

	protected void init() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	public void Dispose() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
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
		return videoWidth;
	}

	@Override
	public int getVideoHeight() {
		return videoHeight;
	}

	@Override
	public boolean isMediaAvailable() {
		lock.lock();
		try {
			return (pipeline != null && pipeline.getState(0L) != State.NULL);
		} finally {
			lock.unlock();
		}
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
	public boolean isSeekable() {
		return !currentLiveSource && emitPositionUpdates && !hasMultipartDemux && mediaType != MediaType.Image && mediaType != MediaType.Unknown;
	}

	@Override
	public int getRepeatCount() {
		return currentRepeatCount;
	}

	public boolean isRepeatingForever() {
		return currentRepeatCount == IMediaRequest.REPEAT_FOREVER;
	}

	@Override
	public float getVideoFPS() {
		lock.lock();
		try {
			if (pipeline == null)
				return 0.0f;
			return actualFPS;
		} finally {
			lock.unlock();
		}
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
	public boolean isAudioAvailable() {
		return this.hasAudio;
	}

	@Override
	public boolean isVideoAvailable() {
		return this.hasVideo;
	}

	@Override
	public long getBufferSize() {
		return bufferSize;
	}

	@Override
	public boolean isAspectRatioMaintained() {
		return maintainAspectRatio;
	}

	@Override
	public IMediaRequest getMediaRequest() {
		lock.lock();
		try {
			return mediaRequest;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public MediaType getMediaType() {
		return mediaType;
	}

	@Override
	public void setBufferSize(long size) {
		this.bufferSize = size;
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

	private static String createColorspaceFilter(final MediaComponent src, final float fps) {
		final String framerate = (fps == IMediaRequest.DEFAULT_FPS || src.currentLiveSource ? null : ", framerate=" + (int)fps + "/1");
		final StringBuilder sb = new StringBuilder(256);

		sb.append(";video/x-raw-rgb, bpp=32, depth=24");
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

	protected static boolean isParser(final Element elem) {
		return isParser(elem.getFactory().getKlass());
	}

	protected static boolean isDecoder(final Element elem) {
		return isDecoder(elem.getFactory().getKlass());
	}
	
	protected static boolean isImage(final Element elem) {
		return isImage(elem.getFactory().getKlass());
	}

	protected static boolean isGeneric(final String factoryClass) {
		return (factoryClass.equals("Generic") || factoryClass.contains("Generic/") || factoryClass.contains("/Generic"));
	}

	protected static boolean isSource(final String factoryClass) {
		return (factoryClass.contains("Source/") || factoryClass.contains("/Source"));
	}

	protected static boolean isParser(final String factoryClass) {
		return (factoryClass.contains("/Demuxer") || factoryClass.contains("Demuxer/"));
	}

	protected static boolean isDecoder(final String factoryClass) {
		return (factoryClass.contains("/Decoder") || factoryClass.contains("Decoder/"));
	}
	
	protected static boolean isImage(final String factoryClass) {
		return (factoryClass.contains("/Image") || factoryClass.contains("Image/"));
	}

	protected static boolean determineIfSingleImage(final Bin bin) {
		//Examine all the elements. Look at the factory class
		//which will look like:
		//    Codec/Decoder/Image (decoder, image)
		//    Codec/Demuxer       (parser)
		//    Source/Network      (source)
		//    Generic             (generic)
		//    Generic/Bin/Decoder (generic, bin)
		//If there is exactly one decoder and zero demuxers/parsers,
		//and there's an image, it's safe (most of the time) to
		//assume it's an image we're looking at.
		String factoryClass;
		int decoderCount = 0;
		boolean imageFound = false;
		for(Element elem : bin.getElementsRecursive()) {
			factoryClass = elem.getFactory().getKlass();
			if (isGeneric(factoryClass) || isSource(factoryClass))
				continue;
			if (isParser(factoryClass) || (isDecoder(factoryClass) && ++decoderCount > 1))
				return false;
			if (isImage(factoryClass))
				imageFound = true;
		}
		return imageFound;
	}

	protected Map<State, Queue<Runnable>> createEmptyStateQueue() {
		//Create a new queue for each state
		Map<State, Queue<Runnable>> newStateQueue = new HashMap<State, Queue<Runnable>>(State.values().length);
		for(State s : State.values())
			newStateQueue.put(s, new ConcurrentLinkedQueue<Runnable>());
		return newStateQueue;
	}

	protected State currentState() {
		return pipeline.getState(0L);
	}

	protected State currentState(long timeout) {
		return pipeline.getState(timeout, TimeUnit.MILLISECONDS);
	}

	protected StateChangeReturn changeState(State state) {
		return changeState(pipeline, state, 0L, null);
	}

	protected StateChangeReturn changeState(State state, long timeout) {
		return changeState(pipeline, state, timeout, null);
	}

	protected StateChangeReturn changeState(State state, Runnable action) {
		return changeState(pipeline, state, 0L, action);
	}

	protected StateChangeReturn changeState(State state, long timeout, Runnable action) {
		return changeState(pipeline, state, timeout, action);
	}

	protected StateChangeReturn changeState(Pipeline pipeline, State state) {
		return changeState(pipeline, state, 0L, null);
	}

	protected StateChangeReturn changeState(Pipeline pipeline, State state, long timeout) {
		return changeState(pipeline, state, timeout, null);
	}

	protected StateChangeReturn changeState(Pipeline pipeline, State state, Runnable action) {
		return changeState(pipeline, state, 0L, action);
	}

	protected StateChangeReturn changeState(Pipeline pipeline, State state, long timeout, Runnable action) {
		if (pipeline == null)
			return StateChangeReturn.FAILURE;
		stateAction(pipeline, state, action);
		if (timeout <= 0L) {
			return pipeline.setState(state);
		} else {
			pipeline.setState(state);
			if (pipeline.getState(timeout, TimeUnit.MILLISECONDS) == state)
				return StateChangeReturn.SUCCESS;
			else
				return StateChangeReturn.FAILURE;
		}
	}

	protected void stateAction(State state, Runnable action) {
		stateAction(pipeline, state, action);
	}

	protected void stateAction(Pipeline pipeline, State state, Runnable action) {
		if (action != null) {
			if (!stateQueue.containsKey(pipeline))
				stateQueue.put(pipeline, createEmptyStateQueue());
			stateQueue.get(pipeline).get(state).add(action);
		}
	}

	protected Queue<Runnable> actionsForState(Pipeline pipeline, State state) {
		if (!stateQueue.containsKey(pipeline))
			return null;
		return stateQueue.get(pipeline).get(state);
	}
	
	protected void clearStateActions(Pipeline pipeline, State state) {
		Queue<Runnable> actions = actionsForState(pipeline, state);
		if (actions != null)
			actions.clear();
	}

	protected void clearAllStateActions(Pipeline pipeline) {
		Map<State, Queue<Runnable>> map = stateQueue.get(pipeline);
		if (map == null)
			return;
		map.clear();
		stateQueue.remove(pipeline);
	}

	protected void clearAllPipelineStateActions() {
		stateQueue.clear();
	}

	protected void insertPaintListener() {
		if (!isUIThread()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					insertPaintListener();
				}
			});
			return;
		}
		addPaintListener(paintListener);
	}

	protected void clearPaintListener() {
		if (!isUIThread()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					clearPaintListener();
				}
			});
			return;
		}
		removePaintListener(paintListener);
	}

	protected void asyncRedraw() {
		display.asyncExec(redrawRunnable);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Interfaces">
	public static interface IErrorListener {
		void handleError(final MediaComponent source, final IMediaRequest request, final ErrorType errorType, final int code, final String message);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Adapters">
	public static abstract class ErrorListenerAdapter implements IErrorListener {
		@Override
		public void handleError(final MediaComponent source, final IMediaRequest request, ErrorType errorType, int code, String message) {
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
				if (currentVideoSink == null)
					return null;

				final Pointer ptr = currentVideoSink.getPointer("last-buffer");
				if (ptr == null)
					return null;
				buffer = MiniObject.objectFor(ptr, Buffer.class, false);
				if (buffer == null)
					return null;

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
		if (buffer == null)
			return null;
		try {
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
		} catch(Throwable t) {
			return null;
		} finally {
		}
	}

	@Override
	public BufferedImage snapshot() {
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
		if (mediaType == MediaType.Image)
			return true;

		State state;
		lock.lock();
		try {
			if (xoverlay != null && pipeline != null && ((state = currentState()) == State.PLAYING || state == State.PAUSED)) {
				xoverlay.expose();
				return true;
			}
			return false;
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Volume">
	@Override
	public boolean mute() {
		lock.lock();
		try {
			if (pipeline == null || currentAudioVolumeElement == null)
				return false;

			boolean shouldMute = !isMuted();
			currentAudioVolumeElement.set("mute", (muted = shouldMute));
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
		lock.lock();
		try {
			if (pipeline == null || currentAudioVolumeElement == null)
				return false;

			int oldVolume = getVolume();
			int newVolume = Math.max(0, Math.min(100, percent));
			currentAudioVolumeElement.set("volume", (double)(volume = newVolume) / 100.0D);
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
		lock.lock();
		try {
			if (isLiveSource())
				return true;
			if (!seek(currentRate, 0L)) {
				return (changeState(State.READY, new Runnable() {
							@Override
							public void run() {
								onPositionUpdate();
								changeState(State.PLAYING);
							}
						}
					)
					!=
					StateChangeReturn.FAILURE
				);
			}
			return true;
		} finally {
			lock.unlock();
		}
	}

	public boolean seekToBeginningAndPause() {
		lock.lock();
		try {
			if (isLiveSource())
				return pause();
			return (changeState(State.READY, new Runnable() {
						@Override
						public void run() {
							onPositionUpdate();
							changeState(State.PAUSED);
						}
					}
				)
				!=
				StateChangeReturn.FAILURE
			);
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

		lock.lock();
		try {
			if (isLiveSource())
				return false;

			final Segment segment = pipeline.querySegment();
			if (segment != null && rate == segment.getRate())
				return true;

			State state = currentState();

			switch(state) {
				case PLAYING:
					changeState(State.PAUSED, new Runnable() {
						@Override
						public void run() {
							adjustPlaybackRate(rate);
						}
					});
					return true;
				case PAUSED:
					break;
				default:
					return false;
			}

			final boolean forwards = (rate >= 0.0);
			final long positionNanoSeconds = pipeline.queryPosition(Format.TIME);
			//final long stop = (forwards ? positionNanoSeconds + SEEK_STOP_DURATION : Math.max(0, positionNanoSeconds - SEEK_STOP_DURATION));
			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
			final long stop = (forwards ? -1 : 0);
			
			final boolean success = pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, begin, SeekType.SET, stop) && changeState(State.PLAYING) != StateChangeReturn.FAILURE;
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
			if (pipeline == null)
				return false;

			if (isLiveSource())
				return false;
			
			State state = currentState();

			switch(state) {
				case PLAYING:
				case PAUSED:
					break;
				default:
					return false;
			}

			final boolean forwards = (rate >= 0.0);
			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
			final long stop = (forwards ? -1 : 0);

			final boolean success = pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, begin, SeekType.SET, stop);
			changeState(State.PLAYING);

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
		lock.lock();
		try {
			if (pipeline == null)
				return false;
			final State state = currentState();
			if (state != State.PAUSED) {
				changeState(pipeline, State.PAUSED, new Runnable() {
					@Override
					public void run() {
						pipeline.sendEvent(new StepEvent(Format.BUFFERS, 1L, 1.0D, true, false));
					}
				});
				return true;
			}
			return pipeline.sendEvent(new StepEvent(Format.BUFFERS, 1L, 1.0D, true, false));
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean stepBackward() {
		return false;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Pause">
	@Override
	public boolean pause() {
		lock.lock();
		try {
			if (currentState() == State.PAUSED)
				return true;
			return changeState(State.PAUSED) != StateChangeReturn.FAILURE;
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Continue">
	@Override
	public boolean unpause() {
		lock.lock();
		try {
			return (
				changeState(State.PLAYING, 2000L,
					new Runnable() {
						@Override
						public void run() {
							adjustPlaybackRate(currentRate);
						}
					}
				)
				!=
				StateChangeReturn.FAILURE
			);
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Stop">
	@Override
	public boolean stop() {
		lock.lock();
		try {
			if (changeState(State.READY, 2000L) == StateChangeReturn.FAILURE)
				return false;
			singleImage = null;
			asyncRedraw();
			return true;
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
		return playPattern(title, VideoTestSrcPattern.Black);
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
	//<editor-fold defaultstate="collapsed" desc="Cleanup">
	protected void resetPipeline(final Pipeline newPipeline) {
		if (newPipeline != null) {
			do {
				newPipeline.setState(State.NULL);
			} while(newPipeline.getState() != State.NULL);
			cleanup(newPipeline);
			newPipeline.dispose();
			pipeline = null;
			asyncRedraw();
		}
	}
	
	protected void cleanup(final Pipeline newPipeline) {
		//Remove any pending actions for this pipeline
		clearAllStateActions(newPipeline);

		//Clean out audio and video bins
		disposeAudioBin(newPipeline);
		disposeVideoBin(newPipeline);

		//Clean out any videotestsrc that may be in the pipeline
		final Element videoTestSrc = newPipeline.getElementByName("videoTestSrc");
		if (videoTestSrc != null) {
			newPipeline.remove(videoTestSrc);
			videoTestSrc.dispose();
		}
	}

	@SuppressWarnings("empty-statement")
	protected synchronized boolean disposeAudioBin(final Pipeline newPipeline) {
		final Bin existingAudioBin = (Bin)newPipeline.getElementByName("audioBin");

		if (existingAudioBin == null)
			return true;

		existingAudioBin.setState(State.NULL);
		while(existingAudioBin.getState() != State.NULL)
			;
		

		for(Pad p : existingAudioBin.getPads()) {
			existingAudioBin.removePad(p);
			p.dispose();
		}

		for(Element e : existingAudioBin.getElements()) {
			existingAudioBin.unlink(e);
			existingAudioBin.remove(e);
			e.dispose();
		}

		newPipeline.remove(existingAudioBin);
		existingAudioBin.dispose();

		return true;
	}

	@SuppressWarnings("empty-statement")
	protected synchronized boolean disposeVideoBin(final Pipeline newPipeline) {
		final Bin existingVideoBin = (Bin)newPipeline.getElementByName("videoBin");

		if (existingVideoBin == null)
			return true;

		existingVideoBin.setState(State.NULL);
		while(existingVideoBin.getState() != State.NULL)
			;

		for(Pad p : existingVideoBin.getPads()) {
			existingVideoBin.removePad(p);
			p.dispose();
		}

		for(Element e : existingVideoBin.getElements()) {
			existingVideoBin.unlink(e);
			existingVideoBin.remove(e);
			e.dispose();
		}

		newPipeline.remove(existingVideoBin);
		existingVideoBin.dispose();

		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Create">
	protected synchronized Pipeline createPipeline(final IMediaRequest newRequest) {
		//gst-launch uridecodebin use-buffering=false name=dec location=http://.../video.avi
		//    dec. ! [ queue ! audioconvert ! audioresample ! autoaudiosink ]
		//    dec. ! [ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]

		final Pipeline newPipeline = new Pipeline("pipeline");
		final Bin uridecodebin = (Bin)ElementFactory.make("uridecodebin", "uridecodebin");
		
		uridecodebin.set("use-buffering", false);
		uridecodebin.set("download", false);
		uridecodebin.set("buffer-duration", TimeUnit.MILLISECONDS.toNanos(500L));
		uridecodebin.set("uri", uriString(newRequest.getURI()));

		newPipeline.add(uridecodebin);

		//<editor-fold defaultstate="collapsed" desc="UriDecodeBin Signals">
		uridecodebin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element elem, Pad pad) {
				onPadAdded(newRequest, newPipeline, uridecodebin, pad);
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

		//<editor-fold defaultstate="collapsed" desc="Bus Signals">
		final Bus bus = newPipeline.getBus();
		bus.connect(new Bus.STATE_CHANGED() {
			@Override
			public void stateChanged(GstObject source, State oldState, State newState, State pendingState) {
				if (source != newPipeline)
					return;
				onStateChanged(newPipeline, uridecodebin, oldState, newState, pendingState);
			}
		});
		bus.connect(new Bus.ERROR() {
			@Override
			public void errorMessage(GstObject source, int code, String message) {
				onError(newRequest, newPipeline, code, message);
			}
		});
		bus.connect(new Bus.SEGMENT_DONE() {
			@Override
			public void segmentDone(GstObject source, Format format, long position) {
				onSegmentDone(newPipeline);
			}
		});
		bus.connect(new Bus.EOS() {
			@Override
			public void endOfStream(GstObject source) {
				onEOS(newPipeline);
			}
		});
		bus.connect(new Bus.BUFFERING() {
			@Override
			public void bufferingData(GstObject source, int percent) {
				onBuffering(newPipeline, percent);
			}
		});
		bus.setSyncHandler(new BusSyncHandler() {
			@Override
			public BusSyncReply syncMessage(Message msg) {
				Structure s = msg.getStructure();
				if (s == null || !s.hasName("prepare-xwindow-id"))
					return BusSyncReply.PASS;
				xoverlay.setWindowID(nativeHandle);
				return BusSyncReply.DROP;
			}
		});
		//</editor-fold>

		return newPipeline;
	}

	protected Element createImageSink(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedVideoSink) {
		final FakeSink videoSink = new FakeSink("videoSink");
		videoSink.set("signal-handoffs", true);
		videoSink.getStaticPad("sink").connect("notify::caps", Object.class, null, new GstCallback() {
			@SuppressWarnings("unused")
			public boolean callback(Pad pad, Pointer unused, Pointer dynamic) {
				return onNotifyCaps(newPipeline, pad);
			}
		});
		//videoSink.connect(new FakeSink.PREROLL_HANDOFF() {
		//	@Override
		//	public void prerollHandoff(FakeSink fakesink, Buffer buffer, Pad pad, Pointer user_data) {
		//		onImageSinkHandoff(newPipeline, buffer);
		//	}
		//});
		videoSink.connect(new Element.HANDOFF() {
			@Override
			public void handoff(Element element, Buffer buffer, Pad pad) {
				onImageSinkHandoff(newPipeline, buffer);
			}
		});
		return videoSink;
	}

	protected Element createVideoSink(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedVideoSink) {
		final Element videoSink = ElementFactory.make(suggestedVideoSink, "videoSink");
		videoSink.set("show-preroll-frame", true);
		videoSink.getStaticPad("sink").connect("notify::caps", Object.class, null, new GstCallback() {
			@SuppressWarnings("unused")
			public boolean callback(Pad pad, Pointer unused, Pointer dynamic) {
				return onNotifyCaps(newPipeline, pad);
			}
		});
		return videoSink;
	}

	protected Element createAudioSink(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedAudioSink) {
		return ElementFactory.make(suggestedAudioSink, "audioSink");
	}

	protected Pad createAudioBin(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final Bin audioBin, final Bin uridecodebin, final Pad pad) {

		//[ queue2 ! volume ! audioconvert ! audioresample ! scaletempo ! audioconvert ! audioresample ! autoaudiosink ]

		final Element audioQueue = ElementFactory.make("queue2", "audioQueue");
		final Element audioVolume = ElementFactory.make("volume", "audioVolume");
		final Element audioConvert = ElementFactory.make("audioconvert", "audioConvert");
		final Element audioResample = ElementFactory.make("audioresample", "audioResample");
		final Element audioScaleTempo = ElementFactory.make("scaletempo", "audioScaleTempo");
		final Element audioConvertAfterScaleTempo = ElementFactory.make("audioconvert", "audioConvertAfterScaleTempo");
		final Element audioResampleAfterScaleTempo = ElementFactory.make("audioresample", "audioResampleAfterScaleTempo");
		final Element audioSink = createAudioSink(newMediaType, newRequest, newPipeline, audioElement);

		audioBin.addMany(audioQueue, audioVolume, audioConvert, audioResample, audioScaleTempo, audioConvertAfterScaleTempo, audioResampleAfterScaleTempo, audioSink);
		Element.linkMany(audioQueue, audioVolume, audioConvert, audioResample, audioScaleTempo, audioConvertAfterScaleTempo, audioResampleAfterScaleTempo, audioSink);

		currentAudioSink = audioSink;
		currentAudioVolumeElement = audioVolume;

		//Set this to whatever was previously set
		audioVolume.set("mute", muted);
		audioVolume.set("volume", (double)volume / 100.0D);

		//Element to connect uridecodebin src pad to.
		return audioQueue.getStaticPad("sink");
	}

	protected Pad createVideoBin(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final Bin videoBin, final Bin uridecodebin, final Pad pad) {

		//[ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]

		final float checked_fps = (newRequest.getFPS() >= IMediaRequest.MINIMUM_FPS ? newRequest.getFPS() : IMediaRequest.DEFAULT_FPS);

		final Element videoQueue;
		final Element videoRate;
		final Element videoColorspace;
		final Element videoCapsFilter;
		final Element videoScale;
		final Element videoSink;

		if (newMediaType != MediaType.Image) {
			if (!currentLiveSource) {
				videoQueue = ElementFactory.make("queue2", "videoQueue");
				videoRate = ElementFactory.make("videorate", "videoRate");
				videoColorspace = ElementFactory.make("ffmpegcolorspace", "videoColorspace");
				videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
				videoScale = ElementFactory.make("videoscale", "videoScale");
				videoSink = createVideoSink(newMediaType, newRequest, newPipeline, videoElement);

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
				videoSink = createVideoSink(newMediaType, newRequest, newPipeline, videoElement);

				//videoCapsFilter.setCaps(Caps.fromString(createColorspaceFilter(this, checked_fps))); //framerate=25/1 means 25 FPS

				videoBin.addMany(videoQueue, videoColorspace, /*videoCapsFilter, videoScale,*/ videoSink);
				Element.linkMany(/*teeVideo,*/ videoQueue, videoColorspace, /*videoCapsFilter, videoScale,*/ videoSink);
			}

			currentVideoSink = videoSink;
			xoverlay = SWTOverlay.wrap(videoSink);
			//The paint listener is removed in the play()/playPattern() methods to ensure
			//that it's also removed for audio-only playback. The only time to insert it is
			//when we're drawing the single image ourselves.
		} else {
			videoSink = createImageSink(newMediaType, newRequest, newPipeline, videoElement);
			videoQueue = ElementFactory.make("queue2", "videoQueue");
			videoColorspace = ElementFactory.make("ffmpegcolorspace", "videoColorspace");
			videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
			videoCapsFilter.setCaps(Caps.fromString("video/x-raw-rgb, bpp=32, depth=24"));

			videoBin.addMany(videoQueue, videoColorspace, videoCapsFilter, videoSink);
			Element.linkMany(/*teeVideo,*/ videoQueue, videoColorspace, videoCapsFilter, videoSink);
			
			currentVideoSink = videoSink;
			insertPaintListener();
		}

		return videoQueue.getStaticPad("sink");
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Signals">
	protected void onUriDecodeBinElementAdded(final Pipeline newPipeline, final Bin uridecodebin, final Element element) {
		//<editor-fold defaultstate="collapsed" desc="Validate arguments">
		//We only care to modify the element if we're using a live source
		if (!currentLiveSource || element == null)
			return;
		final String factoryName = element.getFactory().getName();
		//</editor-fold>

		if (factoryName.startsWith("souphttpsrc")) {
			//element.set("do-timestamp", true);
			element.set("blocksize", bufferSize);
		} else if (factoryName.startsWith("neonhttpsrc")) {
			//element.set("do-timestamp", true);
			element.set("blocksize", bufferSize);
		} else if (factoryName.startsWith("wininetsrc")) {
			element.set("blocksize", bufferSize);
		}
	}

	protected void onDecodeBinElementAdded(final Pipeline newPipeline, final Bin uridecodebin, final Bin decodebin, final Element element) {
		//<editor-fold defaultstate="collapsed" desc="Validate arguments">
		//Determine if what we're looking at is a multipartdemux element
		final String factoryName = element.getFactory().getName();
		if (!"multipartdemux".equalsIgnoreCase(factoryName))
			return;
		//</editor-fold>

		try {
			hasMultipartDemux = true;
			
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

	protected void onPadAdded(final IMediaRequest newRequest, final Pipeline newPipeline, final Bin uridecodebin, final Pad pad) {
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

			hasAudio = true;
			if (mediaType == MediaType.Unknown && mediaType != MediaType.Video)
				mediaType = MediaType.Audio;

			//Create audio bin
			final Bin audioBin = new Bin("audioBin");
			audioBin.addPad(new GhostPad("sink", createAudioBin(mediaType, newRequest, newPipeline, audioBin, uridecodebin, pad)));
			newPipeline.add(audioBin);
			pad.link(audioBin.getStaticPad("sink"));

			audioBin.setState(State.PLAYING);
		} else if (padCaps.startsWith("video/")) {
			disposeVideoBin(newPipeline);
			
			hasVideo = true;
			if (mediaType == MediaType.Unknown || mediaType == MediaType.Audio)
				mediaType = (!determineIfSingleImage(uridecodebin) ? MediaType.Video : MediaType.Image);

			//Create video bin
			final Bin videoBin = new Bin("videoBin");
			videoBin.addPad(new GhostPad("sink", createVideoBin(mediaType, newRequest, newPipeline, videoBin, uridecodebin, pad)));
			newPipeline.add(videoBin);
			pad.link(videoBin.getStaticPad("sink"));

			videoBin.setState(State.PLAYING);
		}
	}

	protected void onImageSinkHandoff(final Pipeline newPipeline, final Buffer buffer) {
		singleImage = swtImageDataSnapshot(buffer);
		display.asyncExec(redrawRunnable);
	}

	protected boolean onNotifyCaps(final Pipeline newPipeline, final Pad pad) {
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
			videoWidth = width;
			videoHeight = height;
			fireVideoDimensionsNegotiated(width, height);
		}
		return true;
	}

	@SuppressWarnings("empty-statement")
	protected void onStateChanged(final Pipeline newPipeline, final Bin uridecodebin, final State oldState, final State newState, final State pendingState) {
		//<editor-fold defaultstate="collapsed" desc="Fire state events">
		switch (newState) {
			case PLAYING:
				if (currentState == State.NULL || currentState == State.READY || currentState == State.PAUSED) {
					currentState = State.PLAYING;
					fireMediaEventPlayed();
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
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Run all actions for this state change">
		Queue<Runnable> actions = actionsForState(newPipeline, newState);
		if (!actions.isEmpty()) {
			Runnable r;
			while((r = actions.poll()) != null) {
				try {
					r.run();
				} catch(Throwable t) {
					t.printStackTrace();
				}
			}
		}
		//</editor-fold>
	}

	protected void onBuffering(final Pipeline newPipeline, int percent) {
		if (!currentLiveSource) {
			if (percent < 100) {
				changeState(newPipeline, State.PAUSED);
			} else if (percent >= 100) {
				changeState(newPipeline, State.PLAYING);
			}
		}
	}
	
	protected void onSegmentDone(final Pipeline newPipeline) {
		onEOS(newPipeline);
	}
	
	protected void onEOS(final Pipeline newPipeline) {
		if (mediaType != MediaType.Image && (currentRepeatCount == IMediaRequest.REPEAT_FOREVER || (currentRepeatCount > 0 && numberOfRepeats < currentRepeatCount))) {
			++numberOfRepeats;
			if (!seekToBeginning()) {
				stop();
				unpause();
			}
			return;
		}
		numberOfRepeats = 0;
		changeState(newPipeline, (mediaType != MediaType.Image ? State.READY : State.PAUSED));
		asyncRedraw();
	}

	protected void onPositionUpdate() {
		if (!emitPositionUpdates)
			return;
		if (lock.tryLock()) {
			try {
				if (pipeline != null) {
					//if (!isSeekable())
					//	return;

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

	protected void onError(final IMediaRequest newRequest, final Pipeline newPipeline, int code, String message) {
		resetPipeline(newPipeline);
		fireHandleError(newRequest, ErrorType.fromNativeValue(code), code, message);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Play">
	@SuppressWarnings("empty-statement")
	public boolean playPattern(final String title, final VideoTestSrcPattern pattern) {
		lock.lock();
		try {
			resetPipeline(pipeline);

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
			hasVideo = false;
			hasAudio = false;
			hasMultipartDemux = false;
			videoWidth = 0;
			videoHeight = 0;
			numberOfRepeats = 0;
			actualFPS = 0.0f;
			currentVideoSink = null;
			currentAudioSink = null;
			currentAudioVolumeElement = null;
			maintainAspectRatio = true;
			mediaType = MediaType.Video;
			clearPaintListener();

			//Save these values
			currentLiveSource = false;
			currentRepeatCount = 0;
			currentRate = 1.0D;
			mediaRequest = newRequest;

			fireMediaEventPlayRequested(newRequest);

			final float checked_fps = (newRequest.getFPS() >= IMediaRequest.MINIMUM_FPS ? newRequest.getFPS() : IMediaRequest.DEFAULT_FPS);

			final Pipeline newPipeline = new Pipeline("pipeline");
			final Element videoTestSrc = ElementFactory.make("videotestsrc", "videoTestSrc");
			videoTestSrc.set("pattern", (long)pattern.getNativeValue());

			final Bin videoBin = new Bin("videoBin");
			final Element videoQueue = ElementFactory.make("queue2", "videoQueue");
			final Element videoRate = ElementFactory.make("videorate", "videoRate");
			final Element videoColorspace = ElementFactory.make("ffmpegcolorspace", "videoColorspace");
			final Element videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
			final Element videoScale = ElementFactory.make("videoscale", "videoScale");
			final Element videoSink = createVideoSink(mediaType, newRequest, newPipeline, videoElement);

			videoRate.set("silent", true);
			videoCapsFilter.setCaps(Caps.fromString(createColorspaceFilter(this, checked_fps))); //framerate=25/1 means 25 FPS

			videoBin.addMany(videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
			Element.linkMany(videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);

			videoBin.addPad(new GhostPad("sink", videoQueue.getStaticPad("sink")));
			videoTestSrc.getStaticPad("src").link(videoBin.getStaticPad("sink"));
			videoBin.setState(State.PLAYING);
			
			newPipeline.addMany(videoTestSrc, videoBin);
			Element.linkMany(videoTestSrc, videoBin);
			
			//<editor-fold defaultstate="collapsed" desc="Signals">
			//<editor-fold defaultstate="collapsed" desc="Bus">
			final Bus bus = newPipeline.getBus();
			bus.connect(new Bus.STATE_CHANGED() {
				@Override
				public void stateChanged(GstObject source, State oldState, State newState, State pendingState) {
					if (source != newPipeline)
						return;
					onStateChanged(newPipeline, null, oldState, newState, pendingState);
				}
			});
			bus.connect(new Bus.ERROR() {
				@Override
				public void errorMessage(GstObject source, int code, String message) {
					onError(newRequest, newPipeline, code, message);
				}
			});
			bus.connect(new Bus.SEGMENT_DONE() {
				@Override
				public void segmentDone(GstObject source, Format format, long position) {
					onSegmentDone(newPipeline);
				}
			});
			bus.connect(new Bus.EOS() {
				@Override
				public void endOfStream(GstObject source) {
					onEOS(newPipeline);
				}
			});
			bus.setSyncHandler(new BusSyncHandler() {
				@Override
				public BusSyncReply syncMessage(Message msg) {
					Structure s = msg.getStructure();
					if (s == null || !s.hasName("prepare-xwindow-id"))
						return BusSyncReply.PASS;
					xoverlay.setWindowID(MediaComponent.this.nativeHandle);
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
			changeState(pipeline, State.PLAYING);
			return true;
		} catch(Throwable t) {
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	@SuppressWarnings("empty-statement")
	public boolean play(final IMediaRequest request) {
		if (request == null)
			return false;

		final URI uri = request.getURI();
		if (uri == null)
			return false;
		
		lock.lock();
		try {
			resetPipeline(pipeline);

			//Reset these values
			hasVideo = false;
			hasAudio = false;
			hasMultipartDemux = false;
			videoWidth = 0;
			videoHeight = 0;
			numberOfRepeats = 0;
			actualFPS = 0.0f;
			currentVideoSink = null;
			currentAudioSink = null;
			currentAudioVolumeElement = null;
			mediaType = MediaType.Unknown;
			clearPaintListener();

			//Save these values
			mediaRequest = request;
			currentLiveSource = request.isLiveSource();
			currentRepeatCount = request.getRepeatCount();
			maintainAspectRatio = request.isAspectRatioMaintained();

			currentRate = 1.0D;
			emitPositionUpdates = true;
			
			fireMediaEventPlayRequested(request);

			final Pipeline newPipeline = createPipeline(request);
			pipeline = newPipeline;

			//Start playing
			//Attempts to ensure that we're using segment seeks (which signals SEGMENT_DONE) to look for repeats instead of EOS
			//pipeline.seek(1.0D, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, 0L, SeekType.SET, -1L);
			changeState(newPipeline, State.PAUSED, new Runnable() {
				@Override
				public void run() {
					asyncRedraw();
					//Seek using segments so we have a more graceful transition b/t plays
					if (isSeekable())
						seek(currentRate, 0L);
					changeState(newPipeline, State.PLAYING);
				}
			});
			return true;
		} catch(Throwable t) {
			//t.printStackTrace();
			return false;
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Paint">
	protected void paintImage(GC g, ImageData imgData) {
		Rectangle r = getClientArea();

		g.setForeground(getBackground());
		g.fillRectangle(r);

		g.setAntialias(SWT.ON);

		Image img = new Image(display, imgData);
		g.drawImage(img, 0, 0, imgData.width, imgData.height, 0, 0, r.width, r.height);
		img.dispose();
	}
	//</editor-fold>
	//</editor-fold>
}


package ossbuild.media.gstreamer.swt;

import org.eclipse.swt.events.PaintEvent;
import ossbuild.media.MediaType;
import ossbuild.media.Scheme;
import ossbuild.media.IMediaRequest;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gstreamer.Bin;
import org.gstreamer.Buffer;
import org.gstreamer.Bus;
import org.gstreamer.BusSyncReply;
import org.gstreamer.Caps;
import org.gstreamer.Closure;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
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
import org.gstreamer.Segment;
import org.gstreamer.State;
import org.gstreamer.StateChangeReturn;
import org.gstreamer.Structure;
import org.gstreamer.elements.FakeSink;
import org.gstreamer.elements.PlayBin2;
import org.gstreamer.event.BusSyncHandler;
import org.gstreamer.event.StepEvent;
import org.gstreamer.lowlevel.GType;
import org.gstreamer.lowlevel.GstTypes;
import org.gstreamer.swt.overlay.SWTOverlay;
import ossbuild.OSFamily;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.media.IMediaPlayer;
import ossbuild.media.MediaRequest;
import ossbuild.media.MediaRequestType;
import ossbuild.media.gstreamer.Colorspace;
import ossbuild.media.gstreamer.ErrorType;
import ossbuild.media.gstreamer.elements.VideoTestSrcPattern;
import static org.gstreamer.lowlevel.GSignalAPI.GSIGNAL_API;

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

	public static final boolean
		  DEFAULT_VIDEO_ACCELERATED = false
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

	protected final long nativeHandle;
	protected final Lock lock = new ReentrantLock();
	protected final String videoElement;
	protected final String audioElement;

	protected Pipeline pipeline;
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
	protected volatile State currentState = State.NULL;

	private Canvas imgCanvas;
	private Canvas videoCanvas;
	private Canvas acceleratedVideoCanvas;
	private StackLayout layout;
	protected final Display display;
	private ImageData currentFrame = null;

	protected boolean acceleratedVideo = DEFAULT_VIDEO_ACCELERATED;

	private List<IErrorListener> errorListeners;
	private final Object errorListenerLock = new Object();

	//Do not lazy load b/c we're checking it with each frame we draw, so we want
	//everything to be as fast as possible.
	private List<IUnacceleratedPaintListener> unacceleratedPaintListeners = new CopyOnWriteArrayList<IUnacceleratedPaintListener>();
	private IUnacceleratedPaintListener[] fastUnacceleratedPaintListeners = new IUnacceleratedPaintListener[0];
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	static {
		//Sets a MS-Windows specific AWT property that prevents heavyweight components 
		//from erasing their background.
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch(Throwable t) {
		}

		Gst.init("test", new String[] { "--gst-disable-segtrap" });
		
		String videoElement;
		String audioElement;
		switch (Sys.getOSFamily()) {
			case Windows:
				videoElement = "dshowvideosink";
				//videoElement = "directdrawsink";
				audioElement = "autoaudiosink";
				break;
			case Unix:
				//videoElement = "xvimagesink"; //gconfaudiosink and gconfvideosink?
				videoElement = "ximagesink";
				//videoElement = "glimagesink";
				//audioElement = "autoaudiosink";
				audioElement = "alsasink";
				//audioElement = "gconfaudiosink";
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
		super(parent, style | SWT.NO_BACKGROUND | swtDoubleBufferStyle());

		this.imgCanvas = new Canvas(this, SWT.NO_FOCUS | SWT.NO_BACKGROUND | swtDoubleBufferStyle());
		this.videoCanvas = new Canvas(this, SWT.NO_FOCUS | swtDoubleBufferStyle());
		this.acceleratedVideoCanvas = new Canvas(this, SWT.EMBEDDED | SWT.NO_FOCUS | SWT.NO_BACKGROUND | swtDoubleBufferStyle());
		this.nativeHandle = SWTOverlay.handle(acceleratedVideoCanvas);
		this.display = getDisplay();

		this.audioElement = !StringUtil.isNullOrEmpty(videoElement) ? audioElement : DEFAULT_AUDIO_ELEMENT;
		this.videoElement = !StringUtil.isNullOrEmpty(videoElement) ? videoElement : DEFAULT_VIDEO_ELEMENT;

		this.positionUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				onPositionUpdate();
			}
		};

		//<editor-fold defaultstate="collapsed" desc="Paint">
		//When showing video, use expose() methods. When showing images, draw the
		//image using SWT.
		acceleratedVideoCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (layout.topControl != acceleratedVideoCanvas || !acceleratedVideoCanvas.isVisible())
					return;
				expose();
			}
		});
		imgCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (layout.topControl != imgCanvas || !imgCanvas.isVisible())
					return;
				paintImage(e.gc, singleImage);
			}
		});
		videoCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (layout.topControl != videoCanvas || !videoCanvas.isVisible())
					return;
				paintUnacceleratedVideo(e.gc, videoCanvas.getBounds(), currentFrame);
			}
		});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="SWT">
		//this.setLayout(new FillLayout());
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

		//<editor-fold defaultstate="collapsed" desc="Layout">
		//Setup SWT layout
		this.setLayout((layout = new StackLayout()));
		layout.marginHeight = layout.marginWidth = 0;
		layout.topControl = imgCanvas;
		//</editor-fold>
		
		showVideoCanvas();
		init();
	}

	protected void init() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	public void Dispose() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters/Setters">
	public boolean isVideoAccelerated() {
		return acceleratedVideo;
	}

	public void setVideoAccelerated(boolean value) {
		this.acceleratedVideo = value;
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
			return (currentState(0L) != State.NULL);
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
			final State state = currentState(0L);
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
			final State state = currentState(0L);
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
			final State state = currentState(0L);
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
			final State state = currentState(0L);
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
			final State state = currentState(0L);
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
			if (pipeline == null)
				return false;

			return (Boolean)pipeline.get("mute");
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int getVolume() {
		lock.lock();
		try {
			if (pipeline == null)
				return 100;

			return Math.max(0, Math.min(100, (int)((Double)pipeline.get("volume") * 100.0D)));
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isAudioAvailable() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;

			int numAudioStreams = (Integer)pipeline.get("n-audio");
			return (numAudioStreams > 0);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isVideoAvailable() {
		lock.lock();
		try {
			if (pipeline == null)
				return false;

			int numVideoStreams = (Integer)pipeline.get("n-video");
			return (numVideoStreams > 0);
		} finally {
			lock.unlock();
		}
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
	private static int swtDoubleBufferStyle() {
		//Macs force double buffering, so using SWT double buffering on top of that would
		//be triple buffering and an unnecessary performance hit.
		if (!Sys.isOSFamily(OSFamily.Mac))
			return SWT.DOUBLE_BUFFERED;
		else
			return 0;
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

	protected void showImageCanvas() {
		//<editor-fold defaultstate="collapsed" desc="Check UI Thread">
		if (!isUIThread()) {
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					showImageCanvas();
				}
			});
			return;
		}
		//</editor-fold>
		
		layout.topControl = imgCanvas;
		imgCanvas.setVisible(true);
		videoCanvas.setVisible(false);
		acceleratedVideoCanvas.setVisible(false);
		layout();
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

		layout.topControl = (acceleratedVideo ? acceleratedVideoCanvas : videoCanvas);
		acceleratedVideoCanvas.setVisible(acceleratedVideo);
		videoCanvas.setVisible(!acceleratedVideo);
		imgCanvas.setVisible(false);
		layout();
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
		videoCanvas.setVisible(false);
		imgCanvas.setVisible(false);
		layout();
	}

	protected Buffer playbinFrame(Pipeline pipeline) {
		if (!(pipeline instanceof PlayBin2))
			return null;

		//Pointer ptr = pipeline.getPointer("frame");
		//Buffer buffer = MiniObject.objectFor(ptr, Buffer.class, false);

		GType playbin2_gtype = GstTypes.getGType(pipeline.getNativeAddress());
		int signalID = GSIGNAL_API.g_signal_lookup("convert-frame", playbin2_gtype);
		if (signalID == 0)
			return null;

		PointerByReference refBuffer = new PointerByReference();
		Caps caps = Caps.fromString(Colorspace.CAPS_IMAGE_COLORSPACE_DEPTH);
		GSIGNAL_API.g_signal_emit(pipeline, signalID, null, caps, refBuffer);
		Buffer buffer = MiniObject.objectFor(refBuffer.getValue(), Buffer.class, false);
		System.out.println(buffer.getCaps());
		caps.dispose();
		return buffer;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Interfaces">
	public static interface IErrorListener {
		void handleError(final MediaComponent source, final IMediaRequest request, final ErrorType errorType, final int code, final String message);
	}

	public static interface IUnacceleratedPaintListener {
		void paintNoVideo(IMediaPlayer src, GC g, Rectangle bounds);
		boolean paintBeforeVideo(IMediaPlayer src, GC g, Rectangle bounds, ImageData frame);
		void paintAfterVideo(IMediaPlayer src, GC g, Rectangle bounds);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Adapters">
	private static abstract class HandoffAdapter implements Element.HANDOFF, FakeSink.PREROLL_HANDOFF {
		public void handoff(Element element, Buffer buffer, Pad pad) {
			handle(buffer);
		}

		public void prerollHandoff(FakeSink fakesink, Buffer buffer, Pad pad, Pointer user_data) {
			handle(buffer);
		}

		protected abstract void handle(Buffer buffer);
	}

	public static abstract class ErrorListenerAdapter implements IErrorListener {
		@Override
		public void handleError(final MediaComponent source, final IMediaRequest request, ErrorType errorType, int code, String message) {
		}
	}

	public static abstract class UnacceleratedPaintListenerAdapter implements IUnacceleratedPaintListener {
		public void paintNoVideo(IMediaPlayer src, GC g, Rectangle bounds) {
		}

		public boolean paintBeforeVideo(IMediaPlayer src, GC g, Rectangle bounds, ImageData frame) {
			return true;
		}

		public void paintAfterVideo(IMediaPlayer src, GC g, Rectangle bounds) {
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

	//<editor-fold defaultstate="collapsed" desc="UnacceleratedPaintListener">
	public boolean addUnacceleratedPaintListener(final IUnacceleratedPaintListener Listener) {
		if (Listener == null)
			return false;
		boolean ret = unacceleratedPaintListeners.add(Listener);
		if (ret)
			recreateUnacceleratedPaintListenerFastList();
		return ret;
	}

	public boolean removeUnacceleratedPaintListener(final IUnacceleratedPaintListener Listener) {
		if (Listener == null)
			return false;
		boolean ret = unacceleratedPaintListeners.remove(Listener);
		if (ret)
			recreateUnacceleratedPaintListenerFastList();
		return ret;
	}

	public boolean containsUnacceleratedPaintListener(final IUnacceleratedPaintListener Listener) {
		if (Listener == null)
			return false;
		return unacceleratedPaintListeners.contains(Listener);
	}

	public boolean clearUnacceleratedPaintListeners() {
		unacceleratedPaintListeners.clear();
		recreateUnacceleratedPaintListenerFastList();
		return true;
	}

	protected void recreateUnacceleratedPaintListenerFastList() {
		fastUnacceleratedPaintListeners = unacceleratedPaintListeners.toArray(new IUnacceleratedPaintListener[unacceleratedPaintListeners.size()]);
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

	//<editor-fold defaultstate="collapsed" desc="UnacceleratedPaintListener">
	protected void fireUnacceleratedPaintNoVideo(GC g, Rectangle bounds) {
		for(int i =  0; i < fastUnacceleratedPaintListeners.length; ++i)
			fastUnacceleratedPaintListeners[i].paintNoVideo(mediaPlayer, g, bounds);
	}

	protected boolean fireUnacceleratedPaintBeforeVideo(GC g, Rectangle bounds, ImageData frame) {
		for(int i =  0; i < fastUnacceleratedPaintListeners.length; ++i)
			if (!fastUnacceleratedPaintListeners[i].paintBeforeVideo(mediaPlayer, g, bounds, frame))
				return false;
		return true;
	}

	protected void fireUnacceleratedPaintAfterVideo(GC g, Rectangle bounds) {
		for(int i =  0; i < fastUnacceleratedPaintListeners.length; ++i)
			fastUnacceleratedPaintListeners[i].paintAfterVideo(mediaPlayer, g, bounds);
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
				if (pipeline == null)
					return null;

				buffer = playbinFrame(pipeline);
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
			ImageData imageData = new ImageData(frame.getWidth(), frame.getHeight(), 24, new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));
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
				if (pipeline == null)
					return null;

				buffer = playbinFrame(pipeline);
				if (buffer == null)
					return null;

				return Colorspace.createBufferedImage(buffer);
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
			imgCanvas.redraw();
			return true;
		}

		State state;
		lock.lock();
		try {
			if (pipeline != null && ((state = currentState()) == State.PLAYING || state == State.PAUSED)) {
				if (xoverlay != null && layout.topControl == acceleratedVideoCanvas) {
					xoverlay.expose();
					return true;
				} else {
					videoCanvas.redraw();
					return true;
				}
			} else {
				videoCanvas.redraw();
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
			if (pipeline == null)
				return false;

			boolean shouldMute = !isMuted();
			pipeline.set("mute", (muted = shouldMute));
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
		lock.lock();
		try {
			if (pipeline == null)
				return false;

			int oldVolume = getVolume();
			int newVolume = Math.max(0, Math.min(100, percent));
			pipeline.set("volume", (double)(volume = newVolume) / 100.0D);
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
			//changeState(State.PLAYING);

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
						StepEvent evt = new StepEvent(Format.BUFFERS, 1L, 1.0D, true, false);
						pipeline.sendEvent(evt);
						evt.dispose();
					}
				});
				return true;
			}
			StepEvent evt = new StepEvent(Format.BUFFERS, 1L, 1.0D, true, false);
			boolean ret = pipeline.sendEvent(evt);
			evt.dispose();
			return ret;
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
			if (pipeline == null)
				return false;

			resetPipeline(pipeline);
			showNone();
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
		if (newPipeline == null)
			return;
		newPipeline.setState(State.NULL);
		//Remove any pending actions for this pipeline
		clearAllStateActions(newPipeline);
		newPipeline.dispose();
		pipeline = null;
		mediaType = MediaType.Unknown;
		singleImage = null;
		currentFrame = null;
		expose();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Create">
	protected Element createImageSink(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedVideoSink, final boolean useHardwareAcceleration) {
		final FakeSink imageSink = new FakeSink("videoSink");
		imageSink.set("num-buffers", 1);
		imageSink.set("signal-handoffs", true);
		imageSink.getStaticPad("sink").connect("notify::caps", new Closure() {
			public boolean invoke(Pad pad, Pointer unused, Pointer dynamic) {
				return onNotifyCaps(newPipeline, pad);
			}
		});
		imageSink.connect(new FakeSink.PREROLL_HANDOFF() {
			@Override
			public void prerollHandoff(FakeSink fakesink, Buffer buffer, Pad pad, Pointer user_data) {
				onImageSinkHandoff(newPipeline, buffer);
			}
		});
		imageSink.connect(new Element.HANDOFF() {
			@Override
			public void handoff(Element element, Buffer buffer, Pad pad) {
				onImageSinkHandoff(newPipeline, buffer);
			}
		});
		return imageSink;
	}

	protected Element createVideoSink(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedVideoSink, final boolean useHardwareAcceleration) {
		if (!useHardwareAcceleration) {
			//Create a bin
			Bin videoSinkBin = new Bin("videoSink");
			FakeSink fakesink = new FakeSink("videoSinkBin_fakesink");
			Element ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", "videoSinkBin_ffmpegcolorspace");
			final Element capsfilter = ElementFactory.make("capsfilter", "videoSinkBin_capsfilter");
			final Caps caps = Caps.fromString(Colorspace.CAPS_IMAGE_COLORSPACE_DEPTH);
			capsfilter.setCaps(caps);

			Pad sinkPad;
			(sinkPad = fakesink.getStaticPad("sink")).connect("notify::caps", new Closure() {
				@SuppressWarnings("unused")
				public boolean invoke(Pad pad, Pointer unused, Pointer dynamic) {
					return onNotifyCaps(newPipeline, pad);
				}
			});
			sinkPad.dispose();

			HandoffAdapter handoffs = new HandoffAdapter() {
				private int[] pixels = new int[0];
				private boolean updatePending = false;
				private Lock bufferLock = new ReentrantLock();

				private final Runnable update = new Runnable() {
					@Override
					public void run() {
						bufferLock.lock();
						try {
							if (!isDisposed()) {
								videoCanvas.redraw();
							}
							updatePending = false;
						} finally {
							bufferLock.unlock();
						}
					}
				};

				@Override
				public void handle(Buffer buffer) {
					Caps caps = buffer.getCaps();
					Structure struct = caps.getStructure(0);
					int width = struct.getInteger("width");
					int height = struct.getInteger("height");

					cycle: {
						if (width >= 0 && height >= 0) {
							if (!bufferLock.tryLock())
								break cycle;
							if (updatePending) {
								bufferLock.unlock();
								break cycle;
							}
							try {
								IntBuffer rgb = buffer.getByteBuffer().asIntBuffer();
								int size = rgb.remaining();
								if (size != pixels.length)
									pixels = new int[size];

								ImageData imageData = new ImageData(width, height, 24, new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));
								rgb.get(pixels, 0, size);
								imageData.setPixels(0, 0, size, pixels, 0);
								
								currentFrame = imageData;
								updatePending = true;
							} finally {
								bufferLock.unlock();
							}
							if (!isDisposed())
								display.asyncExec(update);
						}
					}

					//Cleanup
					buffer.dispose();
					struct.dispose();
					caps.dispose();
				}
			};

			fakesink.set("sync", true);
			fakesink.set("signal-handoffs", true);
			fakesink.connect((Element.HANDOFF)handoffs);
			fakesink.connect((FakeSink.PREROLL_HANDOFF)handoffs);

			videoSinkBin.addMany(ffmpegcolorspace, capsfilter, fakesink);
			Element.linkMany(ffmpegcolorspace, capsfilter, fakesink);

			final Pad videoSinkBinPad = ffmpegcolorspace.getStaticPad("sink");
			final GhostPad videoSinkBinGhostPad = new GhostPad("videoSinkBin_ghostpad", videoSinkBinPad);
			videoSinkBin.addPad(videoSinkBinGhostPad);

			capsfilter.dispose();
			caps.dispose();
			ffmpegcolorspace.dispose();
			//fakesink.dispose();

			return videoSinkBin;
		} else {
			Element videoSink = ElementFactory.make(suggestedVideoSink, "videoSink");
			videoSink.set("force-aspect-ratio", false);
			videoSink.set("show-preroll-frame", true);

			Pad sinkPad;
			(sinkPad = videoSink.getStaticPad("sink")).connect("notify::caps", new Closure() {
				@SuppressWarnings("unused")
				public boolean invoke(Pad pad, Pointer unused, Pointer dynamic) {
					return onNotifyCaps(newPipeline, pad);
				}
			});
			sinkPad.dispose();

			return videoSink;
		}
	}

	protected Element createAudioSink(final MediaType newMediaType, final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedAudioSink, final boolean useHardwareAcceleration) {
		return ElementFactory.make(suggestedAudioSink, "audioSink");
	}

	protected Bin createAudioBin(final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedAudioSink, final boolean useHardwareAcceleration) {
		final Bin audioBin = new Bin("audioBin");
		final Element audioQueue = ElementFactory.make("queue2", "audioQueue");
		final Element audioVolume = ElementFactory.make("volume", "audioVolume");
		final Element audioConvert = ElementFactory.make("audioconvert", "audioConvert");
		final Element audioResample = ElementFactory.make("audioresample", "audioResample");
		final Element audioScaleTempo = ElementFactory.make("scaletempo", "audioScaleTempo");
		final Element audioConvertAfterScaleTempo = ElementFactory.make("audioconvert", "audioConvertAfterScaleTempo");
		final Element audioResampleAfterScaleTempo = ElementFactory.make("audioresample", "audioResampleAfterScaleTempo");
		final Element audioSink = createAudioSink(MediaType.Unknown, newRequest, newPipeline, suggestedAudioSink, useHardwareAcceleration);

		audioBin.addMany(audioQueue, audioVolume, audioConvert, audioResample, audioScaleTempo, audioConvertAfterScaleTempo, audioResampleAfterScaleTempo, audioSink);
		Element.linkMany(audioQueue, audioVolume, audioConvert, audioResample, audioScaleTempo, audioConvertAfterScaleTempo, audioResampleAfterScaleTempo, audioSink);

		final Pad audioBinPad = audioQueue.getStaticPad("sink");
		final GhostPad audioBinGhostPad = new GhostPad("audioBinGhostPad", audioBinPad);
		audioBin.addPad(audioBinGhostPad);

		audioBinGhostPad.dispose();
		audioBinPad.dispose();
		audioSink.dispose();
		audioResampleAfterScaleTempo.dispose();
		audioConvertAfterScaleTempo.dispose();
		audioScaleTempo.dispose();
		audioResample.dispose();
		audioConvert.dispose();
		audioVolume.dispose();
		audioQueue.dispose();

		return audioBin;
	}

	protected Bin createVideoBin(final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedVideoSink, final boolean useHardwareAcceleration) {
		//For now, ignore useHardwareAcceleration -- everything will be done through SWT
		final Bin videoBin = new Bin("videoBin");
		final Element videoSink = createVideoSink(MediaType.Unknown, newRequest, newPipeline, suggestedVideoSink, useHardwareAcceleration);

		if (!currentLiveSource) {
			final float checked_fps = (newRequest.getFPS() >= IMediaRequest.MINIMUM_FPS ? newRequest.getFPS() : IMediaRequest.DEFAULT_FPS);
			final boolean hasFramerate = (checked_fps == IMediaRequest.DEFAULT_FPS || currentLiveSource);

			final String capsFramerateFilter = Colorspace.createKnownColorspaceFilter(hasFramerate, checked_fps);

			final Element videoRate = ElementFactory.make("videorate", "videoRate");
			final Element videoScale = ElementFactory.make("videoscale", "videoScale");
			final Element videoColorSpace = ElementFactory.make("ffmpegcolorspace", "videoColorSpace");
			final Element videoCapsFilter = ElementFactory.make("capsfilter", "videoCaps");
			final Caps videoCaps = Caps.fromString(capsFramerateFilter);
			videoCapsFilter.setCaps(videoCaps);

			videoScale.set("method", 2L /*4Tap*/);

			videoBin.addMany(videoColorSpace, videoRate, videoScale, videoCapsFilter, videoSink);
			Element.linkMany(videoColorSpace, videoRate, videoScale, videoCapsFilter, videoSink);

			final Pad videoBinPad = videoColorSpace.getStaticPad("sink");
			final GhostPad videoBinGhostPad = new GhostPad("videoBinGhostPad", videoBinPad);
			videoBin.addPad(videoBinGhostPad);

			videoBinGhostPad.dispose();
			videoBinPad.dispose();
			videoColorSpace.dispose();
			videoScale.dispose();
			videoRate.dispose();
			videoCapsFilter.dispose();

			//videoCaps.dispose();
		} else {
			final Element videoColorSpace = ElementFactory.make("ffmpegcolorspace", "videoColorSpace");
			final Element videoScale = ElementFactory.make("videoscale", "videoScale");

			videoScale.set("method", 2L /*4Tap*/);

			videoBin.addMany(videoColorSpace, videoScale, videoSink);
			Element.linkMany(videoColorSpace, videoScale, videoSink);

			final Pad videoBinPad = videoColorSpace.getStaticPad("sink");
			final GhostPad videoBinGhostPad = new GhostPad("videoBinGhostPad", videoBinPad);
			videoBin.addPad(videoBinGhostPad);

			videoBinGhostPad.dispose();
			videoBinPad.dispose();
			videoColorSpace.dispose();
			videoScale.dispose();
		}

		xoverlay = null;

		if (useHardwareAcceleration) {
			try {
				xoverlay = SWTOverlay.wrap(videoSink);
			} catch(IllegalArgumentException e) {
			}
		}

		//videoSink.dispose();
		return videoBin;
	}

	protected Bin createImageBin(final IMediaRequest newRequest, final Pipeline newPipeline, final String suggestedVideoSink, final boolean useHardwareAcceleration) {
		final Bin imageBin = new Bin("imageBin");
		final Element imageSink = createImageSink(mediaType, newRequest, newPipeline, suggestedVideoSink, useHardwareAcceleration);
		final Element imageColorSpace = ElementFactory.make("ffmpegcolorspace", "imageColorSpace");
		final Element imageCapsFilter = ElementFactory.make("capsfilter", "imageCaps");
		final Caps imageCaps = Caps.fromString(Colorspace.CAPS_IMAGE_COLORSPACE_DEPTH);
		imageCapsFilter.setCaps(imageCaps);

		imageBin.addMany(imageColorSpace, imageCapsFilter, imageSink);
		Element.linkMany(imageColorSpace, imageCapsFilter, imageSink);

		final Pad imageBinPad = imageColorSpace.getStaticPad("sink");
		final GhostPad imageBinGhostPad = new GhostPad("imageBinGhostPad", imageBinPad);
		imageBin.addPad(imageBinGhostPad);

		imageBinGhostPad.dispose();
		imageBinPad.dispose();
		//imageCaps.dispose();
		imageColorSpace.dispose();
		imageCapsFilter.dispose();
		imageSink.dispose();

		return imageBin;
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

	protected void onImageSinkHandoff(final Pipeline newPipeline, final Buffer buffer) {
		singleImage = swtImageDataSnapshot(buffer);
		showImageCanvas();
		expose();
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
		if (actions != null && !actions.isEmpty()) {
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
	
	protected void onPositionUpdate() {
		if (!emitPositionUpdates)
			return;
		if (lock.tryLock()) {
			try {
				if (pipeline != null) {
					//if (!isSeekable())
					//	return;

					final long position = pipeline.queryPosition(TimeUnit.MILLISECONDS);
					final long duration = pipeline.queryDuration(TimeUnit.MILLISECONDS);
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

	protected void onEOS(GstObject src) {
		if (pipeline == null || !pipeline.equals(src))
			return;
		if (mediaType != MediaType.Image && (currentRepeatCount == IMediaRequest.REPEAT_FOREVER || (currentRepeatCount > 0 && numberOfRepeats < currentRepeatCount))) {
			++numberOfRepeats;
			if (numberOfRepeats == Integer.MAX_VALUE)
				numberOfRepeats = 1;

			Gst.invokeLater(new Runnable() {
				public void run() {
					lock.lock();
					try {
						if (pipeline == null)
							return;

						State state = pipeline.getState(0L);
						pipeline.setState(State.READY);
						//pipeline.set("uri", mediaRequest.getURI());
						if (state != State.PAUSED)
							pipeline.setState(State.PLAYING);
						else
							pipeline.setState(State.PAUSED);
					} finally {
						lock.unlock();
					}
				}
			});
			return;
		}
		numberOfRepeats = 0;
		Gst.invokeLater(new Runnable() {
			public void run() {
				lock.lock();
				try {
					if (pipeline == null)
						return;
					
					if (mediaType != MediaType.Image)
						pipeline.setState(State.NULL);
					else
						pipeline.setState(State.PAUSED);
				} finally {
					lock.unlock();
				}
			}
		});
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Play">
	@SuppressWarnings("empty-statement")
	public boolean playPattern(final String title, final VideoTestSrcPattern pattern) {
		lock.lock();
		try {
			if (pipeline != null) {
				pipeline.setState(State.NULL);
				pipeline.dispose();
				pipeline = null;
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

			final float checked_fps = (newRequest.getFPS() >= IMediaRequest.MINIMUM_FPS ? newRequest.getFPS() : IMediaRequest.DEFAULT_FPS);
			final boolean hasFramerate = (checked_fps == IMediaRequest.DEFAULT_FPS || currentLiveSource);

			final Pipeline newPipeline = new Pipeline("pipeline");
			final Element videoTestSrc = ElementFactory.make("videotestsrc", "videoTestSrc");
			videoTestSrc.set("pattern", (long)pattern.getNativeValue());

			final Element videoQueue = ElementFactory.make("queue2", "videoQueue");
			final Element videoRate = ElementFactory.make("videorate", "videoRate");
			final Element videoColorspace = ElementFactory.make("ffmpegcolorspace", "videoColorspace");
			final Element videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
			final Element videoScale = ElementFactory.make("videoscale", "videoScale");
			final Element videoSink = createVideoSink(mediaType, newRequest, newPipeline, videoElement, acceleratedVideo);
			final Caps videoCaps = Caps.fromString(Colorspace.createKnownColorspaceFilter(hasFramerate, checked_fps, 640, 480));

			videoRate.set("silent", true);
			videoCapsFilter.setCaps(videoCaps);

			newPipeline.addMany(videoTestSrc, videoQueue, videoRate, videoScale, videoCapsFilter, videoColorspace, videoSink);
			Element.linkMany(videoTestSrc, videoQueue, videoRate, videoScale, videoCapsFilter, videoColorspace, videoSink);

			final Bus bus = newPipeline.getBus();
			bus.setSyncHandler(new BusSyncHandler() {
				@Override
				public BusSyncReply syncMessage(Message msg) {
					Structure s = msg.getStructure();
					if (s == null || !s.hasName("prepare-xwindow-id"))
						return BusSyncReply.PASS;
					if (xoverlay != null)
						xoverlay.setWindowID(nativeHandle);
					return BusSyncReply.DROP;
				}
			});
			bus.connect(new Bus.STATE_CHANGED() {
				@Override
				public void stateChanged(GstObject source, State oldState, State newState, State pendingState) {
					if (source == null || !source.equals(newPipeline))
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
					//onSegmentDone(newPipeline);
				}
			});
			bus.connect(new Bus.EOS() {
				@Override
				public void endOfStream(GstObject source) {
					//onEOS(newPipeline);
				}
			});

			xoverlay = null;
			pipeline = newPipeline;
			emitPositionUpdates = false;
			if (acceleratedVideo) {
				try {
					xoverlay = SWTOverlay.wrap(videoSink);
				} catch(Throwable t) {
				}
			}

			videoTestSrc.dispose();
			videoQueue.dispose();
			videoRate.dispose();
			videoScale.dispose();
			videoCapsFilter.dispose();
			videoColorspace.dispose();
			videoSink.dispose();
			videoCaps.dispose();

			//Start playing
			showVideoCanvas();
			newPipeline.setState(State.PLAYING);
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
			if (pipeline != null) {
				pipeline.setState(State.NULL);
				pipeline.dispose();
				pipeline = null;
			}

			//Reset these values
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

			final Pipeline newPipeline = new PlayBin2("pipeline");

			newPipeline.setAutoFlushBus(true);
			newPipeline.set("uri", uri);
			newPipeline.set("mute", muted);
			newPipeline.set("volume", Math.max(0, Math.min(100, volume)) / 100.0D);

			Bus bus = newPipeline.getBus();
			bus.setSyncHandler(new BusSyncHandler() {
				@Override
				public BusSyncReply syncMessage(Message msg) {
					Structure s = msg.getStructure();
					if (s == null || !s.hasName("prepare-xwindow-id"))
						return BusSyncReply.PASS;
					if (xoverlay != null)
						xoverlay.setWindowID(nativeHandle);
					return BusSyncReply.DROP;
				}
			});
			bus.connect(new Bus.ERROR() {
				@Override
				public void errorMessage(GstObject source, int code, String message) {
					onError(request, newPipeline, code, message);
				}
			});
			bus.connect(new Bus.STATE_CHANGED() {
				@Override
				public void stateChanged(GstObject source, State old, State current, State pending) {
					if (source != newPipeline)
						return;
					onStateChanged(newPipeline, null, old, current, pending);
				}
			});
			bus.connect(new Bus.BUFFERING() {
				@Override
				public void bufferingData(GstObject source, int percent) {
					onBuffering(newPipeline, percent);
				}
			});
			bus.connect(new Bus.SEGMENT_DONE() {
				@Override
				public void segmentDone(GstObject source, Format format, long position) {
					onEOS(source);
				}
			});
			bus.connect(new Bus.EOS() {
				@Override
				public void endOfStream(GstObject source) {
					onEOS(source);
				}
			});
			newPipeline.connect(new Bin.ELEMENT_ADDED() {
				@Override
				public void elementAdded(Bin bin, Element element) {
					//<editor-fold defaultstate="collapsed" desc="Check args">
					if (element == null)
						return;
					ElementFactory factory = element.getFactory();
					if (factory == null)
						return;
					String factoryName = factory.getName();
					factory.dispose();
					//</editor-fold>

					if ("uridecodebin".equalsIgnoreCase(factoryName)) {
						((Bin)element).connect(new Element.PAD_ADDED() {
							@Override
							public void padAdded(Element element, Pad pad) {
								if (pad.isLinked())
									return;
								//check media type
								final Caps caps = pad.getCaps();
								final Structure struct = caps.getStructure(0);
								final String padCaps = struct.getName();
								if (!StringUtil.isNullOrEmpty(padCaps)) {
									//
									
									if (padCaps.startsWith("audio/")) {
										if (mediaType == MediaType.Unknown && mediaType != MediaType.Video)
											mediaType = MediaType.Audio;

										final Bin audioBin = createAudioBin(request, newPipeline, audioElement, false);
										newPipeline.set("audio-sink", audioBin);
										audioBin.dispose();

										if (mediaType == MediaType.Audio)
											showNone();

									} else if (padCaps.startsWith("video/")) {
										if (mediaType == MediaType.Unknown || mediaType == MediaType.Audio)
											mediaType = (!determineIfSingleImage((Bin)element) ? MediaType.Video : MediaType.Image);

										if (mediaType == MediaType.Image) {
											//Use a bin that forces an rgb-colorspace filter and then grabs the image from
											//a fakesink at handoff.
											final Bin imageBin = createImageBin(request, newPipeline, videoElement, false);
											newPipeline.set("video-sink", imageBin);
											imageBin.dispose();

											showImageCanvas();
										} else {
											final Bin videoBin = createVideoBin(request, newPipeline, videoElement, acceleratedVideo);
											newPipeline.set("video-sink", videoBin);
											videoBin.dispose();

											showVideoCanvas();
										}

									}
								}
							}
						});
						((Bin)element).connect(new Bin.ELEMENT_ADDED() {
							@Override
							public void elementAdded(Bin bin, Element element) {
								//<editor-fold defaultstate="collapsed" desc="Check args">
								if (element == null)
									return;
								ElementFactory factory = element.getFactory();
								if (factory == null)
									return;
								String factoryName = factory.getName();
								factory.dispose();
								//</editor-fold>

								onUriDecodeBinElementAdded(newPipeline, bin, element);

								if (factoryName.startsWith("decodebin")) {
									((Bin)element).connect(new Bin.ELEMENT_ADDED() {
										@Override
										public void elementAdded(Bin bin, Element element) {
											onDecodeBinElementAdded(newPipeline, null, bin, element);

											//element.dispose();
											//bin.dispose();
										}
									});
								}
								//element.dispose();
								//bin.dispose();
							}
						});
					}
					//element.dispose();
					//bin.dispose();
				}
			});
			
			pipeline = newPipeline;

			newPipeline.setState(State.PLAYING);
			
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
		Rectangle r = getBounds();

		g.setForeground(getBackground());
		g.fillRectangle(r);

		if (imgData == null)
			return;
		
		g.setAntialias(SWT.ON);

		Image img = new Image(display, imgData);
		g.drawImage(img, 0, 0, imgData.width, imgData.height, 0, 0, r.width, r.height);
		img.dispose();
	}

	protected void paintUnacceleratedVideo(GC g, Rectangle bounds, ImageData frame) {
		if (frame == null) {
			g.setBackground(getBackground());
			g.fillRectangle(bounds);

			fireUnacceleratedPaintNoVideo(g, bounds);
			return;
		}

		if (!fireUnacceleratedPaintBeforeVideo(g, bounds, frame))
			return;

		g.setAntialias(SWT.ON);
		g.setInterpolation(SWT.HIGH);
		g.setAdvanced(false);
		
		frame = frame.scaledTo(bounds.width, bounds.height);
		Image img = new Image(g.getDevice(), frame);
		g.drawImage(img, 0, 0, bounds.width, bounds.height, 0, 0, bounds.width, bounds.height);
		img.dispose();

		fireUnacceleratedPaintAfterVideo(g, bounds);
	}
	//</editor-fold>
	//</editor-fold>
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ossbuild.media.gstreamer.swt;

import com.sun.jna.Pointer;
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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import ossbuild.Path;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.extract.Resources;
import ossbuild.extract.processors.FileProcessor;
import ossbuild.media.IMediaPlayer;
import ossbuild.media.IMediaRequest;
import ossbuild.media.MediaRequest;
import ossbuild.media.MediaRequestType;
import ossbuild.media.MediaType;
import ossbuild.media.Scheme;
import ossbuild.media.gstreamer.Bin;
import ossbuild.media.gstreamer.Buffer;
import ossbuild.media.gstreamer.Bus;
import ossbuild.media.gstreamer.BusSyncReply;
import ossbuild.media.gstreamer.Caps;
import ossbuild.media.gstreamer.Colorspace;
import ossbuild.media.gstreamer.Element;
import ossbuild.media.gstreamer.ErrorType;
import ossbuild.media.gstreamer.Format;
import ossbuild.media.gstreamer.Fraction;
import ossbuild.media.gstreamer.GhostPad;
import ossbuild.media.gstreamer.IBin;
import ossbuild.media.gstreamer.IBus;
import ossbuild.media.gstreamer.IElement;
import ossbuild.media.gstreamer.IPipeline;
import ossbuild.media.gstreamer.Message;
import ossbuild.media.gstreamer.Pad;
import ossbuild.media.gstreamer.Pipeline;
import ossbuild.media.gstreamer.State;
import ossbuild.media.gstreamer.StateChangeReturn;
import ossbuild.media.gstreamer.Structure;
import ossbuild.media.gstreamer.api.GStreamer;
import ossbuild.media.gstreamer.api.GTypeConverters;
import ossbuild.media.gstreamer.api.Utils;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;
import ossbuild.media.gstreamer.signals.IBuffering;
import ossbuild.media.gstreamer.signals.IElementAdded;
import ossbuild.media.gstreamer.signals.IEndOfStream;
import ossbuild.media.gstreamer.signals.IError;
import ossbuild.media.gstreamer.signals.IHandoff;
import ossbuild.media.gstreamer.signals.INotifyCaps;
import ossbuild.media.gstreamer.signals.IPadAdded;
import ossbuild.media.gstreamer.signals.ISegmentDone;
import ossbuild.media.gstreamer.signals.IStateChanged;

/**
 *
 * @author David
 */
public class MediaComponentNew extends SWTMediaComponent {
	public static void main(String[] args) {
		Sys.initialize();
		GStreamer.initialize();

		final Display display = new Display();
		final Shell dlg = new Shell(display, SWT.NORMAL | SWT.SHELL_TRIM);

		Button btn;
		GridData gd;

		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;

		dlg.setText("OSSBuild :: Media :: GStreamer :: SWT");
		dlg.setLayout(layout);
		dlg.setSize(700, 700);

		MediaComponentNew comp;

		comp = new MediaComponentNew(dlg, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		final MediaComponentNew mediaComp = comp;

		final Scale scale = new Scale(dlg, SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		scale.setEnabled(false);
		scale.setLayoutData(gd);
		scale.setIncrement(100);
		scale.setPageIncrement(1000);

		final Button btnPlayExample = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayExample.setLayoutData(gd);
		btnPlayExample.setText("Play Example");

		dlg.open();

		final String fileName = "";
		final FileDialog selFile = new FileDialog(dlg, SWT.OPEN);
		selFile.setFilterNames(new String[]{"All Files (*.*)"});
		selFile.setFilterExtensions(new String[]{"*.*"});
		
		final File[] file = new File[] { new File(fileName) };

		btnPlayExample.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					//Extract resource
					Resources.extractAll(ossbuild.extract.Package.newInstance("resources.media", Path.nativeResourcesDirectory, new FileProcessor(false, "example.mov"))).get();
				} catch(Throwable t) {
				}

				file[0] = Path.combine(Path.nativeResourcesDirectory, "example.mov");

				gc();
				
				for (final Control c : dlg.getChildren()) {
					if (c instanceof MediaComponentNew) {
						((MediaComponentNew)c).play(new MediaRequest(MediaRequestType.Video, "Title", false, true, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, Scheme.File, file[0].toURI()));
					}
				}
			}
		});

		while (!dlg.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private static void gc() {
		for(int i = 0; i < 40; ++i)
			System.gc();
	}

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
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	protected final Map<IPipeline, Map<State, Queue<Runnable>>> stateQueue = new HashMap<IPipeline, Map<State, Queue<Runnable>>>(2);

	private boolean currentLiveSource;
	private int volume = 100;
	private boolean muted = false;
	private long bufferSize = DEFAULT_BUFFER_SIZE;
	private boolean hasAudio = false;
	private boolean hasVideo = false;
	private boolean hasMultipartDemux = false;
	private int videoWidth = 0;
	private int videoHeight = 0;
	private float actualFPS;
	private int currentRepeatCount;
	private int numberOfRepeats;
	private boolean emitPositionUpdates = true;
	private long lastPosition = 0L;
	private long lastDuration = 0L;
	private boolean maintainAspectRatio = true;
	private double currentRate = DEFAULT_RATE;
	protected IMediaRequest mediaRequest = null;
	protected volatile State currentState = State.Null;
	
	private MediaType mediaType = MediaType.Unknown;
	private ImageData singleImage = null;
	protected final Display display;

	protected final long nativeHandle;
	protected SWTOverlay xoverlay = null;

	protected final String videoElement;
	protected final String audioElement;
	protected IElement currentVideoSink;
	protected IElement currentAudioSink;
	protected IElement currentAudioVolumeElement;
	protected IPipeline pipeline;

	protected final Runnable redrawRunnable;
	protected final Runnable xoverlayRunnable;
	protected final PaintListener paintListener;

	protected final Lock lock = new ReentrantLock();
	
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
				videoElement = "fakesink";
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

	public MediaComponentNew(Composite parent, int style) {
		this(DEFAULT_VIDEO_ELEMENT, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponentNew(String videoElement, Composite parent, int style) {
		this(videoElement, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponentNew(String videoElement, String audioElement, Composite parent, int style) {
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
					xoverlay.setWindowID(MediaComponentNew.this);
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
				if (pipeline != null)
					resetPipeline(pipeline);
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
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	protected static boolean isParser(final IElement elem) {
		return isParser(elem.getFactoryClass());
	}

	protected static boolean isDecoder(final IElement elem) {
		return isDecoder(elem.getFactoryClass());
	}

	protected static boolean isImage(final IElement elem) {
		return isImage(elem.getFactoryClass());
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

	protected static boolean determineIfSingleImage(final IBin bin) {
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
		final boolean[] imageFound = new boolean[1];
		bin.visitElementsRecursive(new IBin.IElementVisitor() {
			int decoderCount = 0;

			@Override
			public boolean visit(IBin src, IElement element) {
				String factoryClass = element.getFactoryClass();
				if (isGeneric(factoryClass) || isSource(factoryClass))
					return true;
				if (isParser(factoryClass) || (isDecoder(factoryClass) && ++decoderCount > 1)) {
					imageFound[0] = false;
					return false;
				}
				if (isImage(factoryClass)) {
					imageFound[0] = true;
					return false;
				}
				return true;
			}
		});
		return imageFound[0];
	}

	protected Map<State, Queue<Runnable>> createEmptyStateQueue() {
		//Create a new queue for each state
		Map<State, Queue<Runnable>> newStateQueue = new HashMap<State, Queue<Runnable>>(State.values().length);
		for(State s : State.values())
			newStateQueue.put(s, new ConcurrentLinkedQueue<Runnable>());
		return newStateQueue;
	}

	protected State currentState() {
		return pipeline.requestState(0L);
	}

	protected State currentState(long timeout) {
		return pipeline.requestState(TimeUnit.MILLISECONDS, timeout);
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

	protected StateChangeReturn changeState(IPipeline pipeline, State state) {
		return changeState(pipeline, state, 0L, null);
	}

	protected StateChangeReturn changeState(IPipeline pipeline, State state, long timeout) {
		return changeState(pipeline, state, timeout, null);
	}

	protected StateChangeReturn changeState(IPipeline pipeline, State state, Runnable action) {
		return changeState(pipeline, state, 0L, action);
	}

	protected StateChangeReturn changeState(IPipeline pipeline, State state, long timeout, Runnable action) {
		if (pipeline == null)
			return StateChangeReturn.Failure;
		stateAction(pipeline, state, action);
		if (timeout <= 0L) {
			return pipeline.changeState(state);
		} else {
			pipeline.changeState(state);
			if (pipeline.requestState(TimeUnit.MILLISECONDS, timeout) == state)
				return StateChangeReturn.Success;
			else
				return StateChangeReturn.Failure;
		}
	}

	protected void stateAction(State state, Runnable action) {
		stateAction(pipeline, state, action);
	}

	protected void stateAction(IPipeline pipeline, State state, Runnable action) {
		if (action != null) {
			if (!stateQueue.containsKey(pipeline))
				stateQueue.put(pipeline, createEmptyStateQueue());
			stateQueue.get(pipeline).get(state).add(action);
		}
	}

	protected Queue<Runnable> actionsForState(IPipeline pipeline, State state) {
		if (!stateQueue.containsKey(pipeline))
			return null;
		return stateQueue.get(pipeline).get(state);
	}

	protected void clearStateActions(IPipeline pipeline, State state) {
		Queue<Runnable> actions = actionsForState(pipeline, state);
		if (actions != null)
			actions.clear();
	}

	protected void clearAllStateActions(IPipeline pipeline) {
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
		void handleError(final IMediaPlayer source, final IMediaRequest request, final ErrorType errorType, final int code, final String message);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Adapters">
	public static abstract class ErrorListenerAdapter implements IErrorListener {
		@Override
		public void handleError(final IMediaPlayer source, final IMediaRequest request, ErrorType errorType, int code, String message) {
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
				if (currentVideoSink == null || !currentVideoSink.hasProperty("last-buffer"))
					return null;

				buffer = currentVideoSink.get("last-buffer", GTypeConverters.BUFFER);
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
				if (currentVideoSink == null || !currentVideoSink.hasProperty("last-buffer"))
					return null;

				buffer = currentVideoSink.get("last-buffer", GTypeConverters.BUFFER);
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
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="The Meat">
	//<editor-fold defaultstate="collapsed" desc="Cleanup">
	protected void resetPipeline(final IPipeline newPipeline) {
		if (newPipeline != null) {
			do {
				newPipeline.changeState(State.Null);
			} while(newPipeline.requestState() != State.Null);
			cleanup(newPipeline);
			System.out.println(newPipeline.refCount());
			newPipeline.dispose();
			System.out.println(newPipeline.refCount());
			//while(newPipeline.refCount() >= 1)
			//	newPipeline.unref();
			System.out.println(newPipeline.refCount());
			pipeline = null;
			asyncRedraw();
		}
	}

	protected void cleanup(final IPipeline newPipeline) {
		//Remove any pending actions for this pipeline
		clearAllStateActions(newPipeline);

		//Clean out audio and video bins
		disposeAudioBin(newPipeline);
		disposeVideoBin(newPipeline);

		//Clean out any videotestsrc that may be in the pipeline
		final IElement videoTestSrc = newPipeline.elementFromName("videoTestSrc");
		if (videoTestSrc != null) {
			newPipeline.remove(videoTestSrc);
			videoTestSrc.dispose();
		}
	}

	@SuppressWarnings("empty-statement")
	protected synchronized boolean disposeAudioBin(final IPipeline newPipeline) {
		final IBin bin = newPipeline.binFromName("audioBin");

		if (bin == null)
			return true;

		do {
			bin.changeState(State.Null);
		} while(bin.requestState() != State.Null);

		bin.visitPads(new IElement.IPadVisitor() {
			@Override
			public boolean visit(IElement src, Pad pad) {
				bin.removePad(pad);
				return true;
			}
		});

		bin.visitElements(new IBin.IElementVisitor() {
			@Override
			public boolean visit(IBin src, IElement element) {
				Bin.unlink(bin, element);
				bin.remove(element);
				return true;
			}
		});

		newPipeline.remove(bin);
		bin.dispose();

		return true;
	}

	@SuppressWarnings("empty-statement")
	protected synchronized boolean disposeVideoBin(final IPipeline newPipeline) {
		final IBin bin = newPipeline.binFromName("videoBin");

		if (bin == null)
			return true;

		do {
			bin.changeState(State.Null);
		} while(bin.requestState() != State.Null);

		bin.visitPads(new IElement.IPadVisitor() {
			@Override
			public boolean visit(IElement src, Pad pad) {
				bin.removePad(pad);
				return true;
			}
		});

		bin.visitElements(new IBin.IElementVisitor() {
			@Override
			public boolean visit(IBin src, IElement element) {
				Bin.unlink(bin, element);
				bin.remove(element);
				return true;
			}
		});

		newPipeline.remove(bin);
		bin.dispose();

		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Create">
	protected synchronized IPipeline createPipeline(final IMediaRequest newRequest) {
		//gst-launch uridecodebin use-buffering=false name=dec location=http://.../video.avi
		//    dec. ! [ queue ! audioconvert ! audioresample ! autoaudiosink ]
		//    dec. ! [ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]

		final IPipeline newPipeline = Pipeline.make("pipeline");
		System.out.println("createPipeline: " + newPipeline.refCount());

		final IBin uridecodebin = Bin.make("uridecodebin", "uridecodebin");
		System.out.println("createPipeline: " + newPipeline.refCount());

		uridecodebin.set("use-buffering", false);
		uridecodebin.set("download", false);
		uridecodebin.set("buffer-duration", TimeUnit.MILLISECONDS.toNanos(500L));
		uridecodebin.set("uri", Utils.toGstURI(newRequest.getURI()));

		newPipeline.add(uridecodebin);

		System.out.println("createPipeline: " + newPipeline.refCount());

		//<editor-fold defaultstate="collapsed" desc="UriDecodeBin Signals">
		uridecodebin.connect(new IPadAdded() {
			public void padAdded(Pointer pElement, Pointer pPad) {
				Pad pad = Pad.from(pPad);
				onPadAdded(newRequest, newPipeline, uridecodebin, pad);
				pad.dispose();
			}
		});
		uridecodebin.connect(new IElementAdded() {
			public void elementAdded(Pointer pBin, Pointer pElement) {
				System.out.println("uridecodebin::elementAdded: " + newPipeline.refCount());
				//<editor-fold defaultstate="collapsed" desc="Validate arguments">
				if (pElement == null)
					return;

				final IElement element = Element.from(pElement);
				final String factoryName = element.getFactoryName();

				if (StringUtil.isNullOrEmpty(factoryName)) {
					element.dispose();
					return;
				}
				//</editor-fold>

				//<editor-fold defaultstate="collapsed" desc="Connect to decodebin">
				if (factoryName.startsWith("decodebin")) {
					element.connect(new IElementAdded() {
						public void elementAdded(Pointer pBin, Pointer pElement) {
							System.out.println("decodebin::elementAdded: " + newPipeline.refCount());
							IBin decodebin = Bin.from(pBin);
							IElement element = Element.from(pElement);
							onDecodeBinElementAdded(newPipeline, uridecodebin, decodebin, element);
							element.dispose();
							decodebin.dispose();
							System.out.println("decodebin::elementAdded: " + newPipeline.refCount());
						}
					});
				}
				//</editor-fold>

				onUriDecodeBinElementAdded(newPipeline, uridecodebin, element);
				element.dispose();
				System.out.println("uridecodebin::elementAdded: " + newPipeline.refCount());
			}
		});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Bus Signals">
		final IBus bus = newPipeline.getBus();
		bus.connect(new IStateChanged() {
			@Override
			public void stateChanged(Pointer pSrc, State oldState, State newState, State pendingState) {
				if (pSrc == null || !newPipeline.equals(pSrc))
					return;
				onStateChanged(newPipeline, uridecodebin, oldState, newState, pendingState);
			}
		});
		bus.connect(new IError() {
			@Override
			public void error(Pointer pSrc, int code, String message) {
				onError(newRequest, newPipeline, code, message);
			}
		});
		bus.connect(new ISegmentDone() {
			@Override
			public void segmentDone(Pointer pSrc, Format format, long position) {
				onSegmentDone(newPipeline);
			}
		});
		bus.connect(new IEndOfStream() {
			@Override
			public void endOfStream(Pointer pSrc) {
				onEOS(newPipeline);
			}
		});
		bus.connect(new IBuffering() {
			@Override
			public void buffering(Pointer pSrc, int percent) {
				onBuffering(newPipeline, percent);
			}
		});
		bus.syncHandler(new IBusSyncHandler() {
			@Override
			public BusSyncReply handle(Bus bus, Message msg, Pointer src, Pointer data) {
				return onBusSyncHandler(msg);
			}
		});
		//</editor-fold>

		System.out.println("createPipeline: " + newPipeline.refCount());
		return newPipeline;
	}

	protected IElement createImageSink(final MediaType newMediaType, final IMediaRequest newRequest, final IPipeline newPipeline, final String suggestedVideoSink) {
		final Pad sinkPad;
		final IElement videoSink = Element.make("fakesink", "videoSink");
		videoSink.set("signal-handoffs", true);
		
		(sinkPad = videoSink.staticPad("sink")).connect(new INotifyCaps() {
			@Override
			public boolean notifyCaps(Pointer pPad, Pointer pUnused, Pointer pDynamic) {
				Pad pad = Pad.from(pPad);
				boolean ret = onNotifyCaps(newPipeline, pad);
				pad.dispose();
				return ret;
			}
		});
		videoSink.connect(new IHandoff() {
			public void handoff(Pointer pElement, Pointer pBuffer, Pointer pPad) {
				Buffer buffer = Buffer.from(pBuffer);
				onImageSinkHandoff(newPipeline, buffer);
				buffer.dispose();
			}
		});

		sinkPad.dispose();
		return videoSink;
	}

	protected IElement createVideoSink(final MediaType newMediaType, final IMediaRequest newRequest, final IPipeline newPipeline, final String suggestedVideoSink) {
		final Pad sinkPad;
		final IElement videoSink = Element.make(suggestedVideoSink, "videoSink");
		videoSink.set("show-preroll-frame", true);

		(sinkPad = videoSink.staticPad("sink")).connect(new INotifyCaps() {
			@Override
			public boolean notifyCaps(Pointer pPad, Pointer pUnused, Pointer pDynamic) {
				Pad pad = Pad.from(pPad);
				boolean ret = onNotifyCaps(newPipeline, pad);
				pad.dispose();
				return ret;
			}
		});

		sinkPad.dispose();
		return videoSink;
	}

	protected IElement createAudioSink(final MediaType newMediaType, final IMediaRequest newRequest, final IPipeline newPipeline, final String suggestedAudioSink) {
		return Element.make(suggestedAudioSink, "audioSink");
	}

	protected Pad createAudioBin(final MediaType newMediaType, final IMediaRequest newRequest, final IPipeline newPipeline, final IBin audioBin, final IBin uridecodebin, final Pad pad) {

		//[ queue2 ! volume ! audioconvert ! audioresample ! scaletempo ! audioconvert ! audioresample ! autoaudiosink ]

		final IElement audioQueue = Element.make("queue2", "audioQueue");
		final IElement audioVolume = Element.make("volume", "audioVolume");
		final IElement audioConvert = Element.make("audioconvert", "audioConvert");
		final IElement audioResample = Element.make("audioresample", "audioResample");
		final IElement audioScaleTempo = Element.make("scaletempo", "audioScaleTempo");
		final IElement audioConvertAfterScaleTempo = Element.make("audioconvert", "audioConvertAfterScaleTempo");
		final IElement audioResampleAfterScaleTempo = Element.make("audioresample", "audioResampleAfterScaleTempo");
		final IElement audioSink = createAudioSink(newMediaType, newRequest, newPipeline, audioElement);

		audioBin.addAndLinkMany(audioQueue, audioVolume, audioConvert, audioResample, audioScaleTempo, audioConvertAfterScaleTempo, audioResampleAfterScaleTempo, audioSink);

		currentAudioSink = audioSink;
		currentAudioVolumeElement = audioVolume;

		//Set this to whatever was previously set
		audioVolume.set("mute", muted);
		audioVolume.set("volume", (double)volume / 100.0D);

		//Element to connect uridecodebin src pad to.
		return audioQueue.staticPad("sink");
	}

	protected Pad createVideoBin(final MediaType newMediaType, final IMediaRequest newRequest, final IPipeline newPipeline, final IBin videoBin, final IBin uridecodebin, final Pad pad) {

		//[ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]

		final float checked_fps = (newRequest.getFPS() >= IMediaRequest.MINIMUM_FPS ? newRequest.getFPS() : IMediaRequest.DEFAULT_FPS);

		final IElement videoQueue;
		final IElement videoRate;
		final IElement videoColorspace;
		final IElement videoCapsFilter;
		final IElement videoScale;
		final IElement videoSink;

		System.out.println("createVideoBin: " + newPipeline.refCount());

		if (newMediaType != MediaType.Image) {
			if (!currentLiveSource) {
				videoQueue = Element.make("queue2", "videoQueue");
				videoRate = Element.make("videorate", "videoRate");
				videoColorspace = Element.make("ffmpegcolorspace", "videoColorspace");
				videoCapsFilter = Element.make("capsfilter", "videoCapsFilter");
				videoScale = Element.make("videoscale", "videoScale");
				videoSink = createVideoSink(newMediaType, newRequest, newPipeline, videoElement);

				videoRate.set("silent", true);
				videoCapsFilter.setCaps(Caps.from(Colorspace.createKnownColorspaceFilter(checked_fps == IMediaRequest.DEFAULT_FPS || currentLiveSource, checked_fps))); //framerate=25/1 means 25 FPS

				videoBin.addAndLinkMany(videoQueue, videoRate, videoCapsFilter, videoColorspace, videoScale, videoSink);
			} else {
				videoQueue = Element.make("queue2", "videoQueue");
				videoRate = null;
				videoColorspace = Element.make("ffmpegcolorspace", "videoColorspace");
				//videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
				//videoScale = ElementFactory.make("videoscale", "videoScale");
				videoSink = createVideoSink(newMediaType, newRequest, newPipeline, videoElement);

				//videoCapsFilter.setCaps(Caps.fromString(createColorspaceFilter(this, checked_fps))); //framerate=25/1 means 25 FPS

				videoBin.addAndLinkMany(videoQueue, videoColorspace, /*videoCapsFilter, videoScale,*/ videoSink);
			}

			currentVideoSink = videoSink;
			try {
				//If we're testing w/ a non XOverlay video sink (e.g. fakesink),
				//then this will through an exception.
				xoverlay = SWTOverlay.wrap(videoSink);
			} catch(Throwable t) {
				xoverlay = null;
				asyncRedraw();
			}
			
			//The paint listener is removed in the play()/playPattern() methods to ensure
			//that it's also removed for audio-only playback. The only time to insert it is
			//when we're drawing the single image ourselves.
		} else {
			videoSink = createImageSink(newMediaType, newRequest, newPipeline, videoElement);
			videoQueue = Element.make("queue2", "videoQueue");
			videoColorspace = Element.make("ffmpegcolorspace", "videoColorspace");
			videoCapsFilter = Element.make("capsfilter", "videoCapsFilter");
			videoCapsFilter.setCaps(Caps.from("video/x-raw-rgb, bpp=32, depth=24"));

			videoBin.addAndLinkMany(videoQueue, videoColorspace, videoCapsFilter, videoSink);

			currentVideoSink = videoSink;
			insertPaintListener();
		}

		System.out.println("createVideoBin: " + newPipeline.refCount());

		return videoQueue.staticPad("sink");
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Signals">
	protected void onUriDecodeBinElementAdded(final IPipeline newPipeline, final IBin uridecodebin, final IElement element) {
		//<editor-fold defaultstate="collapsed" desc="Validate arguments">
		//We only care to modify the element if we're using a live source
		if (!currentLiveSource || element == null)
			return;
		final String factoryName = element.getFactoryName();
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

	protected void onDecodeBinElementAdded(final IPipeline newPipeline, final IBin uridecodebin, final IBin decodebin, final IElement element) {
		//<editor-fold defaultstate="collapsed" desc="Validate arguments">
		//Determine if what we're looking at is a multipartdemux element
		final String factoryName = element.getFactoryName();
		if (!"multipartdemux".equalsIgnoreCase(factoryName))
			return;
		//</editor-fold>

		hasMultipartDemux = true;

		//Informs multipartdemux elements that it needs to emit the pad added signal as
		//soon as it links the pads. Otherwise it could be some time before it happens.
		//This was primarily added to instantly connect to motion jpeg digital cameras
		//that have a low framerate (e.g. 1 or 2 FPS).
		//It could be an issue with a "live source" that is emitting multiple streams
		//via a multipart mux. There's really not much we can do about something like
		//that in an automatic way -- that is, you'd have to remove this and instead
		//use a custom pipeline to work w/ low framerate digital cameras.
		if (element.hasProperty("single-stream"))
			element.set("single-stream", true);
	}
	
	protected void onPadAdded(final IMediaRequest newRequest, final IPipeline newPipeline, final IBin uridecodebin, final Pad pad) {
		//only link once
		if (pad.isLinked())
			return;

		System.out.println("onPadAdded: " + newPipeline.refCount());

		//check media type
		final Caps caps = pad.getCaps();
		final Structure struct = caps.structureAt(0);
		final String padCaps = struct.name();

		if (StringUtil.isNullOrEmpty(padCaps)) {
			caps.dispose();
			struct.dispose();
			return;
		}

		if (padCaps.startsWith("audio/")) {
			disposeAudioBin(newPipeline);

			hasAudio = true;
			if (mediaType == MediaType.Unknown && mediaType != MediaType.Video)
				mediaType = MediaType.Audio;

			//Create audio bin
			final IBin audioBin = Bin.make("audioBin");
			audioBin.addPad(new GhostPad("sink", createAudioBin(mediaType, newRequest, newPipeline, audioBin, uridecodebin, pad)));
			newPipeline.add(audioBin);
			pad.link(audioBin.staticPad("sink"));

			audioBin.changeState(State.Playing);
		} else if (padCaps.startsWith("video/")) {
			disposeVideoBin(newPipeline);

			hasVideo = true;
			if (mediaType == MediaType.Unknown || mediaType == MediaType.Audio)
				mediaType = (!determineIfSingleImage(uridecodebin) ? MediaType.Video : MediaType.Image);

			//Create video bin
			final IBin videoBin = Bin.make("videoBin");
			videoBin.addPad(new GhostPad("sink", createVideoBin(mediaType, newRequest, newPipeline, videoBin, uridecodebin, pad)));
			newPipeline.add(videoBin);
			pad.link(videoBin.staticPad("sink"));

			videoBin.changeState(State.Playing);
		}

		System.out.println("onPadAdded: " + newPipeline.refCount());
	}

	protected void onImageSinkHandoff(final IPipeline newPipeline, final Buffer buffer) {
		singleImage = swtImageDataSnapshot(buffer);
		display.asyncExec(redrawRunnable);
	}

	protected boolean onNotifyCaps(final IPipeline newPipeline, final Pad pad) {
		System.out.println("onNotifyCaps: " + newPipeline.refCount());

		final Caps caps = pad.getNegotiatedCaps();

		System.out.println("onNotifyCaps: " + newPipeline.refCount());

		if (caps == null)
			return false;

		if (caps.isEmpty()) {
			caps.dispose();
			return false;
		}

		final Structure struct = caps.structureAt(0);
		if (struct == null)
			return false;

		if (struct.fieldExists("framerate")) {
			Fraction framerate = struct.fieldAsFraction("framerate");
			actualFPS = (float)framerate.getNumerator() / (float)framerate.getDenominator();
		}

		if (struct.fieldExists("width") && struct.fieldExists("height")) {
			final int width = struct.fieldAsInt("width");
			final int height = struct.fieldAsInt("height");
			videoWidth = width;
			videoHeight = height;
			fireVideoDimensionsNegotiated(width, height);
		}

		struct.dispose();
		caps.dispose();

		System.out.println("onNotifyCaps: " + newPipeline.refCount());
		return true;
	}
	
	@SuppressWarnings("empty-statement")
	protected void onStateChanged(final IPipeline newPipeline, final IBin uridecodebin, final State oldState, final State newState, final State pendingState) {
		//<editor-fold defaultstate="collapsed" desc="Fire state events">
		switch (newState) {
			case Playing:
				if (currentState == State.Null || currentState == State.Ready || currentState == State.Paused) {
					currentState = State.Playing;
					fireMediaEventPlayed();
				}
				break;
			case Paused:
				if (currentState == State.Playing || currentState == State.Ready) {
					currentState = State.Paused;
					fireMediaEventPaused();
				}
				break;
			case Null:
			case Ready:
				if (currentState == State.Playing || currentState == State.Paused) {
					currentState = State.Ready;
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

	protected void onBuffering(final IPipeline newPipeline, int percent) {
		if (!currentLiveSource) {
			if (percent < 100) {
				changeState(newPipeline, State.Paused);
			} else if (percent >= 100) {
				changeState(newPipeline, State.Playing);
			}
		}
	}

	protected void onSegmentDone(final IPipeline newPipeline) {
		onEOS(newPipeline);
	}

	protected void onEOS(final IPipeline newPipeline) {
		if (mediaType != MediaType.Image && (currentRepeatCount == IMediaRequest.REPEAT_FOREVER || (currentRepeatCount > 0 && numberOfRepeats < currentRepeatCount))) {
			++numberOfRepeats;
			if (!seekToBeginning()) {
				stop();
				unpause();
			}
			return;
		}
		numberOfRepeats = 0;
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (mediaType == MediaType.Image)
					changeState(State.Paused);
				else
					resetPipeline(newPipeline);
				if (!isDisposed())
					redraw();
			}
		});
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

	protected void onError(final IMediaRequest newRequest, final IPipeline newPipeline, int code, String message) {
		resetPipeline(newPipeline);
		fireHandleError(newRequest, ErrorType.fromNativeValue(code), code, message);
	}

	protected BusSyncReply onBusSyncHandler(final Message msg) {
		System.out.println("onBusSyncHandler: " + pipeline.refCount());
		Structure s = msg.getStructure();
		if (s == null || !s. nameEquals("prepare-xwindow-id"))
			return BusSyncReply.Pass;
		xoverlay.setWindowID(nativeHandle);
		System.out.println("onBusSyncHandler: " + pipeline.refCount());
		return BusSyncReply.Drop;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Play">
	public boolean play(IMediaRequest request) {
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

			final IPipeline newPipeline = createPipeline(request);
			pipeline = newPipeline;

			//Start playing
			changeState(newPipeline, State.Playing);

			System.out.println("play: " + newPipeline.refCount());
			return true;
		} catch(Throwable t) {
			t.printStackTrace();
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

	//<editor-fold defaultstate="collapsed" desc="TBD">
	@Override
	protected void componentInitialize() {
		//throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected Runnable createPositionUpdater() {
		//throw new UnsupportedOperationException("Not supported yet.");
		return null;
	}

	public boolean expose() {
		return false;
	}

	public Object getMediaLock() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Scheme[] getValidSchemes() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public MediaType getMediaType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getVideoWidth() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getVideoHeight() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public float getVideoFPS() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isMediaAvailable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isVideoAvailable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAudioAvailable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isSeekable() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isPaused() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isStopped() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isPlaying() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getRepeatCount() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getPosition() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getDuration() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isMuted() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getVolume() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public long getBufferSize() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isAspectRatioMaintained() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IMediaRequest getMediaRequest() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setBufferSize(long size) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean mute() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean adjustVolume(int percent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean seekToBeginning() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean adjustPlaybackRate(double rate) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean stop() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean pause() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean unpause() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean stepForward() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean stepBackward() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playBlackBurst() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playBlackBurst(String Title) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playTestSignal() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean playTestSignal(String Title) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(File file) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(String URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(boolean LiveSource, String URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(int RepeatCount, String URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(int RepeatCount, Scheme Scheme, String URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(String Title, int RepeatCount, Scheme Scheme, String URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(String Title, boolean LiveSource, int RepeatCount, Scheme Scheme, String URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(String Title, boolean LiveSource, boolean MaintainAspectRatio, int RepeatCount, Scheme Scheme, String URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(MediaRequestType RequestType, String Title, boolean LiveSource, boolean MaintainAspectRatio, int RepeatCount, float FPS, URI URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(MediaRequestType RequestType, String Title, boolean LiveSource, boolean MaintainAspectRatio, int RepeatCount, float FPS, Scheme Scheme, URI URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean play(MediaRequestType RequestType, long LastModifiedTime, String Title, boolean LiveSource, boolean MaintainAspectRatio, int RepeatCount, float FPS, Scheme Scheme, URI URI) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	//</editor-fold>
}

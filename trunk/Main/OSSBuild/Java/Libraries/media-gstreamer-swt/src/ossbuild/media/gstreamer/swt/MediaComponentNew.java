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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.swt.SWT;
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
import ossbuild.media.gstreamer.Colorspace;
import ossbuild.media.gstreamer.Element;
import ossbuild.media.gstreamer.ErrorType;
import ossbuild.media.gstreamer.IBin;
import ossbuild.media.gstreamer.IElement;
import ossbuild.media.gstreamer.IPipeline;
import ossbuild.media.gstreamer.Pipeline;
import ossbuild.media.gstreamer.State;
import ossbuild.media.gstreamer.api.GStreamer;
import ossbuild.media.gstreamer.api.GTypeConverters;
import ossbuild.media.gstreamer.api.Utils;
import ossbuild.media.gstreamer.signals.IElementAdded;
import ossbuild.media.gstreamer.signals.IPadAdded;

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
	private MediaType mediaType = MediaType.Unknown;
	private ImageData singleImage = null;
	protected final Display display;

	protected IElement currentVideoSink;
	protected IPipeline pipeline;

	protected final Runnable redrawRunnable;
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
	}

	public MediaComponentNew(Composite parent, int style) {
		this(DEFAULT_VIDEO_ELEMENT, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponentNew(String videoElement, Composite parent, int style) {
		this(videoElement, DEFAULT_AUDIO_ELEMENT, parent, style);
	}

	public MediaComponentNew(String videoElement, String audioElement, Composite parent, int style) {
		super(parent, style | SWT.EMBEDDED | SWT.DOUBLE_BUFFERED);

		this.display = getDisplay();
		
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
			newPipeline.dispose();
			pipeline = null;
			asyncRedraw();
		}
	}

	protected void cleanup(final IPipeline newPipeline) {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Create">
	protected synchronized IPipeline createPipeline(final IMediaRequest newRequest) {
		//gst-launch uridecodebin use-buffering=false name=dec location=http://.../video.avi
		//    dec. ! [ queue ! audioconvert ! audioresample ! autoaudiosink ]
		//    dec. ! [ queue ! videorate silent=true ! ffmpegcolorspace ! video/x-raw-rgb, bpp=32, depth=24 ! directdrawsink show-preroll-frame=true ]

		final IPipeline newPipeline = Pipeline.make("pipeline");
		final IBin uridecodebin = Bin.make("uridecodebin", "uridecodebin");

		uridecodebin.set("use-buffering", false);
		uridecodebin.set("download", false);
		uridecodebin.set("buffer-duration", TimeUnit.MILLISECONDS.toNanos(500L));
		uridecodebin.set("uri", Utils.toGstURI(newRequest.getURI()));

		newPipeline.add(uridecodebin);

		//<editor-fold defaultstate="collapsed" desc="UriDecodeBin Signals">
		uridecodebin.connect(new IPadAdded() {
			public void padAdded(Pointer pElement, Pointer pPad) {
				//onPadAdded(newRequest, newPipeline, uridecodebin, pad);
			}
		});
		uridecodebin.connect(new IElementAdded() {
			public void elementAdded(Pointer pBin, Pointer pElement) {
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
							IBin decodebin = Bin.from(pBin);
							IElement element = Element.from(pElement);
							//onDecodeBinElementAdded(newPipeline, uridecodebin, decodebin, element);
							element.dispose();
							decodebin.dispose();
						}
					});
				}
				//</editor-fold>

				//onUriDecodeBinElementAdded(newPipeline, uridecodebin, element);
				element.dispose();
			}
		});
		/*
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
		/**/
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Bus Signals">
		/*
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
		/**/
		//</editor-fold>

		return newPipeline;
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

			fireMediaEventPlayRequested(request);

			final IPipeline newPipeline = createPipeline(request);
			pipeline = newPipeline;

			pipeline.changeState(State.Playing);
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

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
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_STATES:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "3");
		Sys.initialize();

		Button btn;
		GridData gd;
		MediaComponent comp;
		final Display display = new Display();
		final Shell shell = new Shell(display, SWT.NORMAL | SWT.SHELL_TRIM);

		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;

		shell.setText("OSSBuild GStreamer Examples :: SWT");
		shell.setLayout(layout);
		shell.setSize(500, 500);

		comp = new MediaComponent(shell, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
//		final MediaComponent thisComp = comp;
//		comp.addVideoCapsListener(new MediaComponent.VideoCapsListenerAdapter() {
//			@Override
//			public void videoDimensionsNegotiated(int videoWidth, int videoHeight) {
//				display.syncExec(new Runnable() {
//					@Override
//					public void run() {
//						final Point sz = thisComp.getSize();
//
//						final int height = sz.y;
//						final int width = sz.x;
//						final int videoHeight = thisComp.getFullVideoHeight();
//						final int videoWidth = thisComp.getFullVideoWidth();
//						final int scaledHeight;
//						final int scaledWidth;
//						if (videoWidth > videoHeight) {
//							scaledWidth = (int)(((double)height / (double)videoHeight) * videoWidth);
//							scaledHeight = height;
//						} else {
//							scaledWidth = width;
//							scaledHeight = (int)(((double)width / (double)videoWidth) * videoWidth);
//						}
//						thisComp.setSize(scaledWidth, scaledHeight);
//						//shell.setLayout(null);
//						//shell.layout();
//					}
//				});
//			}
//		});

		comp = new MediaComponent(shell, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

//		comp = new MediaComponent(shell, SWT.NONE);
//		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
//		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
//
//		comp = new MediaComponent(shell, SWT.NONE);
//		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
//		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Button btnPlay = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlay.setLayoutData(gd);
		btnPlay.setText("Play Again");

		Button btnPause = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPause.setLayoutData(gd);
		btnPause.setText("Pause");

		Button btnContinue = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnContinue.setLayoutData(gd);
		btnContinue.setText("Continue");

		Button btnStop = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStop.setLayoutData(gd);
		btnStop.setText("Stop");

		Button btnRateNormal = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateNormal.setLayoutData(gd);
		btnRateNormal.setText("Normal Playback Rate");

		Button btnRateDouble = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateDouble.setLayoutData(gd);
		btnRateDouble.setText("Double Playback Rate");

		Button btnRateBackwards = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateBackwards.setLayoutData(gd);
		btnRateBackwards.setText("Play Backwards");

		Button btnRateDoubleBackwards = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateDoubleBackwards.setLayoutData(gd);
		btnRateDoubleBackwards.setText("Double Play Backwards Rate");

		Button btnStepForward = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStepForward.setLayoutData(gd);
		btnStepForward.setText("Step Forward");

		Button btnStepBackward = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStepBackward.setLayoutData(gd);
		btnStepBackward.setText("Step Backward");

		shell.open();

		final String fileName;
		final FileDialog selFile = new FileDialog(shell, SWT.OPEN);
		selFile.setFilterNames(new String[]{"All Files (*.*)"});
		selFile.setFilterExtensions(new String[]{"*.*"});
		if (StringUtil.isNullOrEmpty(fileName = selFile.open())) {
			Gst.quit();
			return;
		}

		final File file = new File(fileName);
		btnPlay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).play(file);
					}
				}
			}
		});
		btnPause.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).pause();
					}
				}
			}
		});
		btnContinue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).unpause();
					}
				}
			}
		});
		btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).stop();
					}
				}
			}
		});
		btnRateNormal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).adjustPlaybackRate(1.0D);
					}
				}
			}
		});
		btnRateDouble.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).adjustPlaybackRate(20.0D);
					}
				}
			}
		});
		btnRateBackwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).adjustPlaybackRate(-1.0D);
					}
				}
			}
		});
		btnRateDoubleBackwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).adjustPlaybackRate(-2.0D);
					}
				}
			}
		});

		btnStepForward.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).stepForward();
					}
				}
			}
		});
		btnStepBackward.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).stepBackward();
					}
				}
			}
		});

		for (Control c : shell.getChildren()) {
			if (c instanceof MediaComponent) {
				((MediaComponent) c).play(file);
			}
		}

		final MediaComponent mediaComp = comp;
		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(true) {
						mediaComp.saveSnapshot(new File(System.currentTimeMillis() + ".jpg"));
						Thread.sleep(5000L);
					}
				} catch(Throwable t) {
				}
			}
		});
		t.setDaemon(true);
		t.setName("Snapshots");
		t.start();

		//PlayBin2 playbin = new PlayBin2((String)null);
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();

		Gst.quit();
	}

	public static class MediaComponentPlayBin2 extends Composite {
		//<editor-fold defaultstate="collapsed" desc="Variables">

		private PlayBin2 playbin;
		private Element videoSink;
		private boolean buffering = false;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		public MediaComponentPlayBin2(Composite parent, int style) {
			super(parent, style | SWT.EMBEDDED);

			final Display display = getDisplay();

			//<editor-fold defaultstate="collapsed" desc="Determine video sink">
			String videoElement;
			switch (Sys.getOSFamily()) {
				case Windows:
					//videoElement = "dshowvideosink";
					videoElement = "directdrawsink";
					break;
				case Unix:
					videoElement = "xvimagesink"; //gconfaudiosink and gconfvideosink?
					break;
				default:
					videoElement = "xvimagesink";
					break;
			}
			videoSink = ElementFactory.make(videoElement, "video sink");
			//videoSink.set("force-aspect-ratio", true);
			//videoSink.set("renderer", "VMR9");
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Create playbin">
			playbin = new PlayBin2((String) null);
			playbin.setVideoSink(videoSink);
			//playbin.set("mute", true);
			//playbin.set("buffer-duration", 1000L); //1000 nanoseconds
			//playbin.set("buffer-size", 0);
			//playbin.set("buffer-duration", 0L);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Prepare XOverlay support">
			final CustomXOverlay overlay = CustomXOverlay.wrap(videoSink);
			final Runnable handleXOverlay = new Runnable() {

				@Override
				public void run() {
					overlay.setWindowID(MediaComponentPlayBin2.this);
				}
			};
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="Connect bus messages">
			final Bus bus = playbin.getBus();
			bus.connect(new Bus.EOS() {

				@Override
				public void endOfStream(GstObject go) {
					playbin.setState(State.NULL);
				}
			});
			bus.connect(new Bus.ERROR() {

				@Override
				public void errorMessage(GstObject go, int i, String string) {
					playbin.setState(State.NULL);
				}
			});
			bus.connect(new Bus.BUFFERING() {

				@Override
				public void bufferingData(GstObject source, int percent) {
					if (buffering) {
						if (percent < 100) {
							playbin.setState(State.PAUSED);
						} else if (percent >= 100) {
							playbin.setState(State.PLAYING);
						}
					}
					System.out.println(playbin.getState());
				}
			});
			bus.setSyncHandler(new BusSyncHandler() {

				@Override
				public BusSyncReply syncMessage(Message msg) {
					Structure s = msg.getStructure();
					if (s == null || !s.hasName("prepare-xwindow-id")) {
						return BusSyncReply.PASS;
					}
					display.syncExec(handleXOverlay);
					return BusSyncReply.DROP;
				}
			});
			//bus.
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="SWT Events">
			this.addDisposeListener(new DisposeListener() {

				@Override
				public void widgetDisposed(DisposeEvent de) {
					stop();
				}
			});
			//</editor-fold>
		}
		//</editor-fold>

		public void stop() {
			if (playbin.getState() != State.NULL) {
				playbin.setState(State.NULL);
			}
			this.redraw();
		}

		public void playFile(final File File) {
			if (playbin.getState() != State.NULL) {
				playbin.setState(State.NULL);
			}
			try {
				playbin.setInputFile(File);
				//playbin.setURI(new URI("http://mirrorblender.top-ix.org/peach/bigbuckbunny_movies/big_buck_bunny_480p_stereo.ogg"));
				//playbin.setURI(new URI("http://www.warwick.ac.uk/newwebcam/cgi-bin/webcam.pl?dummy=garb"));
				playbin.setState(State.PLAYING);
			} catch (Throwable t) {
				playbin.setState(State.NULL);
			}
		}
	}

	public static class MediaComponent extends Canvas {
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

		public static final int
			  SEEK_FLAG_SKIP = (1 << 4)
		;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Variables">
		protected static final Object snapshotPixelLock = new Object();
		protected static int[] snapshotPixels = null;

		protected final Object lock = new Object();
		protected final String videoElement;
		protected final String audioElement;
		protected final Display display;

		protected Pipeline pipeline;
		protected Element currentVideoSink;
		protected CustomXOverlay xoverlay = null;
		protected int fullVideoWidth = 0;
		protected int fullVideoHeight = 0;
		protected Runnable redrawRunnable;

		protected boolean currentBuffering;
		protected int currentRepeatCount;
		protected int currentFPS;
		protected URI currentURI;

		private CountDownLatch latch = new CountDownLatch(1);

		private List<IMediaEventListener> mediaEventListeners;
		private final Object mediaEventListenerLock = new Object();

		private List<IVideoCapsListener> videoCapsListeners;
		private final Object videoCapsListenerLock = new Object();

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
				return (state == State.NULL);
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
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Helper Methods">
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
			try {
				synchronized(lock) {
					if (currentVideoSink == null)
						return null;

					final Pointer ptr = currentVideoSink.getPointer("last-buffer");
					if (ptr == null)
						return null;
					final Buffer buffer = MiniObject.objectFor(ptr, Buffer.class, false);
					if (buffer == null)
						return null;

					final Caps caps = buffer.getCaps();
					final Structure struct = caps.getStructure(0);
					if (!"video/x-raw-rgb".equalsIgnoreCase(struct.getName()))
						return null;
					
					final int width = struct.getInteger("width");
					final int height = struct.getInteger("height");
					if (width < 1 || height < 1)
						return null;

					//use direct buffer
					final IntBuffer rgb = buffer.getByteBuffer().asIntBuffer();

					//Do what you need here
					final PaletteData paletteData = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
					final ImageData imageData = new ImageData(width, height, 24, paletteData);
					
					//Once allocated, keep this array around and only grow it if we must.
					//It's shared among all instances of this object, so please lock it before use.
					synchronized(snapshotPixelLock) {
						if (snapshotPixels == null || snapshotPixels.length > rgb.remaining())
							snapshotPixels = new int[rgb.remaining()];
						rgb.get(snapshotPixels, 0, rgb.remaining());
						imageData.setPixels(0, 0, snapshotPixels.length, snapshotPixels, 0);
					}
					
					//Cleanup ASAP
					buffer.dispose();

					return imageData;
				}
			} catch(Throwable t) {
				return null;
			}
		}

		public BufferedImage produceSnapshot() {
			try {
				synchronized(lock) {
					if (currentVideoSink == null)
						return null;

					final Pointer ptr = currentVideoSink.getPointer("last-buffer");
					if (ptr == null)
						return null;
					final Buffer buffer = MiniObject.objectFor(ptr, Buffer.class, false);
					if (buffer == null)
						return null;

					final Caps caps = buffer.getCaps();
					final Structure struct = caps.getStructure(0);
					if (!"video/x-raw-rgb".equalsIgnoreCase(struct.getName()))
						return null;

					final int width = struct.getInteger("width");
					final int height = struct.getInteger("height");
					if (width < 1 || height < 1)
						return null;

					//use direct buffer
					final IntBuffer rgb = buffer.getByteBuffer().asIntBuffer();

					final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					img.setAccelerationPriority(0.001f);
					rgb.get(((DataBufferInt)img.getRaster().getDataBuffer()).getData(), 0, rgb.remaining());

					//Cleanup ASAP
					buffer.dispose();

					return img;
				}
			} catch(Throwable t) {
				return null;
			}
		}
		//</editor-fold>

		public boolean adjustPlaybackRate(double rate) {
			if (rate == 0.0f)
				return pause();
			synchronized(lock) {
				if (pipeline == null)
					return false;

				//http://www.gstreamer.net/data/doc/gstreamer/head/gstreamer/html/gstreamer-GstEvent.html
				pipeline.setState(State.PAUSED);
				//boolean ret = pipeline.sendEvent(new SeekEvent(rate, Format.TIME, SeekFlags.ACCURATE, SeekType.SET, pipeline.queryPosition(Format.TIME), SeekType.NONE, -1));
				boolean ret = pipeline.seek(rate, Format.TIME, SEEK_FLAG_SKIP | SeekFlags.FLUSH | SeekFlags.ACCURATE, SeekType.SET, pipeline.queryPosition(Format.TIME), SeekType.NONE, -1);
				pipeline.setState(State.PLAYING);
				return ret;
			}
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
				pipeline.setState(State.PAUSED);
			}
			return true;
		}

		public boolean unpause() {
			synchronized(lock) {
				if (pipeline == null)
					return false;
				pipeline.setState(State.PLAYING);
			}
			return true;
		}

		public boolean stop() {
			synchronized(lock) {
				if (pipeline == null)// || pipeline.getState(0L) == State.NULL)
					return true;
				pipeline.setState(State.NULL);
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

				//gst-launch filesrc/souphttpsrc location=http://.../video.avi ! queue ! decodebin2 name=dec
				//    dec. ! queue ! tee name=t \
				//        t. ! ffmpegcolorspace ! videoscale ! directdrawsink \
				//        t. ! queue ! identity ! ffmpegcolorspace ! videorate ! video/x-raw-rgb, bpp=32, depth=24, framerate=3000/1000 ! fakesink \
				//    dec. ! queue ! audioconvert ! audioresample ! autoaudiosink
				pipeline = new Pipeline("pipeline");

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
				final Element teeQueue = ElementFactory.make("queue", "teeQueue");
				final Element teeVideo = ElementFactory.make("tee", "videoTee");
				videoBin.addMany(teeQueue, teeVideo);
				Element.linkMany(teeQueue, teeVideo);
				//</editor-fold>

				//<editor-fold defaultstate="collapsed" desc="Video">
				final Element videoQueue = ElementFactory.make("queue", "videoQueue");
				final Element videoRate = ElementFactory.make("videorate", "videoRate");
				videoRate.set("silent", true);
				final Element videoColorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
				final Element videoCapsFilter = ElementFactory.make("capsfilter", "videoCapsFilter");
				videoCapsFilter.setCaps(Caps.fromString("video/x-raw-rgb, bpp=32, depth=24" + (checked_fps == DEFAULT_FPS ? StringUtil.empty : ", framerate=" + checked_fps + "/1"))); //framerate=25/1 means 25 FPS
				final Element videoScale = ElementFactory.make("videoscale", "videoScale");
				final Element videoSink = ElementFactory.make(videoElement, "videoSink");
				videoBin.addMany(videoQueue, videoRate, videoColorspace, videoCapsFilter, videoScale, videoSink);
				Element.linkMany(teeVideo, videoQueue, videoRate, videoColorspace, videoCapsFilter, videoScale, videoSink);
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

				videoBin.addPad(new GhostPad("sink", teeQueue.getStaticPad("sink")));
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
					public void stateChanged(GstObject source, State old, State current, State pending) {
						if (current == State.NULL && pending == State.NULL) {
							synchronized(lock) {
								pipeline.dispose();
								pipeline = null;
								currentVideoSink = null;
							}
							display.asyncExec(redrawRunnable);
							latch.countDown();
						}
					}
				});
				bus.connect(new Bus.ERROR() {
					@Override
					public void errorMessage(GstObject source, int code, String message) {
						//System.out.println("Error: code=" + code + " message=" + message);
						pipeline.setState(State.NULL);
						display.asyncExec(redrawRunnable);
						fireHandleError(uri, ErrorType.fromNativeValue(code), code, message);
					}
				});
				bus.connect(new Bus.EOS() {
					@Override
					public void endOfStream(GstObject source) {
						pipeline.setState(State.NULL);
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
}

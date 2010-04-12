package simple.swt;

import com.sun.jna.Pointer;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.Bin;
import org.gstreamer.Bus;
import org.gstreamer.BusSyncReply;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GObject;
import org.gstreamer.GhostPad;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Message;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.gstreamer.elements.PlayBin2;
import org.gstreamer.event.BusSyncHandler;
import ossbuild.OSFamily;
import ossbuild.StringUtil;
import ossbuild.Sys;

public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
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

		comp = new MediaComponent(shell, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		comp = new MediaComponent(shell, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		comp = new MediaComponent(shell, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Button btnPlay = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlay.setLayoutData(gd);
		btnPlay.setText("Play Again");

		Button btnStop = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStop.setLayoutData(gd);
		btnStop.setText("Stop");

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

		for (Control c : shell.getChildren()) {
			if (c instanceof MediaComponent) {
				((MediaComponent) c).play(file);
			}
		}

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

	public static class MediaComponent extends Composite {
		//<editor-fold defaultstate="collapsed" desc="Constants">
		public static final String
			  DEFAULT_VIDEO_ELEMENT
			, DEFAULT_AUDIO_ELEMENT
		;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Variables">
		private final Object lock = new Object();
		private final String videoElement;
		private final String audioElement;
		private final Display display;

		private Pipeline pipeline;
		private boolean buffering = false;

		private CountDownLatch latch = new CountDownLatch(1);
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
			super(parent, style | SWT.EMBEDDED);

			this.display = getDisplay();
			this.audioElement = audioElement;
			this.videoElement = videoElement;
			this.setLayout(new FillLayout());

			//Ensure that we're at 0 so the first call to play can proceed immediately
			latch.countDown();

			//<editor-fold defaultstate="collapsed" desc="SWT Events">
			this.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent ce) {
					//pipeline.getElementByName("videoSink");
					redraw();
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

		public boolean stop() {
			synchronized(lock) {
				if (pipeline == null || pipeline.getState() == State.NULL)
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
		
		public boolean play(URI uri) {
			if (uri == null)
				return false;
			
			synchronized(lock) {
				stop();

				try {
					if (!latch.await(2000L, TimeUnit.MILLISECONDS))
						return false;
				} catch(InterruptedException ie) {
					return false;
				}

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
				decodebin2.set("use-buffering", false);
				//</editor-fold>

				//<editor-fold defaultstate="collapsed" desc="Link decodebin">
				pipeline.addMany(urisrc, decodeQueue, decodebin2);
				Element.linkMany(urisrc, decodeQueue, decodebin2);
				//</editor-fold>

				//<editor-fold defaultstate="collapsed" desc="Create audio output">
				/* create audio output */
				final Bin audioBin = new Bin("Audio Bin");

				final Element audioConvert = ElementFactory.make("audioconvert", "audioConvert");
				final Element audioResample = ElementFactory.make("audioresample", "audioResample");
				final Element audioSink = ElementFactory.make(audioElement, "sink");
				final Element firstElementInAudioBin = audioConvert;

				audioBin.addMany(audioConvert, audioResample, audioSink);
				Element.linkMany(audioConvert, audioResample, audioSink);
				audioBin.addPad(new GhostPad("sink", firstElementInAudioBin.getStaticPad("sink")));
				pipeline.add(audioBin);
				//</editor-fold>

				//<editor-fold defaultstate="collapsed" desc="Create video output">
				/* create video output */
				final Bin videoBin = new Bin("Video Bin");

				final Element ffmpegcolorspace = ElementFactory.make("ffmpegcolorspace", "colorspace");
				//final Element videoQueue = ElementFactory.make("queue", "videoQueue");
				final Element videoScale = ElementFactory.make("videoscale", "videoScale");
				final Element videoSink = ElementFactory.make(videoElement, "videoSink");
				final Element firstElementInVideoBin = ffmpegcolorspace;

				videoBin.addMany(ffmpegcolorspace, videoScale, videoSink);
				Element.linkMany(ffmpegcolorspace, videoScale, videoSink);
				videoBin.addPad(new GhostPad("sink", firstElementInVideoBin.getStaticPad("sink")));
				pipeline.add(videoBin);
				//</editor-fold>

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

				final Bus bus = pipeline.getBus();

				bus.connect(new Bus.STATE_CHANGED() {
					@Override
					public void stateChanged(GstObject source, State old, State current, State pending) {
						if (current == State.NULL && pending == State.NULL) {
							//pipeline.dispose();
							latch.countDown();
						}
					}
				});
				bus.connect(new Bus.ERROR() {
					@Override
					public void errorMessage(GstObject source, int code, String message) {
						//System.out.println("Error: code=" + code + " message=" + message);
						pipeline.setState(State.NULL);
					}
				});
				bus.connect(new Bus.EOS() {
					@Override
					public void endOfStream(GstObject source) {
						pipeline.setState(State.NULL);
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
							if (s == null || !s.hasName("prepare-xwindow-id")) {
								return BusSyncReply.PASS;
							}
							 CustomXOverlay.wrap(videoSink).setWindowID(MediaComponent.this);
							return BusSyncReply.DROP;
						}
					});
				} else {
					 CustomXOverlay.wrap(videoSink).setWindowID(MediaComponent.this);
				}
			}

			pipeline.setState(State.PLAYING);

			return true;
		}
	}
}

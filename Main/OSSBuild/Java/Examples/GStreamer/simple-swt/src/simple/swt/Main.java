package simple.swt;

import java.io.File;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.Bus;
import org.gstreamer.BusSyncReply;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;
import org.gstreamer.Message;
import org.gstreamer.State;
import org.gstreamer.Structure;
import org.gstreamer.elements.PlayBin2;
import org.gstreamer.event.BusSyncHandler;
import ossbuild.StringUtil;
import ossbuild.Sys;

public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_STATES:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_EVENT:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_CAPS:5");
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

		final Button btnBrowse = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnBrowse.setLayoutData(gd);
		btnBrowse.setText("Browse...");

		final Button btnPlay = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlay.setLayoutData(gd);
		btnPlay.setText("Play Again");

		final Button btnPause = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPause.setLayoutData(gd);
		btnPause.setText("Pause");

		final Button btnContinue = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnContinue.setLayoutData(gd);
		btnContinue.setText("Continue");

		final Button btnStop = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStop.setLayoutData(gd);
		btnStop.setText("Stop");

		final Button btnRateNormal = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateNormal.setLayoutData(gd);
		btnRateNormal.setText("Normal Playback Rate");

		final Button btnRateDouble = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateDouble.setLayoutData(gd);
		btnRateDouble.setText("Double Playback Rate");

		final Button btnRateBackwards = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateBackwards.setLayoutData(gd);
		btnRateBackwards.setText("Play Backwards");

		final Button btnRateDoubleBackwards = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateDoubleBackwards.setLayoutData(gd);
		btnRateDoubleBackwards.setText("Double Play Backwards Rate");

		final Button btnStepForward = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStepForward.setLayoutData(gd);
		btnStepForward.setText("Step Forward");

		final Button btnStepBackward = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStepBackward.setLayoutData(gd);
		btnStepBackward.setText("Step Backward");

		final Button btnSnapshot = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnSnapshot.setLayoutData(gd);
		btnSnapshot.setText("Take Snapshot");

		final Button btnSeekToBeginning = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnSeekToBeginning.setLayoutData(gd);
		btnSeekToBeginning.setText("Seek to Beginning");

		shell.open();

		final String fileName;
		final FileDialog selFile = new FileDialog(shell, SWT.OPEN);
		selFile.setFilterNames(new String[]{"All Files (*.*)"});
		selFile.setFilterExtensions(new String[]{"*.*"});
		if (StringUtil.isNullOrEmpty(fileName = selFile.open())) {
			Gst.quit();
			return;
		}

		final File[] file = new File[] { new File(fileName) };
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String fileName;
				final FileDialog selFile = new FileDialog(shell, SWT.OPEN);
				selFile.setFilterNames(new String[]{"All Files (*.*)"});
				selFile.setFilterExtensions(new String[]{"*.*"});
				if (StringUtil.isNullOrEmpty(fileName = selFile.open()))
					return;
				file[0] = new File(fileName);
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).play(file[0]);
					}
				}
			}
		});
		btnPlay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).play(false, 0, MediaComponent.DEFAULT_FPS, file[0].toURI());
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
						((MediaComponent) c).adjustPlaybackRate(2.0D);
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
		btnSeekToBeginning.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).seekToBeginning();
					}
				}
			}
		});
		final MediaComponent mediaComp = comp;
		btnSnapshot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mediaComp.saveSnapshot(new File(System.currentTimeMillis() + ".jpg"));
			}
		});

		mediaComp.addPositionListener(new MediaComponent.IPositionListener() {
			@Override
			public void positionChanged(MediaComponent source, int percent, long position, long duration) {
				System.out.println("percent: " + percent + ", position: " + position + ", duration: " + duration);
			}
		});

		for (Control c : shell.getChildren()) {
			if (c instanceof MediaComponent) {
				((MediaComponent) c).play(false, 0, MediaComponent.DEFAULT_FPS, file[0].toURI());
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
}

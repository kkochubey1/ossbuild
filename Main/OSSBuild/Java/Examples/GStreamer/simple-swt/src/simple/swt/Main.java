package simple.swt;

import java.io.File;
import java.util.concurrent.TimeUnit;
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
import org.eclipse.swt.widgets.Scale;
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
import org.gstreamer.swt.overlay.SWTOverlay;
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
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_ELEMENT_PADS:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:2,GST_CAPS*:3,decodebin*:4,jpeg*:4");
		//Sys.setEnvironmentVariable("GST_DEBUG", "typefindfunctions*:4,jpeg*:4");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:2,GST_CAPS*:3,decodebin*:4,jpeg*:4,queue*:4,multipart*:4");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:2,GST_CAPS*:3,videotestsrc*:4");
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

		comp = new MediaComponent(shell, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		comp = new MediaComponent(shell, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		final Scale scale = new Scale(shell, SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		scale.setEnabled(false);
		scale.setLayoutData(gd);
		scale.setIncrement(100);
		scale.setPageIncrement(1000);

		final Button btnBrowse = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnBrowse.setLayoutData(gd);
		btnBrowse.setText("Browse...");

		final Button btnPlayMJPEG = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayMJPEG.setLayoutData(gd);
		btnPlayMJPEG.setText("Play MJPEG");

		final Button btnPlay = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlay.setLayoutData(gd);
		btnPlay.setText("Play Again");

		final Button btnPlayTestSignal = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayTestSignal.setLayoutData(gd);
		btnPlayTestSignal.setText("Play Test Signal");

		final Button btnPlayBlackBurst = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayBlackBurst.setLayoutData(gd);
		btnPlayBlackBurst.setText("Play Blackburst");

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

		final Button btnMute = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnMute.setLayoutData(gd);
		btnMute.setText("Mute/Unmute");

		final Button btnVolume0 = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnVolume0.setLayoutData(gd);
		btnVolume0.setText("Volume 0%");

		final Button btnVolume50 = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnVolume50.setLayoutData(gd);
		btnVolume50.setText("Volume 50%");

		final Button btnVolume100 = new Button(shell, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnVolume100.setLayoutData(gd);
		btnVolume100.setText("Volume 100%");

		shell.open();

		final String fileName = "";
		final FileDialog selFile = new FileDialog(shell, SWT.OPEN);
		selFile.setFilterNames(new String[]{"All Files (*.*)"});
		selFile.setFilterExtensions(new String[]{"*.*"});
//		if (StringUtil.isNullOrEmpty(fileName = selFile.open())) {
//			Gst.quit();
//			return;
//		}

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
						((MediaComponent) c).play(false, MediaComponent.DEFAULT_REPEAT_COUNT, MediaComponent.DEFAULT_FPS, file[0].toURI());
					}
				}
			}
		});
		btnPlayMJPEG.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						MediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								//((MediaComponent) c).play(true, MediaComponent.DEFAULT_REPEAT_COUNT, MediaComponent.DEFAULT_FPS, "http://www.warwick.ac.uk/newwebcam/cgi-bin/webcam.pl?dummy=garb");
								((MediaComponent) c).play(true, MediaComponent.DEFAULT_REPEAT_COUNT, MediaComponent.DEFAULT_FPS, "http://129.125.136.20/axis-cgi/mjpg/video.cgi?camera=1");
								//((MediaComponent) c).play(false, MediaComponent.DEFAULT_REPEAT_COUNT, MediaComponent.DEFAULT_FPS, "rtsp://s-0-1.sg.softspb.com:554/test/test.mp4");
								//((MediaComponent) c).play(false, MediaComponent.DEFAULT_REPEAT_COUNT, MediaComponent.DEFAULT_FPS, "http://users.design.ucla.edu/~acolubri/test/gstreamer/station-svq1.mov");
							}
						});
					}
				}
			}
		});
		btnPlayBlackBurst.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						MediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((MediaComponent) c).playBlackBurst();
							}
						});
					}
				}
			}
		});
		btnPlayTestSignal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						MediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((MediaComponent) c).playTestSignal();
							}
						});
					}
				}
			}
		});
		btnPlay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						MediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((MediaComponent) c).play(false, 1/*MediaComponent.REPEAT_FOREVER/**/, MediaComponent.DEFAULT_FPS, file[0].toURI());
							}
						});
					}
				}
			}
		});
		btnPause.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						MediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((MediaComponent) c).pause();
							}
						});
					}
				}
			}
		});
		btnContinue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						MediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((MediaComponent) c).unpause();
							}
						});
					}
				}
			}
		});
		btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						MediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((MediaComponent) c).stop();
							}
						});
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

		btnMute.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).mute();
					}
				}
			}
		});
		btnVolume0.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).adjustVolume(0);
					}
				}
			}
		});
		btnVolume50.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).adjustVolume(50);
					}
				}
			}
		});
		btnVolume100.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).adjustVolume(100);
					}
				}
			}
		});

		mediaComp.addAudioListener(new MediaComponent.IAudioListener() {
			@Override
			public void audioMuted(MediaComponent source) {
				System.out.println("muted");
			}

			@Override
			public void audioUnmuted(MediaComponent source) {
				System.out.println("unmuted");
			}

			@Override
			public void audioVolumeChanged(MediaComponent source, int percent) {
				System.out.println("volume change: " + percent);
			}
		});

		mediaComp.addMediaEventListener(new MediaComponent.IMediaEventListener() {
			@Override
			public void mediaStopped(final MediaComponent source) {
				System.out.println("STOPPED");
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (scale.isDisposed())
							return;

						scale.setEnabled(false);
						scale.setSelection(0);
					}
				});
			}

			@Override
			public void mediaPaused(final MediaComponent source) {
				System.out.println("PAUSED");
			}

			@Override
			public void mediaContinued(final MediaComponent source) {
				System.out.println("CONTINUED");
				enableScale(source);
			}

			@Override
			public void mediaPlayed(final MediaComponent source) {
				System.out.println("PLAYED: " + source.getRequestedURI().toString());
				enableScale(source);
			}

			private void enableScale(final MediaComponent source) {
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (scale.isDisposed())
							return;

						scale.setEnabled(source.isSeekable() && scale.getMinimum() < scale.getMaximum());
					}
				});
			}
		});

		mediaComp.addPositionListener(new MediaComponent.IPositionListener() {
			private long lastDuration = 0L;
			private long lastPosition = 0L;

			@Override
			public void positionChanged(final MediaComponent source, final int percent, final long position, final long duration) {
				System.out.println("percent: " + percent + ", position: " + position + ", duration: " + duration);
				if (position != lastPosition || duration != lastDuration) {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							if (scale.isDisposed())
								return;
							
							if (duration != lastDuration) {
								lastDuration = duration;
								if (duration > 0) {
									int totalSeconds = (int)TimeUnit.MILLISECONDS.toSeconds(duration);
									
									scale.setEnabled(true);
									scale.setMinimum(0);
									scale.setMaximum(totalSeconds);
									scale.setIncrement(1);
									if (totalSeconds < 10) //10 seconds
										scale.setPageIncrement(1);
									else if (totalSeconds < 60) //1 minutes
										scale.setPageIncrement(6);
									else if (totalSeconds < 600) //10 minutes
										scale.setPageIncrement(60);
									else if (totalSeconds < 60 * 60) //1 hour
										scale.setPageIncrement(60 * 6);
									else if (totalSeconds < 60 * 60 * 2) //2 hours
										scale.setPageIncrement(60 * 10);
									else if (totalSeconds < 60 * 60 * 24) //1 day
										scale.setPageIncrement(60 * 60);
									else
										scale.setPageIncrement(60 * 60 * 4);
								}
							}
							
							if (position != lastPosition) {
								lastPosition = position;
								scale.setSelection((int)TimeUnit.MILLISECONDS.toSeconds(position) + scale.getMinimum());
							}
						}
					});
				}
			}
		});

		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final long position = TimeUnit.SECONDS.toNanos(scale.getSelection());
				for (Control c : shell.getChildren()) {
					if (c instanceof MediaComponent) {
						((MediaComponent) c).seek(position);
					}
				}
			}
		});

//		for (Control c : shell.getChildren()) {
//			if (c instanceof MediaComponent) {
//				//((MediaComponent) c).play(false, 0, MediaComponent.DEFAULT_FPS, file[0].toURI());
//				((MediaComponent)c).play("http://www.warwick.ac.uk/newwebcam/cgi-bin/webcam.pl?dummy=garb");
//			}
//		}

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
			final SWTOverlay overlay = SWTOverlay.wrap(videoSink);
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

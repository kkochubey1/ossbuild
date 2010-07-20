package simple.swt;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.gstreamer.Gst;
import ossbuild.NativeResource;
import ossbuild.Path;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.extract.IResourcePackage;
import ossbuild.extract.IResourceProcessor;
import ossbuild.extract.ResourceCallback;
import ossbuild.extract.ResourceProgressListenerAdapter;
import ossbuild.extract.Resources;
import ossbuild.extract.processors.FileProcessor;
import ossbuild.init.SystemLoaderInitializeListenerAdapter;
import ossbuild.media.IMediaPlayer;
import ossbuild.media.IMediaRequest;
import ossbuild.media.events.IAudioListener;
import ossbuild.media.events.IMediaEventListener;
import ossbuild.media.events.IPositionListener;
import ossbuild.media.gstreamer.swt.GstMediaComponent;

public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		ConsoleHandler handler = new ConsoleHandler();
		Logger logger = Logger.getLogger(org.gstreamer.lowlevel.NativeObject.class.getName());
		handler.setLevel(Level.SEVERE);
		logger.setLevel(Level.SEVERE);
		logger.addHandler(handler);
		
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_STATES:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_EVENT:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_CAPS:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_ELEMENT_PADS:5");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:2,GST_CAPS*:3,decodebin*:4,jpeg*:4");
		//Sys.setEnvironmentVariable("GST_DEBUG", "typefindfunctions*:4,jpeg*:4");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:2,GST_CAPS*:3,decodebin*:4,jpeg*:4,queue*:4,multipart*:4");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:2,GST_CAPS*:3,videotestsrc*:4");
		Sys.setEnvironmentVariable("GST_DEBUG", "autovideosink:4,d3dvideosink:4");
		
		Sys.initializeRegistry();
		Sys.loadNativeResources(NativeResource.Base);
		Sys.loadNativeResources(NativeResource.XML);
		Sys.loadNativeResources(NativeResource.GLib);
		Sys.loadNativeResources(NativeResource.Images);
		Sys.loadNativeResources(NativeResource.Fonts);
		Sys.loadNativeResources(NativeResource.Graphics);
		Sys.loadNativeResources(NativeResource.SWT);

		final Display display = new Display();
		final Shell shell = new Shell(display, SWT.NONE);

		//Create splash screen
		final Shell splash = new Shell(shell, SWT.NONE | SWT.ON_TOP);
		final ProgressBar progress = new ProgressBar(splash, SWT.SMOOTH);
		final Label title = new Label(splash, SWT.CENTER);
		final Label label = new Label(splash, SWT.NORMAL);
		final Rectangle area = display.getPrimaryMonitor().getClientArea();
		
		title.setText("OSSBuild GStreamer Example");
		title.setFont(new Font(display, "DejaVu Sans", 16, SWT.BOLD));
		title.setForeground(display.getSystemColor(SWT.COLOR_WHITE));

		label.setText("");
		label.setFont(new Font(display, "DejaVu Sans", 7, SWT.NORMAL));
		label.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		
		title.setBounds(0, 10, 320, 50);
		label.setBounds(10, 60, 300, 20);
		progress.setBounds(10, 90, 300, 12);
		splash.setBounds((area.width - 320) / 2, (area.height - 240) / 2, 320, 240);
		splash.setBackgroundMode(SWT.INHERIT_DEFAULT);
		//splash.setBackgroundImage(new Image(display, Main.class.getResourceAsStream("/resources/media/splash.jpg")));
		splash.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		splash.open();
		
		try {
			Sys.loadNativeResourcesAsync(
				NativeResource.GStreamer,

				new ResourceProgressListenerAdapter() {
					@Override
					public void error(final Throwable exception, String message) {
						display.asyncExec(new Runnable() {
							@Override
							public void run() {
								exception.printStackTrace();
								MessageBox mb = new MessageBox(splash, SWT.ICON_ERROR | SWT.OK);
								mb.setText("Error Initializing Application");
								mb.setMessage("Unable to extract and load GStreamer libraries for this platform or JVM.\n\nError: " + exception.getMessage());
								mb.open();
							}
						});
					}

					@Override
					public void begin(int totalNumberOfResources, int totalNumberOfPackages, long totalNumberOfBytes, long startTime) {
						display.asyncExec(new Runnable() {
							@Override
							public void run() {
								progress.setIndeterminate(false);
							}
						});
					}

					@Override
					public void reportMessage(IResourceProcessor resource, IResourcePackage pkg, final String key, final String message) {
						display.asyncExec(new Runnable() {
							@Override
							public void run() {
								if (!StringUtil.isNullOrEmpty(message))
									label.setText(message);
								else
									label.setText(" ");
							}
						});
					}

					@Override
					public void reportResourceComplete(IResourceProcessor resource, IResourcePackage pkg, final int totalNumberOfResources, final int totalNumberOfPackages, final long totalNumberOfBytes, final long numberOfBytesCompleted, final int numberOfResourcesCompleted, final int numberOfPackagesCompleted, final long startTime, final long duration, final String message) {
						display.asyncExec(new Runnable() {
							@Override
							public void run() {
								double percent = ((double)numberOfResourcesCompleted / (double)totalNumberOfResources);
								progress.setSelection(progress.getMinimum() + (int)(Math.abs(progress.getMaximum() - progress.getMinimum()) * percent));
							}
						});
					}

					@Override
					public void end(boolean success, int totalNumberOfResources, int totalNumberOfPackages, long totalNumberOfBytes, long numberOfBytesCompleted, int numberOfResourcesCompleted, int numberOfPackagesCompleted, long startTime, long endTime) {
						if (success) {
							display.asyncExec(new Runnable() {
								@Override
								public void run() {
									label.setText("Initializing system...");
									progress.setIndeterminate(true);
								}
							});
						} else {
							display.asyncExec(new Runnable() {
								@Override
								public void run() {
									splash.close();
									splash.dispose();
									System.exit(1);
								}
							});
						}
					}
				},

				new ResourceCallback() {
					@Override
					protected void completed(Resources rsrcs, Object t) {
						try {
							Sys.initializeSystem(new SystemLoaderInitializeListenerAdapter() {
								@Override
								public void afterAllSystemLoadersInitialized() {
									Sys.cleanRegistry();
									Gst.init();

									display.asyncExec(new Runnable() {
										@Override
										public void run() {
											splash.close();
											splash.dispose();

											loadUI(display, shell);
										}
									});
								}
							});
						} catch (Throwable tr) {
						}
					}
				}
			);
		} catch(Throwable t) {
			splash.close();
			splash.dispose();
			MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			mb.setText("Error Initializing Application");
			mb.setMessage("Unable to extract and load GStreamer libraries for this platform or JVM.");
			mb.open();
			shell.close();
			shell.dispose();
			System.exit(1);
		}

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
		System.exit(0);
	}

	private static void loadUI(final Display display, final Shell topShell) {
		final Shell dlg = new Shell(topShell, SWT.NORMAL | SWT.SHELL_TRIM);

		dlg.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent de) {
				topShell.close();
				topShell.dispose();
			}
		});

		Button btn;
		GridData gd;
		GstMediaComponent comp;

		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;

		dlg.setText("OSSBuild GStreamer Examples :: SWT");
		dlg.setLayout(layout);
		dlg.setSize(700, 700);

		comp = new GstMediaComponent(dlg, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		final GstMediaComponent mediaComp = comp;

		comp = new GstMediaComponent(dlg, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		comp = new GstMediaComponent(dlg, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		comp = new GstMediaComponent(dlg, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
//
//		comp = new GstMediaComponent(dlg, SWT.NONE);
//		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
//		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
//
//		comp = new GstMediaComponent(dlg, SWT.NONE);
//		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
//		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
//
//		comp = new GstMediaComponent(dlg, SWT.NONE);
//		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
//		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
//
//		comp = new GstMediaComponent(dlg, SWT.NONE);
//		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
//		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		final Scale scale = new Scale(dlg, SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		scale.setEnabled(false);
		scale.setLayoutData(gd);
		scale.setIncrement(100);
		scale.setPageIncrement(1000);

		final Button btnBrowse = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnBrowse.setLayoutData(gd);
		btnBrowse.setText("Browse...");

		final Button btnPlayImage = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayImage.setLayoutData(gd);
		btnPlayImage.setText("Play Image");

		final Button btnPlayMJPEG = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayMJPEG.setLayoutData(gd);
		btnPlayMJPEG.setText("Play MJPEG");

		final Button btnPlayExample = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayExample.setLayoutData(gd);
		btnPlayExample.setText("Play Example");

		final Button btnPlayExampleMJPG = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayExampleMJPG.setLayoutData(gd);
		btnPlayExampleMJPG.setText("Play Example MJPG");

		final Button btnPlay = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlay.setLayoutData(gd);
		btnPlay.setText("Play Again");

		final Button btnPlayForever = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayForever.setLayoutData(gd);
		btnPlayForever.setText("Play Again Forever");

		final Button btnPlayTestSignal = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayTestSignal.setLayoutData(gd);
		btnPlayTestSignal.setText("Play Test Signal");

		final Button btnPlayBlackBurst = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPlayBlackBurst.setLayoutData(gd);
		btnPlayBlackBurst.setText("Play Blackburst");

		final Button btnPause = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnPause.setLayoutData(gd);
		btnPause.setText("Pause");

		final Button btnContinue = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnContinue.setLayoutData(gd);
		btnContinue.setText("Continue");

		final Button btnStop = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStop.setLayoutData(gd);
		btnStop.setText("Stop");

		final Button btnRateNormal = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateNormal.setLayoutData(gd);
		btnRateNormal.setText("Normal Playback Rate");

		final Button btnRateDouble = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateDouble.setLayoutData(gd);
		btnRateDouble.setText("Double Playback Rate");

		final Button btnRateBackwards = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateBackwards.setLayoutData(gd);
		btnRateBackwards.setText("Play Backwards");

		final Button btnRateDoubleBackwards = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnRateDoubleBackwards.setLayoutData(gd);
		btnRateDoubleBackwards.setText("Double Play Backwards Rate");

		final Button btnStepForward = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStepForward.setLayoutData(gd);
		btnStepForward.setText("Step Forward");

		final Button btnStepBackward = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnStepBackward.setLayoutData(gd);
		btnStepBackward.setText("Step Backward");

		final Button btnSnapshot = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnSnapshot.setLayoutData(gd);
		btnSnapshot.setText("Take Snapshot");

		final Button btnSeekToBeginning = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnSeekToBeginning.setLayoutData(gd);
		btnSeekToBeginning.setText("Seek to Beginning");

		final Button btnMute = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnMute.setLayoutData(gd);
		btnMute.setText("Mute/Unmute");

		final Button btnVolume0 = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnVolume0.setLayoutData(gd);
		btnVolume0.setText("Volume 0%");

		final Button btnVolume50 = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnVolume50.setLayoutData(gd);
		btnVolume50.setText("Volume 50%");

		final Button btnVolume100 = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnVolume100.setLayoutData(gd);
		btnVolume100.setText("Volume 100%");

		final Button btnGarbageCollect = new Button(dlg, SWT.NORMAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		//gd.horizontalSpan = 2;
		btnGarbageCollect.setLayoutData(gd);
		btnGarbageCollect.setText("Garbage Collect");

		dlg.open();

		final String fileName = "";
		final FileDialog selFile = new FileDialog(dlg, SWT.OPEN);
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
				final FileDialog selFile = new FileDialog(dlg, SWT.OPEN);
				selFile.setFilterNames(new String[]{"All Files (*.*)"});
				selFile.setFilterExtensions(new String[]{"*.*"});
				if (StringUtil.isNullOrEmpty(fileName = selFile.open()))
					return;
				file[0] = new File(fileName);
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).play(false, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, file[0].toURI().toString());
					}
				}
			}
		});
		btnPlayMJPEG.addSelectionListener(new SelectionAdapter() {
			int index = 0;
			String[] uri = new String[] {
				  "http://www.serveurperso.com:81/axis-cgi/mjpg/video.cgi"
				, "http://www.warwick.ac.uk/newwebcam/cgi-bin/webcam.pl?dummy=garb"
				//, "http://www.google.com/test/"
				//, "http://www.asfjasflasf.com/"
				//, "http://samples.mplayerhq.hu/mov/RQ004F14.MOV"
				//, "http://users.design.ucla.edu/~acolubri/test/gstreamer/station-svq1.mov"
				//, "rtsp://s-0-1.sg.softspb.com:554/test/test.mp4"
			};

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).play(true, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, uri[index]);
								if (++index >= uri.length)
									index = 0;
							}
						});
					}
				}
			}
		});
		btnPlayImage.addSelectionListener(new SelectionAdapter() {
			int index = 0;
			String[] images = new String[] {
				  "http://arsdictum.com/images/madonastann.jpg"
				, "http://yeinjee.com/travel/wp-content/uploads/2007/08/paris-notre-dame-top.jpg"
				, "http://upload.wikimedia.org/wikipedia/commons/7/7a/Basketball.png"
				, "http://www.google.com/intl/en_ALL/images/srpr/logo1w.png"
			};

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).play(false, IMediaRequest.REPEAT_FOREVER, IMediaRequest.DEFAULT_FPS, images[index]);
								if (++index >= images.length)
									index = 0;
							}
						});
					}
				}
			}
		});
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
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).play(false, IMediaRequest.DEFAULT_REPEAT_COUNT, IMediaRequest.DEFAULT_FPS, file[0].toURI().toString());
							}
						});
					}
				}
			}
		});
		btnPlayExampleMJPG.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					//Extract resource
					Resources.extractAll(ossbuild.extract.Package.newInstance("resources.media", Path.nativeResourcesDirectory, new FileProcessor(false, "example.mjpg"))).get();
				} catch(Throwable t) {
				}

				file[0] = Path.combine(Path.nativeResourcesDirectory, "example.mjpg");
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).play(false, IMediaRequest.REPEAT_FOREVER, 2.0f, file[0].toURI().toString());
							}
						});
					}
				}
			}
		});
		btnPlayBlackBurst.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).playBlackBurst();
							}
						});
					}
				}
			}
		});
		btnPlayTestSignal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).playTestSignal();
							}
						});
					}
				}
			}
		});
		btnPlay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).play(false, 1/*IMediaRequest.REPEAT_FOREVER/**/, IMediaRequest.DEFAULT_FPS, file[0].toURI().toString());
							}
						});
					}
				}
			}
		});
		btnPlayForever.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).play(false, IMediaRequest.REPEAT_FOREVER, IMediaRequest.DEFAULT_FPS, file[0].toURI().toString());
							}
						});
					}
				}
			}
		});
		btnPause.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).pause();
							}
						});
					}
				}
			}
		});
		btnContinue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).unpause();
							}
						});
					}
				}
			}
		});
		btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (final Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						GstMediaComponent.execute(new Runnable() {
							@Override
							public void run() {
								((GstMediaComponent) c).stop();
							}
						});
					}
				}
			}
		});
		btnRateNormal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).adjustPlaybackRate(1.0D);
					}
				}
			}
		});
		btnRateDouble.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).adjustPlaybackRate(2.0D);
					}
				}
			}
		});
		btnRateBackwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).adjustPlaybackRate(-1.0D);
					}
				}
			}
		});
		btnRateDoubleBackwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).adjustPlaybackRate(-2.0D);
					}
				}
			}
		});
		btnStepForward.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).stepForward();
					}
				}
			}
		});
		btnStepBackward.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).stepBackward();
					}
				}
			}
		});
		btnSeekToBeginning.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).seekToBeginning();
					}
				}
			}
		});
		btnSnapshot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mediaComp.saveSnapshot(new File(System.currentTimeMillis() + ".jpg"));
			}
		});
		btnMute.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).mute();
					}
				}
			}
		});
		btnVolume0.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).adjustVolume(0);
					}
				}
			}
		});
		btnVolume50.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).adjustVolume(50);
					}
				}
			}
		});
		btnVolume100.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).adjustVolume(100);
					}
				}
			}
		});
		btnGarbageCollect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				System.gc();
			}
		});

		mediaComp.addAudioListener(new IAudioListener() {
			@Override
			public void audioMuted(IMediaPlayer source) {
				System.out.println("muted");
			}

			@Override
			public void audioUnmuted(IMediaPlayer source) {
				System.out.println("unmuted");
			}

			@Override
			public void audioVolumeChanged(IMediaPlayer source, int percent) {
				System.out.println("volume change: " + percent);
			}
		});

		mediaComp.addMediaEventListener(new IMediaEventListener() {
			@Override
			public void mediaStopped(final IMediaPlayer source) {
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
			public void mediaPaused(final IMediaPlayer source) {
				System.out.println("PAUSED");
			}

			@Override
			public void mediaContinued(final IMediaPlayer source) {
				System.out.println("CONTINUED");
				enableScale(source);
			}

			@Override
			public void mediaPlayRequested(final IMediaPlayer source, final IMediaRequest request) {
				System.out.println("PLAY REQUESTED: " + source.getMediaRequest().getURI().toString());
			}

			@Override
			public void mediaPlayed(final IMediaPlayer source) {
				System.out.println("PLAYED: " + source.getMediaRequest().getURI().toString());
				enableScale(source);
			}

			private void enableScale(final IMediaPlayer source) {
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

		mediaComp.addPositionListener(new IPositionListener() {
			private long lastDuration = 0L;
			private long lastPosition = 0L;

			@Override
			public void positionChanged(final IMediaPlayer source, final int percent, final long position, final long duration) {
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
				for (Control c : dlg.getChildren()) {
					if (c instanceof GstMediaComponent) {
						((GstMediaComponent) c).seek(position);
					}
				}
			}
		});
	}
}

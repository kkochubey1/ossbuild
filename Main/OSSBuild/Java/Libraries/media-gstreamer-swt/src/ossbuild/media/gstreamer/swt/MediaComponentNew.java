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
import org.eclipse.swt.widgets.Event;
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
import ossbuild.media.MediaRequestType;
import ossbuild.media.MediaType;
import ossbuild.media.Scheme;
import ossbuild.media.events.IAudioListener;
import ossbuild.media.events.IMediaEventListener;
import ossbuild.media.events.IPositionListener;
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
import ossbuild.media.gstreamer.SeekFlags;
import ossbuild.media.gstreamer.SeekType;
import ossbuild.media.gstreamer.State;
import ossbuild.media.gstreamer.StateChangeReturn;
import ossbuild.media.gstreamer.Structure;
import ossbuild.media.gstreamer.api.GStreamer;
import ossbuild.media.gstreamer.api.GTypeConverters;
import ossbuild.media.gstreamer.api.Utils;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;
import ossbuild.media.gstreamer.events.StepEvent;
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
		Sys.setEnvironmentVariable("GST_DEBUG", "GST_REFCOUNTING:3");
		
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

		GstMediaComponent comp;

		comp = new GstMediaComponent(dlg, SWT.NONE);
		comp.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

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

		final GstMediaComponent mediaComp = comp;

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
				  "http://129.125.136.20/axis-cgi/mjpg/video.cgi?camera=1"
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

		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					while(true) {
						display.asyncExec(new Runnable() {
							public void run() {
								btnPlayExample.notifyListeners(SWT.Selection, new Event());
							}
						});
						Thread.sleep(3000L);
					}
				} catch(Throwable t) {
				}
			}
		});
		t.setDaemon(true);
		t.setName("test click thread");
		t.start();

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
				videoElement = "directdrawsink";
				//videoElement = "fakesink";
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

	@Override
	protected void componentInitialize() {
		//throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected Runnable createPositionUpdater() {
		return positionUpdateRunnable;
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
			return (currentState(0L) != State.Null);
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
			return (state == State.Paused || state == State.Ready);
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
			return (state == State.Null || state == State.Ready);
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
			return (state == State.Playing);
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
			if (state != State.Playing || state != State.Paused)
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
			if (state != State.Playing || state != State.Paused)
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
					return true;
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

	//<editor-fold defaultstate="collapsed" desc="Expose">
	public boolean expose() {
		if (isDisposed())
			return false;
		if (mediaType == MediaType.Image)
			return true;

		State state;
		lock.lock();
		try {
			if (xoverlay != null && pipeline != null && ((state = currentState()) == State.Playing || state == State.Paused)) {
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
				return (changeState(State.Ready, new Runnable() {
							@Override
							public void run() {
								onPositionUpdate();
								changeState(State.Playing);
							}
						}
					)
					!=
					StateChangeReturn.Failure
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
			return (changeState(State.Ready, new Runnable() {
						@Override
						public void run() {
							onPositionUpdate();
							changeState(State.Paused);
						}
					}
				)
				!=
				StateChangeReturn.Failure
			);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean adjustPlaybackRate(final double rate) {
		return false;
//		//TODO: Figure out why playing backwards (rate is negative) isn't working
//
//		if (rate < 0.0f)
//			return false;
//
//		if (rate == 0.0f)
//			return pause();
//
//		lock.lock();
//		try {
//			if (isLiveSource())
//				return false;
//
//			final Segment segment = pipeline.querySegment();
//			if (segment != null && rate == segment.getRate())
//				return true;
//
//			State state = currentState();
//
//			switch(state) {
//				case PLAYING:
//					changeState(State.PAUSED, new Runnable() {
//						@Override
//						public void run() {
//							adjustPlaybackRate(rate);
//						}
//					});
//					return true;
//				case PAUSED:
//					break;
//				default:
//					return false;
//			}
//
//			final boolean forwards = (rate >= 0.0);
//			final long positionNanoSeconds = pipeline.queryPosition(Format.TIME);
//			//final long stop = (forwards ? positionNanoSeconds + SEEK_STOP_DURATION : Math.max(0, positionNanoSeconds - SEEK_STOP_DURATION));
//			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
//			final long stop = (forwards ? -1 : 0);
//
//			final boolean success = pipeline.seek(rate, Format.TIME, SeekFlags.FLUSH | SeekFlags.SEGMENT, SeekType.SET, begin, SeekType.SET, stop) && changeState(State.PLAYING) != StateChangeReturn.FAILURE;
//			if (success)
//				currentRate = rate;
//
//			return success;
//		} finally {
//			lock.unlock();
//		}
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
				case Playing:
				case Paused:
					break;
				default:
					return false;
			}

			final boolean forwards = (rate >= 0.0);
			final long begin = (forwards ? positionNanoSeconds : positionNanoSeconds);
			final long stop = (forwards ? -1 : 0);

			final boolean success = pipeline.seek(rate, Format.Time, SeekFlags.toNative(SeekFlags.Flush, SeekFlags.Segment), SeekType.Set, begin, SeekType.Set, stop);
			changeState(State.Playing);

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
			if (state != State.Paused) {
				changeState(pipeline, State.Paused, new Runnable() {
					@Override
					public void run() {
						StepEvent evt = new StepEvent(Format.Buffers, 1L, 1.0D, true, false);
						pipeline.sendEvent(evt);
						evt.dispose();
					}
				});
				return true;
			}
			StepEvent evt = new StepEvent(Format.Buffers, 1L, 1.0D, true, false);
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
			if (currentState() == State.Paused)
				return true;
			return changeState(State.Paused) != StateChangeReturn.Failure;
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
				changeState(State.Playing, 2000L,
					new Runnable() {
						@Override
						public void run() {
							adjustPlaybackRate(currentRate);
						}
					}
				)
				!=
				StateChangeReturn.Failure
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
			resetPipeline(pipeline);
			singleImage = null;
			asyncRedraw();
			return true;
		} finally {
			lock.unlock();
		}
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="The Meat">
	//<editor-fold defaultstate="collapsed" desc="Cleanup">
	protected void resetPipeline(final IPipeline newPipeline) {
		if (newPipeline != null) {
			ui(new Runnable() {
				@Override
				public void run() {
					do {
						newPipeline.changeState(State.Null);
					} while(newPipeline.requestState() != State.Null);
					//cleanup(newPipeline);
					System.out.println(newPipeline.refCount());
					newPipeline.dispose();
					System.out.println(newPipeline.refCount());
					//while(newPipeline.refCount() >= 1)
					//	newPipeline.unref();
					System.out.println(newPipeline.refCount());
					pipeline = null;
					asyncRedraw();
				}
			});
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
			struct.dispose();
			caps.dispose();
			return;
		}

		if (padCaps.startsWith("audio/")) {
			disposeAudioBin(newPipeline);

			hasAudio = true;
			if (mediaType == MediaType.Unknown && mediaType != MediaType.Video)
				mediaType = MediaType.Audio;

			//Create audio bin
			final IBin audioBin = Bin.make("audioBin");
			final Pad audioPad = createAudioBin(mediaType, newRequest, newPipeline, audioBin, uridecodebin, pad);
			final GhostPad audioGhostPad = GhostPad.from("audioGhostPadSink", audioPad);

			audioBin.addPad(audioGhostPad);
			newPipeline.add(audioBin);
			pad.link(audioGhostPad);

			audioBin.changeState(State.Playing);

			audioGhostPad.dispose();
			audioPad.dispose();
			audioBin.dispose();

		} else if (padCaps.startsWith("video/")) {
			disposeVideoBin(newPipeline);

			hasVideo = true;
			if (mediaType == MediaType.Unknown || mediaType == MediaType.Audio)
				mediaType = (!determineIfSingleImage(uridecodebin) ? MediaType.Video : MediaType.Image);

			//Create video bin
			final IBin videoBin = Bin.make("videoBin");
			final Pad videoPad = createVideoBin(mediaType, newRequest, newPipeline, videoBin, uridecodebin, pad);
			final GhostPad videoGhostPad = GhostPad.from("videoGhostPadSink", videoPad);

			System.out.println("onPadAdded, videoBin: " + videoBin.refCount());
			System.out.println("onPadAdded, videoPad: " + videoPad.refCount());
			System.out.println("onPadAdded, videoGhostPad: " + videoGhostPad.refCount());
			
			videoBin.addPad(videoGhostPad);
			newPipeline.add(videoBin);
			pad.link(videoGhostPad);

			System.out.println("onPadAdded, videoBin: " + videoBin.refCount());
			System.out.println("onPadAdded, videoPad: " + videoPad.refCount());
			System.out.println("onPadAdded, videoGhostPad: " + videoGhostPad.refCount());

			videoBin.changeState(State.Playing);

			//videoPad.unref();
			//videoGhostPad.unref();
			
			videoGhostPad.dispose();
			videoPad.dispose();
			videoBin.dispose();

			System.out.println("onPadAdded, videoBin: " + videoBin.refCount());
			System.out.println("onPadAdded, videoPad: " + videoPad.refCount());
			System.out.println("onPadAdded, videoGhostPad: " + videoGhostPad.refCount());
		}

		struct.dispose();
		caps.dispose();

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
			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!seekToBeginning()) {
						changeState(State.Ready);
						changeState(State.Playing);
					}
				}
			});
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
		newPipeline.dispose();
		asyncRedraw();
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

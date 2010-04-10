
package simple.swt;

import com.sun.jna.Pointer;
import java.io.File;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
		selFile.setFilterNames(new String[] { "All Files (*.*)" });
		selFile.setFilterExtensions(new String[] { "*.*" });
		if (StringUtil.isNullOrEmpty(fileName = selFile.open())) {
			Gst.quit();
			return;
		}

		final File file = new File(fileName);
		btnPlay.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for(Control c : shell.getChildren())
					if (c instanceof MediaComponent)
						((MediaComponent)c).playFile(file);
			}
		});
		btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for(Control c : shell.getChildren())
					if (c instanceof MediaComponent)
						((MediaComponent)c).stop();
			}
		});

		for(Control c : shell.getChildren())
			if (c instanceof MediaComponent)
				((MediaComponent)c).playFile(file);
		
		//PlayBin2 playbin = new PlayBin2((String)null);
		while(!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
		display.dispose();

		Gst.quit();
	}

	public static class MediaComponent extends Composite {
		//<editor-fold defaultstate="collapsed" desc="Variables">
		private PlayBin2 playbin;
		private Element videoSink;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		public MediaComponent(Composite parent, int style) {
			super(parent, style | SWT.EMBEDDED);

			final Display display = getDisplay();

			//<editor-fold defaultstate="collapsed" desc="Determine video sink">
			String videoElement;
			switch(Sys.getOSFamily()) {
				case Windows:
					//videoElement = "dshowvideosink";
					videoElement = "directdrawsink";
					break;
				case Unix:
					videoElement = "xvimagesink";
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
			playbin = new PlayBin2((String)null);
			playbin.setVideoSink(videoSink);
			//</editor-fold>
			
			//<editor-fold defaultstate="collapsed" desc="Prepare XOverlay support">
			final CustomXOverlay overlay = CustomXOverlay.wrap(videoSink);
			final Runnable handleXOverlay = new Runnable() {
				@Override
				public void run() {
					overlay.setWindowID(MediaComponent.this);
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
			bus.setSyncHandler(new BusSyncHandler() {
				@Override
				public BusSyncReply syncMessage(Message msg) {
					Structure s = msg.getStructure();
					if (s == null || !s.hasName("prepare-xwindow-id"))
						return BusSyncReply.PASS;
					display.syncExec(handleXOverlay);
					return BusSyncReply.DROP;
				}
			});
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="SWT Events">
			//</editor-fold>
		}
		//</editor-fold>

		public void stop() {
			if (playbin.getState() != State.NULL)
				playbin.setState(State.NULL);
			this.redraw();
		}

		public void playFile(final File File) {
			if (playbin.getState() != State.NULL)
				playbin.setState(State.NULL);
			playbin.setInputFile(File);
			playbin.setState(State.PLAYING);
		}
	}
}

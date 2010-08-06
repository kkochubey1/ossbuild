
package ossbuild.media.gstreamer.swt;

import com.sun.jna.Native;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.swt.widgets.Composite;
import org.gstreamer.swt.overlay.SWTOverlay;
import ossbuild.Path;
import ossbuild.StringUtil;
import ossbuild.Sys;
import ossbuild.media.IMediaRequest;
import ossbuild.media.gstreamer.ErrorType;
import ossbuild.media.gstreamer.ErrorType;

/**
 *
 * @author David Hoyt <dhoyt@llnl.gov>
 */
public class GstPlayer {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String
		  GST_PLAYER_FILENAME = "gst-player" + suffixForOS()
	;

	public static final File
		  GST_PLAYER_DIR = Path.combine(Path.nativeResourcesDirectory, "bin/")
	;

	public static final File
		  GST_PLAYER_FILE = Path.combine(GST_PLAYER_DIR, GST_PLAYER_FILENAME)
	;

	public static final String
		  GST_PLAYER = GST_PLAYER_FILE.getAbsolutePath()
	;

	public static final ThreadGroup
		  PROCESS_THREAD_GROUP
		, PROCESS_REAPER_THREAD_GROUP
		, PROCESS_MONITOR_THREAD_GROUP
	;

	public static final long
		  MAX_PING_INTERVAL = 2000
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Enums">
	public static enum Category {
		  Unknown   ("")

		, Event     ("event")
		, Response  ("response")
		, Error     ("error")
		;

		//<editor-fold defaultstate="collapsed" desc="Variables">
		private String name;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		Category(String name) {
			this.name = name;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Getters">
		public String getName() {
			return name;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Public Methods">
		public static Category fromName(String name) {
			if (StringUtil.isNullOrEmpty(name))
				return Unknown;
			name = name.trim();
			for(Category c : values())
				if (c.name.equalsIgnoreCase(name))
					return c;
			return Unknown;
		}
		//</editor-fold>
	}

	public static enum Event {
		  Unknown       ("")

		, Ping          ("ping")
		, WindowSize    ("window_size")
		, Playing       ("playing")
		, Paused        ("paused")
		, Stopped       ("stopped")
		, Quit          ("quit")

		, FPS           ("fps")
		, Seekable      ("seekable")
		, Live          ("live")
		, URI           ("uri")
		, Duration      ("duration")
		, Position      ("position")
		, Repeat        ("repeat")
		, MediaType     ("media_type")
		, Volume        ("volume")
		, Mute          ("mute")
		, Snapshot      ("snapshot")
		;

		//<editor-fold defaultstate="collapsed" desc="Variables">
		private String name;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		Event(String name) {
			this.name = name;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Getters">
		public String getName() {
			return name;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Public Methods">
		public static Event fromName(String name) {
			if (StringUtil.isNullOrEmpty(name))
				return Unknown;
			name = name.trim();
			for(Event e : values())
				if (e.name.equalsIgnoreCase(name))
					return e;
			return Unknown;
		}
		//</editor-fold>
	}

	public static enum Response {
		  Unknown       ("")

		, QueryPosition ("query_position")
		, QueryDuration ("query_duration")
		, QueryVolume   ("query_volume")
		, QueryMute     ("query_mute")
		, Ping          ("ping")
		;

		//<editor-fold defaultstate="collapsed" desc="Variables">
		private String name;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		Response(String name) {
			this.name = name;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Getters">
		public String getName() {
			return name;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Public Methods">
		public static Response fromName(String name) {
			if (StringUtil.isNullOrEmpty(name))
				return Unknown;
			name = name.trim();
			for(Response r : values())
				if (r.name.equalsIgnoreCase(name))
					return r;
			return Unknown;
		}
		//</editor-fold>
	}

	public static enum MediaType {
		  Unknown   ("",      0)

		, Still     ("still", (1))
		, Audio     ("audio", (1 << 1))
		, Video     ("video", (1 << 2))
		;

		//<editor-fold defaultstate="collapsed" desc="Variables">
		private String name;
		private int flag;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		MediaType(String name, int flag) {
			this.name = name;
			this.flag = flag;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Getters">
		public int getFlag() {
			return flag;
		}
		
		public String getName() {
			return name;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Public Methods">
		public static MediaType fromName(String name) {
			if (StringUtil.isNullOrEmpty(name))
				return Unknown;
			name = name.trim();
			for(MediaType m : values())
				if (m.name.equalsIgnoreCase(name))
					return m;
			return Unknown;
		}

		public static int addFlag(int value, MediaType m) {
			return (value | m.flag);
		}

		public static boolean isStill(int value) {
			return ((value & Still.flag) == Still.flag);
		}

		public static boolean isAudio(int value) {
			return ((value & Audio.flag) == Audio.flag);
		}

		public static boolean isVideo(int value) {
			return ((value & Video.flag) == Video.flag);
		}

		public static ossbuild.media.MediaType toCommonMediaType(int value) {
			if (isStill(value))
				return ossbuild.media.MediaType.Image;
			else if (isVideo(value))
				return ossbuild.media.MediaType.Video;
			else if (isAudio(value))
				return ossbuild.media.MediaType.Audio;
			else
				return ossbuild.media.MediaType.Unknown;
		}
		//</editor-fold>
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Variables">
	private static final Collection<GstPlayer> runningPlayers = new LinkedBlockingQueue<GstPlayer>();
	private static final AtomicInteger inputMonitorCount = new AtomicInteger(0);

	private List<IEventListener> eventListeners;
	private final Object eventListenerLock = new Object();

	private List<IResponseListener> responseListeners;
	private final Object responseListenerLock = new Object();

	private List<IErrorListener> errorListeners;
	private final Object errorListenerLock = new Object();

	private IMediaRequest request;
	private boolean seekable;
	private float fps;
	private boolean live;
	private boolean playing;
	private boolean paused;
	private boolean muted;
	private int mediaType;
	private int volume;
	private int videoWidth;
	private int videoHeight;
	private int repeatCount;
	private long nativeHandle;
	private long bufferSize;
	private long lastKnownDuration;
	private long lastKnownPosition;
	private long lastPing;
	private String lastSnapshot;
	private Thread inputMonitor;
	private ProcessBuilder procBuilder;
	private Process proc;
	private BufferedReader input;
	private BufferedWriter output;
	private final Object lock = new Object();
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Initialization">
	//<editor-fold defaultstate="collapsed" desc="Static Initialization">
	static {
		//Create a thread group that we'll place process monitors beneath.
		PROCESS_THREAD_GROUP = new ThreadGroup(findMainThreadGroup(), "GStreamer");
		PROCESS_REAPER_THREAD_GROUP = new ThreadGroup(PROCESS_THREAD_GROUP, "Process Reapers");
		PROCESS_MONITOR_THREAD_GROUP = new ThreadGroup(PROCESS_THREAD_GROUP, "Monitors");

		launchProcessReaper();
	}

	private static void launchProcessReaper() {
		//Launch the process reaper and ensure that it's started up before returning.
		final boolean[] pleaseExit = new boolean[1];
		final Semaphore startup = new Semaphore(0);
		final Semaphore wait = new Semaphore(0);
		final Semaphore end = new Semaphore(0);
		pleaseExit[0] = false;

		final Thread t = new Thread(PROCESS_REAPER_THREAD_GROUP, new Runnable() {
			@Override
			public void run() {
				try {
					//Notify main thread that we've started up.
					startup.release();

					while(!pleaseExit[0]) {
						reapUnresponsiveProcesses();
						wait.tryAcquire(500L, TimeUnit.MILLISECONDS);
					}
					//Ensure that all gst processes are gone.
					reapAllProcesses();

					//Notify shutdown hook that we're done.
					end.release();
				} catch(Throwable t) {
				}
			}
		}, "Reaper 1");
		t.setDaemon(true);
		t.start();
		startup.acquireUninterruptibly();

		Runtime.getRuntime().addShutdownHook(new Thread(PROCESS_REAPER_THREAD_GROUP, new Runnable() {
			@Override
			public void run() {
				try {
					//Tell reaper thread to exit.
					pleaseExit[0] = true;
					wait.release();

					//Wait for reaper thread to exit.
					end.acquire();
				} catch(Throwable t) {
				}
			}
		}, "Reaper Shutdown"));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Constructors">
	public GstPlayer() {
		reset();
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Dispose">
	public void Dispose() {
		destroy();
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Getters">
	public long getLastPing() {
		return lastPing;
	}

	public long getPingInterval() {
		if (lastPing <= 0L)
			return 0L;
		return (System.currentTimeMillis() - lastPing);
	}

	public float getVideoFPS() {
		return fps;
	}

	public int getVolume() {
		return volume;
	}

	public boolean isMuted() {
		return muted;
	}

	public int getRepeatCount() {
		return repeatCount;
	}

	public int getVideoWidth() {
		return videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	public long getLastKnownPosition() {
		return lastKnownPosition;
	}

	public long getLastKnownDuration() {
		return lastKnownDuration;
	}

	public boolean isRunning() {
		synchronized(lock) {
			return (inputMonitor != null && inputMonitor.isAlive());
		}
	}

	public boolean isPlaying() {
		return playing;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isLive() {
		return live;
	}

	public boolean isSeekable() {
		return seekable;
	}

	public boolean isRepeatingForever() {
		return (request != null ? false : request.isRepeatForever());
	}

	public IMediaRequest getMediaRequest() {
		return request;
	}

	public boolean isVideoAvailable() {
		return (MediaType.isStill(mediaType) || MediaType.isVideo(mediaType));
	}

	public boolean isAudioAvailable() {
		return (MediaType.isAudio(mediaType));
	}

	public boolean isStill() {
		return (MediaType.isStill(mediaType));
	}

	public boolean isVideo() {
		return (MediaType.isVideo(mediaType));
	}

	public boolean isAudio() {
		return (MediaType.isAudio(mediaType));
	}

	public long getBufferSize() {
		return bufferSize;
	}

	public ossbuild.media.MediaType getCommonMediaType() {
		return MediaType.toCommonMediaType(mediaType);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	private static String suffixForOS() {
		switch(Sys.getOSFamily()) {
			case Windows:
				return ".exe";
			default:
				return StringUtil.empty;
		}
	}

	/**
	 * Locates a thread group based on its name. If there are 2 or more groups with the same name,
	 * it will return the first one it finds.
	 *
	 * @return null if the thread group was not found
	 */
	private static final ThreadGroup findThreadGroup(String Name) {
		ThreadGroup root = Thread.currentThread().getThreadGroup();
		while(root.getParent() != null)
			root = root.getParent();

		//Check if the root is what we're looking for
		if (Name.equalsIgnoreCase(root.getName()))
			return root;

		return findThreadGroup(root, Name);
	}

	/**
	 * @see #findThread(java.lang.String)
	 */
	private static final ThreadGroup findThreadGroup(ThreadGroup Parent, String Name) {
		if (Parent == null)
			return null;

		ThreadGroup[] groups = new ThreadGroup[Parent.activeGroupCount() * 2];

		int gCount = Parent.enumerate(groups, false);

		ThreadGroup g;
		for(int i = 0; i < gCount; ++i) {
			if (Name.equalsIgnoreCase(groups[i].getName()))
				return groups[i];
			if ((g = findThreadGroup(groups[i], Name)) != null)
				return g;
		}

		return null;
	}

	private static final ThreadGroup findMainThreadGroup() {
		//Find the /system/main/ thread group.
		ThreadGroup gSys = findThreadGroup("system");
		if (gSys == null)
			return null;
		ThreadGroup gMain = findThreadGroup(gSys, "main");
		if (gMain == null)
			return null;
		return gMain;
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

	private void reset() {
		if (proc != null)
			destroy();

		mediaType = MediaType.Unknown.getFlag();
		repeatCount = 0;
		bufferSize = 0L;

		lastKnownPosition = 0L;
		lastKnownDuration = 0L;
		videoWidth = 0;
		videoHeight = 0;
		volume = 100;
		muted = false;
		live = false;
		playing = false;
		paused = false;
		seekable = false;
		fps = 0.0f;
		lastPing = 0L;
		inputMonitor = null;
		input = null;
		output = null;
		proc = null;
		request = null;
		nativeHandle = 0L;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Listeners">
	//<editor-fold defaultstate="collapsed" desc="Event">
	public static interface IEventListener {
		void handleEvent(final GstPlayer source, final IMediaRequest request, final Event event, final Object[] args);
	}

	public boolean addEventListener(final IEventListener Listener) {
		if (Listener == null)
			return false;
		synchronized(eventListenerLock) {
			if (eventListeners == null)
				eventListeners = new CopyOnWriteArrayList<IEventListener>();
			return eventListeners.add(Listener);
		}
	}

	public boolean removeEventListener(final IEventListener Listener) {
		if (Listener == null)
			return false;
		synchronized(eventListenerLock) {
			if (eventListeners == null || eventListeners.isEmpty())
				return true;
			return eventListeners.remove(Listener);
		}
	}

	public boolean containsEventListener(final IEventListener Listener) {
		if (Listener == null)
			return false;
		synchronized(eventListenerLock) {
			if (eventListeners == null || eventListeners.isEmpty())
				return true;
			return eventListeners.contains(Listener);
		}
	}

	public boolean clearEventListeners() {
		synchronized(eventListenerLock) {
			if (eventListeners == null || eventListeners.isEmpty())
				return true;
			eventListeners.clear();
			return true;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Response">
	public static interface IResponseListener {
		void handleResponse(final GstPlayer source, final IMediaRequest request, final Response event, final Object[] args);
	}

	public boolean addResponseListener(final IResponseListener Listener) {
		if (Listener == null)
			return false;
		synchronized(responseListenerLock) {
			if (responseListeners == null)
				responseListeners = new CopyOnWriteArrayList<IResponseListener>();
			return responseListeners.add(Listener);
		}
	}

	public boolean removeResponseListener(final IResponseListener Listener) {
		if (Listener == null)
			return false;
		synchronized(responseListenerLock) {
			if (responseListeners == null || responseListeners.isEmpty())
				return true;
			return responseListeners.remove(Listener);
		}
	}

	public boolean containsResponseListener(final IResponseListener Listener) {
		if (Listener == null)
			return false;
		synchronized(responseListenerLock) {
			if (responseListeners == null || responseListeners.isEmpty())
				return true;
			return responseListeners.contains(Listener);
		}
	}

	public boolean clearResponseListeners() {
		synchronized(responseListenerLock) {
			if (responseListeners == null || responseListeners.isEmpty())
				return true;
			responseListeners.clear();
			return true;
		}
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Error">
	public static interface IErrorListener {
		void handleError(final GstPlayer source, final IMediaRequest request, final int code, final String message);
	}

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
	//<editor-fold defaultstate="collapsed" desc="Event">
	protected void fireHandleEvent(final Event event, final Object... args) {
		if (eventListeners == null || eventListeners.isEmpty())
			return;
		for(IEventListener listener : eventListeners)
			listener.handleEvent(this, request, event, args);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Response">
	protected void fireHandleResponse(final Response response, final Object... args) {
		if (responseListeners == null || responseListeners.isEmpty())
			return;
		for(IResponseListener listener : responseListeners)
			listener.handleResponse(this, request, response, args);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Error">
	protected void fireHandleError(final int code, final String message) {
		if (errorListeners == null || errorListeners.isEmpty())
			return;
		for(IErrorListener listener : errorListeners)
			listener.handleError(this, request, code, message);
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Methods">
	//<editor-fold defaultstate="collapsed" desc="Overloads">
	public boolean launch(IMediaRequest request) {
		return launch(StringUtil.empty, StringUtil.empty, 0L, false, 100, 0L, request);
	}

	public boolean launch(String videoElement, String audioElement, boolean mute, int volume, long bufferSize, IMediaRequest request) {
		return launch(videoElement, audioElement, 0L, mute, volume, bufferSize, request);
	}

	public boolean launch(String videoElement, String audioElement, Window nativeHandle, boolean mute, int volume, long bufferSize, IMediaRequest request) {
		return launch(videoElement, audioElement, Native.getWindowID(nativeHandle), mute, volume, bufferSize, request);
	}

	public boolean launch(String videoElement, String audioElement, Composite nativeHandle, boolean mute, int volume, long bufferSize, IMediaRequest request) {
		return launch(videoElement, audioElement, SWTOverlay.handle(nativeHandle), mute, volume, bufferSize, request);
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Commands">
	public boolean mute() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("mute");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean unmute() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("unmute");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean toggleMute() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("toggle_mute");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean pause() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("pause");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean unpause() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("continue");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean stepForward() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("step_forward");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean stepBackward() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("step_backward");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean seek(long time) {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("time_seek " + time);
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean createSnapshot() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("snapshot");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Queries">
	public boolean queryPosition() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("query_position");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean queryDuration() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("query_duration");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean queryMute() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("query_mute");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean queryVolume() {
		synchronized(lock) {
			if (proc == null)
				return false;

			try {
				command("query_volume");
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}
	//</editor-fold>
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="The Meat">
	private static void reapAllProcesses() {
		for(GstPlayer p : runningPlayers) {
			p.destroy();
		}
	}

	private static void reapUnresponsiveProcesses() {
		for(GstPlayer p : runningPlayers) {
			if (p.getPingInterval() > MAX_PING_INTERVAL) {
				System.out.println("REAPING PROCESS");
				p.destroy();
			}
		}
	}

	private ProcessBuilder createProcBuilder(String videoElement, String audioElement, long nativeHandle, boolean mute, int volume, long bufferSize, IMediaRequest request) {
		ProcessBuilder builder = new ProcessBuilder(
			GST_PLAYER
			, "--command-mode"
			, "--ping-interval=1000"
			, (request.isRepeatForever() ? "--repeat-forever" : "--repeat-count=" + request.getRepeatCount())
			, "-v", Double.toString((double)Math.max(0, Math.min(100, volume)) / 100.0)
			, "-u", uriString(request.getURI())
		);

		List<String> commands = builder.command();

		if (!StringUtil.isNullOrEmpty(videoElement))
			commands.add("--video-sink=" + videoElement);

		if (!StringUtil.isNullOrEmpty(audioElement))
			commands.add("--audio-sink=" + audioElement);

		if (request.getFPS() != IMediaRequest.DEFAULT_FPS) {
			int fps_n = (int)(request.getFPS() * 1000.0f);
			int fps_d = 1000;

			commands.add("--fps-n=" + Integer.toString(fps_n));
			commands.add("--fps-d=" + Integer.toString(fps_d));
		}

		if (bufferSize > 0L)
			commands.add("--buffer-size=" + Long.toString(bufferSize));

		if (mute)
			commands.add("--mute");

		if (request.isLiveSource())
			commands.add("--live");

		if (nativeHandle != 0L)
			commands.add("--window-id=" + Long.toString(nativeHandle));

		builder.directory(GST_PLAYER_DIR);
		builder.redirectErrorStream(true);

		return builder;
	}
	
	public boolean launch(String videoElement, String audioElement, long nativeHandle, boolean mute, int volume, long bufferSize, IMediaRequest request) {
		final Semaphore startup = new Semaphore(0);
		final Semaphore inputReady = new Semaphore(0);

		try {
			synchronized(lock) {
				reset();

				this.muted = mute;
				this.volume = volume;
				this.request = request;
				this.bufferSize = bufferSize;
				this.nativeHandle = nativeHandle;
				this.procBuilder = createProcBuilder(videoElement, audioElement, nativeHandle, mute, volume, bufferSize, request);
				
				//Create a thread to handle this
				this.inputMonitor = new Thread(PROCESS_MONITOR_THREAD_GROUP, new Runnable() {
					@Override
					public void run() {
						try {
							String line;

							runningPlayers.add(GstPlayer.this);

							//Inform the launching thread that this thread has started.
							//Then wait for the launching thread to create a BufferedReader
							//for us to read lines from.
							startup.release();
							inputReady.acquire();

							//By this point, we should have an input object.
							//So start reading in gst-player's output one line at a time.
							while(input != null && (line = input.readLine()) != null)
								parseInput(line);

						} catch(Throwable t) {
						} finally {
							//Close out the input/output.
							synchronized(lock) {
								try { if (input != null) input.close(); input = null; } catch(IOException ie) { }
								try { if (output != null) output.close(); output = null; } catch(IOException ie) { }

								//Ensure that our thread count matches the actual number of
								//processes we're monitoring.
								inputMonitorCount.decrementAndGet();

								//Ensure that the process has ended.
								proc.destroy();

								//Remove from our list of running players.
								runningPlayers.remove(GstPlayer.this);

								//If the process ends correctly, then this might result in multiple
								//calls -- but it shouldn't affect listeners if they're resilient enough.
								processEvent(Event.Stopped, null);
								processEvent(Event.Quit, null);
							}
						}
					}
				}, "Monitor");
				inputMonitorCount.incrementAndGet();
				this.inputMonitor.setDaemon(false);
				this.inputMonitor.start();
				startup.acquire();

				this.proc = procBuilder.start();
				if (proc != null) {
					this.input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					this.output = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), "ASCII"));
				}
				inputReady.release();

				return true;
			}
		} catch(Throwable t) {
			if (inputReady.hasQueuedThreads())
				inputReady.release();
			return false;
		}
	}

	public boolean stop() {
		synchronized(lock) {
			if (proc == null)
				return true;
			
			try {
				//Send the quit command.
				command("quit");

				//Closing the input stream will cause the thread to exit if it
				//hasn't already.
				if (input != null)
					input.close();
			} catch(Throwable t) {
				return false;
			}
		}
		return true;
	}

	public boolean destroy() {
		synchronized(lock) {
			if (!isRunning())
				return true;
			if (proc != null)
				if (!stop())
					proc.destroy();
		}
		return true;
	}

	protected boolean command(String cmd) {
		if (output == null || StringUtil.isNullOrEmpty(cmd))
			return false;
		try {
			output.write(cmd);
			output.newLine();
			output.flush();
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

	protected boolean parseInput(String line) {
		if (StringUtil.isNullOrEmpty(line))
			return true;

		String[] args = line.split(",", 4);
		if (args.length < 2)
			return false;

		Category cat = Category.fromName(args[0]);
		switch(cat) {
			case Event:
				processEvent(Event.fromName(args[1]), args);
				break;
			case Response:
				processResponse(Response.fromName(args[1]), args);
				break;
			case Error:
				if (args.length >= 3) {
					int errorCode;
					try {
						errorCode = Integer.parseInt(args[1].trim());
					} catch(Throwable t) {
						errorCode = ErrorType.Unknown.getNativeValue();
					}
					processError(errorCode, args[2].trim());
				}
				break;
			default:
				break;
		}
		return true;
	}

	protected boolean processEvent(Event event, String[] args) {
		if (event == Event.Unknown)
			return false;

		try {
			synchronized(lock) {
				switch(event) {
					case Ping:
						lastPing = System.currentTimeMillis();
						break;
					case WindowSize:
						if (args.length >= 4) {
							//Set to temp vars first in case the arguments aren't valid ints.
							//We don't want to overwrite member vars w/ potentially invalid values.
							//We employ this method any time there's more than one potential var
							//that could be affected.
							int width = Integer.parseInt(args[2].trim());
							int height = Integer.parseInt(args[3].trim());
							videoWidth = width;
							videoHeight = height;
							fireHandleEvent(event, width, height);
						}
						break;
					case FPS:
						if (args.length >= 3) {
							fps = Float.parseFloat(args[2].trim());
							fireHandleEvent(event, fps);
						}
						break;
					case Seekable:
						if (args.length >= 3) {
							seekable = (Integer.parseInt(args[2].trim()) != 0);
							fireHandleEvent(event, seekable);
						}
						break;
					case Live:
						if (args.length >= 3) {
							live = (Integer.parseInt(args[2].trim()) != 0);
							fireHandleEvent(event, live);
						}
						break;
					case Duration:
						if (args.length >= 3) {
							lastKnownDuration = Long.parseLong(args[2].trim());
							fireHandleEvent(event, lastKnownDuration);
						}
						break;
					case Position:
						if (args.length >= 3) {
							lastKnownPosition = Long.parseLong(args[2].trim());
							fireHandleEvent(event, lastKnownPosition);
						}
						break;
					case MediaType:
						if (args.length >= 3) {
							MediaType newMediaType = MediaType.fromName(args[2].trim());
							mediaType = MediaType.addFlag(mediaType, newMediaType);
							fireHandleEvent(event, mediaType, newMediaType);
						}
						break;
					case Volume:
						if (args.length >= 3) {
							double d = Double.parseDouble(args[2].trim());
							int vol = Math.max(0, Math.min(100, (int)(d * 100.0D)));
							volume = vol;
							fireHandleEvent(event, volume);
						}
						break;
					case Mute:
						if (args.length >= 3) {
							muted = (Integer.parseInt(args[2].trim()) != 0);
							fireHandleEvent(event, muted);
						}
						break;
					case Snapshot:
						if (args.length >= 3) {
							lastSnapshot = args[2].trim();
							fireHandleEvent(event, lastSnapshot);
						}
						break;
					case Playing:
						playing = true;
						paused = false;
						fireHandleEvent(event, playing, paused);
						break;
					case Paused:
						playing = true;
						paused = true;
						fireHandleEvent(event, playing, paused);
						break;
					case Stopped:
					case Quit:
						playing = false;
						paused = false;
						fireHandleEvent(event, playing, paused);
						break;
					case Repeat:
						if (args.length >= 3) {
							repeatCount = Integer.parseInt(args[2].trim());
							fireHandleEvent(event, repeatCount);
						}
						break;
					default:
						fireHandleEvent(event, (Object[])args);
						break;
				}
			}
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

	protected boolean processError(int code, String message) {
		synchronized(lock) {
			fireHandleError(code, message);
			return true;
		}
	}

	protected boolean processResponse(Response response, String[] args) {
		try {
			synchronized(lock) {
				switch(response) {
					case Ping:
						fireHandleResponse(response);
						break;
					case QueryPosition:
						if (args.length >= 3) {
							lastKnownPosition = Long.parseLong(args[2].trim());
							fireHandleResponse(response, lastKnownPosition);
						}
						break;
					case QueryDuration:
						if (args.length >= 3) {
							lastKnownDuration = Long.parseLong(args[2].trim());
							fireHandleResponse(response, lastKnownDuration);
						}
						break;
					case QueryMute:
						if (args.length >= 3) {
							muted = (Integer.parseInt(args[2].trim()) != 0);
							fireHandleResponse(response, muted);
						}
						break;
					case QueryVolume:
						if (args.length >= 3) {
							double d = Double.parseDouble(args[2].trim());
							int vol = Math.max(0, Math.min(100, (int)(d * 100.0D)));
							volume = vol;
							fireHandleResponse(response, volume);
						}
					default:
						fireHandleResponse(response, (Object[])args);
						break;
				}
				return true;
			}
		} catch(Throwable t) {
			return false;
		}
	}
	//</editor-fold>
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ossbuild.gst;

import ossbuild.media.gstreamer.IElement;
import ossbuild.media.gstreamer.BusSyncReply;
import ossbuild.media.gstreamer.Element;
import ossbuild.media.gstreamer.Bus;
import ossbuild.media.gstreamer.Message;
import ossbuild.media.gstreamer.IPipeline;
import ossbuild.media.gstreamer.IBin;
import ossbuild.media.gstreamer.State;
import ossbuild.media.gstreamer.Bin;
import ossbuild.media.gstreamer.Pipeline;
import ossbuild.media.gstreamer.IBus;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.io.File;
import junit.framework.JUnit4TestAdapter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ossbuild.Path;
import ossbuild.Sys;
import ossbuild.extract.Resources;
import ossbuild.extract.processors.FileProcessor;
import ossbuild.media.gstreamer.Caps;
import ossbuild.media.gstreamer.Structure;
import ossbuild.media.gstreamer.api.GStreamer;
import ossbuild.media.gstreamer.api.GTypeConverters;
import ossbuild.media.gstreamer.callbacks.IBusSyncHandler;
import ossbuild.media.gstreamer.elements.VideoTestSrcPattern;
import static org.junit.Assert.*;

/**
 *
 * @author hoyt6
 */
public class Platform {
	//<editor-fold defaultstate="collapsed" desc="Main">
	public static void main(String[] args) {
		junit.textui.TestRunner.run(new JUnit4TestAdapter(Platform.class));
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Init Test Platform">
	private static File EXAMPLE_VIDEO;

	@BeforeClass
	public static void setUpClass() throws Exception {
		Sys.setEnvironmentVariable("G_SLICE", "always-malloc");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:1,GST_REGISTRY*:1");
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_REFCOUNTING*:3");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:3");
		Sys.initialize();
		//GStreamer.initialize("unit-test");

		try {
			//Extract resource
			Resources.extractAll(ossbuild.extract.Package.newInstance("ossbuild.gst", Path.nativeResourcesDirectory, new FileProcessor(false, "example.mov"))).get();
		} catch(Throwable t) {
		}

		EXAMPLE_VIDEO = Path.combine(Path.nativeResourcesDirectory, "example.mov");
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Init Test">
	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Helper Methods">
	protected void gc() {
		gc(20);
	}

	protected void gc(int times) {
		for(int i = 0; i < times; ++i)
			System.gc();
	}
	//</editor-fold>

	//@Test
	public void testElement() throws InterruptedException {
		assertTrue(true);

		for(int i = 0; i < 20000; ++i) {
			IElement elem = Element.make("fakesink");
			//System.out.println(elem.getPointer());
			elem.dispose();
			assertTrue(elem.isDisposed());

			if ((i % 1000) == 0 && i > 0) {
				gc();
				System.out.println("Completed " + i + " iterations...");
			}
		}
	}

	//@Test
	public void testPipeline() throws InterruptedException {
		assertTrue(true);
		
		for(int i = 0; i < 20000; ++i) {
			IPipeline p = new Pipeline("unit-test-pipeline");
			p.dispose();
			assertTrue(p.isDisposed());

			if ((i % 1000) == 0 && i > 0) {
				gc();
				System.out.println("Completed " + i + " iterations...");
			}
		}
	}

	//@Test
	public void testBin() throws InterruptedException {
		assertTrue(true);

		for(int i = 0; i < 20000; ++i) {
			IBin b = new Bin("unit-test-bin");
			b.dispose();
			assertTrue(b.isDisposed());

			if ((i % 1000) == 0 && i > 0) {
				gc();
				System.out.println("Completed " + i + " iterations...");
			}
		}
	}

	//@Test
	public void testBus() throws InterruptedException {
		assertTrue(true);

		for(int i = 0; i < 20000; ++i) {
			IPipeline p = new Pipeline("unit-test-pipeline");
			IBus b = p.getBus();
			b.syncHandler(new IBusSyncHandler() {
				@Override
				public BusSyncReply handle(Bus bus, Message msg, Pointer src, Pointer data) {
					return BusSyncReply.Drop;
				}
			});
			p.changeState(State.Playing);
			p.changeState(State.Null);
			p.dispose();
			assertTrue(p.isDisposed());

			if ((i % 1000) == 0 && i > 0) {
				gc();
				System.out.println("Completed " + i + " iterations...");
			}
		}
	}

	//@Test
	public void testPipelineElementsWithExplicitDispose() throws InterruptedException {
		assertTrue(true);

		for(int i = 0; i < 20000; ++i) {
			IPipeline p = new Pipeline("unit-test-pipeline");
			IElement fakesrc = Element.make("fakesrc", "fakesrc");
			IElement fakesink = Element.make("fakesink", "fakesink");

			p.addAndLinkMany(fakesrc, fakesink);

			fakesrc.dispose();
			p.dispose();
			fakesink.dispose();

			if ((i % 1000) == 0 && i > 0) {
				gc();
				System.out.println("Completed " + i + " iterations...");
			}
		}
	}

	//@Test
	public void testPipelineElementsDisposeOnFinalize() throws InterruptedException {
		assertTrue(true);

		for(int i = 0; i < 20000; ++i) {
			IPipeline p = new Pipeline("unit-test-pipeline");
			IElement videotestsrc = Element.make("videotestsrc", "videotestsrc");
			IElement fakesink = Element.make("fakesink", "fakesink");

			p.addAndLinkMany(videotestsrc, fakesink);

			assertFalse((Boolean)videotestsrc.get("is-live"));

			p.dispose();

			if ((i % 1000) == 0 && i > 0) {
				gc();
				System.out.println("Completed " + i + " iterations...");
			}
		}
	}

	//@Test
	public void testSimpleStateChange() throws InterruptedException {
		assertTrue(true);

		for(int i = 0; i < 4; ++i) {
			IPipeline p = Pipeline.make("unit-test-pipeline");
			p.busSyncHandler(new IBusSyncHandler() {
				@Override
				public BusSyncReply handle(Bus bus, Message msg, Pointer src, Pointer data) {
					//System.out.println(msg);
					switch(msg.getMessageType()) {
						case StateChanged:
							IntByReference pOldState = new IntByReference();
							IntByReference pNewState = new IntByReference();
							IntByReference pPendingState = new IntByReference();
							GStreamer.gst_message_parse_state_changed(msg.getPointer(), pOldState, pNewState, pPendingState);
							State oldState = State.fromNative(pOldState.getValue());
							State newState = State.fromNative(pNewState.getValue());
							State pendingState = State.fromNative(pPendingState.getValue());

							//System.out.println("State change from " + oldState + " to " + newState);
							break;
					}
					return BusSyncReply.Drop;
				}
			});

			IElement videotestsrc = Element.make("videotestsrc", "myvideotestsrc");
			IElement ffmpegcolorspace = Element.make("ffmpegcolorspace", "ffmpegcolorspace");
			IElement videosink = Element.make("autovideosink", "autovideosink");
			
			assertNotNull(videotestsrc.getName());
			assertEquals("myvideotestsrc", videotestsrc.getName());

			VideoTestSrcPattern pattern;
			for(int j = 0; j < 2000; ++j) {
				videotestsrc.set("pattern", VideoTestSrcPattern.Circular.getNativeValue());
				pattern = videotestsrc.get("pattern", GTypeConverters.VIDEO_TEST_SRC_PATTERN);
			}
			assertTrue(videotestsrc.get("is-live") != null);
			

			p.addAndLinkMany(videotestsrc, ffmpegcolorspace, videosink);

			for(int j = 0; j < 50; ++j) {
				p.changeState(State.Playing);
				p.changeState(State.Null);

				if ((j % 10) == 0 && j > 0) {
					gc();
					System.out.println("Iteration " + (i + 1) + ": Completed " + j + " state transitions...");
				}
			}
			p.dispose();
			videotestsrc.dispose();
			ffmpegcolorspace.dispose();
			videosink.dispose();
			assertTrue(p.isDisposed());
			
			gc();
		}
	}

	//@Test
	public void testElementInOut() throws InterruptedException {
		IPipeline playbin = Pipeline.make("playbin2", "playbin");
		IElement myvideosink = Element.make("directdrawsink", "myvideosink");
		playbin.set("video-sink", myvideosink);
		myvideosink.dispose();
		for(int i = 0; i < 50000; ++i) {
			playbin.set("video-sink", playbin.get("video-sink"));
		}
		playbin.dispose();
	}

	@Test
	public void testStructure() throws InterruptedException {
		System.out.println("Generating lots of structure objects...");
		for(int i = 0; i < 5; ++i) {
			for(int j = 0; j < 20000; ++j) {
				Structure s = Structure.newEmpty("blah");
				assertNotNull(s);
				assertNotNull(s.getPointer());
				s.dispose();
			}
			gc();
		}

		Caps c = Caps.from("video/x-raw-rgb,framerate=10/1,bpp=32;video/x-raw-yuv,framerate=20/4");
		assertNotNull(c);
	}

	//@Test
	public void testCaps() throws InterruptedException {
		System.out.println("Generating lots of caps objects...");
		for(int i = 0; i < 5; ++i) {
			for(int j = 0; j < 20000; ++j) {
				Caps c = Caps.newAny();
				assertTrue(c.isAny());
				c.dispose();

				c = Caps.newEmpty();
				assertTrue(c.isEmpty());
				c.dispose();
			}
			gc();
		}

		Caps c = Caps.from("video/x-raw-rgb,framerate=10/1,bpp=32;video/x-raw-yuv,framerate=20/4");
		assertNotNull(c);
	}

	//@Test
	public void testPlaybin() throws InterruptedException {
		assertTrue(true);

		for(int i = 0; i < 4; ++i) {
			IPipeline playbin = Pipeline.make("playbin2", "playbin");
			IBin bin = Bin.make("uridecodebin", "uridecodebin");
			IElement videosink = playbin.get("video-sink");
			IElement myvideosink = Element.make("directdrawsink", "myvideosink");

			playbin.set("video-sink", myvideosink);

			bin.set("uri", EXAMPLE_VIDEO);
			playbin.set("uri", EXAMPLE_VIDEO);
			
			playbin.changeState(State.Playing);
			playbin.requestState(-1L);
			videosink = playbin.get("video-sink");

			assertNotNull(videosink);
			Thread.sleep(5000L);
			playbin.changeState(State.Null);

			gc();
		}
	}
}

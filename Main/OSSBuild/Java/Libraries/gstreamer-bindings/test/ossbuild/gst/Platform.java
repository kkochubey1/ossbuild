/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ossbuild.gst;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import junit.framework.JUnit4TestAdapter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ossbuild.Sys;
import ossbuild.gst.api.GStreamer;
import ossbuild.gst.callbacks.IBusSyncHandler;
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
	@BeforeClass
	public static void setUpClass() throws Exception {
		Sys.setEnvironmentVariable("G_SLICE", "always-malloc");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:1,GST_REGISTRY*:1");
		//Sys.setEnvironmentVariable("GST_DEBUG", "GST_REFCOUNTING*:3");
		//Sys.setEnvironmentVariable("GST_DEBUG", "*:3");
		Sys.initialize();
		//GStreamer.initialize("unit-test");
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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
			assertTrue(p.isDisposed());
			
			gc();
		}
	}
}

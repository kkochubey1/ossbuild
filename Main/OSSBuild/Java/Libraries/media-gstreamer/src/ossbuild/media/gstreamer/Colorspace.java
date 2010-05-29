
package ossbuild.media.gstreamer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.gstreamer.Buffer;
import org.gstreamer.Caps;
import org.gstreamer.Structure;
import ossbuild.StringUtil;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
public class Colorspace {
	//<editor-fold defaultstate="collapsed" desc="Constants">
	public static final String[] VALID_COLORSPACES = {
		  "video/x-raw-rgb"
		, "video/x-raw-yuv"
	};

	public static final String[] VALID_YUV_FORMATS = {
		  "YUY2"
	};

	public static final String[] VALID_DIRECTDRAW_COLORSPACES = {
		  "video/x-raw-rgb"
	};

	public static final String
		  CAPS_IMAGE_COLORSPACE_DEPTH = "video/x-raw-rgb,bpp=32,depth=24";//,red_mask=" + 0x00FF0000 + ",green_mask=" + 0x0000FF00 + ",blue_mask=" + 0x000000FF
	;
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Classes">
	public static class Frame {
		//<editor-fold defaultstate="collapsed" desc="Variables">
		private IntBuffer buffer;
		private int width;
		private int height;
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Initialization">
		private Frame(int width, int height, IntBuffer buffer) {
			this.width = width;
			this.height = height;
			this.buffer = buffer;
		}
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc="Getters">
		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public IntBuffer getBuffer() {
			return buffer;
		}
		//</editor-fold>
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc="Public Static Methods">
	public static IntBuffer convertToRGB(final ByteBuffer bb, final int width, final int height, final String colorspace, final String fourcc) {
		if (!isKnownColorspace(colorspace))
			return null;

		if (isRGBColorspace(colorspace)) {
			return bb.asIntBuffer();
		} else if(isYUVColorspace(colorspace)) {
			if ("YUY2".equalsIgnoreCase(fourcc) || "YUYV".equalsIgnoreCase(fourcc) || "YUNV".equalsIgnoreCase(fourcc) || "V422".equalsIgnoreCase(fourcc))
				return yuyv2rgb(bb, width, height);
			else
				return null;
		} else {
			return null;
		}
	}

	public static IntBuffer yuyv2rgb(final ByteBuffer bb, final int width, final int height) {
		//Courtesy jcam
		//    http://www.stenza.org/packages/jcam.tgz

		final ByteBuffer destbb = ByteBuffer.allocate(4 * width * height);
		destbb.order(ByteOrder.BIG_ENDIAN);
		bb.order(ByteOrder.BIG_ENDIAN);

		int y1, u, y2, v;
		int cb, cr, cg;
		int r, g, b;

		int halfWidth = width / 2;
		int sstride = width*2;
		int dstride = width*4;

		int isrcindex, idestindex;

		for (int i = 0; i < height; ++i) {
			for (int j = 0; j < halfWidth; ++j) {
				isrcindex = i * sstride + 4*j;
				idestindex = i * dstride + 8*j;

				y1 = bb.get(isrcindex + 0)&0xff;
				u  = bb.get(isrcindex + 1)&0xff;
				y2 = bb.get(isrcindex + 2)&0xff;
				v  = bb.get(isrcindex + 3)&0xff;

				cb = ((u-128) * 454) >> 8;
				cr = ((v-128) * 359) >> 8;
				cg = ((v-128) * 183 + (u-128) * 88) >> 8;

				r = y1 + cr;
				b = y1 + cb;
				g = y1 - cg;

				destbb.put(idestindex + 0, (byte)0);
				destbb.put(idestindex + 1, (byte)Math.max(0, Math.min(255, r)));
				destbb.put(idestindex + 2, (byte)Math.max(0, Math.min(255, g)));
				destbb.put(idestindex + 3, (byte)Math.max(0, Math.min(255, b)));

				r = y2 + cr;
				b = y2 + cb;
				g = y2 - cg;

				destbb.put(idestindex + 4, (byte)0);
				destbb.put(idestindex + 5, (byte)Math.max(0, Math.min(255, r)));
				destbb.put(idestindex + 6, (byte)Math.max(0, Math.min(255, g)));
				destbb.put(idestindex + 7, (byte)Math.max(0, Math.min(255, b)));
			}
		}

		//destbb.flip();
		return destbb.asIntBuffer();
	}

	public static byte clamp(int min, int max, int value) {
		if (value < min)
			return (byte)min;
		if (value > max)
			return (byte)max;
		return (byte)(value);
	}

	public static boolean isRGBColorspace(final String colorspace) {
		return VALID_COLORSPACES[0].equalsIgnoreCase(colorspace);
	}

	public static boolean isYUVColorspace(final String colorspace) {
		return VALID_COLORSPACES[1].equalsIgnoreCase(colorspace);
	}

	public static boolean isKnownYUVFormat(final String yuvFormat) {
		if (StringUtil.isNullOrEmpty(yuvFormat))
			return false;
		for(String cs : VALID_YUV_FORMATS)
			if (cs.equalsIgnoreCase(yuvFormat))
				return true;
		return false;
	}

	public static boolean isKnownColorspace(final String colorspace) {
		if (StringUtil.isNullOrEmpty(colorspace))
			return false;
		for(String cs : VALID_COLORSPACES)
			if (cs.equalsIgnoreCase(colorspace))
				return true;
		return false;
	}

	public static String createKnownColorspaceFilter(final boolean includeFramerate, final float fps) {
		return createKnownColorspaceFilter(includeFramerate, fps, 0, 0);
	}
	public static String createKnownColorspaceFilter(final boolean includeFramerate, final float fps, final int width, final int height) {
		final String framerate = (includeFramerate ? null : ", framerate=" + (int)fps + "/1");
		final String dimensions = (width <= 0 || height <= 0 ? null : ",width=" + width + ",height=" + height);
		final StringBuilder sb = new StringBuilder(256);

		sb.append(';');
		sb.append(CAPS_IMAGE_COLORSPACE_DEPTH);
		if (framerate != null)
			sb.append(framerate);
		if (dimensions != null)
			sb.append(dimensions);
		
		for(int i = 0; i < VALID_COLORSPACES.length; ++i) {
			sb.append(';');
			sb.append(VALID_COLORSPACES[i]);
			if (framerate != null)
				sb.append(framerate);
			if (dimensions != null)
				sb.append(dimensions);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}

	public static Frame createRGBFrame(final Buffer buffer) {
		if (buffer == null)
			return null;

		try {
			final Caps caps = buffer.getCaps();
			if (caps == null || caps.size() <= 0)
				return null;

			final Structure struct = caps.getStructure(0);
			if (struct == null || !struct.hasIntField("width") || !struct.hasIntField("height"))
				return null;
			
			final int width = struct.getInteger("width");
			final int height = struct.getInteger("height");
			if (width < 1 || height < 1)
				return null;

			//Convert to RGB using the provided direct buffer
			return new Frame(width, height, convertToRGB(buffer.getByteBuffer(), width, height, struct.getName(), struct.hasField("format") ? struct.getFourccString("format") : null));
		} catch(Throwable t) {
			return null;
		} finally {
		}
	}

	public static BufferedImage createBufferedImage(final Buffer buffer) {
		return createBufferedImage(createRGBFrame(buffer));
	}

	public static BufferedImage createBufferedImage(final Frame frame) {
		if (frame == null)
			return null;

		try {

			final BufferedImage img = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB);
			img.setAccelerationPriority(0.001f);
			frame.buffer.get(((DataBufferInt)img.getRaster().getDataBuffer()).getData(), 0, frame.buffer.remaining());

			return img;
		} catch(Throwable t) {
			return null;
		} finally {
		}
	}
	//</editor-fold>
}

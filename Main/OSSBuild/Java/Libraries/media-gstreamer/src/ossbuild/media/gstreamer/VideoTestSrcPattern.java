
package ossbuild.media.gstreamer;

import org.gstreamer.lowlevel.EnumMapper;
import org.gstreamer.lowlevel.IntegerEnum;

/**
 * The different types of URI direction.
 */
public enum VideoTestSrcPattern implements IntegerEnum {
    /**  */
    SMPTE(0),
	/**  */
    SNOW(1),
	/**  */
    BLACK(2),
	/**  */
    WHITE(3),
	/**  */
    RED(4),
	/**  */
    GREEN(5),
	/**  */
    BLUE(6),
	/**  */
    CHECKERS1(7),
	/**  */
    CHECKERS2(8),
	/**  */
    CHECKERS4(9),
	/**  */
    CHECKERS8(10),
	/**  */
    CIRCULAR(11),
	/**  */
    BLINK(12),
	/**  */
    SMPTE75(13),
	/**  */
    ZONE_PLATE(14),
	/**  */
    GAMUT(15);

    private final int value;

    VideoTestSrcPattern(int value) {
        this.value = value;
    }

    /**
     * Gets the integer value of the enum.
     * @return The integer value for this enum.
     */
	@Override
	public int intValue() {
        return value;
    }

    /**
     * Returns the enum constant of this type with the specified integer value.
     * @param state integer value.
     * @return Enum constant.
     * @throws java.lang.IllegalArgumentException if the enum type has no constant with the specified value.
     */
    public static final VideoTestSrcPattern valueOf(int pattern) {
        return EnumMapper.getInstance().valueOf(pattern, VideoTestSrcPattern.class);
    }
}

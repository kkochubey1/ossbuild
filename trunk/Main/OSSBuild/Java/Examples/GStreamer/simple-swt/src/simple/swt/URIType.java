
package simple.swt;

import org.gstreamer.lowlevel.EnumMapper;
import org.gstreamer.lowlevel.IntegerEnum;

 /**
  * The different types of URI direction.
  */
 public enum URIType implements IntegerEnum {
     /** The URI direction is unknown */
     UNKNOWN(0),
     /** The URI is a consumer. */
     SINK(1),
     /** The URI is a producer. */
     SRC(2);

     URIType(int value) {
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
     private final int value;

     /**
      * Returns the enum constant of this type with the specified integer value.
      * @param state integer value.
      * @return Enum constant.
      * @throws java.lang.IllegalArgumentException if the enum type has no constant with the specified value.
      */
     public static final URIType valueOf(int uriType) {
         return EnumMapper.getInstance().valueOf(uriType, URIType.class);
     }
 }

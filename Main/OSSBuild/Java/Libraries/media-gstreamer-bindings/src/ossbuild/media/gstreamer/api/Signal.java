
package ossbuild.media.gstreamer.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author David Hoyt <dhoyt@hoytsoft.org>
 */
@Inherited()
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Signal {
	String name();
	String detail() default "";
}

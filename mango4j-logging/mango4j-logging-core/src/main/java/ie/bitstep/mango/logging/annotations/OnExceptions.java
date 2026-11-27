package ie.bitstep.mango.logging.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Container for repeated {@link OnException}. Not used directly.
 */
@Retention(RUNTIME)
@Target({METHOD})
public @interface OnExceptions {
	OnException[] value();
}

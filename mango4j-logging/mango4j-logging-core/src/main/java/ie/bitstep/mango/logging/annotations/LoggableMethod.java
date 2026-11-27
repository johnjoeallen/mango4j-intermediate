package ie.bitstep.mango.logging.annotations;

import ie.bitstep.mango.logging.enums.TrackingType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({METHOD})
public @interface LoggableMethod {

	boolean isolated() default false;

	TrackingType tracking() default TrackingType.TRACK;

	boolean logReturn() default false;

	/**
	 * What to log about an exception thrown by this method, unless a matching {@link OnException} says otherwise.
	 * An empty array logs nothing about the exception.
	 */
	ExceptionDetail[] logException() default {ExceptionDetail.MESSAGE, ExceptionDetail.CAUSE, ExceptionDetail.STACK_TRACE};

	boolean forceErrorOnException() default false;

}

package ie.bitstep.mango.spring.logging.annotations;

import ie.bitstep.mango.spring.logging.enums.TrackingType;

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

	boolean logException() default true;

	boolean forceErrorOnException() default false;

}

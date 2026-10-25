package ie.bitstep.mango.spring.logging.annotations;

import ie.bitstep.mango.utils.masking.Masker;
import ie.bitstep.mango.utils.masking.NoMasker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface LoggableField {

	String name() default "";

	/**
	 * @return A masking class
	 * @deprecated use the new @Mask annotation
	 */
	@Deprecated(since = "31/10/2024", forRemoval = false)
	Class<? extends Masker> mask() default NoMasker.class;

	int truncateLen() default 0;

	LoggableView view() default LoggableView.ALL;
}

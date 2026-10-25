package com.mastercard.commons.mango.logging.annotations;

import com.mastercard.commons.mango.utils.converters.NoConverter;
import com.mastercard.commons.mango.utils.masking.Masker;
import com.mastercard.commons.mango.utils.masking.NoMasker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.UnaryOperator;

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

	/**
	 * @return A converter class
	 * @deprecated use the new @Converter annotation
	 */
	@Deprecated(since = "31/10/2024", forRemoval = false)
	Class<? extends UnaryOperator<Object>> convert() default NoConverter.class;

	int truncateLen() default 0;

	LoggableView view() default LoggableView.ALL;
}

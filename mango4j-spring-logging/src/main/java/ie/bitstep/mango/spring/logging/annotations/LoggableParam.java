package ie.bitstep.mango.spring.logging.annotations;

import ie.bitstep.mango.utils.converters.NoConverter;
import ie.bitstep.mango.utils.masking.Masker;
import ie.bitstep.mango.utils.masking.NoMasker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.UnaryOperator;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface LoggableParam {

	String name();

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

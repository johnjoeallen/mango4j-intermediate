package ie.bitstep.mango.spring.crypto.caas.delegate.config;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation used to tell PIT mutation tests to ignore a class or method
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DoNotMutate {
	@AliasFor("reason")
	String value() default "";

	String reason() default "";
}
package ie.bitstep.mango.logging.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares how a method logs the given exception types. Presence of a matching {@code @OnException}
 * also opts the method in to recording an exception that was simply propagated from the last
 * method it called (which is otherwise skipped to avoid duplicate logging).
 * <p>
 * Rules are evaluated in declaration order; the first rule with a matching type wins.
 */
@Retention(RUNTIME)
@Target({METHOD})
@Repeatable(OnExceptions.class)
public @interface OnException {

	Class<? extends Throwable>[] type() default {Throwable.class};

	ExceptionDetail[] detail();
}

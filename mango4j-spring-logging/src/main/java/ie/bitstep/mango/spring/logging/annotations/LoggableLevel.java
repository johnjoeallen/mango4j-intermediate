package ie.bitstep.mango.spring.logging.annotations;

import org.slf4j.event.Level;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Ashley Waldron (e062130) on 11/8/2017.
 */
@Retention(RUNTIME)
@Target({TYPE})
public @interface LoggableLevel {
	Level level();
}

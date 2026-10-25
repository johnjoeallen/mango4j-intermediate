package ie.bitstep.mango.spring.observe.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Disconnect an observation from it's parent before calling onExit</p>
 *
 * <p>When the parent's onExit is called the associated child observation
 * will not be in the list of children</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Segregate {
}

package ie.bitstep.mango.spring.observe.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the tags to create for an observation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ObservationTags {
	/**
	 * The list of base tags
	 *
	 * @return array of strings listing the tags to include in the observation
	 */
	String[] base();

	/**
	 * The list of tags to create permutations of (can be omitted)
	 *
	 * @return array of strings listing the tags to create permutation on in the observation
	 */
	String[] permutations() default {};
}

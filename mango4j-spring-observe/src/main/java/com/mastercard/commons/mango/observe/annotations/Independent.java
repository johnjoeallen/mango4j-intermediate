package com.mastercard.commons.mango.observe.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alternate for @Segregate
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Segregate
public @interface Independent {
}

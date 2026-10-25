package com.mastercard.commons.mango.insights.annotations;

import com.mastercard.commons.mango.utils.masking.Masker;
import com.mastercard.commons.mango.utils.masking.NoMasker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface InsightVariable {
	String name();

	Class<? extends Masker> mask() default NoMasker.class;

	int truncateLen() default 0;
}

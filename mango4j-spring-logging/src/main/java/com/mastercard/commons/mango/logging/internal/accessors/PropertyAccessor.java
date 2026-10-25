package com.mastercard.commons.mango.logging.internal.accessors;

import java.lang.annotation.Annotation;

public interface PropertyAccessor {
	String getName();

	<T extends Annotation> T getAnnotation(Class<T> var1);

	boolean isIncludeInAll();

	Object getValue(Object target);
}
